package com.ims.service;

import com.ims.dto.StatusUpdateRequest;
import com.ims.exception.IncidentNotFoundException;
import com.ims.exception.RCAIncompleteException;
import com.ims.model.WorkItem;
import com.ims.model.WorkItem.WorkItemStatus;
import com.ims.repository.RCARepository;
import com.ims.repository.WorkItemRepository;
import com.ims.statemachine.IncidentStateMachine;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link IncidentService}.
 *
 * <p>
 * All external dependencies (repositories, state machine, messaging) are
 * replaced with Mockito mocks so tests remain fast and purely in-memory.
 * </p>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("IncidentService")
class IncidentServiceTest {

    @Mock
    private WorkItemRepository workItemRepository;
    @Mock
    private RCARepository rcaRepository;
    @Mock
    private IncidentStateMachine stateMachine;
    @Mock
    private DashboardService dashboardService;
    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @InjectMocks
    private IncidentService incidentService;

    private WorkItem openIncident;
    private WorkItem investigatingIncident;
    private WorkItem resolvedIncident;

    @BeforeEach
    void setUp() {
        openIncident = WorkItem.builder()
                .id(1L)
                .componentId("CACHE_CLUSTER_01")
                .componentType("DISTRIBUTED_CACHE")
                .status(WorkItemStatus.OPEN)
                .priority("P0")
                .signalCount(1)
                .build();

        investigatingIncident = WorkItem.builder()
                .id(2L)
                .componentId("DB_PRIMARY")
                .componentType("RDBMS")
                .status(WorkItemStatus.INVESTIGATING)
                .priority("P1")
                .signalCount(3)
                .build();

        resolvedIncident = WorkItem.builder()
                .id(3L)
                .componentId("API_GW")
                .componentType("API")
                .status(WorkItemStatus.RESOLVED)
                .priority("P2")
                .signalCount(2)
                .build();
    }

    // getAllIncidents

    @Nested
    @DisplayName("getAllIncidents()")
    class GetAllIncidents {

        @Test
        @DisplayName("delegates to repository with supplied Pageable")
        void delegatesToRepository() {
            Pageable pageable = PageRequest.of(0, 10);
            Page<WorkItem> page = new PageImpl<>(List.of(openIncident));
            when(workItemRepository.findAll(pageable)).thenReturn(page);

            Page<WorkItem> result = incidentService.getAllIncidents(pageable);

            assertThat(result.getContent()).containsExactly(openIncident);
            verify(workItemRepository).findAll(pageable);
        }
    }

    // getIncidentById

    @Nested
    @DisplayName("getIncidentById()")
    class GetIncidentById {

        @Test
        @DisplayName("returns incident when found")
        void returnsIncidentWhenFound() {
            when(workItemRepository.findById(1L)).thenReturn(Optional.of(openIncident));

            WorkItem result = incidentService.getIncidentById(1L);

            assertThat(result).isEqualTo(openIncident);
        }

        @Test
        @DisplayName("throws IncidentNotFoundException when ID missing")
        void throwsWhenNotFound() {
            when(workItemRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> incidentService.getIncidentById(99L))
                    .isInstanceOf(IncidentNotFoundException.class)
                    .hasMessageContaining("99");
        }
    }

    // getActiveIncidents

    @Nested
    @DisplayName("getActiveIncidents()")
    class GetActiveIncidents {

        @Test
        @DisplayName("excludes CLOSED incidents")
        void excludesClosedIncidents() {
            when(workItemRepository.findByStatusNot(WorkItemStatus.CLOSED))
                    .thenReturn(List.of(openIncident, investigatingIncident));

            List<WorkItem> result = incidentService.getActiveIncidents();

            assertThat(result).hasSize(2);
            assertThat(result).noneMatch(w -> w.getStatus() == WorkItemStatus.CLOSED);
        }
    }

    @Nested
    @DisplayName("moveToInvestigating()")
    class MoveToInvestigating {

        @Test
        @DisplayName("saves incident with INVESTIGATING status and broadcasts update")
        void savesAndBroadcasts() {
            StatusUpdateRequest req = new StatusUpdateRequest();
            req.setAssignedTo("alice");
            req.setNotes("Checking cache cluster");

            when(workItemRepository.findById(1L)).thenReturn(Optional.of(openIncident));
            when(workItemRepository.save(any(WorkItem.class))).thenAnswer(inv -> inv.getArgument(0));

            WorkItem result = incidentService.moveToInvestigating(1L, req);

            assertThat(result.getStatus()).isEqualTo(WorkItemStatus.INVESTIGATING);
            assertThat(result.getAssignedTo()).isEqualTo("alice");
            assertThat(result.getNotes()).isEqualTo("Checking cache cluster");

            verify(stateMachine).transition(openIncident, WorkItemStatus.INVESTIGATING);
            verify(messagingTemplate).convertAndSend(eq("/topic/incidents"), any(WorkItem.class));
        }

        @Test
        @DisplayName("propagates exception from state machine (illegal transition)")
        void propagatesStateMachineException() {
            StatusUpdateRequest req = new StatusUpdateRequest();
            when(workItemRepository.findById(1L)).thenReturn(Optional.of(openIncident));
            doThrow(new com.ims.exception.InvalidStateTransitionException("bad transition"))
                    .when(stateMachine).transition(any(), any());

            assertThatThrownBy(() -> incidentService.moveToInvestigating(1L, req))
                    .isInstanceOf(com.ims.exception.InvalidStateTransitionException.class);
        }
    }

    // Resolved

    @Nested
    @DisplayName("moveToResolved()")
    class MoveToResolved {

        @Test
        @DisplayName("saves incident with RESOLVED status and broadcasts update")
        void savesAndBroadcasts() {
            StatusUpdateRequest req = new StatusUpdateRequest();
            req.setNotes("Fix deployed");

            when(workItemRepository.findById(2L)).thenReturn(Optional.of(investigatingIncident));
            when(workItemRepository.save(any(WorkItem.class))).thenAnswer(inv -> inv.getArgument(0));

            WorkItem result = incidentService.moveToResolved(2L, req);

            assertThat(result.getStatus()).isEqualTo(WorkItemStatus.RESOLVED);
            assertThat(result.getNotes()).isEqualTo("Fix deployed");

            verify(stateMachine).transition(investigatingIncident, WorkItemStatus.RESOLVED);
            verify(messagingTemplate).convertAndSend(eq("/topic/incidents"), any(WorkItem.class));
        }
    }

    // ToClosed

    @Nested
    @DisplayName("moveToClosed()")
    class MoveToClosed {

        @Test
        @DisplayName("closes incident when RCA exists and refreshes dashboard cache")
        void closesWhenRcaExists() {
            StatusUpdateRequest req = new StatusUpdateRequest();
            req.setNotes("Post-mortem complete");

            when(workItemRepository.findById(3L)).thenReturn(Optional.of(resolvedIncident));
            when(rcaRepository.existsByWorkItemId(3L)).thenReturn(true);
            when(workItemRepository.save(any(WorkItem.class))).thenAnswer(inv -> inv.getArgument(0));

            WorkItem result = incidentService.moveToClosed(3L, req);

            assertThat(result.getStatus()).isEqualTo(WorkItemStatus.CLOSED);
            assertThat(result.getClosedAt()).isNotNull();

            verify(dashboardService).refreshCache();
            verify(messagingTemplate).convertAndSend(eq("/topic/incidents"), any(WorkItem.class));
        }

        @Test
        @DisplayName("throws RCAIncompleteException when RCA is missing")
        void throwsWhenRcaMissing() {
            StatusUpdateRequest req = new StatusUpdateRequest();

            when(workItemRepository.findById(3L)).thenReturn(Optional.of(resolvedIncident));
            when(rcaRepository.existsByWorkItemId(3L)).thenReturn(false);

            assertThatThrownBy(() -> incidentService.moveToClosed(3L, req))
                    .isInstanceOf(RCAIncompleteException.class)
                    .hasMessageContaining("3");

            verify(workItemRepository, never()).save(any());
            verify(dashboardService, never()).refreshCache();
        }
    }

    // deleteIncident

    @Nested
    @DisplayName("deleteIncident()")
    class DeleteIncident {

        @Test
        @DisplayName("deletes incident from repository")
        void deletesIncident() {
            when(workItemRepository.findById(1L)).thenReturn(Optional.of(openIncident));

            incidentService.deleteIncident(1L);

            verify(workItemRepository).delete(openIncident);
        }

        @Test
        @DisplayName("throws IncidentNotFoundException when incident does not exist")
        void throwsWhenNotFound() {
            when(workItemRepository.findById(55L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> incidentService.deleteIncident(55L))
                    .isInstanceOf(IncidentNotFoundException.class);
        }
    }
}
