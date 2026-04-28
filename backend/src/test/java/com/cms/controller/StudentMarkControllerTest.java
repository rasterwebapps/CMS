package com.cms.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.cms.dto.StudentMarkDto;
import com.cms.dto.StudentMarkRequest;
import com.cms.model.enums.MarkStatus;
import com.cms.service.StudentMarkService;
import com.fasterxml.jackson.databind.ObjectMapper;

@WebMvcTest(controllers = StudentMarkController.class)
@AutoConfigureMockMvc(addFilters = false)
class StudentMarkControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private StudentMarkService studentMarkService;

    private StudentMarkDto dto() {
        return new StudentMarkDto(1L, 1L, "Math", 1L, 1L, "Alice",
            MarkStatus.PRESENT, new BigDecimal("75"), new BigDecimal("100"), null);
    }

    @Test
    void shouldUpsert() throws Exception {
        StudentMarkRequest req = new StudentMarkRequest(1L, 1L, MarkStatus.PRESENT,
            new BigDecimal("75"), null);
        when(studentMarkService.upsert(any())).thenReturn(dto());
        mockMvc.perform(post("/api/student-marks")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    void shouldGetById() throws Exception {
        when(studentMarkService.getById(1L)).thenReturn(dto());
        mockMvc.perform(get("/api/student-marks/1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.markStatus").value("PRESENT"));
    }

    @Test
    void shouldGetByExamEvent() throws Exception {
        when(studentMarkService.getByExamEvent(1L)).thenReturn(List.of(dto()));
        mockMvc.perform(get("/api/student-marks?examEventId=1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].id").value(1));
    }

    @Test
    void shouldGetByEnrollment() throws Exception {
        when(studentMarkService.getByEnrollment(1L)).thenReturn(List.of(dto()));
        mockMvc.perform(get("/api/student-marks?enrollmentId=1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].studentName").value("Alice"));
    }
}
