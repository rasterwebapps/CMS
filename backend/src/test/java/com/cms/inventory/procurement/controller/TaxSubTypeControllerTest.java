package com.cms.inventory.procurement.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.cms.exception.ResourceNotFoundException;
import com.cms.inventory.procurement.dto.TaxSubTypeRequest;
import com.cms.inventory.procurement.dto.TaxSubTypeResponse;
import com.cms.inventory.procurement.service.TaxSubTypeService;
import com.fasterxml.jackson.databind.ObjectMapper;

@WebMvcTest(controllers = TaxSubTypeController.class)
@AutoConfigureMockMvc(addFilters = false)
class TaxSubTypeControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @MockitoBean private TaxSubTypeService taxSubTypeService;

    @Test
    void shouldCreateSubType() throws Exception {
        Instant now = Instant.now();
        when(taxSubTypeService.create(any(TaxSubTypeRequest.class)))
            .thenReturn(new TaxSubTypeResponse(1L, 2L, "GST 18%", "INTRASTATE", "CGST", new BigDecimal("50"), true, now, now));

        mockMvc.perform(post("/inventory/procurement/tax-sub-types")
                .with(jwt())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new TaxSubTypeRequest(2L, "INTRASTATE", "CGST", new BigDecimal("50"), true))))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.componentName").value("CGST"))
            .andExpect(jsonPath("$.splitPercent").value(50));
    }

    @Test
    void shouldReturnBadRequestWhenSplitPercentExceeds100() throws Exception {
        mockMvc.perform(post("/inventory/procurement/tax-sub-types")
                .with(jwt())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new TaxSubTypeRequest(2L, "INTRASTATE", "CGST", new BigDecimal("150"), true))))
            .andExpect(status().isBadRequest());
    }

    @Test
    void shouldRejectDuplicateComponentAsBadRequest() throws Exception {
        when(taxSubTypeService.create(any(TaxSubTypeRequest.class)))
            .thenThrow(new IllegalArgumentException("'CGST' already exists for this tax under INTRASTATE"));

        mockMvc.perform(post("/inventory/procurement/tax-sub-types")
                .with(jwt())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new TaxSubTypeRequest(2L, "INTRASTATE", "CGST", new BigDecimal("50"), true))))
            .andExpect(status().isBadRequest());
    }

    @Test
    void shouldFindByTaxRule() throws Exception {
        Instant now = Instant.now();
        when(taxSubTypeService.findByTaxRule(2L)).thenReturn(List.of(
            new TaxSubTypeResponse(1L, 2L, "GST 18%", "INTRASTATE", "CGST", new BigDecimal("50"), true, now, now)));

        mockMvc.perform(get("/inventory/procurement/tax-sub-types").with(jwt()).param("taxRuleId", "2"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void shouldFindSubTypeById() throws Exception {
        Instant now = Instant.now();
        when(taxSubTypeService.findById(1L))
            .thenReturn(new TaxSubTypeResponse(1L, 2L, "GST 18%", "INTRASTATE", "CGST", new BigDecimal("50"), true, now, now));

        mockMvc.perform(get("/inventory/procurement/tax-sub-types/1").with(jwt()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.componentName").value("CGST"));
    }

    @Test
    void shouldReturnNotFoundWhenSubTypeMissing() throws Exception {
        when(taxSubTypeService.findById(999L)).thenThrow(new ResourceNotFoundException("Tax sub-type not found with id: 999"));
        mockMvc.perform(get("/inventory/procurement/tax-sub-types/999").with(jwt())).andExpect(status().isNotFound());
    }

    @Test
    void shouldUpdateSubType() throws Exception {
        Instant now = Instant.now();
        when(taxSubTypeService.update(eq(1L), any(TaxSubTypeRequest.class)))
            .thenReturn(new TaxSubTypeResponse(1L, 2L, "GST 18%", "INTRASTATE", "CGST", new BigDecimal("60"), true, now, now));

        mockMvc.perform(put("/inventory/procurement/tax-sub-types/1")
                .with(jwt())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new TaxSubTypeRequest(2L, "INTRASTATE", "CGST", new BigDecimal("60"), true))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.splitPercent").value(60));
    }

    @Test
    void shouldDeleteSubType() throws Exception {
        doNothing().when(taxSubTypeService).delete(1L);
        mockMvc.perform(delete("/inventory/procurement/tax-sub-types/1").with(jwt())).andExpect(status().isNoContent());
        verify(taxSubTypeService).delete(1L);
    }
}
