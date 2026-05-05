package com.ims.service.alert;

import com.ims.model.WorkItem;

public interface AlertStrategy {
    void sendAlert(WorkItem workItem);
}
