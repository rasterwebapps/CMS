package com.cms.inventory.procurement.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
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

import com.cms.exception.ResourceNotFoundException;
import com.cms.inventory.procurement.dto.TaxTypeRequest;
import com.cms.inventory.procurement.dto.TaxTypeResponse;
import com.cms.inventory.procurement.service.TaxTypeService;
import com.fasterxml.jackson.databind.ObjectMapper;

@WebMvcTest(controllers = TaxTypeController.class)
@AutoConfigureMockMvc(addFilters = false)
class TaxTypeControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @MockitoBean private TaxTypeService taxTypeService;

    @Test
    void shouldCreateTaxType() throws Exception {
        Instant now = Instant.now();
        when(taxTypeService.create(any(TaxTypeRequest.class)))
            .thenReturn(new TaxTypeResponse(1L, "GST", "Goods and Services Tax", true, now, now));

        mockMvc.perform(post("/inventory/procurement/tax-types")
                .with(jwt())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new TaxTypeRequest("GST", "Goods and Services Tax", true))))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").value(1))
            .andExpect(jsonPath("$.name").value("GST"));
    }

    @Test
    void shouldReturnBadRequestWhenNameBlank() throws Exception {
        mockMvc.perform(post("/inventory/procurement/tax-types")
                .with(jwt())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new TaxTypeRequest("", null, true))))
            .andExpect(status().isBadRequest());
    }

    @Test
    void shouldFindAllTaxTypes() throws Exception {
        Instant now = Instant.now();
        when(taxTypeService.findAll(false)).thenReturn(List.of(new TaxTypeResponse(1L, "GST", null, true, now, now)));

        mockMvc.perform(get("/inventory/procurement/tax-types").with(jwt()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].name").value("GST"));
    }

    @Test
    void shouldFindTaxTypeById() throws Exception {
        Instant now = Instant.now();
        when(taxTypeService.findById(1L)).thenReturn(new TaxTypeResponse(1L, "GST", null, true, now, now));

        mockMvc.perform(get("/inventory/procurement/tax-types/1").with(jwt()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.name").value("GST"));
    }

    @Test
    void shouldReturnNotFoundWhenTaxTypeMissing() throws Exception {
        when(taxTypeService.findById(999L)).thenThrow(new ResourceNotFoundException("Tax type not found with id: 999"));

        mockMvc.perform(get("/inventory/procurement/tax-types/999").with(jwt()))
            .andExpect(status().isNotFound());
    }

    @Test
    void shouldUpdateTaxType() throws Exception {
        Instant now = Instant.now();
        when(taxTypeService.update(eq(1L), any(TaxTypeRequest.class)))
            .thenReturn(new TaxTypeResponse(1L, "VAT", null, true, now, now));

        mockMvc.perform(put("/inventory/procurement/tax-types/1")
                .with(jwt())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new TaxTypeRequest("VAT", null, true))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.name").value("VAT"));
    }

    @Test
    void shouldDeleteTaxType() throws Exception {
        doNothing().when(taxTypeService).delete(1L);
        mockMvc.perform(delete("/inventory/procurement/tax-types/1").with(jwt())).andExpect(status().isNoContent());
        verify(taxTypeService).delete(1L);
    }

    @Test
    void shouldUpdateTaxTypeStatus() throws Exception {
        Instant now = Instant.now();
        when(taxTypeService.updateStatus(eq(1L), any())).thenReturn(new com.cms.dto.ActiveStatusUpdateResponse(1L, false, now));

        mockMvc.perform(patch("/inventory/procurement/tax-types/1/status")
                .with(jwt())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"isActive": false}
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.isActive").value(false));
    }

    @Test
    void shouldCheckNameExists() throws Exception {
        when(taxTypeService.nameExists("GST", null)).thenReturn(true);
        mockMvc.perform(get("/inventory/procurement/tax-types/name-exists").with(jwt()).param("value", "GST"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").value(true));
    }
}
