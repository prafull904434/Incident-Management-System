package com.ims.controller;

import com.ims.dto.ApiResponse;
import com.ims.dto.RCARequest;
import com.ims.service.RCAService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/incidents")
@RequiredArgsConstructor
public class RCAController {

        private final RCAService rcaService;

        // Submit RCA
        @PostMapping("/{id}/rca")
        public ResponseEntity<ApiResponse<?>> submitRCA(
                        @PathVariable Long id,
                        @RequestBody RCARequest request) {

                Object response = rcaService.submitRCA(id, request);

                return ResponseEntity.ok(
                                ApiResponse.success(response, "RCA submitted successfully"));
        }

        // Get RCA by Incident ID
        @GetMapping("/{id}/rca")
        public ResponseEntity<ApiResponse<?>> getRCA(
                        @PathVariable Long id) {

                Object response = rcaService.getRCAByIncidentId(id);

                return ResponseEntity.ok(
                                ApiResponse.success(response, "RCA fetched successfully"));
        }

        // Update RCA
        @PutMapping("/{id}/rca")
        public ResponseEntity<ApiResponse<?>> updateRCA(
                        @PathVariable Long id,
                        @RequestBody RCARequest request) {

                Object response = rcaService.updateRCA(id, request);

                return ResponseEntity.ok(
                                ApiResponse.success(response, "RCA updated successfully"));
        }

        // Get MTTR
        @GetMapping("/{id}/mttr")
        public ResponseEntity<ApiResponse<?>> getMTTR(
                        @PathVariable Long id) {

                Object response = rcaService.getMTTR(id);

                return ResponseEntity.ok(
                                ApiResponse.success(response, "MTTR calculated successfully"));
        }
}