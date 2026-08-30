package com.wego.carpark.app.service;

import com.wego.carpark.app.adapter.NearbyResponseAdapter;
import com.wego.carpark.app.data.dto.FetchDtos.NearbyRequest;
import com.wego.carpark.app.data.dto.FetchDtos.NearbyResponse;
import com.wego.carpark.app.repository.CarparkRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CarparkFetchService {

    private final CarparkRepository carparkRepository;
    private final NearbyResponseAdapter nearbyResponseAdapter;

    public NearbyResponse getNearbyAvailable(NearbyRequest request) {
        return nearbyResponseAdapter.adapt(carparkRepository.findNearbyAvailableCarparks(
                request.latitude(), request.longitude(), request.radiusMeters(), request.lotType(), request.limit()));
    }
}
