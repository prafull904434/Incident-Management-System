package com.ims.ingestion;

import com.ims.dto.SignalRequest;
import com.ims.service.SignalService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicLong;

@Slf4j
@Component
@RequiredArgsConstructor
public class SignalIngestionPipeline {

    private final SignalService signalService;

    private final LinkedBlockingQueue<SignalRequest> buffer = new LinkedBlockingQueue<>(50000); // Max 50k signals in
                                                                                                // buffer

    // Throughput Counter
    private final AtomicLong signalCounter = new AtomicLong(0);
    private final AtomicLong lastCounterReset = new AtomicLong(
            System.currentTimeMillis());

    // Add Signal to Buffer
    public boolean enqueue(SignalRequest signal) {
        boolean added = buffer.offer(signal); // Non-blocking offer
        if (added) {
            signalCounter.incrementAndGet();
        } else {
            log.warn(" Buffer full! Signal dropped for component: {}",
                    signal.getComponentId());
        }
        return added;
    }

    // Process Buffer Every 100ms
    @Scheduled(fixedDelay = 100)
    public void processBatch() {
        if (buffer.isEmpty())
            return;

        List<SignalRequest> batch = new ArrayList<>();
        // Drain up to 500 signals per batch
        buffer.drainTo(batch, 500);

        if (!batch.isEmpty()) {
            try {
                signalService.ingestBatch(batch);
                log.debug(" Processed batch of {} signals", batch.size());
            } catch (Exception e) {
                log.error(" Failed to process batch: {}", e.getMessage());
                // Re-queue failed signals (retry logic)
                batch.forEach(signal -> buffer.offer(signal));
            }
        }
    }

    // Print Throughput Every 5 Seconds
    @Scheduled(fixedDelay = 5000)
    public void printThroughputMetrics() {
        long now = System.currentTimeMillis();
        long elapsed = now - lastCounterReset.getAndSet(now);
        long count = signalCounter.getAndSet(0);

        double signalsPerSec = (count / (elapsed / 1000.0));

        log.info("📊 Throughput Metrics | " +
                "Signals/sec: {:.2f} | " +
                "Buffer size: {} | " +
                "Total processed: {}",
                signalsPerSec,
                buffer.size(),
                count);
    }

    public int getBufferSize() {
        return buffer.size();
    }

    public long getCurrentThroughput() {
        return signalCounter.get();
    }
}