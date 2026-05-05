package com.ims.service.alert;

import com.ims.model.WorkItem;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component("P2AlertStrategy")
public class P2AlertStrategy implements AlertStrategy {
    @Override
    public void sendAlert(WorkItem workItem) {
        log.info(
                " [ALERT P2-MEDIUM] WorkItem #{} | Component: {} | Status: {} — Scheduled review.",
                workItem.getId(), workItem.getComponentId(), workItem.getStatus());
    }
}
