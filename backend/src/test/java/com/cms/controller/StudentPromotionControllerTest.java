package com.cms.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;

import com.cms.dto.CohortTermOption;
import com.cms.dto.PromotionDecisionInput;
import com.cms.dto.PromotionExecuteRequest;
import com.cms.dto.PromotionExecuteResponse;
import com.cms.dto.PromotionPreviewRequest;
import com.cms.dto.PromotionPreviewResponse;
import com.cms.model.enums.PromotionOutcome;
import com.cms.service.StudentPromotionService;

@WebMvcTest(controllers = StudentPromotionController.class)
@AutoConfigureMockMvc(addFilters = false)
class StudentPromotionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private StudentPromotionService studentPromotionService;

    @Test
    void activeTerms_returnsOk() throws Exception {
        when(studentPromotionService.getActiveTermsForCohort(1L))
            .thenReturn(List.of(new CohortTermOption(1L, "2024-2025 ODD", 32)));

        mockMvc.perform(get("/student-promotions/active-terms").param("cohortId", "1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].termInstanceId").value(1))
            .andExpect(jsonPath("$[0].enrolledCount").value(32));

        verify(studentPromotionService).getActiveTermsForCohort(1L);
    }

    @Test
    void suggestedNextTerm_returnsOkWhenSuggestionExists() throws Exception {
        when(studentPromotionService.suggestNextTerm(1L))
            .thenReturn(new CohortTermOption(2L, "2024-2025 EVEN", 0));

        mockMvc.perform(get("/student-promotions/suggested-next-term").param("fromTermInstanceId", "1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.termInstanceId").value(2));
    }

    @Test
    void suggestedNextTerm_returnsNoContentWhenNoSuggestion() throws Exception {
        when(studentPromotionService.suggestNextTerm(1L)).thenReturn(null);

        mockMvc.perform(get("/student-promotions/suggested-next-term").param("fromTermInstanceId", "1"))
            .andExpect(status().isNoContent());
    }

    @Test
    void preview_returnsOk() throws Exception {
        PromotionPreviewRequest request = new PromotionPreviewRequest(1L, 1L, 2L);
        PromotionPreviewResponse response = new PromotionPreviewResponse(
            1L, "COHORT-1", 1L, "2024-2025 ODD", 2L, "2024-2025 EVEN", 8, 8, List.of());

        when(studentPromotionService.previewPromotion(any(PromotionPreviewRequest.class))).thenReturn(response);

        mockMvc.perform(post("/student-promotions/preview")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.cohortId").value(1))
            .andExpect(jsonPath("$.programTotalTerms").value(8));

        verify(studentPromotionService).previewPromotion(any(PromotionPreviewRequest.class));
    }

    @Test
    void execute_returnsOk() throws Exception {
        PromotionExecuteRequest request = new PromotionExecuteRequest(1L, 1L, 2L,
            List.of(new PromotionDecisionInput(1L, PromotionOutcome.PROMOTED, null)), false, false);
        PromotionExecuteResponse response = new PromotionExecuteResponse(1, 0, 0, 0, 0, List.of(), null, null);

        when(studentPromotionService.executePromotion(any(PromotionExecuteRequest.class), eq("system")))
            .thenReturn(response);

        mockMvc.perform(post("/student-promotions/execute")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.promotedCount").value(1));

        verify(studentPromotionService).executePromotion(any(PromotionExecuteRequest.class), eq("system"));
    }

    @Test
    void history_byCohort_returnsOk() throws Exception {
        when(studentPromotionService.getHistoryByCohort(1L)).thenReturn(List.of());

        mockMvc.perform(get("/student-promotions/history").param("cohortId", "1"))
            .andExpect(status().isOk());

        verify(studentPromotionService).getHistoryByCohort(1L);
    }

    @Test
    void history_byStudent_returnsOk() throws Exception {
        when(studentPromotionService.getHistoryByStudent(1L)).thenReturn(List.of());

        mockMvc.perform(get("/student-promotions/history").param("studentId", "1"))
            .andExpect(status().isOk());

        verify(studentPromotionService).getHistoryByStudent(1L);
    }

    @Test
    void history_withNoParams_returnsBadRequest() throws Exception {
        mockMvc.perform(get("/student-promotions/history"))
            .andExpect(status().isBadRequest());
    }
}
