package com.wego.carpark.app.data.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public final class GovSgDtos {

    private GovSgDtos() {
    }

    public record LiveResponse(List<LiveItem> items) {
    }

    public record LiveItem(
            @JsonProperty("timestamp") String timestamp,
            @JsonProperty("carpark_data") List<CarparkData> carparkData) {
    }

    public record CarparkData(
            @JsonProperty("carpark_number") String carparkNumber,
            @JsonProperty("update_datetime") String updateDatetime,
            @JsonProperty("carpark_info") List<CarparkInfo> carparkInfo) {
    }

    public record CarparkInfo(
            @JsonProperty("lot_type") String lotType,
            @JsonProperty("total_lots") int totalLots,
            @JsonProperty("lots_available") int lotsAvailable) {
    }

    public record StaticResponse(Result result) {
    }

    public record Result(List<StaticCarpark> records) {
    }

    public record StaticCarpark(
            @JsonProperty("car_park_no") String carParkNo,
            @JsonProperty("address") String address,
            @JsonProperty("x_coord") double xCoord,
            @JsonProperty("y_coord") double yCoord,
            @JsonProperty("car_park_type") String carParkType,
            @JsonProperty("type_of_parking_system") String typeOfParkingSystem,
            @JsonProperty("short_term_parking") String shortTermParking,
            @JsonProperty("free_parking") String freeParking,
            @JsonProperty("night_parking") String nightParking,
            @JsonProperty("car_park_decks") int carParkDecks,
            @JsonProperty("gantry_height") double gantryHeight,
            @JsonProperty("car_park_basement") String carParkBasement) {
    }
}
