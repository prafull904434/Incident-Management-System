package com.ims.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ims.dto.SignalRequest;
import com.ims.ingestion.SignalIngestionPipeline;
import com.ims.service.SignalService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = SignalController.class)
class SignalControllerMvcTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private SignalService signalService;
    @MockBean
    private SignalIngestionPipeline pipeline;

    @Test
    void ingestReturnsAcceptedWhenPipelineAccepts() throws Exception {
        when(pipeline.enqueue(any(SignalRequest.class))).thenReturn(true);

        SignalRequest body = SignalRequest.builder()
                .componentId("X")
                .componentType("API")
                .errorCode("E")
                .severity("P1")
                .message("err")
                .timestamp(LocalDateTime.now())
                .build();

        mockMvc.perform(post("/api/v1/signals/ingest")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.success").value(true));

        verify(pipeline).enqueue(any(SignalRequest.class));
    }

    @Test
    void ingestReturns503WhenBufferFull() throws Exception {
        when(pipeline.enqueue(any(SignalRequest.class))).thenReturn(false);

        SignalRequest body = SignalRequest.builder()
                .componentId("X")
                .componentType("API")
                .errorCode("E")
                .severity("P1")
                .message("err")
                .timestamp(LocalDateTime.now())
                .build();

        mockMvc.perform(post("/api/v1/signals/ingest")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void batchIngestHandlesEmptyPayload() throws Exception {
        mockMvc.perform(post("/api/v1/signals/ingest/batch")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("[]"))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.data").value(0));
    }

    @Test
    void getSignalsDelegatesToService() throws Exception {
        mockMvc.perform(get("/api/v1/signals/42"))
                .andExpect(status().isOk());

        verify(signalService).getSignalsByWorkItemId("42");
    }
}
