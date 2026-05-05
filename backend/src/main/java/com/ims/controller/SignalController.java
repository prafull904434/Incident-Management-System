package com.ims.controller;

import com.ims.dto.ApiResponse;
import com.ims.dto.SignalRequest;
import com.ims.service.SignalService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import org.springframework.http.HttpStatus;

import java.time.Duration;

@RestController
@RequestMapping("/api/v1/signals")
public class SignalController {

    private final SignalService signalService;
    private final com.ims.ingestion.SignalIngestionPipeline pipeline;

    // Rate Limiter: 5000 requests per second maximum API throughput
    private final Bucket bucket;

    public SignalController(SignalService signalService, com.ims.ingestion.SignalIngestionPipeline pipeline) {
        this.signalService = signalService;
        this.pipeline = pipeline;
        Bandwidth limit = Bandwidth.classic(5000, Refill.greedy(5000, Duration.ofSeconds(1)));
        this.bucket = Bucket.builder().addLimit(limit).build();
    }

    // Ingest Single Signal (Async)
    @PostMapping("/ingest")
    public ResponseEntity<ApiResponse<?>> ingestSignal(
            @RequestBody SignalRequest request) {
        if (!bucket.tryConsume(1)) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .body(ApiResponse.error("Rate limit exceeded"));
        }
        boolean accepted = pipeline.enqueue(request);
        if (accepted) {
            return ResponseEntity.accepted()
                    .body(ApiResponse.success(null, "Signal accepted for async processing"));
        } else {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(ApiResponse.error("Ingestion buffer full"));
        }
    }

    // Ingest Batch Signals (Async)
    @PostMapping("/ingest/batch")
    public ResponseEntity<ApiResponse<?>> ingestBatch(
            @RequestBody List<SignalRequest> requests) {
        if (requests == null || requests.isEmpty()) {
            return ResponseEntity.accepted()
                    .body(ApiResponse.success(0, "Batch partially/fully accepted (0/0)"));
        }
        if (!bucket.tryConsume(requests.size())) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .body(ApiResponse.error("Rate limit exceeded"));
        }
        int acceptedCount = 0;
        for (SignalRequest req : requests) {
            if (pipeline.enqueue(req))
                acceptedCount++;
        }
        return ResponseEntity.accepted()
                .body(ApiResponse.success(acceptedCount,
                        "Batch partially/fully accepted (" + acceptedCount + "/" + requests.size() + ")"));
    }

    // Get Signals by Work Item ID
    @GetMapping("/{workItemId}")
    public ResponseEntity<ApiResponse<?>> getSignalsByWorkItem(
            @PathVariable String workItemId) {
        return ResponseEntity.ok(
                ApiResponse.success(
                        signalService.getSignalsByWorkItemId(workItemId),
                        "Signals fetched successfully"));
    }
}