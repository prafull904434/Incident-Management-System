package com.ims.ingestion;

import com.ims.dto.SignalRequest;
import com.ims.service.SignalService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link SignalIngestionPipeline}.
 *
 * <p>
 * Validates the buffer queue semantics (enqueue, drain, retry-on-failure)
 * and the throughput counter logic independently of the database.
 * </p>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("SignalIngestionPipeline")
class SignalIngestionPipelineTest {

    @Mock
    private SignalService signalService;

    private SignalIngestionPipeline pipeline;

    @BeforeEach
    void setUp() {
        pipeline = new SignalIngestionPipeline(signalService);
    }

    // enqueue

    @Test
    @DisplayName("enqueue returns true and increments counter for first signal")
    void enqueueReturnsTrueAndIncrementsCounter() {
        SignalRequest req = buildSignalRequest("CACHE_01");

        boolean result = pipeline.enqueue(req);

        assertThat(result).isTrue();
        assertThat(pipeline.getBufferSize()).isEqualTo(1);
        assertThat(pipeline.getCurrentThroughput()).isEqualTo(1L);
    }

    @Test
    @DisplayName("enqueue increments counter for each accepted signal")
    void enqueueIncrementsForEachSignal() {
        for (int i = 0; i < 10; i++) {
            pipeline.enqueue(buildSignalRequest("COMP_" + i));
        }

        assertThat(pipeline.getBufferSize()).isEqualTo(10);
        assertThat(pipeline.getCurrentThroughput()).isEqualTo(10L);
    }

    @Test
    @DisplayName("getBufferSize returns 0 on fresh pipeline")
    void bufferSizeIsZeroInitially() {
        assertThat(pipeline.getBufferSize()).isEqualTo(0);
    }

    @Test
    @DisplayName("getCurrentThroughput returns 0 on fresh pipeline")
    void throughputIsZeroInitially() {
        assertThat(pipeline.getCurrentThroughput()).isEqualTo(0L);
    }

    // processBatch

    @Test
    @DisplayName("processBatch is a no-op when buffer is empty")
    void processBatchNoOpWhenEmpty() {
        pipeline.processBatch();

        verifyNoInteractions(signalService);
    }

    @Test
    @DisplayName("processBatch drains all buffered signals into a single service call")
    void processBatchDrainsBuffer() {
        pipeline.enqueue(buildSignalRequest("A"));
        pipeline.enqueue(buildSignalRequest("B"));
        pipeline.enqueue(buildSignalRequest("C"));

        pipeline.processBatch();

        assertThat(pipeline.getBufferSize()).isEqualTo(0);

        ArgumentCaptor<List<SignalRequest>> cap = ArgumentCaptor.forClass(List.class);
        verify(signalService).ingestBatch(cap.capture());
        assertThat(cap.getValue()).hasSize(3);
    }

    @Test
    @DisplayName("processBatch re-queues all signals on service failure")
    void processBatchReQueuesOnFailure() {
        pipeline.enqueue(buildSignalRequest("X"));
        pipeline.enqueue(buildSignalRequest("Y"));

        doThrow(new RuntimeException("DB down")).when(signalService).ingestBatch(anyList());

        pipeline.processBatch();

        // Signals should have been re-queued
        assertThat(pipeline.getBufferSize()).isEqualTo(2);
    }

    @Test
    @DisplayName("processBatch caps drain at 500 signals per invocation")
    void processBatchCapsAt500() {
        // Enqueue 600 signals
        for (int i = 0; i < 600; i++) {
            pipeline.enqueue(buildSignalRequest("COMP_" + i));
        }

        pipeline.processBatch();

        // 100 should remain (600 - 500 drained)
        assertThat(pipeline.getBufferSize()).isEqualTo(100);

        ArgumentCaptor<List<SignalRequest>> cap = ArgumentCaptor.forClass(List.class);
        verify(signalService).ingestBatch(cap.capture());
        assertThat(cap.getValue()).hasSize(500);
    }

    // Helper

    private SignalRequest buildSignalRequest(String componentId) {
        return SignalRequest.builder()
                .componentId(componentId)
                .componentType("DISTRIBUTED_CACHE")
                .errorCode("CONNECTION_TIMEOUT")
                .severity("P1")
                .message("Test signal for " + componentId)
                .timestamp(LocalDateTime.now())
                .build();
    }
}
