package com.ims.repository;

import com.ims.model.Signal;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface SignalRepository extends MongoRepository<Signal, String> {

        // Find Signals by Work Item
        List<Signal> findByWorkItemId(Long workItemId);

        List<Signal> findByComponentId(String componentId);

        // Count Signals in Time Window
        long countByComponentIdAndTimestampBetween(
                        String componentId,
                        LocalDateTime start,
                        LocalDateTime end);

        List<Signal> findByComponentIdAndTimestampBetween(
                        String componentId,
                        LocalDateTime start,
                        LocalDateTime end);

        // Count by Severity
        long countBySeverity(String severity);
}