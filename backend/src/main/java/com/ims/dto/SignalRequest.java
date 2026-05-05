package com.ims.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SignalRequest {

    // Which component sent the signal
    @NotBlank(message = "Component ID is required")
    private String componentId;

    // Type of component
    @NotBlank(message = "Component type is required")
    private String componentType;

    @NotBlank(message = "Error code is required")
    private String errorCode;

    // Priority level: P0, P1, P2
    @NotBlank(message = "Severity is required")
    private String severity;

    @NotBlank(message = "Message is required")
    private String message;

    private LocalDateTime timestamp;

    private Map<String, String> metadata;
}