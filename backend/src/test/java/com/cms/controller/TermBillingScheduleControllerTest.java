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

import java.math.BigDecimal;
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

import com.cms.dto.TermBillingScheduleDto;
import com.cms.dto.TermBillingScheduleRequest;
import com.cms.exception.ResourceNotFoundException;
import com.cms.model.enums.LateFeeType;
import com.cms.model.enums.TermType;
import com.cms.service.TermBillingScheduleService;
import com.fasterxml.jackson.databind.ObjectMapper;

@WebMvcTest(controllers = TermBillingScheduleController.class)
@AutoConfigureMockMvc(addFilters = false)
class TermBillingScheduleControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private TermBillingScheduleService service;

    @Test
    void shouldCreateOrUpdateTermBillingSchedule() throws Exception {
        TermBillingScheduleRequest request = new TermBillingScheduleRequest(
            1L, TermType.ODD, LocalDate.of(2026, 7, 31),
            LateFeeType.FLAT, new BigDecimal("500"), 5);

        TermBillingScheduleDto dto = createDto(1L, 1L, "2026-2027", TermType.ODD,
            LocalDate.of(2026, 7, 31), LateFeeType.FLAT, new BigDecimal("500"), 5);

        when(service.createOrUpdate(any(TermBillingScheduleRequest.class))).thenReturn(dto);

        mockMvc.perform(post("/term-billing-schedules")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").value(1))
            .andExpect(jsonPath("$.termType").value("ODD"))
            .andExpect(jsonPath("$.lateFeeType").value("FLAT"))
            .andExpect(jsonPath("$.graceDays").value(5));

        verify(service).createOrUpdate(any(TermBillingScheduleRequest.class));
    }

    @Test
    void shouldReturnBadRequestWhenRequiredFieldsMissing() throws Exception {
        String json = """
            {"termType": "ODD", "dueDate": "2026-07-31"}
            """;

        mockMvc.perform(post("/term-billing-schedules")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
            .andExpect(status().isBadRequest());
    }

    @Test
    void shouldUpdateTermBillingSchedule() throws Exception {
        TermBillingScheduleRequest request = new TermBillingScheduleRequest(
            1L, TermType.ODD, LocalDate.of(2026, 8, 1),
            LateFeeType.PER_DAY, new BigDecimal("50"), 3);

        TermBillingScheduleDto dto = createDto(1L, 1L, "2026-2027", TermType.ODD,
            LocalDate.of(2026, 8, 1), LateFeeType.PER_DAY, new BigDecimal("50"), 3);

        when(service.update(eq(1L), any(TermBillingScheduleRequest.class))).thenReturn(dto);

        mockMvc.perform(put("/term-billing-schedules/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.lateFeeType").value("PER_DAY"))
            .andExpect(jsonPath("$.graceDays").value(3));

        verify(service).update(eq(1L), any(TermBillingScheduleRequest.class));
    }

    @Test
    void shouldGetByAcademicYear() throws Exception {
        TermBillingScheduleDto oddDto = createDto(1L, 1L, "2026-2027", TermType.ODD,
            LocalDate.of(2026, 7, 31), LateFeeType.FLAT, new BigDecimal("500"), 0);
        TermBillingScheduleDto evenDto = createDto(2L, 1L, "2026-2027", TermType.EVEN,
            LocalDate.of(2027, 1, 31), LateFeeType.FLAT, new BigDecimal("500"), 0);

        when(service.getAllForAcademicYear(1L)).thenReturn(List.of(oddDto, evenDto));

        mockMvc.perform(get("/term-billing-schedules").param("academicYearId", "1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(2))
            .andExpect(jsonPath("$[0].termType").value("ODD"))
            .andExpect(jsonPath("$[1].termType").value("EVEN"));

        verify(service).getAllForAcademicYear(1L);
    }

    @Test
    void shouldGetById() throws Exception {
        TermBillingScheduleDto dto = createDto(1L, 1L, "2026-2027", TermType.ODD,
            LocalDate.of(2026, 7, 31), LateFeeType.FLAT, new BigDecimal("500"), 0);

        when(service.getById(1L)).thenReturn(dto);

        mockMvc.perform(get("/term-billing-schedules/1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(1));

        verify(service).getById(1L);
    }

    @Test
    void shouldReturnNotFoundWhenScheduleNotExists() throws Exception {
        when(service.getById(999L))
            .thenThrow(new ResourceNotFoundException("Term billing schedule not found with id: 999"));

        mockMvc.perform(get("/term-billing-schedules/999"))
            .andExpect(status().isNotFound());
    }

    @Test
    void shouldComputeLateFee() throws Exception {
        when(service.computeLateFee(1L, TermType.ODD, LocalDate.of(2026, 8, 5)))
            .thenReturn(new BigDecimal("500"));

        mockMvc.perform(get("/term-billing-schedules/late-fee")
                .param("academicYearId", "1")
                .param("termType", "ODD")
                .param("paymentDate", "2026-08-05"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").value(500));

        verify(service).computeLateFee(1L, TermType.ODD, LocalDate.of(2026, 8, 5));
    }

    private TermBillingScheduleDto createDto(Long id, Long ayId, String ayName, TermType termType,
                                              LocalDate dueDate, LateFeeType lateFeeType,
                                              BigDecimal lateFeeAmount, Integer graceDays) {
        return new TermBillingScheduleDto(id, ayId, ayName, termType, dueDate, lateFeeType,
            lateFeeAmount, graceDays, Instant.now(), Instant.now());
    }
}
