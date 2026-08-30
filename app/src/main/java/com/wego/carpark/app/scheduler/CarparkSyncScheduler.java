package com.wego.carpark.app.scheduler;

import com.wego.carpark.app.client.GovSgClient;
import com.wego.carpark.app.data.dto.GovSgDtos.LiveResponse;
import com.wego.carpark.app.data.dto.GovSgDtos.StaticResponse;
import com.wego.carpark.app.repository.CarparkRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class CarparkSyncScheduler {

    private final GovSgClient govSgClient;
    private final CarparkRepository carparkRepository;

    @Scheduled(fixedRate = 60_000)
    public void syncLiveAvailability() {
        try {
            LiveResponse response = govSgClient.fetchLiveAvailability();
            if (response != null && response.items() != null && !response.items().isEmpty()) {
                carparkRepository.batchUpsertLiveAvailability(response.items().getFirst().carparkData());
                log.info("Successfully synced live carpark availability.");
            }
        } catch (Exception exception) {
            log.error("Failed to sync live availability; serving the last known state.", exception);
        }
    }

    @EventListener(ApplicationReadyEvent.class)
    @Scheduled(cron = "0 0 3 * * ?")
    public void syncStaticData() {
        try {
            StaticResponse response = govSgClient.fetchStaticCarparks();
            if (response != null && response.result() != null && response.result().records() != null) {
                carparkRepository.batchUpsertStaticData(response.result().records());
                log.info("Successfully synced {} static carparks.", response.result().records().size());
            }
        } catch (Exception exception) {
            log.error("Failed to sync static carpark data.", exception);
        }
    }
}
