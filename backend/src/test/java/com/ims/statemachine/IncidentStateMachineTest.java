package com.ims.statemachine;

import com.ims.exception.InvalidStateTransitionException;
import com.ims.model.WorkItem;
import com.ims.statemachine.state.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringJUnitConfig(classes = {
        IncidentStateMachine.class,
        OpenState.class,
        InvestigatingState.class,
        ResolvedState.class,
        ClosedState.class
})
class IncidentStateMachineTest {

    @Autowired
    private IncidentStateMachine stateMachine;

    @Test
    void openAllowsMoveToInvestigating() {
        WorkItem wi = new WorkItem();
        wi.setStatus(WorkItem.WorkItemStatus.OPEN);

        stateMachine.transition(wi, WorkItem.WorkItemStatus.INVESTIGATING);
    }

    @Test
    void closedCannotMoveToInvestigating() {
        WorkItem wi = new WorkItem();
        wi.setStatus(WorkItem.WorkItemStatus.CLOSED);

        assertThatThrownBy(() ->
                stateMachine.transition(wi, WorkItem.WorkItemStatus.INVESTIGATING))
                .isInstanceOf(InvalidStateTransitionException.class);
    }

    @Test
    void investigatingAllowsResolve() {
        WorkItem wi = new WorkItem();
        wi.setStatus(WorkItem.WorkItemStatus.INVESTIGATING);

        stateMachine.transition(wi, WorkItem.WorkItemStatus.RESOLVED);
    }

    @Test
    void resolvingAllowsClosed() {
        WorkItem wi = new WorkItem();
        wi.setStatus(WorkItem.WorkItemStatus.RESOLVED);

        stateMachine.transition(wi, WorkItem.WorkItemStatus.CLOSED);
    }

    @Test
    void isValidTransitionMirrorsTransitions() {
        org.assertj.core.api.Assertions.assertThat(stateMachine.isValidTransition(
                        WorkItem.WorkItemStatus.OPEN,
                        WorkItem.WorkItemStatus.INVESTIGATING))
                .isTrue();
        org.assertj.core.api.Assertions.assertThat(stateMachine.isValidTransition(
                        WorkItem.WorkItemStatus.CLOSED,
                        WorkItem.WorkItemStatus.INVESTIGATING))
                .isFalse();
    }
}
