package com.ims.controller;

import com.ims.dto.ApiResponse;
import com.ims.dto.StatusUpdateRequest;
import com.ims.service.IncidentService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/incidents")
@RequiredArgsConstructor
public class IncidentController {

    private final IncidentService incidentService;

    // Get All Incidents (Paginated)
    @GetMapping
    public ResponseEntity<ApiResponse<?>> getAllIncidents(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(
                ApiResponse.success(
                        incidentService.getAllIncidents(pageable),
                        "Incidents fetched successfully"));
    }

    // Get Incident By ID
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<?>> getIncidentById(
            @PathVariable Long id) {
        return ResponseEntity.ok(
                ApiResponse.success(
                        incidentService.getIncidentById(id),
                        "Incident fetched successfully"));
    }

    // Get Active Incidents
    @GetMapping("/active")
    public ResponseEntity<ApiResponse<?>> getActiveIncidents() {
        return ResponseEntity.ok(
                ApiResponse.success(
                        incidentService.getActiveIncidents(),
                        "Active incidents fetched successfully"));
    }

    // Get Incidents By Priority
    @GetMapping("/severity/{priority}")
    public ResponseEntity<ApiResponse<?>> getIncidentsByPriority(
            @PathVariable String priority) {
        return ResponseEntity.ok(
                ApiResponse.success(
                        incidentService.getIncidentsByPriority(priority),
                        "Incidents fetched by priority"));
    }

    // OPEN -INVESTIGATING
    @PostMapping("/{id}/investigate")
    public ResponseEntity<ApiResponse<?>> investigate(
            @PathVariable Long id,
            @RequestBody StatusUpdateRequest request) {
        return ResponseEntity.ok(
                ApiResponse.success(
                        incidentService.moveToInvestigating(id, request),
                        "Incident moved to INVESTIGATING"));
    }

    // INVESTIGATING -RESOLVED
    @PostMapping("/{id}/resolve")
    public ResponseEntity<ApiResponse<?>> resolve(
            @PathVariable Long id,
            @RequestBody StatusUpdateRequest request) {
        return ResponseEntity.ok(
                ApiResponse.success(
                        incidentService.moveToResolved(id, request),
                        "Incident moved to RESOLVED"));
    }

    // RESOLVED - CLOSED (requires RCA)
    @PostMapping("/{id}/close")
    public ResponseEntity<ApiResponse<?>> close(
            @PathVariable Long id,
            @RequestBody StatusUpdateRequest request) {
        return ResponseEntity.ok(
                ApiResponse.success(
                        incidentService.moveToClosed(id, request),
                        "Incident moved to CLOSED"));
    }

    // Update Status
    @PatchMapping("/{id}/status")
    public ResponseEntity<ApiResponse<?>> updateStatus(
            @PathVariable Long id,
            @RequestBody StatusUpdateRequest request) {
        return ResponseEntity.ok(
                ApiResponse.success(
                        incidentService.updateStatus(id, request),
                        "Status updated successfully"));
    }

    // Delete Incident (Admin)
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<?>> deleteIncident(
            @PathVariable Long id) {
        incidentService.deleteIncident(id);
        return ResponseEntity.ok(
                ApiResponse.success(null, "Incident deleted successfully"));
    }
}