package com.cms.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.cms.dto.SystemConfigurationRequest;
import com.cms.dto.SystemConfigurationResponse;
import com.cms.exception.ResourceNotFoundException;
import com.cms.model.enums.ConfigDataType;
import com.cms.service.SystemConfigurationService;
import com.fasterxml.jackson.databind.ObjectMapper;

@WebMvcTest(controllers = SystemConfigurationController.class)
@AutoConfigureMockMvc(addFilters = false)
class SystemConfigurationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private SystemConfigurationService systemConfigurationService;

    @Test
    void shouldCreateSystemConfiguration() throws Exception {
        SystemConfigurationRequest request = new SystemConfigurationRequest(
            "penalty.daily_rate", "100", "Daily late fee penalty rate",
            ConfigDataType.DECIMAL, "PENALTY", true
        );

        SystemConfigurationResponse response = createResponse(1L, "penalty.daily_rate", "100",
            "Daily late fee penalty rate", ConfigDataType.DECIMAL, "PENALTY", true);

        when(systemConfigurationService.create(any(SystemConfigurationRequest.class)))
            .thenReturn(response);

        mockMvc.perform(post("/system-configurations")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").value(1))
            .andExpect(jsonPath("$.configKey").value("penalty.daily_rate"))
            .andExpect(jsonPath("$.configValue").value("100"));

        verify(systemConfigurationService).create(any(SystemConfigurationRequest.class));
    }

    @Test
    void shouldFindAllSystemConfigurations() throws Exception {
        SystemConfigurationResponse response = createResponse(1L, "penalty.daily_rate", "100",
            "Daily late fee penalty rate", ConfigDataType.DECIMAL, "PENALTY", true);

        when(systemConfigurationService.findAll()).thenReturn(List.of(response));

        mockMvc.perform(get("/system-configurations"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(1));

        verify(systemConfigurationService).findAll();
    }

    @Test
    void shouldFindByCategory() throws Exception {
        SystemConfigurationResponse response = createResponse(1L, "college.name", "SKS College",
            "College name", ConfigDataType.STRING, "BRANDING", true);

        when(systemConfigurationService.findByCategory("BRANDING"))
            .thenReturn(List.of(response));

        mockMvc.perform(get("/system-configurations").param("category", "BRANDING"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(1));

        verify(systemConfigurationService).findByCategory("BRANDING");
    }

    @Test
    void shouldFindById() throws Exception {
        SystemConfigurationResponse response = createResponse(1L, "penalty.daily_rate", "100",
            "Daily late fee penalty rate", ConfigDataType.DECIMAL, "PENALTY", true);

        when(systemConfigurationService.findById(1L)).thenReturn(response);

        mockMvc.perform(get("/system-configurations/1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(1))
            .andExpect(jsonPath("$.configKey").value("penalty.daily_rate"));

        verify(systemConfigurationService).findById(1L);
    }

    @Test
    void shouldReturnNotFoundById() throws Exception {
        when(systemConfigurationService.findById(999L))
            .thenThrow(new ResourceNotFoundException("System configuration not found with id: 999"));

        mockMvc.perform(get("/system-configurations/999"))
            .andExpect(status().isNotFound());

        verify(systemConfigurationService).findById(999L);
    }

    @Test
    void shouldFindByKey() throws Exception {
        SystemConfigurationResponse response = createResponse(1L, "penalty.daily_rate", "100",
            "Daily late fee penalty rate", ConfigDataType.DECIMAL, "PENALTY", true);

        when(systemConfigurationService.findByKey("penalty.daily_rate"))
            .thenReturn(Optional.of(response));

        mockMvc.perform(get("/system-configurations/key/penalty.daily_rate"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.configKey").value("penalty.daily_rate"));

        verify(systemConfigurationService).findByKey("penalty.daily_rate");
    }

    @Test
    void shouldReturnNotFoundByKey() throws Exception {
        when(systemConfigurationService.findByKey("nonexistent")).thenReturn(Optional.empty());

        mockMvc.perform(get("/system-configurations/key/nonexistent"))
            .andExpect(status().isNotFound());

        verify(systemConfigurationService).findByKey("nonexistent");
    }

    @Test
    void shouldUpdateSystemConfiguration() throws Exception {
        SystemConfigurationRequest request = new SystemConfigurationRequest(
            "penalty.daily_rate", "200", "Updated penalty rate",
            ConfigDataType.DECIMAL, "PENALTY", true
        );

        SystemConfigurationResponse response = createResponse(1L, "penalty.daily_rate", "200",
            "Updated penalty rate", ConfigDataType.DECIMAL, "PENALTY", true);

        when(systemConfigurationService.update(eq(1L), any(SystemConfigurationRequest.class)))
            .thenReturn(response);

        mockMvc.perform(put("/system-configurations/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.configValue").value("200"));

        verify(systemConfigurationService).update(eq(1L), any(SystemConfigurationRequest.class));
    }

    @Test
    void shouldDeleteSystemConfiguration() throws Exception {
        doNothing().when(systemConfigurationService).delete(1L);

        mockMvc.perform(delete("/system-configurations/1"))
            .andExpect(status().isNoContent());

        verify(systemConfigurationService).delete(1L);
    }

    @Test
    void shouldReturnNotFoundWhenDeleting() throws Exception {
        doThrow(new ResourceNotFoundException("System configuration not found with id: 999"))
            .when(systemConfigurationService).delete(999L);

        mockMvc.perform(delete("/system-configurations/999"))
            .andExpect(status().isNotFound());

        verify(systemConfigurationService).delete(999L);
    }

    private SystemConfigurationResponse createResponse(Long id, String configKey, String configValue,
                                                        String description, ConfigDataType dataType,
                                                        String category, Boolean isEditable) {
        Instant now = Instant.now();
        return new SystemConfigurationResponse(
            id, configKey, configValue, description, dataType, category, isEditable, now, now
        );
    }
}
