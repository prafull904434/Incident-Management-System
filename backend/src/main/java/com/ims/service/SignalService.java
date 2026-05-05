package com.ims.service;

import com.github.benmanes.caffeine.cache.Cache;
import com.ims.dto.SignalRequest;
import com.ims.model.Signal;
import com.ims.model.WorkItem;
import com.ims.repository.SignalRepository;
import com.ims.repository.WorkItemRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class SignalService {

    private final SignalRepository signalRepository;
    private final WorkItemRepository workItemRepository;
    private final AlertService alertService;
    private final Cache<String, Long> signalComponentWorkItemDebouncer;

    public List<Signal> getSignalsByWorkItemId(String workItemId) {
        Long id = Long.parseLong(workItemId.trim());
        return signalRepository.findByWorkItemId(id);
    }

    @Transactional
    public void ingestBatch(List<SignalRequest> batch) {
        if (batch == null || batch.isEmpty()) {
            return;
        }
        for (SignalRequest req : batch) {
            ingestOne(req);
        }
    }

    private void ingestOne(SignalRequest req) {
        LocalDateTime ts = req.getTimestamp() != null ? req.getTimestamp() : LocalDateTime.now();
        String compId = req.getComponentId();

        Long cachedId = signalComponentWorkItemDebouncer.getIfPresent(compId);
        Optional<WorkItem> workItemOpt = Optional.empty();
        boolean debounced = false;

        if (cachedId != null) {
            workItemOpt = workItemRepository.findById(cachedId)
                    .filter(w -> !WorkItem.WorkItemStatus.CLOSED.equals(w.getStatus()));
            debounced = workItemOpt.isPresent();
        }
        if (workItemOpt.isEmpty()) {
            workItemOpt = workItemRepository.findByComponentIdAndStatusNot(compId,
                    WorkItem.WorkItemStatus.CLOSED);
            debounced = false;
        }

        WorkItem workItem = workItemOpt.orElse(null);
        if (workItem == null) {
            WorkItem created = WorkItem.builder()
                    .componentId(compId)
                    .componentType(req.getComponentType())
                    .priority(normalizeSeverity(req.getSeverity()))
                    .status(WorkItem.WorkItemStatus.OPEN)
                    .signalCount(1)
                    .build();
            workItem = workItemRepository.save(created);
            alertService.sendAlert(workItem);
            debounced = false;
            signalComponentWorkItemDebouncer.put(compId, workItem.getId());
            persistSignal(workItem.getId(), req, ts, debounced);
            log.info(" New WorkItem created: {} for component: {}", workItem.getId(), compId);
            return;
        }

        signalComponentWorkItemDebouncer.put(compId, workItem.getId());

        int next = workItem.getSignalCount() != null ? workItem.getSignalCount() + 1 : 1;
        workItem.setSignalCount(next);
        workItem = workItemRepository.save(workItem);

        persistSignal(workItem.getId(), req, ts, debounced);
    }

    private void persistSignal(Long workItemId, SignalRequest req, LocalDateTime ts, boolean debounced) {
        Signal sig = Signal.builder()
                .workItemId(workItemId)
                .componentId(req.getComponentId())
                .componentType(req.getComponentType())
                .errorCode(req.getErrorCode())
                .severity(normalizeSeverity(req.getSeverity()))
                .message(req.getMessage())
                .timestamp(ts)
                .metadata(req.getMetadata())
                .debounced(debounced)
                .build();
        signalRepository.save(sig);
    }

    private static String normalizeSeverity(String severity) {
        return severity != null ? severity.toUpperCase() : "UNKNOWN";
    }
}
