package com.ims.statemachine.state;

import com.ims.exception.InvalidStateTransitionException;
import com.ims.model.WorkItem;

public interface IncidentState {
    
    WorkItem.WorkItemStatus getStatus();

    default void toInvestigating(WorkItem context) {
        throw new InvalidStateTransitionException("Cannot transition from " + getStatus() + " to INVESTIGATING");
    }

    default void toResolved(WorkItem context) {
        throw new InvalidStateTransitionException("Cannot transition from " + getStatus() + " to RESOLVED");
    }

    default void toClosed(WorkItem context) {
        throw new InvalidStateTransitionException("Cannot transition from " + getStatus() + " to CLOSED");
    }
}
