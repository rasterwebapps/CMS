package com.cms.controller;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doNothing;
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
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.cms.dto.SemesterResultDto;
import com.cms.model.enums.ResultStatus;
import com.cms.service.SemesterResultService;

@WebMvcTest(controllers = SemesterResultController.class)
@AutoConfigureMockMvc(addFilters = false)
class SemesterResultControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private SemesterResultService semesterResultService;

    private SemesterResultDto dto() {
        return new SemesterResultDto(1L, 10L, 100L, "Alice Smith",
            5L, "2026-2027 ODD",
            new BigDecimal("100"), new BigDecimal("75"),
            new BigDecimal("75.00"), ResultStatus.PASS, false);
    }

    @Test
    void computeForEnrollment_returns200() throws Exception {
        when(semesterResultService.computeForEnrollment(10L)).thenReturn(dto());

        mockMvc.perform(post("/api/semester-results/compute")
                .param("enrollmentId", "10"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    void computeForTermInstance_returns200() throws Exception {
        doNothing().when(semesterResultService).computeResultsForTermInstance(anyLong());

        mockMvc.perform(post("/api/semester-results/compute-term")
                .param("termInstanceId", "5"))
            .andExpect(status().isOk());
    }

    @Test
    void getResults_byTermInstance_returns200() throws Exception {
        when(semesterResultService.getByTermInstance(5L)).thenReturn(List.of(dto()));

        mockMvc.perform(get("/api/semester-results").param("termInstanceId", "5"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].id").value(1));
    }

    @Test
    void getResults_byStudent_returns200() throws Exception {
        when(semesterResultService.getByStudent(100L)).thenReturn(List.of(dto()));

        mockMvc.perform(get("/api/semester-results").param("studentId", "100"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].studentId").value(100));
    }

    @Test
    void getByEnrollment_returns200() throws Exception {
        when(semesterResultService.getByEnrollment(10L)).thenReturn(dto());

        mockMvc.perform(get("/api/semester-results/enrollment/10"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.enrollmentId").doesNotExist());
    }

    @Test
    void lockResult_returns200() throws Exception {
        when(semesterResultService.lockResult(1L)).thenReturn(dto());

        mockMvc.perform(post("/api/semester-results/1/lock"))
            .andExpect(status().isOk());
    }
}
