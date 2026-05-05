package com.ims.statemachine.state;

import com.ims.model.WorkItem;
import org.springframework.stereotype.Component;

@Component("INVESTIGATINGState")
public class InvestigatingState implements IncidentState {

    @Override
    public WorkItem.WorkItemStatus getStatus() {
        return WorkItem.WorkItemStatus.INVESTIGATING;
    }

    @Override
    public void toResolved(WorkItem context) {
        context.setStatus(WorkItem.WorkItemStatus.RESOLVED);
    }
}
