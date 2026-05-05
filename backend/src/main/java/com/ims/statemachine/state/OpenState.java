package com.ims.statemachine.state;

import com.ims.model.WorkItem;
import org.springframework.stereotype.Component;

@Component("OPENState")
public class OpenState implements IncidentState {

    @Override
    public WorkItem.WorkItemStatus getStatus() {
        return WorkItem.WorkItemStatus.OPEN;
    }

    @Override
    public void toInvestigating(WorkItem context) {
        context.setStatus(WorkItem.WorkItemStatus.INVESTIGATING);
    }
}
