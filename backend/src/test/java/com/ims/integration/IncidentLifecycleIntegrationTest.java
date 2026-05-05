package com.ims.integration;

import com.ims.dto.RCARequest;
import com.ims.dto.SignalRequest;
import com.ims.dto.StatusUpdateRequest;
import com.ims.exception.RCAIncompleteException;
import com.ims.model.RCA;
import com.ims.model.Signal;
import com.ims.model.WorkItem;
import com.ims.repository.RCARepository;
import com.ims.repository.SignalRepository;
import com.ims.repository.WorkItemRepository;
import com.ims.service.IncidentService;
import com.ims.service.RCAService;
import com.ims.service.SignalService;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;

/**
 * Integration tests for the full signal → incident lifecycle.
 *
 * <p>
 * Uses {@code @SpringBootTest} to boot up the full application context with H2
 * in-memory.
 * MongoDB auto-configuration is disabled in {@code application-test.properties}
 * to
 * avoid requiring an embedded MongoDB binary. Instead, the
 * {@link SignalRepository}
 * is mocked using {@code @MockBean}.
 * </p>
 */
@SpringBootTest
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("Signal Ingestion → Incident Lifecycle (Integration)")
class IncidentLifecycleIntegrationTest {

    @Autowired
    private WorkItemRepository workItemRepository;
    @Autowired
    private RCARepository rcaRepository;

    @Autowired
    private SignalService signalService;
    @Autowired
    private IncidentService incidentService;
    @Autowired
    private RCAService rcaService;

    // We mock MongoDB because we excluded Mongo auto-configuration in test
    // properties
    @MockBean
    private SignalRepository signalRepository;

    @MockBean
    private SimpMessagingTemplate messagingTemplate;

    @BeforeEach
    void setup() {
        rcaRepository.deleteAll();
        workItemRepository.deleteAll();

        lenient().doNothing().when(messagingTemplate).convertAndSend(anyString(), any(Object.class));
        lenient().when(signalRepository.save(any(Signal.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    @Order(1)
    @DisplayName("1. Signal ingestion creates a new OPEN WorkItem")
    void signalCreatesWorkItem() {
        signalService.ingestBatch(List.of(buildSignalRequest("CACHE_CLUSTER_01", "P0")));

        List<WorkItem> workItems = workItemRepository.findAll();
        assertThat(workItems).hasSize(1);

        WorkItem wi = workItems.get(0);
        assertThat(wi.getComponentId()).isEqualTo("CACHE_CLUSTER_01");
        assertThat(wi.getStatus()).isEqualTo(WorkItem.WorkItemStatus.OPEN);
        assertThat(wi.getPriority()).isEqualTo("P0");
        assertThat(wi.getSignalCount()).isEqualTo(1);
    }

    // Scenario

    @Test
    @Order(2)
    @DisplayName("2. Repeated signals for the same component are debounced (single WorkItem)")
    void duplicateSignalsAreDebouncedIntoOneWorkItem() {
        signalService.ingestBatch(List.of(
                buildSignalRequest("DB_PRIMARY", "P1"),
                buildSignalRequest("DB_PRIMARY", "P1"),
                buildSignalRequest("DB_PRIMARY", "P1")));

        List<WorkItem> all = workItemRepository.findAll();
        assertThat(all).hasSize(1);
        assertThat(all.get(0).getSignalCount()).isEqualTo(3);
    }

    // Scenario

    @Test
    @Order(3)
    @DisplayName("3. Signals from different components each create their own WorkItem")
    void differentComponentsCreateSeparateWorkItems() {
        signalService.ingestBatch(List.of(
                buildSignalRequest("CACHE_01", "P0"),
                buildSignalRequest("DB_01", "P1"),
                buildSignalRequest("API_GW", "P2")));

        assertThat(workItemRepository.count()).isEqualTo(3);
    }

    // Scenario 4

    @Test
    @Order(4)
    @DisplayName("4. Full OPEN → INVESTIGATING → RESOLVED → CLOSED lifecycle with RCA")
    void fullLifecyclePassesWithRCA() {
        signalService.ingestBatch(List.of(buildSignalRequest("API_GW", "P1")));
        Long id = workItemRepository.findAll().get(0).getId();

        StatusUpdateRequest investigateReq = new StatusUpdateRequest();
        investigateReq.setAssignedTo("alice");
        investigateReq.setNotes("Reproducing the issue");
        WorkItem investigating = incidentService.moveToInvestigating(id, investigateReq);
        assertThat(investigating.getStatus()).isEqualTo(WorkItem.WorkItemStatus.INVESTIGATING);

        WorkItem resolved = incidentService.moveToResolved(id, new StatusUpdateRequest());
        assertThat(resolved.getStatus()).isEqualTo(WorkItem.WorkItemStatus.RESOLVED);

        RCA rca = rcaService.submitRCA(id, buildRcaRequest(
                LocalDateTime.of(2026, 5, 1, 10, 0),
                LocalDateTime.of(2026, 5, 1, 11, 30)));
        assertThat(rca.getMttrSeconds()).isEqualTo(5400L); // 1h 30m

        WorkItem closed = incidentService.moveToClosed(id, new StatusUpdateRequest());
        assertThat(closed.getStatus()).isEqualTo(WorkItem.WorkItemStatus.CLOSED);
        assertThat(closed.getClosedAt()).isNotNull();
    }

    // Scenario 5

    @Test
    @Order(5)
    @DisplayName("5. Closing an incident without RCA throws RCAIncompleteException")
    void cannotCloseWithoutRCA() {
        signalService.ingestBatch(List.of(buildSignalRequest("QUEUE_SVC", "P2")));
        Long id = workItemRepository.findAll().get(0).getId();

        incidentService.moveToInvestigating(id, new StatusUpdateRequest());
        incidentService.moveToResolved(id, new StatusUpdateRequest());

        assertThatThrownBy(() -> incidentService.moveToClosed(id, new StatusUpdateRequest()))
                .isInstanceOf(RCAIncompleteException.class);

        // Status must not have changed
        assertThat(workItemRepository.findById(id).orElseThrow().getStatus())
                .isEqualTo(WorkItem.WorkItemStatus.RESOLVED);
    }

    // Scenario 6

    @Test
    @Order(6)
    @DisplayName("6. Invalid transition OPEN → RESOLVED is rejected by the state machine")
    void invalidTransitionIsRejected() {
        signalService.ingestBatch(List.of(buildSignalRequest("DB_REPLICA", "P1")));
        Long id = workItemRepository.findAll().get(0).getId();

        assertThatThrownBy(() -> incidentService.moveToResolved(id, new StatusUpdateRequest()))
                .isInstanceOf(com.ims.exception.InvalidStateTransitionException.class);

        assertThat(workItemRepository.findById(id).orElseThrow().getStatus())
                .isEqualTo(WorkItem.WorkItemStatus.OPEN);
    }

    // Scenario 7

    @Test
    @Order(7)
    @DisplayName("7. MTTR is computed correctly for a 2-hour incident")
    void mttrCalculationIsAccurate() {
        signalService.ingestBatch(List.of(buildSignalRequest("COMPUTE_01", "P0")));
        Long id = workItemRepository.findAll().get(0).getId();

        incidentService.moveToInvestigating(id, new StatusUpdateRequest());
        incidentService.moveToResolved(id, new StatusUpdateRequest());

        RCA rca = rcaService.submitRCA(id, buildRcaRequest(
                LocalDateTime.of(2026, 5, 1, 8, 0, 0),
                LocalDateTime.of(2026, 5, 1, 10, 0, 0)));

        assertThat(rca.getMttrSeconds()).isEqualTo(7200L);
        assertThat(rca.getMttrFormatted()).isEqualTo("2 hours 0 minutes");
    }

    // Scenario 8

    @Test
    @Order(8)
    @DisplayName("8. Submitting a second RCA for the same incident is rejected")
    void duplicateRCAIsRejected() {
        signalService.ingestBatch(List.of(buildSignalRequest("AUTH_SVC", "P1")));
        Long id = workItemRepository.findAll().get(0).getId();

        incidentService.moveToInvestigating(id, new StatusUpdateRequest());
        incidentService.moveToResolved(id, new StatusUpdateRequest());

        RCARequest rcaReq = buildRcaRequest(
                LocalDateTime.of(2026, 5, 1, 8, 0),
                LocalDateTime.of(2026, 5, 1, 9, 0));

        rcaService.submitRCA(id, rcaReq);

        assertThatThrownBy(() -> rcaService.submitRCA(id, rcaReq))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("already exists");
    }

    private SignalRequest buildSignalRequest(String componentId, String severity) {
        return SignalRequest.builder()
                .componentId(componentId)
                .componentType("TEST")
                .errorCode("TEST_ERROR")
                .severity(severity)
                .message("Integration test signal for " + componentId)
                .timestamp(LocalDateTime.now())
                .build();
    }

    private RCARequest buildRcaRequest(LocalDateTime start, LocalDateTime end) {
        return RCARequest.builder()
                .incidentStart(start)
                .incidentEnd(end)
                .rootCauseCategory("CODE_BUG")
                .rootCauseDescription("Test root cause description")
                .fixApplied("Test fix applied")
                .preventionSteps("Test prevention steps")
                .build();
    }
}
