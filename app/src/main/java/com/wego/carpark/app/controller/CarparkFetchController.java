package com.wego.carpark.app.controller;

import com.wego.carpark.app.data.dto.FetchDtos.NearbyRequest;
import com.wego.carpark.app.data.dto.FetchDtos.NearbyResponse;
import com.wego.carpark.app.service.CarparkFetchService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/carparks")
@RequiredArgsConstructor
@Validated
public class CarparkFetchController {

    private final CarparkFetchService fetchService;

    @GetMapping("/nearby")
    public ResponseEntity<NearbyResponse> getNearbyCarparks(
            @RequestParam @Min(-90) @Max(90) double latitude,
            @RequestParam @Min(-180) @Max(180) double longitude,
            @RequestParam(required = false, defaultValue = "2000") double radiusMeters,
            @RequestParam(required = false) String lotType,
            @RequestParam(required = false, defaultValue = "20") int limit) {
        return ResponseEntity.ok(fetchService.getNearbyAvailable(
                new NearbyRequest(latitude, longitude, radiusMeters, lotType, limit)));
    }
}
