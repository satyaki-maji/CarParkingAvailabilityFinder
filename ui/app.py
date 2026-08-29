import os
from copy import deepcopy
from datetime import datetime, timezone as utc_timezone
from zoneinfo import ZoneInfo

import folium
import pydeck as pdk
import requests
import streamlit as st
from streamlit_folium import st_folium


BACKEND_URL = os.getenv("BACKEND_URL", "http://localhost:8080")
DEFAULT_LATITUDE = 1.3521
DEFAULT_LONGITUDE = 103.8198
TIMEZONES = [
    "Asia/Singapore",
    "Asia/Kolkata",
    "UTC",
    "Asia/Tokyo",
    "Europe/London",
    "America/New_York",
]


def localize_timestamp(timestamp, timezone_name):
    if not timestamp:
        return None
    parsed = datetime.fromisoformat(timestamp.replace("Z", "+00:00"))
    if parsed.tzinfo is None:
        parsed = parsed.replace(tzinfo=utc_timezone.utc)
    return parsed.astimezone(ZoneInfo(timezone_name)).strftime("%Y-%m-%d %H:%M:%S %Z")


def fetch_carparks(latitude, longitude, radius_meters, lot_type, limit):
    params = {
        "latitude": latitude,
        "longitude": longitude,
        "radiusMeters": radius_meters,
        "limit": limit,
    }
    if lot_type != "All":
        params["lotType"] = lot_type
    response = requests.get(f"{BACKEND_URL}/api/v1/carparks/nearby", params=params, timeout=10)
    response.raise_for_status()
    return response.json()


st.set_page_config(page_title="Singapore Parking Finder", page_icon="🅿️", layout="wide")
header_title, header_clock = st.columns([3, 1])
header_title.title("🅿️ Singapore Parking Finder")
with header_clock:
    timezone_name = st.selectbox("Timezone", TIMEZONES, index=0, key="timezone_name")
    current_time = datetime.now(ZoneInfo(timezone_name)).strftime("%Y-%m-%d %H:%M:%S %Z")
    st.markdown(f"<div style='text-align:right;font-size:0.9rem'>🕒 {current_time}<br><small>All times shown in {timezone_name}</small></div>", unsafe_allow_html=True)
st.caption("Live HDB car-park availability, sorted by distance from your selected location.")

if "search_latitude" not in st.session_state:
    st.session_state.search_latitude = DEFAULT_LATITUDE
if "search_longitude" not in st.session_state:
    st.session_state.search_longitude = DEFAULT_LONGITUDE

st.subheader("Choose a location")
st.caption("Click anywhere on the map to set the search latitude and longitude.")
location_picker = folium.Map(
    location=[st.session_state.search_latitude, st.session_state.search_longitude],
    zoom_start=13,
    control_scale=True,
)
folium.Marker(
    [st.session_state.search_latitude, st.session_state.search_longitude],
    tooltip="Current search location",
    icon=folium.Icon(color="blue", icon="crosshairs", prefix="fa"),
).add_to(location_picker)
map_click = st_folium(location_picker, height=360, width="stretch", key="location-picker")

if map_click and map_click.get("last_clicked"):
    clicked = map_click["last_clicked"]
    clicked_latitude = round(clicked["lat"], 6)
    clicked_longitude = round(clicked["lng"], 6)
    if (clicked_latitude, clicked_longitude) != (
        st.session_state.search_latitude,
        st.session_state.search_longitude,
    ):
        st.session_state.search_latitude = clicked_latitude
        st.session_state.search_longitude = clicked_longitude
        st.rerun()

with st.sidebar:
    st.header("Search area")
    latitude = st.number_input("Latitude", min_value=-90.0, max_value=90.0, format="%.6f", key="search_latitude")
    longitude = st.number_input("Longitude", min_value=-180.0, max_value=180.0, format="%.6f", key="search_longitude")
    radius_meters = st.slider("Search radius (metres)", min_value=250, max_value=5000, value=2000, step=250)
    lot_type = st.selectbox("Vehicle type", ["All", "C", "H", "Y", "M"])
    limit = st.slider("Maximum results", min_value=5, max_value=50, value=20, step=5)
    search = st.button("Find parking", type="primary", width="stretch")

if "response" not in st.session_state:
    search = True

if search:
    try:
        with st.spinner("Finding available car parks..."):
            st.session_state.response = fetch_carparks(latitude, longitude, radius_meters, lot_type, limit)
            st.session_state.search_location = {"latitude": latitude, "longitude": longitude}
    except requests.RequestException as error:
        st.error(f"Could not reach the parking API at {BACKEND_URL}: {error}")
        st.stop()

response = st.session_state.response
results = response.get("results", [])
localized_response = deepcopy(response)
for item in localized_response.get("results", []):
    item["lastSyncTime"] = localize_timestamp(item.get("lastSyncTime"), timezone_name)

if response.get("isDataStale"):
    st.warning(response.get("warning") or "Availability data may be stale.")

available_lots = sum(item["lotsAvailable"] for item in results)
nearest_distance = min((item["distanceMeters"] for item in results), default=0)
first, second, third = st.columns(3)
first.metric("Available car parks", len(results))
second.metric("Available lots shown", available_lots)
third.metric("Nearest result", f"{nearest_distance:.0f} m" if results else "—")

if not results:
    st.info("No available car parks were found in this search area. Try a larger radius.")
    st.stop()

visual_tab, json_tab = st.tabs(["Map & results", "API JSON"])

with visual_tab:
    map_rows = [
        {
            "latitude": item["latitude"],
            "longitude": item["longitude"],
            "address": item["address"],
            "lots": item["lotsAvailable"],
            "distance": item["distanceMeters"],
        }
        for item in results
    ]
    search_location = st.session_state.search_location
    layers = [
        pdk.Layer(
            "ScatterplotLayer",
            data=map_rows,
            get_position="[longitude, latitude]",
            get_radius=70,
            get_fill_color="[26, 166, 87, 210]",
            pickable=True,
        ),
        pdk.Layer(
            "ScatterplotLayer",
            data=[search_location],
            get_position="[longitude, latitude]",
            get_radius=100,
            get_fill_color="[31, 119, 180, 230]",
        ),
    ]
    st.pydeck_chart(pdk.Deck(
        layers=layers,
        initial_view_state=pdk.ViewState(latitude=search_location["latitude"], longitude=search_location["longitude"], zoom=13),
        tooltip={"text": "{address}\n{lots} lots available\n{distance} m away"},
    ))

    st.subheader("Nearby availability")
    st.dataframe(
        [
            {
                "Car park": item["carparkNumber"],
                "Address": item["address"],
                "Lot type": item["lotType"],
                "Available": item["lotsAvailable"],
                "Total": item["totalLots"],
                "Distance (m)": round(item["distanceMeters"]),
                "Last synced": localize_timestamp(item.get("lastSyncTime"), timezone_name),
            }
            for item in results
        ],
        width="stretch",
        hide_index=True,
    )

with json_tab:
    st.caption(f"Backend response with last-sync timestamps converted to {timezone_name}.")
    st.json(localized_response)
