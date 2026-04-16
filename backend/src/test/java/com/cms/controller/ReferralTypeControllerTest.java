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

import com.cms.dto.ReferralTypeRequest;
import com.cms.dto.ReferralTypeResponse;
import com.cms.exception.ResourceNotFoundException;
import com.cms.service.ReferralTypeService;
import com.fasterxml.jackson.databind.ObjectMapper;

@WebMvcTest(controllers = ReferralTypeController.class)
@AutoConfigureMockMvc(addFilters = false)
class ReferralTypeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private ReferralTypeService referralTypeService;

    @Test
    void shouldCreateReferralType() throws Exception {
        ReferralTypeRequest request = new ReferralTypeRequest(
            "Staff", "STAFF", new BigDecimal("5000.00"), true, "Staff referral", true
        );

        ReferralTypeResponse response = createResponse(1L, "Staff", "STAFF", new BigDecimal("5000.00"));

        when(referralTypeService.create(any(ReferralTypeRequest.class))).thenReturn(response);

        mockMvc.perform(post("/referral-types")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").value(1))
            .andExpect(jsonPath("$.name").value("Staff"))
            .andExpect(jsonPath("$.code").value("STAFF"));

        verify(referralTypeService).create(any(ReferralTypeRequest.class));
    }

    @Test
    void shouldFindAll() throws Exception {
        ReferralTypeResponse response = createResponse(1L, "Staff", "STAFF", BigDecimal.ZERO);

        when(referralTypeService.findAll()).thenReturn(List.of(response));

        mockMvc.perform(get("/referral-types"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(1));

        verify(referralTypeService).findAll();
    }

    @Test
    void shouldFindActiveOnly() throws Exception {
        ReferralTypeResponse response = createResponse(1L, "Staff", "STAFF", BigDecimal.ZERO);

        when(referralTypeService.findActive()).thenReturn(List.of(response));

        mockMvc.perform(get("/referral-types").param("activeOnly", "true"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(1));

        verify(referralTypeService).findActive();
    }

    @Test
    void shouldFindById() throws Exception {
        ReferralTypeResponse response = createResponse(1L, "Staff", "STAFF", BigDecimal.ZERO);

        when(referralTypeService.findById(1L)).thenReturn(response);

        mockMvc.perform(get("/referral-types/1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(1))
            .andExpect(jsonPath("$.name").value("Staff"));

        verify(referralTypeService).findById(1L);
    }

    @Test
    void shouldReturnNotFoundWhenFindById() throws Exception {
        when(referralTypeService.findById(999L))
            .thenThrow(new ResourceNotFoundException("Referral type not found with id: 999"));

        mockMvc.perform(get("/referral-types/999"))
            .andExpect(status().isNotFound());

        verify(referralTypeService).findById(999L);
    }

    @Test
    void shouldUpdate() throws Exception {
        ReferralTypeRequest request = new ReferralTypeRequest(
            "Staff Updated", "STAFF", new BigDecimal("10000.00"), true, "Updated", true
        );

        ReferralTypeResponse response = createResponse(1L, "Staff Updated", "STAFF", new BigDecimal("10000.00"));

        when(referralTypeService.update(eq(1L), any(ReferralTypeRequest.class))).thenReturn(response);

        mockMvc.perform(put("/referral-types/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.name").value("Staff Updated"));

        verify(referralTypeService).update(eq(1L), any(ReferralTypeRequest.class));
    }

    @Test
    void shouldDelete() throws Exception {
        doNothing().when(referralTypeService).delete(1L);

        mockMvc.perform(delete("/referral-types/1"))
            .andExpect(status().isNoContent());

        verify(referralTypeService).delete(1L);
    }

    @Test
    void shouldReturnNotFoundWhenDeleting() throws Exception {
        doThrow(new ResourceNotFoundException("Referral type not found with id: 999"))
            .when(referralTypeService).delete(999L);

        mockMvc.perform(delete("/referral-types/999"))
            .andExpect(status().isNotFound());

        verify(referralTypeService).delete(999L);
    }

    private ReferralTypeResponse createResponse(Long id, String name, String code, BigDecimal commissionAmount) {
        Instant now = Instant.now();
        return new ReferralTypeResponse(id, name, code, commissionAmount, true, name + " description", true, now, now);
    }
}
