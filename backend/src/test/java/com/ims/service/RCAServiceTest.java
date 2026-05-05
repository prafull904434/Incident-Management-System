package com.ims.service;

import com.ims.dto.RCARequest;
import com.ims.exception.IncidentNotFoundException;
import com.ims.model.RCA;
import com.ims.model.WorkItem;
import com.ims.repository.RCARepository;
import com.ims.repository.WorkItemRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("RCAService")
class RCAServiceTest {

    @Mock
    private RCARepository rcaRepository;
    @Mock
    private WorkItemRepository workItemRepository;

    @InjectMocks
    private RCAService rcaService;

    private WorkItem sampleWorkItem;
    private RCARequest sampleRequest;

    @BeforeEach
    void setUp() {
        sampleWorkItem = WorkItem.builder()
                .id(10L)
                .componentId("DB_PRIMARY")
                .componentType("RDBMS")
                .status(WorkItem.WorkItemStatus.RESOLVED)
                .priority("P1")
                .signalCount(5)
                .build();

        sampleRequest = RCARequest.builder()
                .incidentStart(LocalDateTime.of(2026, 5, 1, 10, 0))
                .incidentEnd(LocalDateTime.of(2026, 5, 1, 12, 30))
                .rootCauseCategory("INFRASTRUCTURE")
                .rootCauseDescription("Primary DB ran out of disk space due to unrotated WAL logs.")
                .fixApplied("Cleared old WAL files, expanded disk volume to 500GB.")
                .preventionSteps("Add disk-usage alert at 80%, automate WAL rotation.")
                .build();
    }

    // submitRCA

    @Nested
    @DisplayName("submitRCA()")
    class SubmitRCA {

        @Test
        @DisplayName("creates and persists RCA when incident exists and no RCA yet")
        void createsRcaSuccessfully() {
            when(workItemRepository.findById(10L)).thenReturn(Optional.of(sampleWorkItem));
            when(rcaRepository.existsByWorkItemId(10L)).thenReturn(false);
            when(rcaRepository.save(any(RCA.class))).thenAnswer(inv -> inv.getArgument(0));

            RCA result = rcaService.submitRCA(10L, sampleRequest);

            assertThat(result.getWorkItemId()).isEqualTo(10L);
            assertThat(result.getRootCauseCategory()).isEqualTo("INFRASTRUCTURE");
            assertThat(result.getFixApplied()).isEqualTo("Cleared old WAL files, expanded disk volume to 500GB.");
            verify(rcaRepository).save(any(RCA.class));
        }

        @Test
        @DisplayName("throws IncidentNotFoundException when incident does not exist")
        void throwsWhenIncidentMissing() {
            when(workItemRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> rcaService.submitRCA(99L, sampleRequest))
                    .isInstanceOf(IncidentNotFoundException.class)
                    .hasMessageContaining("99");

            verify(rcaRepository, never()).save(any());
        }

        @Test
        @DisplayName("throws IllegalStateException when RCA already exists for incident")
        void throwsWhenRcaAlreadyExists() {
            when(workItemRepository.findById(10L)).thenReturn(Optional.of(sampleWorkItem));
            when(rcaRepository.existsByWorkItemId(10L)).thenReturn(true);

            assertThatThrownBy(() -> rcaService.submitRCA(10L, sampleRequest))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("already exists");

            verify(rcaRepository, never()).save(any());
        }

        @Test
        @DisplayName("saves all mandatory RCA fields correctly")
        void savesAllFields() {
            when(workItemRepository.findById(10L)).thenReturn(Optional.of(sampleWorkItem));
            when(rcaRepository.existsByWorkItemId(10L)).thenReturn(false);
            when(rcaRepository.save(any(RCA.class))).thenAnswer(inv -> inv.getArgument(0));

            RCA result = rcaService.submitRCA(10L, sampleRequest);

            assertThat(result.getIncidentStart()).isEqualTo(sampleRequest.getIncidentStart());
            assertThat(result.getIncidentEnd()).isEqualTo(sampleRequest.getIncidentEnd());
            assertThat(result.getRootCauseDescription())
                    .isEqualTo("Primary DB ran out of disk space due to unrotated WAL logs.");
            assertThat(result.getPreventionSteps())
                    .isEqualTo("Add disk-usage alert at 80%, automate WAL rotation.");
        }
    }

    // getRCAByIncidentId

    @Nested
    @DisplayName("getRCAByIncidentId()")
    class GetRCAByIncidentId {

        @Test
        @DisplayName("returns RCA when found")
        void returnsRcaWhenFound() {
            RCA rca = RCA.builder().id(1L).workItemId(10L).build();
            when(rcaRepository.findByWorkItemId(10L)).thenReturn(Optional.of(rca));

            RCA result = rcaService.getRCAByIncidentId(10L);

            assertThat(result.getWorkItemId()).isEqualTo(10L);
        }

        @Test
        @DisplayName("throws RuntimeException when RCA not found")
        void throwsWhenNotFound() {
            when(rcaRepository.findByWorkItemId(10L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> rcaService.getRCAByIncidentId(10L))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("not found");
        }
    }

    @Nested
    @DisplayName("updateRCA()")
    class UpdateRCA {

        @Test
        @DisplayName("updates all RCA fields and saves")
        void updatesFieldsAndSaves() {
            RCA existingRca = RCA.builder()
                    .id(5L)
                    .workItemId(10L)
                    .rootCauseCategory("UNKNOWN")
                    .rootCauseDescription("TBD")
                    .fixApplied("TBD")
                    .preventionSteps("TBD")
                    .incidentStart(LocalDateTime.of(2026, 5, 1, 10, 0))
                    .incidentEnd(LocalDateTime.of(2026, 5, 1, 11, 0))
                    .build();

            when(rcaRepository.findByWorkItemId(10L)).thenReturn(Optional.of(existingRca));
            when(rcaRepository.save(any(RCA.class))).thenAnswer(inv -> inv.getArgument(0));

            RCA result = rcaService.updateRCA(10L, sampleRequest);

            assertThat(result.getRootCauseCategory()).isEqualTo("INFRASTRUCTURE");
            assertThat(result.getIncidentEnd()).isEqualTo(sampleRequest.getIncidentEnd());
            verify(rcaRepository).save(existingRca);
        }
    }

    @Nested
    @DisplayName("getMTTR()")
    class GetMTTR {

        @Test
        @DisplayName("returns mttrSeconds from persisted RCA")
        void returnsMttrSeconds() {
            RCA rca = RCA.builder()
                    .workItemId(10L)
                    .mttrSeconds(9000L) // 2.5 hours
                    .build();
            when(rcaRepository.findByWorkItemId(10L)).thenReturn(Optional.of(rca));

            Long mttr = rcaService.getMTTR(10L);

            assertThat(mttr).isEqualTo(9000L);
        }
    }
}
