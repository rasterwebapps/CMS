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

import com.cms.dto.FeeStructureRequest;
import com.cms.dto.FeeStructureResponse;
import com.cms.exception.ResourceNotFoundException;
import com.cms.model.enums.FeeType;
import com.cms.service.FeeStructureService;
import com.fasterxml.jackson.databind.ObjectMapper;

@WebMvcTest(controllers = FeeStructureController.class)
@AutoConfigureMockMvc(addFilters = false)
class FeeStructureControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private FeeStructureService feeStructureService;

    @Test
    void shouldCreateFeeStructure() throws Exception {
        FeeStructureRequest request = new FeeStructureRequest(
            1L, 1L, FeeType.TUITION, new BigDecimal("50000.00"), "Tuition fee", true, true
        );

        FeeStructureResponse response = createResponse(1L, FeeType.TUITION, new BigDecimal("50000.00"));

        when(feeStructureService.create(any(FeeStructureRequest.class))).thenReturn(response);

        mockMvc.perform(post("/fee-structures")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").value(1))
            .andExpect(jsonPath("$.feeType").value("TUITION"));

        verify(feeStructureService).create(any(FeeStructureRequest.class));
    }

    @Test
    void shouldFindAllFeeStructures() throws Exception {
        FeeStructureResponse response = createResponse(1L, FeeType.TUITION, new BigDecimal("50000.00"));

        when(feeStructureService.findAll()).thenReturn(List.of(response));

        mockMvc.perform(get("/fee-structures"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(1));

        verify(feeStructureService).findAll();
    }

    @Test
    void shouldFindByProgramId() throws Exception {
        FeeStructureResponse response = createResponse(1L, FeeType.TUITION, new BigDecimal("50000.00"));

        when(feeStructureService.findByProgramId(1L)).thenReturn(List.of(response));

        mockMvc.perform(get("/fee-structures").param("programId", "1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(1));

        verify(feeStructureService).findByProgramId(1L);
    }

    @Test
    void shouldFindById() throws Exception {
        FeeStructureResponse response = createResponse(1L, FeeType.TUITION, new BigDecimal("50000.00"));

        when(feeStructureService.findById(1L)).thenReturn(response);

        mockMvc.perform(get("/fee-structures/1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(1));

        verify(feeStructureService).findById(1L);
    }

    @Test
    void shouldReturnNotFound() throws Exception {
        when(feeStructureService.findById(999L))
            .thenThrow(new ResourceNotFoundException("Fee structure not found with id: 999"));

        mockMvc.perform(get("/fee-structures/999"))
            .andExpect(status().isNotFound());

        verify(feeStructureService).findById(999L);
    }

    @Test
    void shouldUpdateFeeStructure() throws Exception {
        FeeStructureRequest request = new FeeStructureRequest(
            1L, 1L, FeeType.LAB_FEE, new BigDecimal("10000.00"), "Lab fee", true, true
        );

        FeeStructureResponse response = createResponse(1L, FeeType.LAB_FEE, new BigDecimal("10000.00"));

        when(feeStructureService.update(eq(1L), any(FeeStructureRequest.class))).thenReturn(response);

        mockMvc.perform(put("/fee-structures/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.feeType").value("LAB_FEE"));

        verify(feeStructureService).update(eq(1L), any(FeeStructureRequest.class));
    }

    @Test
    void shouldDeleteFeeStructure() throws Exception {
        doNothing().when(feeStructureService).delete(1L);

        mockMvc.perform(delete("/fee-structures/1"))
            .andExpect(status().isNoContent());

        verify(feeStructureService).delete(1L);
    }

    @Test
    void shouldReturnNotFoundWhenDeleting() throws Exception {
        doThrow(new ResourceNotFoundException("Fee structure not found with id: 999"))
            .when(feeStructureService).delete(999L);

        mockMvc.perform(delete("/fee-structures/999"))
            .andExpect(status().isNotFound());

        verify(feeStructureService).delete(999L);
    }

    private FeeStructureResponse createResponse(Long id, FeeType feeType, BigDecimal amount) {
        Instant now = Instant.now();
        return new FeeStructureResponse(
            id, 1L, "B.Tech CS", 1L, "2024-25", feeType, amount,
            "Description", true, true, now, now
        );
    }
}
