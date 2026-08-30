package com.wego.carpark.app.adapter;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.wego.carpark.app.data.dao.CarparkAvailabilityRow;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;

class NearbyResponseAdapterTest {

    private final NearbyResponseAdapter adapter = new NearbyResponseAdapter();

    @Test
    void marksResultsStaleWhenLatestSyncIsOlderThanFiveMinutes() {
        LocalDateTime oldSync = LocalDateTime.now().minusMinutes(6);
        var response = adapter.adapt(List.of(row(oldSync)));

        assertTrue(response.isDataStale());
        assertTrue(response.warning().contains("delayed"));
    }

    @Test
    void treatsRecentAvailabilityAsFresh() {
        var response = adapter.adapt(List.of(row(LocalDateTime.now().minusMinutes(1))));

        assertFalse(response.isDataStale());
        assertNull(response.warning());
    }

    @Test
    void doesNotDeclareEmptyResultsStaleWithoutSyncData() {
        var response = adapter.adapt(List.of());

        assertFalse(response.isDataStale());
        assertNull(response.warning());
    }

    private static CarparkAvailabilityRow row(LocalDateTime syncTime) {
        return new CarparkAvailabilityRow("A1", "Address", "C", 100, 20, 25,
                1.35, 103.82, syncTime, syncTime);
    }
}
