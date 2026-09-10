package com.cms.inventory.procurement.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.cms.inventory.procurement.dto.TaxJurisdictionSettingsRequest;
import com.cms.inventory.procurement.dto.TaxJurisdictionSettingsResponse;
import com.cms.inventory.procurement.service.TaxJurisdictionSettingsService;
import com.fasterxml.jackson.databind.ObjectMapper;

@WebMvcTest(controllers = TaxJurisdictionSettingsController.class)
@AutoConfigureMockMvc(addFilters = false)
class TaxJurisdictionSettingsControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @MockitoBean private TaxJurisdictionSettingsService settingsService;

    @Test
    void shouldReturnEmptySettingsWhenNotConfigured() throws Exception {
        when(settingsService.find()).thenReturn(new TaxJurisdictionSettingsResponse(null, null, null));

        mockMvc.perform(get("/inventory/procurement/tax-jurisdiction-settings").with(jwt()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.homeState").doesNotExist());
    }

    @Test
    void shouldReturnConfiguredHomeState() throws Exception {
        Instant now = Instant.now();
        when(settingsService.find()).thenReturn(new TaxJurisdictionSettingsResponse("Tamil Nadu", now, "admin"));

        mockMvc.perform(get("/inventory/procurement/tax-jurisdiction-settings").with(jwt()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.homeState").value("Tamil Nadu"));
    }

    @Test
    void shouldSaveHomeState() throws Exception {
        Instant now = Instant.now();
        // The security filter chain is disabled in this WebMvcTest slice (addFilters = false), so
        // @AuthenticationPrincipal Jwt resolves null regardless of .with(jwt()...) here -- same
        // established limitation as TimetableControllerTest, hence anyString() rather than
        // asserting a specific actor value.
        when(settingsService.save(any(TaxJurisdictionSettingsRequest.class), anyString()))
            .thenReturn(new TaxJurisdictionSettingsResponse("Tamil Nadu", now, "system"));

        mockMvc.perform(put("/inventory/procurement/tax-jurisdiction-settings")
                .with(jwt().jwt(j -> j.claim("preferred_username", "admin")))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new TaxJurisdictionSettingsRequest("Tamil Nadu"))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.homeState").value("Tamil Nadu"));

        verify(settingsService).save(eq(new TaxJurisdictionSettingsRequest("Tamil Nadu")), anyString());
    }

    @Test
    void shouldReturnBadRequestWhenHomeStateBlank() throws Exception {
        mockMvc.perform(put("/inventory/procurement/tax-jurisdiction-settings")
                .with(jwt())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new TaxJurisdictionSettingsRequest(""))))
            .andExpect(status().isBadRequest());
    }
}
