package com.cms.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.cms.dto.CurriculumVersionDto;
import com.cms.dto.CurriculumVersionRequest;
import com.cms.exception.ResourceNotFoundException;
import com.cms.service.CurriculumVersionService;
import com.fasterxml.jackson.databind.ObjectMapper;

@WebMvcTest(controllers = CurriculumVersionController.class)
@AutoConfigureMockMvc(addFilters = false)
class CurriculumVersionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private CurriculumVersionService curriculumVersionService;

    @Test
    void shouldCreateCurriculumVersion() throws Exception {
        CurriculumVersionRequest request = new CurriculumVersionRequest(1L, "BSCN-2026", 1L, true);
        CurriculumVersionDto dto = createDto(1L, 1L, "BSc Nursing", "BSCN-2026", 1L, "2026-2027", true);

        when(curriculumVersionService.createCurriculumVersion(any(CurriculumVersionRequest.class))).thenReturn(dto);

        mockMvc.perform(post("/curriculum-versions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").value(1))
            .andExpect(jsonPath("$.versionName").value("BSCN-2026"))
            .andExpect(jsonPath("$.isActive").value(true));

        verify(curriculumVersionService).createCurriculumVersion(any(CurriculumVersionRequest.class));
    }

    @Test
    void shouldReturnBadRequestWhenVersionNameIsBlank() throws Exception {
        CurriculumVersionRequest request = new CurriculumVersionRequest(1L, "", 1L, true);

        mockMvc.perform(post("/curriculum-versions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturnBadRequestWhenProgramIdIsNull() throws Exception {
        String json = """
            {"versionName": "BSCN-2026", "effectiveFromAcademicYearId": 1, "isActive": true}
            """;

        mockMvc.perform(post("/curriculum-versions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
            .andExpect(status().isBadRequest());
    }

    @Test
    void shouldGetCurriculumVersionsByProgram() throws Exception {
        CurriculumVersionDto dto = createDto(1L, 1L, "BSc Nursing", "BSCN-2026", 1L, "2026-2027", true);

        when(curriculumVersionService.getCurriculumVersionsByProgram(1L)).thenReturn(List.of(dto));

        mockMvc.perform(get("/curriculum-versions").param("programId", "1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].versionName").value("BSCN-2026"));

        verify(curriculumVersionService).getCurriculumVersionsByProgram(1L);
    }

    @Test
    void shouldGetCurriculumVersionById() throws Exception {
        CurriculumVersionDto dto = createDto(1L, 1L, "BSc Nursing", "BSCN-2026", 1L, "2026-2027", true);

        when(curriculumVersionService.getById(1L)).thenReturn(dto);

        mockMvc.perform(get("/curriculum-versions/1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(1))
            .andExpect(jsonPath("$.versionName").value("BSCN-2026"));

        verify(curriculumVersionService).getById(1L);
    }

    @Test
    void shouldReturnNotFoundWhenCurriculumVersionNotExists() throws Exception {
        when(curriculumVersionService.getById(999L))
            .thenThrow(new ResourceNotFoundException("Curriculum version not found with id: 999"));

        mockMvc.perform(get("/curriculum-versions/999"))
            .andExpect(status().isNotFound());
    }

    @Test
    void shouldUpdateCurriculumVersion() throws Exception {
        CurriculumVersionRequest request = new CurriculumVersionRequest(1L, "BSCN-2026-Updated", 1L, false);
        CurriculumVersionDto dto = createDto(1L, 1L, "BSc Nursing", "BSCN-2026-Updated", 1L, "2026-2027", false);

        when(curriculumVersionService.update(eq(1L), any(CurriculumVersionRequest.class))).thenReturn(dto);

        mockMvc.perform(put("/curriculum-versions/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.versionName").value("BSCN-2026-Updated"))
            .andExpect(jsonPath("$.isActive").value(false));

        verify(curriculumVersionService).update(eq(1L), any(CurriculumVersionRequest.class));
    }

    @Test
    void shouldCloneCurriculumVersion() throws Exception {
        CurriculumVersionDto dto = createDto(2L, 1L, "BSc Nursing", "BSCN-2027", 2L, "2027-2028", true);

        when(curriculumVersionService.cloneCurriculumVersion(1L, "BSCN-2027", 2L)).thenReturn(dto);

        mockMvc.perform(post("/curriculum-versions/1/clone")
                .param("newVersionName", "BSCN-2027")
                .param("newEffectiveAcademicYearId", "2"))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.versionName").value("BSCN-2027"));

        verify(curriculumVersionService).cloneCurriculumVersion(1L, "BSCN-2027", 2L);
    }

    private CurriculumVersionDto createDto(Long id, Long programId, String programName,
                                            String versionName, Long ayId, String ayName, Boolean isActive) {
        return new CurriculumVersionDto(id, programId, programName, versionName, ayId, ayName, isActive,
            Instant.now(), Instant.now());
    }
}
