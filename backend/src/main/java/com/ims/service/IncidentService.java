package com.ims.service;

import com.ims.dto.StatusUpdateRequest;
import com.ims.exception.IncidentNotFoundException;
import com.ims.exception.RCAIncompleteException;
import com.ims.model.WorkItem;
import com.ims.repository.RCARepository;
import com.ims.repository.WorkItemRepository;
import com.ims.statemachine.IncidentStateMachine;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class IncidentService {

    private final WorkItemRepository workItemRepository;
    private final RCARepository rcaRepository;
    private final IncidentStateMachine stateMachine;
    private final DashboardService dashboardService;
    private final SimpMessagingTemplate messagingTemplate;

    // Get All Incidents Paginated
    public Page<WorkItem> getAllIncidents(Pageable pageable) {
        return workItemRepository.findAll(pageable);
    }

    public WorkItem getIncidentById(Long id) {
        return workItemRepository.findById(id)
                .orElseThrow(() -> new IncidentNotFoundException("Incident not found: " + id));
    }

    public List<WorkItem> getActiveIncidents() {
        return workItemRepository.findByStatusNot(
                WorkItem.WorkItemStatus.CLOSED);
    }

    public List<WorkItem> getIncidentsByPriority(String priority) {
        return workItemRepository.findByPriority(priority);
    }

    @Transactional
    public WorkItem moveToInvestigating(Long id, StatusUpdateRequest request) {
        WorkItem workItem = getIncidentById(id);

        // Validate transition via State Machine
        stateMachine.transition(workItem,
                WorkItem.WorkItemStatus.INVESTIGATING);

        workItem.setStatus(WorkItem.WorkItemStatus.INVESTIGATING);
        workItem.setAssignedTo(request.getAssignedTo());
        workItem.setNotes(request.getNotes());

        WorkItem saved = workItemRepository.save(workItem);

        // Push live update to frontend via WebSocket
        pushLiveUpdate(saved);
        return saved;
    }

    @Transactional
    public WorkItem moveToResolved(Long id, StatusUpdateRequest request) {
        WorkItem workItem = getIncidentById(id);

        stateMachine.transition(workItem,
                WorkItem.WorkItemStatus.RESOLVED);

        workItem.setStatus(WorkItem.WorkItemStatus.RESOLVED);
        workItem.setNotes(request.getNotes());

        WorkItem saved = workItemRepository.save(workItem);
        pushLiveUpdate(saved);
        return saved;
    }

    @Transactional
    public WorkItem moveToClosed(Long id, StatusUpdateRequest request) {
        WorkItem workItem = getIncidentById(id);

        // Block close if RCA is missing
        boolean rcaExists = rcaRepository.existsByWorkItemId(id);
        if (!rcaExists) {
            throw new RCAIncompleteException("RCA is missing for incident: " + id);
        }

        stateMachine.transition(workItem,
                WorkItem.WorkItemStatus.CLOSED);

        workItem.setStatus(WorkItem.WorkItemStatus.CLOSED);
        workItem.setClosedAt(LocalDateTime.now());
        workItem.setNotes(request.getNotes());

        WorkItem saved = workItemRepository.save(workItem);

        // Refresh dashboard cache
        dashboardService.refreshCache();
        pushLiveUpdate(saved);
        return saved;
    }

    @Transactional
    public WorkItem updateStatus(Long id, StatusUpdateRequest request) {
        WorkItem workItem = getIncidentById(id);
        WorkItem.WorkItemStatus newStatus = WorkItem.WorkItemStatus.valueOf(request.getStatus());

        stateMachine.transition(workItem, newStatus);
        workItem.setStatus(newStatus);

        return workItemRepository.save(workItem);
    }

    // Delete Incident
    @Transactional
    public void deleteIncident(Long id) {
        WorkItem workItem = getIncidentById(id);
        workItemRepository.delete(workItem);
        log.info(" Incident deleted: {}", id);
    }

    // Push WebSocket Update
    private void pushLiveUpdate(WorkItem workItem) {
        messagingTemplate.convertAndSend("/topic/incidents", workItem);
        log.info(" WebSocket update pushed for incident: {}",
                workItem.getId());
    }
}