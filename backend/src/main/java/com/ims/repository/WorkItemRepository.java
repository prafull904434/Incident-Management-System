package com.ims.repository;

import com.ims.model.WorkItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WorkItemRepository extends JpaRepository<WorkItem, Long> {

    // Find Active Incidents
    List<WorkItem> findByStatusNot(WorkItem.WorkItemStatus status);

    List<WorkItem> findByStatus(WorkItem.WorkItemStatus status);

    List<WorkItem> findByPriority(String priority);

    // Find By Component ID
    Optional<WorkItem> findByComponentIdAndStatusNot(
            String componentId,
            WorkItem.WorkItemStatus status);

    // Count By Status
    long countByStatus(WorkItem.WorkItemStatus status);

    long countByPriority(String priority);

    // Dashboard Summary Query
    @Query("SELECT w.status, COUNT(w) FROM WorkItem w GROUP BY w.status")
    List<Object[]> countGroupByStatus();

    @Query("SELECT w.priority, COUNT(w) FROM WorkItem w GROUP BY w.priority")
    List<Object[]> countGroupByPriority();
}