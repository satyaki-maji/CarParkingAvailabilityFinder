package com.wego.carpark.app.adapter;

import com.wego.carpark.app.data.dao.CarparkAvailabilityRow;
import com.wego.carpark.app.data.dto.FetchDtos.CarparkResult;
import com.wego.carpark.app.data.dto.FetchDtos.NearbyResponse;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class NearbyResponseAdapter {

    private static final int STALE_THRESHOLD_MINUTES = 5;
    private static final String STALE_WARNING = "Live API ingestion is currently delayed. Availability data may be stale.";

    public NearbyResponse adapt(List<CarparkAvailabilityRow> rows) {
        List<CarparkResult> results = rows.stream()
                .map(row -> new CarparkResult(
                        row.carparkNumber(), row.address(), row.lotType(), row.totalLots(), row.lotsAvailable(),
                        row.distanceMeters(), row.latitude(), row.longitude(), row.updateDatetime(), row.lastSyncTime()))
                .toList();
        LocalDateTime latestSync = results.stream()
                .map(CarparkResult::lastSyncTime)
                .filter(java.util.Objects::nonNull)
                .max(LocalDateTime::compareTo)
                .orElse(null);
        boolean isStale = latestSync != null
                && ChronoUnit.MINUTES.between(latestSync, LocalDateTime.now()) > STALE_THRESHOLD_MINUTES;
        return new NearbyResponse(results, isStale, isStale ? STALE_WARNING : null);
    }
}
