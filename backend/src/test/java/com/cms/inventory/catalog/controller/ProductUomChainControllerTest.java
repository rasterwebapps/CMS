package com.cms.inventory.catalog.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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

import com.cms.inventory.catalog.dto.ProductUomChainSaveRequest;
import com.cms.inventory.catalog.dto.ProductUomChainVersionResponse;
import com.cms.inventory.catalog.dto.ProductUomLevelRequest;
import com.cms.inventory.catalog.dto.ProductUomLevelResponse;
import com.cms.inventory.catalog.service.ProductUomChainService;
import com.fasterxml.jackson.databind.ObjectMapper;

@WebMvcTest(controllers = ProductUomChainController.class)
@AutoConfigureMockMvc(addFilters = false)
class ProductUomChainControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @MockitoBean private ProductUomChainService chainService;

    @Test
    void shouldReturnActiveVersion() throws Exception {
        Instant now = Instant.now();
        when(chainService.getActiveVersion(10L)).thenReturn(new ProductUomChainVersionResponse(
            100L, 1, true, "admin", now, List.of(new ProductUomLevelResponse(1L, 1L, "TABLET", "Tablet", 0, BigDecimal.ONE, false))));

        mockMvc.perform(get("/inventory/products/10/uom-chain/active").with(jwt()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.versionNo").value(1))
            .andExpect(jsonPath("$.isActive").value(true))
            .andExpect(jsonPath("$.levels.length()").value(1));
    }

    @Test
    void shouldReturnNullBodyWhenNoActiveVersionConfigured() throws Exception {
        when(chainService.getActiveVersion(10L)).thenReturn(null);
        mockMvc.perform(get("/inventory/products/10/uom-chain/active").with(jwt()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").doesNotExist());
    }

    @Test
    void shouldListVersions() throws Exception {
        Instant now = Instant.now();
        when(chainService.listVersions(10L)).thenReturn(List.of(
            new ProductUomChainVersionResponse(101L, 2, true, "admin", now, List.of()),
            new ProductUomChainVersionResponse(100L, 1, false, "admin", now, List.of())));

        mockMvc.perform(get("/inventory/products/10/uom-chain/versions").with(jwt()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(2))
            .andExpect(jsonPath("$[0].versionNo").value(2));
    }

    @Test
    void shouldSaveNewVersion() throws Exception {
        Instant now = Instant.now();
        var request = new ProductUomChainSaveRequest(List.of(
            new ProductUomLevelRequest(1L, 0, BigDecimal.ONE, false),
            new ProductUomLevelRequest(2L, 1, BigDecimal.TEN, true)));
        when(chainService.saveVersion(eq(10L), any(ProductUomChainSaveRequest.class), anyString()))
            .thenReturn(new ProductUomChainVersionResponse(200L, 2, true, "system", now, List.of()));

        mockMvc.perform(post("/inventory/products/10/uom-chain/versions")
                .with(jwt())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(200))
            .andExpect(jsonPath("$.versionNo").value(2));

        verify(chainService).saveVersion(eq(10L), any(ProductUomChainSaveRequest.class), anyString());
    }

    @Test
    void shouldReturnBadRequestWhenLevelsEmpty() throws Exception {
        mockMvc.perform(post("/inventory/products/10/uom-chain/versions")
                .with(jwt())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new ProductUomChainSaveRequest(List.of()))))
            .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturnBadRequestWhenChainValidationFails() throws Exception {
        when(chainService.saveVersion(eq(10L), any(ProductUomChainSaveRequest.class), anyString()))
            .thenThrow(new IllegalArgumentException("The chain must include level 0 (the product's base unit)"));

        var request = new ProductUomChainSaveRequest(List.of(new ProductUomLevelRequest(2L, 1, BigDecimal.TEN, false)));
        mockMvc.perform(post("/inventory/products/10/uom-chain/versions")
                .with(jwt())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest());
    }
}
