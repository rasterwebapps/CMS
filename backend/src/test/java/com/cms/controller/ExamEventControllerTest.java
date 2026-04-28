package com.cms.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.cms.dto.ExamEventDto;
import com.cms.dto.ExamEventRequest;
import com.cms.model.enums.ExamSessionStatus;
import com.cms.model.enums.ExamSessionType;
import com.cms.service.ExamEventService;
import com.fasterxml.jackson.databind.ObjectMapper;

@WebMvcTest(controllers = ExamEventController.class)
@AutoConfigureMockMvc(addFilters = false)
class ExamEventControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private ExamEventService examEventService;

    private ExamEventDto dto() {
        return new ExamEventDto(1L, 1L, ExamSessionType.FINAL, ExamSessionStatus.PUBLISHED,
            1L, "Math", "MATH101", LocalDate.of(2024, 11, 10),
            new BigDecimal("100"), new BigDecimal("40"));
    }

    @Test
    void shouldCreate() throws Exception {
        ExamEventRequest req = new ExamEventRequest(1L, 1L, LocalDate.of(2024, 11, 10),
            new BigDecimal("100"), new BigDecimal("40"));
        when(examEventService.create(any())).thenReturn(dto());
        mockMvc.perform(post("/api/exam-events")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    void shouldGetById() throws Exception {
        when(examEventService.getById(1L)).thenReturn(dto());
        mockMvc.perform(get("/api/exam-events/1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.subjectCode").value("MATH101"));
    }

    @Test
    void shouldGetByExamSession() throws Exception {
        when(examEventService.getByExamSession(1L)).thenReturn(List.of(dto()));
        mockMvc.perform(get("/api/exam-events?examSessionId=1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].id").value(1));
    }

    @Test
    void shouldGetByTermInstance() throws Exception {
        when(examEventService.getByTermInstance(1L)).thenReturn(List.of(dto()));
        mockMvc.perform(get("/api/exam-events?termInstanceId=1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].id").value(1));
    }

    @Test
    void shouldUpdate() throws Exception {
        ExamEventRequest req = new ExamEventRequest(1L, 1L, LocalDate.of(2024, 11, 11),
            new BigDecimal("100"), new BigDecimal("40"));
        when(examEventService.update(any(), any())).thenReturn(dto());
        mockMvc.perform(put("/api/exam-events/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
            .andExpect(status().isOk());
    }

    @Test
    void shouldDelete() throws Exception {
        mockMvc.perform(delete("/api/exam-events/1"))
            .andExpect(status().isNoContent());
    }
}
