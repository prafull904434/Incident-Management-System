package com.ims.service.alert;

import com.ims.model.WorkItem;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component("P1AlertStrategy")
public class P1AlertStrategy implements AlertStrategy {
    @Override
    public void sendAlert(WorkItem workItem) {
        log.warn(
                "[ALERT P1-HIGH] WorkItem #{} | Component: {} | Status: {} — Urgent investigation needed.",
                workItem.getId(), workItem.getComponentId(), workItem.getStatus());
    }
}
