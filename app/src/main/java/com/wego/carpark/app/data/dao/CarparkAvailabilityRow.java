package com.wego.carpark.app.data.dao;

import java.time.LocalDateTime;

/**
 * Projection returned by the carpark repository's nearby-availability query.
 */
public record CarparkAvailabilityRow(
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
