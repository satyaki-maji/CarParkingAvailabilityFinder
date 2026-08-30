package com.wego.carpark.app.client;

import com.wego.carpark.app.data.dto.GovSgDtos.LiveResponse;
import com.wego.carpark.app.data.dto.GovSgDtos.StaticResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class GovSgClient {

    private static final String LIVE_API_URL = "https://api.data.gov.sg/v1/transport/carpark-availability";
    private static final String STATIC_API_URL = "https://data.gov.sg/api/action/datastore_search?resource_id=d_23f946fa557947f93a8043bbef41dd09&limit=3000";
    private final RestClient restClient;

    public GovSgClient() {
        this.restClient = RestClient.create();
    }

    public LiveResponse fetchLiveAvailability() {
        return restClient.get().uri(LIVE_API_URL).retrieve().body(LiveResponse.class);
    }

    public StaticResponse fetchStaticCarparks() {
        return restClient.get().uri(STATIC_API_URL).retrieve().body(StaticResponse.class);
    }
}
