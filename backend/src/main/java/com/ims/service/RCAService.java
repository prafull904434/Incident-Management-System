package com.ims.service;

import com.ims.dto.RCARequest;
import com.ims.exception.IncidentNotFoundException;
import com.ims.model.RCA;
import com.ims.model.WorkItem;
import com.ims.repository.RCARepository;
import com.ims.repository.WorkItemRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class RCAService {

    private final RCARepository rcaRepository;
    private final WorkItemRepository workItemRepository;

    @Transactional
    public RCA submitRCA(Long incidentId, RCARequest request) {
        WorkItem workItem = workItemRepository.findById(incidentId)
                .orElseThrow(() -> new IncidentNotFoundException("Incident not found: " + incidentId));

        if (rcaRepository.existsByWorkItemId(incidentId)) {
            throw new IllegalStateException("RCA already exists for incident: " + incidentId);
        }

        RCA rca = RCA.builder()
                .workItemId(incidentId)
                .incidentStart(request.getIncidentStart())
                .incidentEnd(request.getIncidentEnd())
                .rootCauseCategory(request.getRootCauseCategory())
                .rootCauseDescription(request.getRootCauseDescription())
                .fixApplied(request.getFixApplied())
                .preventionSteps(request.getPreventionSteps())
                .build();

        return rcaRepository.save(rca);
    }

    public RCA getRCAByIncidentId(Long incidentId) {
        return rcaRepository.findByWorkItemId(incidentId)
                .orElseThrow(() -> new RuntimeException("RCA not found for incident: " + incidentId));
    }

    @Transactional
    public RCA updateRCA(Long incidentId, RCARequest request) {
        RCA rca = getRCAByIncidentId(incidentId);

        rca.setIncidentStart(request.getIncidentStart());
        rca.setIncidentEnd(request.getIncidentEnd());
        rca.setRootCauseCategory(request.getRootCauseCategory());
        rca.setRootCauseDescription(request.getRootCauseDescription());
        rca.setFixApplied(request.getFixApplied());
        rca.setPreventionSteps(request.getPreventionSteps());

        return rcaRepository.save(rca);
    }

    public Long getMTTR(Long incidentId) {
        RCA rca = getRCAByIncidentId(incidentId);
        return rca.getMttrSeconds();
    }
}