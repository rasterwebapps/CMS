package com.cms.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.cms.dto.ProgramDocumentRequirementsRequest;
import com.cms.dto.ProgramDocumentRequirementsResponse;
import com.cms.dto.ProgramRequest;
import com.cms.dto.ProgramResponse;
import com.cms.dto.ProgramStatusUpdateResponse;
import com.cms.exception.ResourceNotFoundException;
import com.cms.model.enums.ProgramStatus;
import com.cms.service.ProgramService;
import com.fasterxml.jackson.databind.ObjectMapper;

@WebMvcTest(controllers = ProgramController.class)
@AutoConfigureMockMvc(addFilters = false)
class ProgramControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private ProgramService programService;

    @Test
    void shouldCreateProgram() throws Exception {
        ProgramRequest request = new ProgramRequest("UG Program", "UG", 4, null, null, null, null, null);

        Instant now = Instant.now();
        ProgramResponse response = new ProgramResponse(1L, "UG Program", "UG", 4, 8, null, com.cms.model.enums.AssessmentPattern.TERM_BASED, Set.of(), Set.of(), null, null, null, now, now);

        when(programService.create(any(ProgramRequest.class))).thenReturn(response);

        mockMvc.perform(post("/programs")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").value(1))
            .andExpect(jsonPath("$.name").value("UG Program"))
            .andExpect(jsonPath("$.code").value("UG"))
            .andExpect(jsonPath("$.durationYears").value(4));

        verify(programService).create(any(ProgramRequest.class));
    }

    @Test
    void shouldReturnBadRequestWhenNameIsBlank() throws Exception {
        ProgramRequest request = new ProgramRequest("", "UG", 4, null, null, null, null, null);

        mockMvc.perform(post("/programs")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturnBadRequestWhenCodeIsBlank() throws Exception {
        ProgramRequest request = new ProgramRequest("UG Program", "", 4, null, null, null, null, null);

        mockMvc.perform(post("/programs")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturnBadRequestWhenDurationIsNull() throws Exception {
        String jsonRequest = """
            {
                "name": "UG Program",
                "code": "UG"
            }
            """;

        mockMvc.perform(post("/programs")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonRequest))
            .andExpect(status().isBadRequest());
    }

    @Test
    void shouldFindAllPrograms() throws Exception {
        Instant now = Instant.now();
        ProgramResponse prog1 = new ProgramResponse(1L, "Bachelor", "BACHELOR", 4, 8, null, com.cms.model.enums.AssessmentPattern.TERM_BASED, Set.of(), Set.of(), null, null, null, now, now);
        ProgramResponse prog2 = new ProgramResponse(2L, "Master", "MASTER", 2, 4, null, com.cms.model.enums.AssessmentPattern.TERM_BASED, Set.of(), Set.of(), null, null, null, now, now);

        when(programService.findAll()).thenReturn(List.of(prog1, prog2));

        mockMvc.perform(get("/programs"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(2))
            .andExpect(jsonPath("$[0].id").value(1))
            .andExpect(jsonPath("$[0].name").value("Bachelor"))
            .andExpect(jsonPath("$[1].id").value(2))
            .andExpect(jsonPath("$[1].name").value("Master"));

        verify(programService).findAll();
    }

    @Test
    void shouldReturnEmptyListWhenNoPrograms() throws Exception {
        when(programService.findAll()).thenReturn(List.of());

        mockMvc.perform(get("/programs"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(0));

        verify(programService).findAll();
    }

    @Test
    void shouldFindProgramById() throws Exception {
        Instant now = Instant.now();
        ProgramResponse response = new ProgramResponse(1L, "Bachelor", "BACHELOR", 4, 8, null, com.cms.model.enums.AssessmentPattern.TERM_BASED, Set.of(), Set.of(), null, null, null, now, now);

        when(programService.findById(1L)).thenReturn(response);

        mockMvc.perform(get("/programs/1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(1))
            .andExpect(jsonPath("$.name").value("Bachelor"))
            .andExpect(jsonPath("$.code").value("BACHELOR"));

        verify(programService).findById(1L);
    }

    @Test
    void shouldReturnNotFoundWhenProgramNotExists() throws Exception {
        when(programService.findById(999L))
            .thenThrow(new ResourceNotFoundException("Program not found with id: 999"));

        mockMvc.perform(get("/programs/999"))
            .andExpect(status().isNotFound());

        verify(programService).findById(999L);
    }

    @Test
    void shouldUpdateProgram() throws Exception {
        ProgramRequest request = new ProgramRequest("Bachelor Updated", "BACHELOR", 4, null, null, null, null, null);

        Instant now = Instant.now();
        ProgramResponse response = new ProgramResponse(1L, "Bachelor Updated", "BACHELOR", 4, 8, null, com.cms.model.enums.AssessmentPattern.TERM_BASED, Set.of(), Set.of(), null, null, null, now, now);

        when(programService.update(eq(1L), any(ProgramRequest.class))).thenReturn(response);

        mockMvc.perform(put("/programs/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(1))
            .andExpect(jsonPath("$.name").value("Bachelor Updated"))
            .andExpect(jsonPath("$.code").value("BACHELOR"));

        verify(programService).update(eq(1L), any(ProgramRequest.class));
    }

    @Test
    void shouldReturnNotFoundWhenUpdatingNonExistentProgram() throws Exception {
        ProgramRequest request = new ProgramRequest("Name", "CODE", 4, null, null, null, null, null);

        when(programService.update(eq(999L), any(ProgramRequest.class)))
            .thenThrow(new ResourceNotFoundException("Program not found with id: 999"));

        mockMvc.perform(put("/programs/999")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isNotFound());

        verify(programService).update(eq(999L), any(ProgramRequest.class));
    }

    @Test
    void shouldReturnBadRequestWhenUpdatingWithInvalidData() throws Exception {
        ProgramRequest request = new ProgramRequest("", "", 4, null, null, null, null, null);

        mockMvc.perform(put("/programs/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest());
    }

    @Test
    void shouldUpdateProgramStatus() throws Exception {
        Instant now = Instant.now();
        when(programService.updateStatus(eq(1L), any(com.cms.dto.ProgramStatusUpdateRequest.class)))
            .thenReturn(new ProgramStatusUpdateResponse(1L, ProgramStatus.INACTIVE, now));

        mockMvc.perform(patch("/programs/1/status")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"status": "INACTIVE", "reason": "retired"}
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(1))
            .andExpect(jsonPath("$.status").value("INACTIVE"));
    }

    @Test
    void shouldDeleteProgram() throws Exception {
        doNothing().when(programService).delete(1L);

        mockMvc.perform(delete("/programs/1"))
            .andExpect(status().isNoContent());

        verify(programService).delete(1L);
    }

    @Test
    void shouldReturnNotFoundWhenDeletingNonExistentProgram() throws Exception {
        doThrow(new ResourceNotFoundException("Program not found with id: 999"))
            .when(programService).delete(999L);

        mockMvc.perform(delete("/programs/999"))
            .andExpect(status().isNotFound());

        verify(programService).delete(999L);
    }

    @Test
    void shouldGetDocumentRequirements() throws Exception {
        when(programService.getDocumentRequirements(96L))
            .thenReturn(new ProgramDocumentRequirementsResponse(
                Set.of("TENTH_MARKSHEET", "TRANSCRIPT"), Set.of("AADHAR_CARD")));

        mockMvc.perform(get("/programs/96/document-types"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.mandatory").isArray())
            .andExpect(jsonPath("$.optional").isArray());

        verify(programService).getDocumentRequirements(96L);
    }

    @Test
    void shouldSetDocumentRequirements() throws Exception {
        when(programService.setDocumentRequirements(eq(96L), any(ProgramDocumentRequirementsRequest.class)))
            .thenReturn(new ProgramDocumentRequirementsResponse(
                Set.of("TENTH_MARKSHEET"), Set.of("AADHAR_CARD")));

        String payload = objectMapper.writeValueAsString(
            new ProgramDocumentRequirementsRequest(Set.of("TENTH_MARKSHEET"), Set.of("AADHAR_CARD")));

        mockMvc.perform(put("/programs/96/document-types")
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.mandatory").isArray())
            .andExpect(jsonPath("$.optional").isArray());

        verify(programService).setDocumentRequirements(eq(96L), any(ProgramDocumentRequirementsRequest.class));
    }

    @Test
    void shouldReturnBadRequestForUnreadableJson() throws Exception {
        mockMvc.perform(put("/programs/96/document-types")
                .contentType(MediaType.APPLICATION_JSON)
                .content("not-json"))
            .andExpect(status().isBadRequest());
    }
}

