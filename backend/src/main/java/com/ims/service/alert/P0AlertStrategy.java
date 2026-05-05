package com.ims.service.alert;

import com.ims.model.WorkItem;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component("P0AlertStrategy")
public class P0AlertStrategy implements AlertStrategy {
    @Override
    public void sendAlert(WorkItem workItem) {
        log.error(
                "[ALERT P0-CRITICAL] WorkItem #{} | Component: {} | Status: {} — Immediate action required! Paging on-call.",
                workItem.getId(), workItem.getComponentId(), workItem.getStatus());
    }
}
