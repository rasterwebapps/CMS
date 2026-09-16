package com.cms.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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

import com.cms.dto.GuardianRequest;
import com.cms.dto.GuardianResponse;
import com.cms.dto.WardSummaryResponse;
import com.cms.service.GuardianService;
import com.fasterxml.jackson.databind.ObjectMapper;

@WebMvcTest(controllers = GuardianController.class)
@AutoConfigureMockMvc(addFilters = false)
class GuardianControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private GuardianService guardianService;

    @Test
    void shouldListGuardians() throws Exception {
        GuardianResponse response = new GuardianResponse(1L, "Test", "Guardian", "g@test.com", "999", "Mother", Instant.now());
        when(guardianService.findAll()).thenReturn(List.of(response));

        mockMvc.perform(get("/guardians"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].email").value("g@test.com"));
    }

    @Test
    void shouldCreateGuardian() throws Exception {
        GuardianRequest request = new GuardianRequest("Test", "Guardian", "new@test.com", "999", "Mother");
        GuardianResponse response = new GuardianResponse(1L, "Test", "Guardian", "new@test.com", "999", "Mother", Instant.now());
        when(guardianService.create(any(GuardianRequest.class))).thenReturn(response);

        mockMvc.perform(post("/guardians")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    void shouldRejectInvalidGuardianCreate() throws Exception {
        GuardianRequest invalid = new GuardianRequest("", "", "not-an-email", null, null);

        mockMvc.perform(post("/guardians")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalid)))
            .andExpect(status().isBadRequest());
    }

    @Test
    void shouldCheckEmailExists() throws Exception {
        when(guardianService.emailExists("taken@test.com", null)).thenReturn(true);

        mockMvc.perform(get("/guardians/email-exists").param("value", "taken@test.com"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").value(true));
    }

    @Test
    void shouldLinkWardToGuardian() throws Exception {
        mockMvc.perform(post("/guardians/1/wards/10").param("isPrimary", "true"))
            .andExpect(status().isCreated());

        verify(guardianService).linkToStudent(1L, 10L, true);
    }

    @Test
    void shouldReturnMyWards() throws Exception {
        WardSummaryResponse ward = new WardSummaryResponse(45L, "Oviya Thangam", "GNM4-015", true);
        when(guardianService.findMyWards(eq(""))).thenReturn(List.of(ward));

        mockMvc.perform(get("/guardian/wards"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].studentId").value(45));
    }
}
