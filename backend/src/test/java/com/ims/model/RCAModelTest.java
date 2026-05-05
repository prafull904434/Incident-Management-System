package com.ims.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.*;

@DisplayName("RCA Model")
class RCAModelTest {

    @Nested
    @DisplayName("MTTR calculation")
    class MttrCalculation {

        @Test
        @DisplayName("correctly computes MTTR in seconds")
        void computesMttrInSeconds() {
            RCA rca = RCA.builder()
                    .workItemId(1L)
                    .incidentStart(LocalDateTime.of(2026, 5, 1, 10, 0, 0))
                    .incidentEnd(LocalDateTime.of(2026, 5, 1, 12, 30, 0))
                    .rootCauseCategory("INFRASTRUCTURE")
                    .rootCauseDescription("Disk full")
                    .fixApplied("Cleared disk")
                    .preventionSteps("Add alerts")
                    .build();

            // Simulate JPA lifecycle
            rca.onCreate();

            // 2h 30m = 9000 seconds
            assertThat(rca.getMttrSeconds()).isEqualTo(9000L);
        }

        @Test
        @DisplayName("formats MTTR as 'X hours Y minutes' when >= 1 hour")
        void formatsHoursAndMinutes() {
            RCA rca = buildRca(
                    LocalDateTime.of(2026, 5, 1, 10, 0, 0),
                    LocalDateTime.of(2026, 5, 1, 12, 30, 0));
            rca.onCreate();

            assertThat(rca.getMttrFormatted()).isEqualTo("2 hours 30 minutes");
        }

        @Test
        @DisplayName("formats MTTR as 'X minutes Y seconds' when < 1 hour")
        void formatsMinutesAndSeconds() {
            RCA rca = buildRca(
                    LocalDateTime.of(2026, 5, 1, 10, 0, 0),
                    LocalDateTime.of(2026, 5, 1, 10, 45, 20));
            rca.onCreate();

            assertThat(rca.getMttrFormatted()).isEqualTo("45 minutes 20 seconds");
        }

        @Test
        @DisplayName("formats MTTR as 'X seconds' when < 1 minute")
        void formatsSeconds() {
            RCA rca = buildRca(
                    LocalDateTime.of(2026, 5, 1, 10, 0, 0),
                    LocalDateTime.of(2026, 5, 1, 10, 0, 42));
            rca.onCreate();

            assertThat(rca.getMttrFormatted()).isEqualTo("42 seconds");
        }

        @Test
        @DisplayName("MTTR is zero seconds for same start and end time")
        void zeroMttrForSameTime() {
            LocalDateTime t = LocalDateTime.of(2026, 5, 1, 10, 0, 0);
            RCA rca = buildRca(t, t);
            rca.onCreate();

            assertThat(rca.getMttrSeconds()).isEqualTo(0L);
            assertThat(rca.getMttrFormatted()).isEqualTo("0 seconds");
        }

        @Test
        @DisplayName("recalculates MTTR on update")
        void recalculatesOnUpdate() {
            RCA rca = buildRca(
                    LocalDateTime.of(2026, 5, 1, 10, 0, 0),
                    LocalDateTime.of(2026, 5, 1, 11, 0, 0) // 1 hour
            );
            rca.onCreate();
            assertThat(rca.getMttrSeconds()).isEqualTo(3600L);

            // Update end time
            rca.setIncidentEnd(LocalDateTime.of(2026, 5, 1, 12, 0, 0)); // 2 hours
            rca.onUpdate();

            assertThat(rca.getMttrSeconds()).isEqualTo(7200L);
        }
    }

    // submittedAt

    @Nested
    @DisplayName("submittedAt timestamp")
    class SubmittedAt {

        @Test
        @DisplayName("sets submittedAt on onCreate")
        void setsSubmittedAtOnCreate() {
            RCA rca = buildRca(
                    LocalDateTime.of(2026, 5, 1, 10, 0, 0),
                    LocalDateTime.of(2026, 5, 1, 11, 0, 0));

            assertThat(rca.getSubmittedAt()).isNull();
            rca.onCreate();
            assertThat(rca.getSubmittedAt()).isNotNull().isBeforeOrEqualTo(LocalDateTime.now());
        }
    }

    private RCA buildRca(LocalDateTime start, LocalDateTime end) {
        return RCA.builder()
                .workItemId(1L)
                .incidentStart(start)
                .incidentEnd(end)
                .rootCauseCategory("CODE_BUG")
                .rootCauseDescription("NullPointerException in order service")
                .fixApplied("Added null check")
                .preventionSteps("Add test coverage")
                .build();
    }
}
