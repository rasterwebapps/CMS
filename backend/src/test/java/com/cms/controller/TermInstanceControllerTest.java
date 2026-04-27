package com.cms.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.cms.dto.TermInstanceDto;
import com.cms.dto.TermInstanceUpdateRequest;
import com.cms.exception.ResourceNotFoundException;
import com.cms.model.enums.TermInstanceStatus;
import com.cms.model.enums.TermType;
import com.cms.service.TermInstanceService;
import com.fasterxml.jackson.databind.ObjectMapper;

@WebMvcTest(controllers = TermInstanceController.class)
@AutoConfigureMockMvc(addFilters = false)
class TermInstanceControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private TermInstanceService termInstanceService;

    @Test
    void shouldGetTermInstancesByAcademicYear() throws Exception {
        TermInstanceDto dto = createDto(1L, 1L, "2026-2027", TermType.ODD,
            LocalDate.of(2026, 6, 1), LocalDate.of(2026, 11, 30), TermInstanceStatus.PLANNED);

        when(termInstanceService.getTermInstancesByAcademicYear(1L)).thenReturn(List.of(dto));

        mockMvc.perform(get("/term-instances").param("academicYearId", "1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].id").value(1))
            .andExpect(jsonPath("$[0].termType").value("ODD"))
            .andExpect(jsonPath("$[0].status").value("PLANNED"));

        verify(termInstanceService).getTermInstancesByAcademicYear(1L);
    }

    @Test
    void shouldGetTermInstanceById() throws Exception {
        TermInstanceDto dto = createDto(1L, 1L, "2026-2027", TermType.ODD,
            LocalDate.of(2026, 6, 1), LocalDate.of(2026, 11, 30), TermInstanceStatus.OPEN);

        when(termInstanceService.getById(1L)).thenReturn(dto);

        mockMvc.perform(get("/term-instances/1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(1))
            .andExpect(jsonPath("$.termType").value("ODD"))
            .andExpect(jsonPath("$.status").value("OPEN"));

        verify(termInstanceService).getById(1L);
    }

    @Test
    void shouldReturnNotFoundWhenTermInstanceNotExists() throws Exception {
        when(termInstanceService.getById(999L))
            .thenThrow(new ResourceNotFoundException("Term instance not found with id: 999"));

        mockMvc.perform(get("/term-instances/999"))
            .andExpect(status().isNotFound());
    }

    @Test
    void shouldUpdateTermInstance() throws Exception {
        TermInstanceUpdateRequest request = new TermInstanceUpdateRequest(
            LocalDate.of(2026, 7, 1), LocalDate.of(2026, 12, 15), TermInstanceStatus.OPEN);

        TermInstanceDto dto = createDto(1L, 1L, "2026-2027", TermType.ODD,
            LocalDate.of(2026, 7, 1), LocalDate.of(2026, 12, 15), TermInstanceStatus.OPEN);

        when(termInstanceService.updateTermInstance(eq(1L), any(TermInstanceUpdateRequest.class))).thenReturn(dto);

        mockMvc.perform(put("/term-instances/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("OPEN"));

        verify(termInstanceService).updateTermInstance(eq(1L), any(TermInstanceUpdateRequest.class));
    }

    @Test
    void shouldReturnBadRequestWhenInvalidStatusTransition() throws Exception {
        TermInstanceUpdateRequest request = new TermInstanceUpdateRequest(null, null, TermInstanceStatus.LOCKED);

        when(termInstanceService.updateTermInstance(eq(1L), any(TermInstanceUpdateRequest.class)))
            .thenThrow(new IllegalArgumentException("Invalid status transition"));

        mockMvc.perform(put("/term-instances/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest());
    }

    private TermInstanceDto createDto(Long id, Long ayId, String ayName, TermType type,
                                       LocalDate start, LocalDate end, TermInstanceStatus status) {
        return new TermInstanceDto(id, ayId, ayName, type, start, end, status, Instant.now(), Instant.now());
    }
}
