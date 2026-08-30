package com.wego.carpark.app.data.dto;

import java.time.LocalDateTime;
import java.util.List;

public final class FetchDtos {

    private FetchDtos() {
    }

    public record NearbyRequest(double latitude, double longitude, double radiusMeters, String lotType, int limit) {
        public NearbyRequest {
            if (radiusMeters <= 0) {
                radiusMeters = 2000.0;
            }
            if (limit <= 0) {
                limit = 20;
            }
        }
    }

    public record CarparkResult(
            String carparkNumber,
            String address,
            String lotType,
            int totalLots,
            int lotsAvailable,
            double distanceMeters,
            double latitude,
            double longitude,
            LocalDateTime updateDatetime,
            LocalDateTime lastSyncTime) {
    }

    public record NearbyResponse(List<CarparkResult> results, boolean isDataStale, String warning) {
    }
}
