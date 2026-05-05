package com.ims.statemachine.state;

import com.ims.model.WorkItem;
import org.springframework.stereotype.Component;

@Component("RESOLVEDState")
public class ResolvedState implements IncidentState {

    @Override
    public WorkItem.WorkItemStatus getStatus() {
        return WorkItem.WorkItemStatus.RESOLVED;
    }

    @Override
    public void toClosed(WorkItem context) {
        context.setStatus(WorkItem.WorkItemStatus.CLOSED);
    }
}
