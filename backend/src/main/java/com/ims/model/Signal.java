package com.ims.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
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
@Document(collection = "signals")
public class Signal {

    @Id
    private String id;

    // Link to Work Item in PostgreSQL
    private Long workItemId;

    private String componentId;

    private String componentType;

    private String errorCode;

    // P0, P1, P2
    private String severity;

    private String message;

    private LocalDateTime timestamp;

    private Map<String, String> metadata;

    private boolean debounced;
}