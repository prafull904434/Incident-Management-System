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
@Table(name = "rca_records")
public class RCA {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private Long workItemId;

    @Column(nullable = false)
    private LocalDateTime incidentStart;

    @Column(nullable = false)
    private LocalDateTime incidentEnd;

    @Column(nullable = false)
    private String rootCauseCategory;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String rootCauseDescription;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String fixApplied;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String preventionSteps;

    // MTTR in seconds
    private Long mttrSeconds;

    private String mttrFormatted;

    // When RCA was submitted
    private LocalDateTime submittedAt;

    @PrePersist
    protected void onCreate() {
        submittedAt = LocalDateTime.now();
        calculateMTTR();
    }

    @PreUpdate
    protected void onUpdate() {
        calculateMTTR();
    }

    private void calculateMTTR() {
        if (incidentStart != null && incidentEnd != null) {
            mttrSeconds = java.time.Duration.between(
                    incidentStart, incidentEnd).getSeconds();
            mttrFormatted = formatMTTR(mttrSeconds);
        }
    }

    // Format MTTR
    private String formatMTTR(long seconds) {
        long hours = seconds / 3600;
        long minutes = (seconds % 3600) / 60;
        long secs = seconds % 60;

        if (hours > 0) {
            return hours + " hours " + minutes + " minutes";
        } else if (minutes > 0) {
            return minutes + " minutes " + secs + " seconds";
        } else {
            return secs + " seconds";
        }
    }
}