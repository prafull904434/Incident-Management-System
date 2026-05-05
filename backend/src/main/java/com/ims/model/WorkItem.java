package com.ims.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "work_items")
public class WorkItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String componentId;

    @Column(nullable = false)
    private String componentType;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private WorkItemStatus status;

    // P0, P1, P2
    @Column(nullable = false)
    private String priority;

    private String assignedTo;

    private String notes;

    @Column(nullable = false)
    @Builder.Default
    private Integer signalCount = 0;

    // When first signal arrived
    @Column(nullable = false)
    private LocalDateTime createdAt;

    // Last status update time
    private LocalDateTime updatedAt;

    private LocalDateTime closedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (status == null)
            status = WorkItemStatus.OPEN;
        if (signalCount == null)
            signalCount = 0;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // Status Enum
    public enum WorkItemStatus {
        OPEN,
        INVESTIGATING,
        RESOLVED,
        CLOSED
    }
}