package com.ims.service;

import com.ims.model.WorkItem;
import com.ims.service.alert.AlertStrategy;
import com.ims.service.alert.DefaultAlertStrategy;
import com.ims.service.alert.P0AlertStrategy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class AlertServiceTest {

    private P0AlertStrategy p0Spy;
    private DefaultAlertStrategy defaultSpy;
    private AlertService alertService;

    @BeforeEach
    void setUp() {
        p0Spy = spy(new P0AlertStrategy());
        defaultSpy = spy(new DefaultAlertStrategy());
        Map<String, AlertStrategy> strategies = new HashMap<>();
        strategies.put("P0AlertStrategy", p0Spy);
        strategies.put("DefaultAlertStrategy", defaultSpy);
        alertService = new AlertService(strategies);
    }

    @Test
    void routesP0ToP0Strategy() {
        WorkItem wi = WorkItem.builder()
                .id(42L)
                .componentId("cache-1")
                .priority("P0")
                .status(WorkItem.WorkItemStatus.OPEN)
                .signalCount(1)
                .build();

        alertService.sendAlert(wi);

        verify(p0Spy).sendAlert(wi);
        verifyNoInteractions(defaultSpy);
    }

    @Test
    void unknownPriorityUsesDefaultStrategyWhenPresent() {
        WorkItem wi = WorkItem.builder()
                .id(43L)
                .componentId("api")
                .priority("P9")
                .status(WorkItem.WorkItemStatus.OPEN)
                .signalCount(1)
                .build();

        alertService.sendAlert(wi);

        ArgumentCaptor<WorkItem> cap = ArgumentCaptor.forClass(WorkItem.class);
        verify(defaultSpy).sendAlert(cap.capture());
        verifyNoInteractions(p0Spy);
        assertThat(cap.getValue().getId()).isEqualTo(43L);
    }

    @Test
    void nullPriorityIsTreatedAsUnknown() {
        WorkItem wi = WorkItem.builder()
                .id(44L)
                .componentId("x")
                .priority(null)
                .status(WorkItem.WorkItemStatus.OPEN)
                .signalCount(1)
                .build();

        alertService.sendAlert(wi);

        verify(defaultSpy).sendAlert(any());
        verifyNoInteractions(p0Spy);
    }
}
