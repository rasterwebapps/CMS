package com.cms.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.cms.dto.FeeDemandDto;
import com.cms.exception.ResourceNotFoundException;
import com.cms.model.enums.DemandStatus;
import com.cms.service.FeeDemandService;

@WebMvcTest(controllers = FeeDemandController.class)
@AutoConfigureMockMvc(addFilters = false)
class FeeDemandControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private FeeDemandService feeDemandService;

    private FeeDemandDto buildDemand(Long id, Long enrollmentId, Long termInstanceId, DemandStatus status) {
        return new FeeDemandDto(id, enrollmentId, 1L, "Test Student", "CS-2024",
            termInstanceId, "Term 1", 1L, "2024-25",
            BigDecimal.valueOf(10000), LocalDate.now().plusDays(30),
            BigDecimal.ZERO, BigDecimal.valueOf(10000), status);
    }

    // -------------------------------------------------------------------------
    // GET /api/fee-demands with enrollmentId
    // -------------------------------------------------------------------------

    @Test
    void shouldGetDemandByEnrollmentId() throws Exception {
        FeeDemandDto demand = buildDemand(1L, 5L, 1L, DemandStatus.UNPAID);
        when(feeDemandService.getDemandByEnrollment(5L)).thenReturn(demand);

        mockMvc.perform(get("/api/fee-demands").param("enrollmentId", "5"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].id").value(1))
            .andExpect(jsonPath("$[0].enrollmentId").value(5));
    }

    @Test
    void shouldGetDemandsByTermInstance() throws Exception {
        List<FeeDemandDto> demands = List.of(
            buildDemand(1L, 1L, 10L, DemandStatus.UNPAID),
            buildDemand(2L, 2L, 10L, DemandStatus.PAID));
        when(feeDemandService.getDemandsByTermInstance(10L)).thenReturn(demands);

        mockMvc.perform(get("/api/fee-demands").param("termInstanceId", "10"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    void shouldGetDemandsByTermInstanceAndStatus() throws Exception {
        List<FeeDemandDto> demands = List.of(buildDemand(1L, 1L, 10L, DemandStatus.UNPAID));
        when(feeDemandService.getDemandsByTermInstanceAndStatus(10L, DemandStatus.UNPAID)).thenReturn(demands);

        mockMvc.perform(get("/api/fee-demands")
                .param("termInstanceId", "10")
                .param("status", "UNPAID"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].status").value("UNPAID"));
    }

    @Test
    void shouldReturnBadRequestWhenNoQueryParams() throws Exception {
        mockMvc.perform(get("/api/fee-demands"))
            .andExpect(status().isBadRequest());
    }

    // -------------------------------------------------------------------------
    // GET /api/fee-demands/{id}
    // -------------------------------------------------------------------------

    @Test
    void shouldGetDemandById() throws Exception {
        FeeDemandDto demand = buildDemand(1L, 1L, 1L, DemandStatus.UNPAID);
        when(feeDemandService.getById(1L)).thenReturn(demand);

        mockMvc.perform(get("/api/fee-demands/1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(1));
    }

    // -------------------------------------------------------------------------
    // POST /api/fee-demands/generate
    // -------------------------------------------------------------------------

    @Test
    void shouldGenerateDemands() throws Exception {
        when(feeDemandService.generateDemandsForTermInstance(5L)).thenReturn(15);

        mockMvc.perform(post("/api/fee-demands/generate").param("termInstanceId", "5"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.demandsCreated").value(15));
    }
}

