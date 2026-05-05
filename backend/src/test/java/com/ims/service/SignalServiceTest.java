package com.ims.service;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.ims.dto.SignalRequest;
import com.ims.model.Signal;
import com.ims.model.WorkItem;
import com.ims.repository.SignalRepository;
import com.ims.repository.WorkItemRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("SignalService")
class SignalServiceTest {

    @Mock
    private SignalRepository signalRepository;
    @Mock
    private WorkItemRepository workItemRepository;
    @Mock
    private AlertService alertService;

    // Real Caffeine cache so we can test cache-hit / miss paths properly
    private Cache<String, Long> debouncer;
    private SignalService signalService;

    @BeforeEach
    void setUp() {
        debouncer = Caffeine.newBuilder()
                .expireAfterWrite(Duration.ofSeconds(10))
                .maximumSize(1_000)
                .build();
        signalService = new SignalService(signalRepository, workItemRepository, alertService, debouncer);
    }

    @Nested
    @DisplayName("getSignalsByWorkItemId()")
    class GetSignalsByWorkItemId {

        @Test
        @DisplayName("parses string ID and delegates to repository")
        void parsesAndDelegates() {
            Signal sig = Signal.builder().workItemId(7L).componentId("X").build();
            when(signalRepository.findByWorkItemId(7L)).thenReturn(List.of(sig));

            List<Signal> result = signalService.getSignalsByWorkItemId("7");

            assertThat(result).containsExactly(sig);
            verify(signalRepository).findByWorkItemId(7L);
        }

        @Test
        @DisplayName("handles whitespace-padded ID string")
        void handlesPaddedId() {
            when(signalRepository.findByWorkItemId(5L)).thenReturn(List.of());

            List<Signal> result = signalService.getSignalsByWorkItemId("  5  ");

            assertThat(result).isEmpty();
            verify(signalRepository).findByWorkItemId(5L);
        }
    }

    // ingestBatch

    @Nested
    @DisplayName("ingestBatch()")
    class IngestBatch {

        @Test
        @DisplayName("no-ops on null batch")
        void noOpOnNull() {
            signalService.ingestBatch(null);
            verifyNoInteractions(workItemRepository, signalRepository, alertService);
        }

        @Test
        @DisplayName("no-ops on empty batch")
        void noOpOnEmpty() {
            signalService.ingestBatch(List.of());
            verifyNoInteractions(workItemRepository, signalRepository, alertService);
        }

        @Test
        @DisplayName("creates new WorkItem when no existing open incident for component")
        void createsNewWorkItemOnCacheMiss() {
            SignalRequest req = buildSignalRequest("CACHE_01", "P0");

            when(workItemRepository.findByComponentIdAndStatusNot("CACHE_01", WorkItem.WorkItemStatus.CLOSED))
                    .thenReturn(Optional.empty());

            WorkItem created = WorkItem.builder()
                    .id(100L)
                    .componentId("CACHE_01")
                    .componentType("DISTRIBUTED_CACHE")
                    .priority("P0")
                    .status(WorkItem.WorkItemStatus.OPEN)
                    .signalCount(1)
                    .build();
            when(workItemRepository.save(any(WorkItem.class))).thenReturn(created);
            when(signalRepository.save(any(Signal.class))).thenAnswer(inv -> inv.getArgument(0));

            signalService.ingestBatch(List.of(req));

            // WorkItem created and alerted
            verify(alertService).sendAlert(created);

            // Signal persisted with debounced=false
            ArgumentCaptor<Signal> sigCap = ArgumentCaptor.forClass(Signal.class);
            verify(signalRepository).save(sigCap.capture());
            assertThat(sigCap.getValue().isDebounced()).isFalse();
            assertThat(sigCap.getValue().getSeverity()).isEqualTo("P0");
        }

        @Test
        @DisplayName("increments signalCount on existing open WorkItem (debounce hit)")
        void incrementsSignalCountOnCacheHit() {
            SignalRequest req = buildSignalRequest("DB_01", "P1");

            WorkItem existing = WorkItem.builder()
                    .id(200L)
                    .componentId("DB_01")
                    .componentType("RDBMS")
                    .priority("P1")
                    .status(WorkItem.WorkItemStatus.OPEN)
                    .signalCount(5)
                    .build();

            // Pre-populate cache so cache-hit path is taken
            debouncer.put("DB_01", 200L);
            when(workItemRepository.findById(200L)).thenReturn(Optional.of(existing));
            when(workItemRepository.save(any(WorkItem.class))).thenAnswer(inv -> inv.getArgument(0));
            when(signalRepository.save(any(Signal.class))).thenAnswer(inv -> inv.getArgument(0));

            signalService.ingestBatch(List.of(req));

            verifyNoInteractions(alertService);

            ArgumentCaptor<WorkItem> wiCap = ArgumentCaptor.forClass(WorkItem.class);
            verify(workItemRepository).save(wiCap.capture());
            assertThat(wiCap.getValue().getSignalCount()).isEqualTo(6);

            ArgumentCaptor<Signal> sigCap = ArgumentCaptor.forClass(Signal.class);
            verify(signalRepository).save(sigCap.capture());
            assertThat(sigCap.getValue().isDebounced()).isTrue();
        }

        @Test
        @DisplayName("normalises severity to uppercase")
        void normalisesSeverityToUppercase() {
            SignalRequest req = buildSignalRequest("API_GW", "p2"); // lowercase

            when(workItemRepository.findByComponentIdAndStatusNot("API_GW", WorkItem.WorkItemStatus.CLOSED))
                    .thenReturn(Optional.empty());

            WorkItem saved = WorkItem.builder()
                    .id(300L)
                    .componentId("API_GW")
                    .componentType("API")
                    .priority("P2")
                    .status(WorkItem.WorkItemStatus.OPEN)
                    .signalCount(1)
                    .build();
            when(workItemRepository.save(any(WorkItem.class))).thenReturn(saved);
            when(signalRepository.save(any(Signal.class))).thenAnswer(inv -> inv.getArgument(0));

            signalService.ingestBatch(List.of(req));

            ArgumentCaptor<Signal> cap = ArgumentCaptor.forClass(Signal.class);
            verify(signalRepository).save(cap.capture());
            assertThat(cap.getValue().getSeverity()).isEqualTo("P2");
        }

        @Test
        @DisplayName("skips CLOSED WorkItem from cache and creates a fresh one")
        void skipsCachedClosedWorkItem() {
            SignalRequest req = buildSignalRequest("QUEUE_01", "P1");

            // Cache has an ID pointing to a CLOSED work item
            debouncer.put("QUEUE_01", 50L);
            WorkItem closedWI = WorkItem.builder()
                    .id(50L)
                    .componentId("QUEUE_01")
                    .status(WorkItem.WorkItemStatus.CLOSED)
                    .signalCount(10)
                    .build();
            when(workItemRepository.findById(50L)).thenReturn(Optional.of(closedWI));

            // DB also returns empty for open incident query
            when(workItemRepository.findByComponentIdAndStatusNot("QUEUE_01", WorkItem.WorkItemStatus.CLOSED))
                    .thenReturn(Optional.empty());

            WorkItem newWI = WorkItem.builder()
                    .id(51L)
                    .componentId("QUEUE_01")
                    .status(WorkItem.WorkItemStatus.OPEN)
                    .signalCount(1)
                    .build();
            when(workItemRepository.save(any(WorkItem.class))).thenReturn(newWI);
            when(signalRepository.save(any(Signal.class))).thenAnswer(inv -> inv.getArgument(0));

            signalService.ingestBatch(List.of(req));

            // Should create a new WorkItem and send alert
            verify(alertService).sendAlert(newWI);
        }
    }

    private SignalRequest buildSignalRequest(String componentId, String severity) {
        return SignalRequest.builder()
                .componentId(componentId)
                .componentType("TEST_COMPONENT")
                .errorCode("TEST_ERROR")
                .severity(severity)
                .message("Test signal")
                .timestamp(LocalDateTime.now())
                .build();
    }
}
