package com.wego.carpark.app.repository;

import com.wego.carpark.app.data.dto.GovSgDtos.CarparkData;
import com.wego.carpark.app.data.dto.GovSgDtos.CarparkInfo;
import com.wego.carpark.app.data.dto.GovSgDtos.StaticCarpark;
import com.wego.carpark.app.data.dao.CarparkAvailabilityRow;
import java.sql.PreparedStatement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class CarparkRepository {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");
    private final JdbcTemplate jdbcTemplate;

    public CarparkRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<CarparkAvailabilityRow> findNearbyAvailableCarparks(
            double latitude, double longitude, double radiusMeters, String lotType, int limit) {
        StringBuilder sql = new StringBuilder("""
                SELECT c.carpark_number, c.address, a.lot_type, a.total_lots, a.lots_available,
                       a.update_datetime, a.last_sync_time,
                       ST_Y(c.location::geometry) AS latitude,
                       ST_X(c.location::geometry) AS longitude,
                       ST_Distance(c.location, ST_SetSRID(ST_MakePoint(?, ?), 4326)::geography) AS distance_meters
                FROM carpark c
                JOIN carpark_availability a ON c.carpark_number = a.carpark_number
                WHERE a.lots_available > 0
                  AND ST_DWithin(c.location, ST_SetSRID(ST_MakePoint(?, ?), 4326)::geography, ?)
                """);
        List<Object> args = new ArrayList<>(List.of(longitude, latitude, longitude, latitude, radiusMeters));

        if (lotType != null && !lotType.isBlank()) {
            sql.append(" AND a.lot_type = ? ");
            args.add(lotType);
        }

        sql.append("""
                ORDER BY c.location <-> ST_SetSRID(ST_MakePoint(?, ?), 4326)::geography
                LIMIT ?
                """);
        args.add(longitude);
        args.add(latitude);
        args.add(limit);

        return jdbcTemplate.query(sql.toString(), (rs, rowNum) -> new CarparkAvailabilityRow(
                rs.getString("carpark_number"), rs.getString("address"), rs.getString("lot_type"),
                rs.getInt("total_lots"), rs.getInt("lots_available"), rs.getDouble("distance_meters"),
                rs.getDouble("latitude"), rs.getDouble("longitude"),
                rs.getTimestamp("update_datetime") == null ? null : rs.getTimestamp("update_datetime").toLocalDateTime(),
                rs.getTimestamp("last_sync_time") == null ? null : rs.getTimestamp("last_sync_time").toLocalDateTime()),
                args.toArray());
    }

    @Transactional
    public void batchUpsertLiveAvailability(List<CarparkData> liveDataList) {
        String sql = """
                INSERT INTO carpark_availability
                (carpark_number, lot_type, total_lots, lots_available, update_datetime, last_sync_time)
                VALUES (?, ?, ?, ?, ?, ?)
                ON CONFLICT (carpark_number, lot_type) DO UPDATE SET
                    total_lots = EXCLUDED.total_lots,
                    lots_available = EXCLUDED.lots_available,
                    update_datetime = EXCLUDED.update_datetime,
                    last_sync_time = EXCLUDED.last_sync_time
                """;
        List<Object[]> batchArgs = new ArrayList<>();
        Timestamp syncTime = Timestamp.valueOf(LocalDateTime.now());

        for (CarparkData data : liveDataList) {
            Timestamp updateTime = data.updateDatetime() == null
                    ? syncTime
                    : Timestamp.valueOf(LocalDateTime.parse(data.updateDatetime(), FORMATTER));
            for (CarparkInfo info : data.carparkInfo()) {
                batchArgs.add(new Object[]{data.carparkNumber(), info.lotType(), info.totalLots(),
                        info.lotsAvailable(), updateTime, syncTime});
            }
        }
        jdbcTemplate.batchUpdate(sql, batchArgs);
    }

    @Transactional
    public void batchUpsertStaticData(List<StaticCarpark> staticDataList) {
        String sql = """
                INSERT INTO carpark
                (carpark_number, address, x_coord, y_coord, car_park_type, type_of_parking_system,
                short_term_parking, free_parking, night_parking, car_park_decks, gantry_height, car_park_basement, location)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?,
                    ST_Transform(ST_SetSRID(ST_MakePoint(?, ?), 3414), 4326)::geography)
                ON CONFLICT (carpark_number) DO UPDATE SET
                    address = EXCLUDED.address, car_park_type = EXCLUDED.car_park_type,
                    type_of_parking_system = EXCLUDED.type_of_parking_system, free_parking = EXCLUDED.free_parking,
                    night_parking = EXCLUDED.night_parking, location = EXCLUDED.location
                """;
        jdbcTemplate.batchUpdate(sql, staticDataList, staticDataList.size(), (PreparedStatement ps, StaticCarpark cp) -> {
            ps.setString(1, cp.carParkNo()); ps.setString(2, cp.address()); ps.setDouble(3, cp.xCoord());
            ps.setDouble(4, cp.yCoord()); ps.setString(5, cp.carParkType()); ps.setString(6, cp.typeOfParkingSystem());
            ps.setString(7, cp.shortTermParking()); ps.setString(8, cp.freeParking()); ps.setString(9, cp.nightParking());
            ps.setInt(10, cp.carParkDecks()); ps.setDouble(11, cp.gantryHeight()); ps.setString(12, cp.carParkBasement());
            ps.setDouble(13, cp.xCoord()); ps.setDouble(14, cp.yCoord());
        });
    }
}
