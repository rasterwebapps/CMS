package com.cms.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.cms.dto.ExamSessionDto;
import com.cms.dto.ExamSessionRequest;
import com.cms.model.enums.ExamSessionStatus;
import com.cms.model.enums.ExamSessionType;
import com.cms.service.ExamSessionService;
import com.fasterxml.jackson.databind.ObjectMapper;

@WebMvcTest(controllers = ExamSessionController.class)
@AutoConfigureMockMvc(addFilters = false)
class ExamSessionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private ExamSessionService examSessionService;

    private ExamSessionDto dto() {
        return new ExamSessionDto(1L, 1L, "2024 Sem 1", ExamSessionType.FINAL,
            ExamSessionStatus.DRAFT, LocalDate.of(2024, 11, 1), LocalDate.of(2024, 11, 30));
    }

    @Test
    void shouldCreateExamSession() throws Exception {
        ExamSessionRequest req = new ExamSessionRequest(1L, ExamSessionType.FINAL,
            LocalDate.of(2024, 11, 1), LocalDate.of(2024, 11, 30));
        when(examSessionService.create(any())).thenReturn(dto());

        mockMvc.perform(post("/api/exam-sessions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    void shouldGetById() throws Exception {
        when(examSessionService.getById(1L)).thenReturn(dto());
        mockMvc.perform(get("/api/exam-sessions/1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.sessionType").value("FINAL"));
    }

    @Test
    void shouldGetByTermInstance() throws Exception {
        when(examSessionService.getByTermInstance(1L)).thenReturn(List.of(dto()));
        mockMvc.perform(get("/api/exam-sessions?termInstanceId=1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].id").value(1));
    }

    @Test
    void shouldPublish() throws Exception {
        ExamSessionDto published = new ExamSessionDto(1L, 1L, "2024 Sem 1", ExamSessionType.FINAL,
            ExamSessionStatus.PUBLISHED, LocalDate.of(2024, 11, 1), LocalDate.of(2024, 11, 30));
        when(examSessionService.publish(1L)).thenReturn(published);
        mockMvc.perform(post("/api/exam-sessions/1/publish"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("PUBLISHED"));
    }

    @Test
    void shouldLock() throws Exception {
        ExamSessionDto locked = new ExamSessionDto(1L, 1L, "2024 Sem 1", ExamSessionType.FINAL,
            ExamSessionStatus.LOCKED, LocalDate.of(2024, 11, 1), LocalDate.of(2024, 11, 30));
        when(examSessionService.lock(1L)).thenReturn(locked);
        mockMvc.perform(post("/api/exam-sessions/1/lock"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("LOCKED"));
    }
}
