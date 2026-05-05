package com.ims.service;

import com.ims.model.WorkItem;
import com.ims.service.alert.AlertStrategy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class AlertService {

    // Spring auto-injects all beans implementing AlertStrategy into this map.
    private final Map<String, AlertStrategy> strategies;

    public void sendAlert(WorkItem workItem) {
        String priority = workItem.getPriority() != null ? workItem.getPriority().toUpperCase() : "UNKNOWN";
        String beanName = priority + "AlertStrategy";

        AlertStrategy strategy = strategies.getOrDefault(beanName, strategies.get("DefaultAlertStrategy"));

        if (strategy != null) {
            strategy.sendAlert(workItem);
        } else {
            log.warn("No alert strategy found for priority {}", priority);
        }
    }
}
