package com.ims.service;

import com.ims.model.WorkItem;
import com.ims.repository.SignalRepository;
import com.ims.repository.WorkItemRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.CacheManager;
import org.springframework.cache.Cache;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class DashboardService {

    private final WorkItemRepository workItemRepository;
    private final SignalRepository signalRepository;
    private final CacheManager cacheManager;

    private static final String CACHE_KEY = "dashboard:summary";

    public Map<String, Object> getSummary() {
        Map<String, Object> summary = new HashMap<>();

        // Status counts
        Map<String, Long> byStatus = new HashMap<>();
        byStatus.put("OPEN",
                workItemRepository.countByStatus(
                        WorkItem.WorkItemStatus.OPEN));
        byStatus.put("INVESTIGATING",
                workItemRepository.countByStatus(
                        WorkItem.WorkItemStatus.INVESTIGATING));
        byStatus.put("RESOLVED",
                workItemRepository.countByStatus(
                        WorkItem.WorkItemStatus.RESOLVED));
        byStatus.put("CLOSED",
                workItemRepository.countByStatus(
                        WorkItem.WorkItemStatus.CLOSED));

        // Priority counts
        Map<String, Long> byPriority = new HashMap<>();
        byPriority.put("P0",
                workItemRepository.countByPriority("P0"));
        byPriority.put("P1",
                workItemRepository.countByPriority("P1"));
        byPriority.put("P2",
                workItemRepository.countByPriority("P2"));

        // Total active
        long totalActive = workItemRepository.countByStatus(
                WorkItem.WorkItemStatus.OPEN) +
                workItemRepository.countByStatus(
                        WorkItem.WorkItemStatus.INVESTIGATING);

        summary.put("totalActive", totalActive);
        summary.put("byStatus", byStatus);
        summary.put("byPriority", byPriority);

        // Save to cache
        Cache cache = cacheManager.getCache(CACHE_KEY);
        if (cache != null) {
            cache.put(CACHE_KEY, summary);
        }
        log.info("📊 Dashboard summary refreshed and cached");

        return summary;
    }

    // Get Realtime State from Cache
    public Object getRealtimeState() {
        Cache cache = cacheManager.getCache(CACHE_KEY);
        if (cache != null) {
            Cache.ValueWrapper cached = cache.get(CACHE_KEY);
            if (cached != null && cached.get() != null) {
                log.info("⚡ Serving dashboard from cache");
                return cached.get();
            }
        }
        // Fallback to DB if cache miss
        log.info(" Cache miss — fetching from DB");
        return getSummary();
    }

    // Get Throughput Metrics
    public Map<String, Object> getMetrics() {
        Map<String, Object> metrics = new HashMap<>();
        metrics.put("totalSignals", signalRepository.count());
        metrics.put("p0Signals", signalRepository.countBySeverity("P0"));
        metrics.put("p1Signals", signalRepository.countBySeverity("P1"));
        metrics.put("p2Signals", signalRepository.countBySeverity("P2"));
        return metrics;
    }

    // Get Heatmap Data
    public Map<String, Object> getHeatmap() {
        Map<String, Object> heatmap = new HashMap<>();
        List<WorkItem> allIncidents = workItemRepository.findAll();

        Map<String, Long> componentFailures = new HashMap<>();
        allIncidents.forEach(incident -> {
            componentFailures.merge(
                    incident.getComponentId(), 1L, Long::sum);
        });

        heatmap.put("componentFailures", componentFailures);
        return heatmap;
    }

    // Refresh Cache
    public void refreshCache() {
        getSummary();
        log.info("🔄 Dashboard cache refreshed");
    }
}