package com.ims.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ims.dto.StatusUpdateRequest;
import com.ims.model.WorkItem;
import com.ims.service.IncidentService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Slice tests for {@link IncidentController}.
 *
 * <p>
 * Uses {@code @WebMvcTest} to spin up only the web layer — no database or
 * service logic is executed. The {@link IncidentService} is fully mocked.
 * </p>
 */
@WebMvcTest(controllers = IncidentController.class)
@DisplayName("IncidentController MVC")
class IncidentControllerMvcTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private IncidentService incidentService;

    private WorkItem sampleWorkItem(Long id, WorkItem.WorkItemStatus status) {
        return WorkItem.builder()
                .id(id)
                .componentId("CACHE_01")
                .componentType("DISTRIBUTED_CACHE")
                .status(status)
                .priority("P0")
                .signalCount(3)
                .build();
    }

    // GET /api/v1/incidents

    @Nested
    @DisplayName("GET /api/v1/incidents")
    class GetAllIncidents {

        @Test
        @DisplayName("returns 200 with paginated incidents")
        void returns200WithPage() throws Exception {
            WorkItem wi = sampleWorkItem(1L, WorkItem.WorkItemStatus.OPEN);
            when(incidentService.getAllIncidents(any())).thenReturn(
                    new PageImpl<>(List.of(wi), PageRequest.of(0, 10), 1));

            mockMvc.perform(get("/api/v1/incidents")
                    .param("page", "0").param("size", "10"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.content[0].id").value(1))
                    .andExpect(jsonPath("$.data.content[0].status").value("OPEN"));
        }
    }

    @Nested
    @DisplayName("GET /api/v1/incidents/{id}")
    class GetById {

        @Test
        @DisplayName("returns 200 with incident details")
        void returns200() throws Exception {
            when(incidentService.getIncidentById(1L))
                    .thenReturn(sampleWorkItem(1L, WorkItem.WorkItemStatus.OPEN));

            mockMvc.perform(get("/api/v1/incidents/1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.componentId").value("CACHE_01"));
        }

        @Test
        @DisplayName("returns 404 when incident not found (handled by GlobalExceptionHandler)")
        void returns404WhenNotFound() throws Exception {
            when(incidentService.getIncidentById(99L))
                    .thenThrow(new com.ims.exception.IncidentNotFoundException("Incident not found: 99"));

            mockMvc.perform(get("/api/v1/incidents/99"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.error").value("Incident not found: 99"));
        }
    }

    // GET /api/v1/incidents/active

    @Nested
    @DisplayName("GET /api/v1/incidents/active")
    class GetActiveIncidents {

        @Test
        @DisplayName("returns 200 with non-closed incidents")
        void returns200() throws Exception {
            WorkItem wi = sampleWorkItem(2L, WorkItem.WorkItemStatus.INVESTIGATING);
            when(incidentService.getActiveIncidents()).thenReturn(List.of(wi));

            mockMvc.perform(get("/api/v1/incidents/active"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data[0].status").value("INVESTIGATING"));
        }
    }

    // POST /api/v1/incidents/{id}/investigate

    @Nested
    @DisplayName("POST /api/v1/incidents/{id}/investigate")
    class Investigate {

        @Test
        @DisplayName("returns 200 after successful transition to INVESTIGATING")
        void returns200OnSuccess() throws Exception {
            WorkItem updated = sampleWorkItem(1L, WorkItem.WorkItemStatus.INVESTIGATING);
            when(incidentService.moveToInvestigating(eq(1L), any(StatusUpdateRequest.class)))
                    .thenReturn(updated);

            StatusUpdateRequest req = new StatusUpdateRequest();
            req.setAssignedTo("alice");
            req.setNotes("Investigating now");

            mockMvc.perform(post("/api/v1/incidents/1/investigate")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.status").value("INVESTIGATING"));
        }

        @Test
        @DisplayName("returns 400 on invalid state transition")
        void returns400OnInvalidTransition() throws Exception {
            when(incidentService.moveToInvestigating(eq(1L), any()))
                    .thenThrow(new com.ims.exception.InvalidStateTransitionException(
                            "Cannot transition from CLOSED to INVESTIGATING"));

            StatusUpdateRequest req = new StatusUpdateRequest();

            mockMvc.perform(post("/api/v1/incidents/1/investigate")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false));
        }
    }

    // POST /api/v1/incidents/{id}/resolve

    @Nested
    @DisplayName("POST /api/v1/incidents/{id}/resolve")
    class Resolve {

        @Test
        @DisplayName("returns 200 after successful transition to RESOLVED")
        void returns200OnSuccess() throws Exception {
            WorkItem updated = sampleWorkItem(2L, WorkItem.WorkItemStatus.RESOLVED);
            when(incidentService.moveToResolved(eq(2L), any(StatusUpdateRequest.class)))
                    .thenReturn(updated);

            StatusUpdateRequest req = new StatusUpdateRequest();
            req.setNotes("Fix applied");

            mockMvc.perform(post("/api/v1/incidents/2/resolve")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.status").value("RESOLVED"));
        }
    }

    @Nested
    @DisplayName("POST /api/v1/incidents/{id}/close")
    class Close {

        @Test
        @DisplayName("returns 200 when RCA exists and incident is closed")
        void returns200WhenRcaExists() throws Exception {
            WorkItem updated = sampleWorkItem(3L, WorkItem.WorkItemStatus.CLOSED);
            when(incidentService.moveToClosed(eq(3L), any(StatusUpdateRequest.class)))
                    .thenReturn(updated);

            StatusUpdateRequest req = new StatusUpdateRequest();

            mockMvc.perform(post("/api/v1/incidents/3/close")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.status").value("CLOSED"));
        }

        @Test
        @DisplayName("returns 422 when RCA is missing")
        void returns422WhenRcaMissing() throws Exception {
            when(incidentService.moveToClosed(eq(3L), any()))
                    .thenThrow(new com.ims.exception.RCAIncompleteException("RCA is missing for incident: 3"));

            StatusUpdateRequest req = new StatusUpdateRequest();

            mockMvc.perform(post("/api/v1/incidents/3/close")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isUnprocessableEntity())
                    .andExpect(jsonPath("$.error").value("RCA is missing for incident: 3"));
        }
    }

    // DELETE /api/v1/incidents/{id}

    @Nested
    @DisplayName("DELETE /api/v1/incidents/{id}")
    class DeleteIncident {

        @Test
        @DisplayName("returns 200 on successful delete")
        void returns200OnDelete() throws Exception {
            doNothing().when(incidentService).deleteIncident(1L);

            mockMvc.perform(delete("/api/v1/incidents/1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("Incident deleted successfully"));
        }
    }
}
