package com.wego.carpark.app.scheduler;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.wego.carpark.app.client.GovSgClient;
import com.wego.carpark.app.repository.CarparkRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CarparkSyncSchedulerTest {

    @Mock
    GovSgClient govSgClient;

    @Mock
    CarparkRepository repository;

    @Test
    void failedLiveFetchKeepsLastKnownStateAndDoesNotWrite() {
        when(govSgClient.fetchLiveAvailability()).thenThrow(new RuntimeException("network unavailable"));

        new CarparkSyncScheduler(govSgClient, repository).syncLiveAvailability();

        verify(repository, never()).batchUpsertLiveAvailability(org.mockito.ArgumentMatchers.anyList());
    }

    @Test
    void emptyLiveResponseDoesNotOverwriteExistingAvailability() {
        when(govSgClient.fetchLiveAvailability()).thenReturn(null);

        new CarparkSyncScheduler(govSgClient, repository).syncLiveAvailability();

        verify(repository, never()).batchUpsertLiveAvailability(org.mockito.ArgumentMatchers.anyList());
    }
}
