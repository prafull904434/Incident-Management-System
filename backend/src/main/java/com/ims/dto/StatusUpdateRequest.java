package com.ims.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StatusUpdateRequest {

    // Target status to transition to
    // INVESTIGATING, RESOLVED, CLOSED
    @NotBlank(message = "Status is required")
    private String status;

    private String assignedTo;

    private String notes;
}