package com.ims.service.alert;

import com.ims.model.WorkItem;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component("DefaultAlertStrategy")
public class DefaultAlertStrategy implements AlertStrategy {
    @Override
    public void sendAlert(WorkItem workItem) {
        log.info(
            "📋 [ALERT] WorkItem #{} | Component: {} | Priority: {} | Status: {}",
            workItem.getId(), workItem.getComponentId(), workItem.getPriority(), workItem.getStatus());
    }
}
