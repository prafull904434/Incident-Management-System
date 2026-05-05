package com.ims.statemachine;

import com.ims.model.WorkItem;
import com.ims.model.WorkItem.WorkItemStatus;
import com.ims.statemachine.state.IncidentState;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class IncidentStateMachine {

    // Inject all IncidentState beans
    private final Map<String, IncidentState> states;

    public void transition(WorkItem workItem, WorkItemStatus newStatus) {
        WorkItemStatus currentStatus = workItem.getStatus();
        IncidentState currentState = states.get(currentStatus.name() + "State");

        if (currentState == null) {
            throw new IllegalStateException("State handler not found for " + currentStatus);
        }

        // Delegate the transition attempt to the current state object
        switch (newStatus) {
            case INVESTIGATING -> currentState.toInvestigating(workItem);
            case RESOLVED -> currentState.toResolved(workItem);
            case CLOSED -> currentState.toClosed(workItem);
            default -> throw new IllegalArgumentException("Unsupported transition target: " + newStatus);
        }
    }

    // Check if Transition is Valid
    public boolean isValidTransition(WorkItemStatus current, WorkItemStatus next) {
        try {
            IncidentState currentState = states.get(current.name() + "State");
            if (currentState == null)
                return false;

            // Try transition on a dummy object
            WorkItem dummy = new WorkItem();
            dummy.setStatus(current);

            switch (next) {
                case INVESTIGATING -> currentState.toInvestigating(dummy);
                case RESOLVED -> currentState.toResolved(dummy);
                case CLOSED -> currentState.toClosed(dummy);
                default -> {
                    return false;
                }
            }
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
