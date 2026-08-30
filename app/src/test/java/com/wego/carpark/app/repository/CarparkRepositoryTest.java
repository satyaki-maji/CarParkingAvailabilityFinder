package com.wego.carpark.app.repository;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.wego.carpark.app.data.dto.GovSgDtos.CarparkData;
import com.wego.carpark.app.data.dto.GovSgDtos.CarparkInfo;
import com.wego.carpark.app.data.dto.GovSgDtos.StaticCarpark;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

@ExtendWith(MockitoExtension.class)
class CarparkRepositoryTest {

    @Mock
    JdbcTemplate jdbcTemplate;

    @Test
    void staticUpsertTransformsSvy21CoordinatesToWgs84() {
        AtomicReference<String> sql = new AtomicReference<>();
        doAnswer(invocation -> {
            sql.set(invocation.getArgument(0));
            return new int[][]{{1}};
        }).when(jdbcTemplate).batchUpdate(anyString(), any(List.class), anyInt(), any());

        new CarparkRepository(jdbcTemplate).batchUpsertStaticData(List.of(staticCarpark()));

        assertTrue(sql.get().contains("ST_Transform(ST_SetSRID(ST_MakePoint(?, ?), 3414), 4326)"));
    }

    @Test
    void nearbyQueryUsesPostgisRadiusAndNearestNeighbourOrdering() {
        when(jdbcTemplate.query(anyString(), any(RowMapper.class), any(Object[].class))).thenReturn(List.of());

        new CarparkRepository(jdbcTemplate).findNearbyAvailableCarparks(1.35, 103.82, 2000, "C", 10);

        ArgumentCaptor<String> sql = ArgumentCaptor.forClass(String.class);
        verify(jdbcTemplate).query(sql.capture(), any(RowMapper.class), any(Object[].class));
        String query = sql.getValue();
        assertTrue(query.contains("ST_DWithin"));
        assertTrue(query.contains("ORDER BY c.location <->"));
        assertTrue(query.contains("a.lot_type = ?"));
        assertTrue(query.contains("LIMIT ?"));
    }

    @Test
    void liveUpsertIncludesFreshnessTimestampAndConflictUpdate() {
        AtomicReference<String> sql = new AtomicReference<>();
        doAnswer(invocation -> {
            sql.set(invocation.getArgument(0));
            return new int[]{1};
        }).when(jdbcTemplate).batchUpdate(anyString(), any(List.class));

        CarparkData data = new CarparkData("A1", "2026-08-30T00:00:00", List.of(new CarparkInfo("C", 100, 25)));
        new CarparkRepository(jdbcTemplate).batchUpsertLiveAvailability(List.of(data));

        assertTrue(sql.get().contains("last_sync_time"));
        assertTrue(sql.get().contains("ON CONFLICT (carpark_number, lot_type) DO UPDATE"));
    }

    private static StaticCarpark staticCarpark() {
        return new StaticCarpark("A1", "Address", 28000.0, 38000.0, "MULTI", "ELECTRONIC",
                "WHOLE DAY", "NO", "YES", 2, 2.0, "N");
    }
}
