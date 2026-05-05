package com.ims.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RCARequest {

    @NotNull(message = "Incident start time is required")
    private LocalDateTime incidentStart;

    @NotNull(message = "Incident end time is required")
    private LocalDateTime incidentEnd;

    @NotBlank(message = "Root cause category is required")
    private String rootCauseCategory;

    @NotBlank(message = "Root cause description is required")
    private String rootCauseDescription;

    @NotBlank(message = "Fix applied is required")
    private String fixApplied;

    @NotBlank(message = "Prevention steps are required")
    private String preventionSteps;
}