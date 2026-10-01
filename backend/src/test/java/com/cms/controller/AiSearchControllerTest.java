package com.cms.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.cms.ai.AiStudentSearchService;
import com.cms.ai.rag.DocumentRagSearchService;
import com.cms.dto.AdmissionDocumentRagResult;
import com.cms.dto.StudentResponse;
import com.cms.model.enums.DocumentType;
import com.cms.model.enums.StudentStatus;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Security filters are disabled here (matching every other {@code @WebMvcTest} controller test
 * in this codebase, e.g. ProgramControllerTest/GuardianFeeControllerTest) -- this suite verifies
 * request/response wiring, not the {@code @PreAuthorize("STUDENT_AI_SEARCH_VIEW")} gate itself.
 * That gate is verified manually per the AI Smart Search plan's verification steps (log in as a
 * role without the permission, confirm the nav entry/route are inaccessible).
 */
@WebMvcTest(controllers = AiSearchController.class)
@AutoConfigureMockMvc(addFilters = false)
class AiSearchControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AiStudentSearchService aiStudentSearchService;

    @MockitoBean
    private DocumentRagSearchService documentRagSearchService;

    @Test
    void searchStudents_returnsTheServiceResult() throws Exception {
        StudentResponse student = new StudentResponse(
            1L, "R001", "Asha", "Menon", "Asha Menon", "asha@example.com", "9000000000",
            null, null, null, null, null, null, 1, null, null, StudentStatus.ACTIVE,
            null, null, null, null, null, null, null, null, null, null, null, null,
            null, null, null, null, null, null, null, null, null, null, null);

        when(aiStudentSearchService.search(eq("find students who studied at St. Mary's School"), any(Pageable.class)))
            .thenReturn(new PageImpl<>(java.util.List.of(student)));

        mockMvc.perform(post("/ai-search/students")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(
                    new com.cms.dto.AiStudentSearchRequest("find students who studied at St. Mary's School"))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content[0].firstName").value("Asha"));
    }

    @Test
    void searchStudents_returns400ForAnUnparseableQuery() throws Exception {
        when(aiStudentSearchService.search(eq("gibberish"), any(Pageable.class)))
            .thenThrow(new IllegalArgumentException("Couldn't understand that query. Try rephrasing it."));

        mockMvc.perform(post("/ai-search/students")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new com.cms.dto.AiStudentSearchRequest("gibberish"))))
            .andExpect(status().isBadRequest());
    }

    @Test
    void searchAdmissionDocuments_returnsTheServiceResult() throws Exception {
        AdmissionDocumentRagResult result = new AdmissionDocumentRagResult(
            5L, 50L, DocumentType.TRANSFER_CERTIFICATE, "tc.pdf", "Jane Doe", "ADM-001",
            "Transfer certificate issued to Jane Doe by St. Mary's School", 0.87);

        when(documentRagSearchService.search("where did Jane study before?"))
            .thenReturn(java.util.List.of(result));

        mockMvc.perform(post("/ai-search/admission-documents")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(
                    new com.cms.dto.AiDocumentSearchRequest("where did Jane study before?"))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].studentName").value("Jane Doe"))
            .andExpect(jsonPath("$[0].similarity").value(0.87));
    }

    @Test
    void searchAdmissionDocuments_returnsEmptyListWhenNothingClearsTheSimilarityThreshold() throws Exception {
        when(documentRagSearchService.search("irrelevant question")).thenReturn(java.util.List.of());

        mockMvc.perform(post("/ai-search/admission-documents")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new com.cms.dto.AiDocumentSearchRequest("irrelevant question"))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isEmpty());
    }
}
