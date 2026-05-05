package com.ims.statemachine.state;

import com.ims.model.WorkItem;
import org.springframework.stereotype.Component;

@Component("CLOSEDState")
public class ClosedState implements IncidentState {

    @Override
    public WorkItem.WorkItemStatus getStatus() {
        return WorkItem.WorkItemStatus.CLOSED;
    }

}
