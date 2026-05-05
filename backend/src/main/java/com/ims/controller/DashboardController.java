package com.ims.controller;

import com.ims.dto.ApiResponse;
import com.ims.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    // Summary
    @GetMapping("/summary")
    public ResponseEntity<ApiResponse<?>> getSummary() {
        return ResponseEntity.ok(
                ApiResponse.success(
                        dashboardService.getRealtimeState(),
                        "Dashboard summary retrieved successfully"));
    }

    // Realtime State
    @GetMapping("/realtime")
    public ResponseEntity<ApiResponse<?>> getRealtimeState() {
        return ResponseEntity.ok(
                ApiResponse.success(
                        dashboardService.getRealtimeState(),
                        "Realtime state fetched from cache"));
    }

    // Throughput Metrics
    @GetMapping("/metrics")
    public ResponseEntity<ApiResponse<?>> getMetrics() {
        return ResponseEntity.ok(
                ApiResponse.success(
                        dashboardService.getMetrics(),
                        "Metrics fetched successfully"));
    }

    // Component Failure Heatmap
    @GetMapping("/heatmap")
    public ResponseEntity<ApiResponse<?>> getHeatmap() {
        return ResponseEntity.ok(
                ApiResponse.success(
                        dashboardService.getHeatmap(),
                        "Heatmap data fetched successfully"));
    }
}