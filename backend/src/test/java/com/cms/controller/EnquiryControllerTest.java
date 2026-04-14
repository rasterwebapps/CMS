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
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.cms.dto.EnquiryRequest;
import com.cms.dto.EnquiryResponse;
import com.cms.exception.ResourceNotFoundException;
import com.cms.model.enums.EnquirySource;
import com.cms.model.enums.EnquiryStatus;
import com.cms.service.EnquiryService;
import com.fasterxml.jackson.databind.ObjectMapper;

@WebMvcTest(controllers = EnquiryController.class)
@AutoConfigureMockMvc(addFilters = false)
class EnquiryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private EnquiryService enquiryService;

    @Test
    void shouldCreateEnquiry() throws Exception {
        EnquiryRequest request = new EnquiryRequest(
            "Ravi Kumar", "ravi@email.com", "9876543210", 1L,
            LocalDate.of(2024, 6, 15), EnquirySource.WALK_IN, EnquiryStatus.NEW,
            null, "Admin", "Interested in CS", new BigDecimal("50000.00")
        );

        EnquiryResponse response = createResponse(1L, "Ravi Kumar", EnquirySource.WALK_IN, EnquiryStatus.NEW);

        when(enquiryService.create(any(EnquiryRequest.class))).thenReturn(response);

        mockMvc.perform(post("/enquiries")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").value(1))
            .andExpect(jsonPath("$.name").value("Ravi Kumar"));

        verify(enquiryService).create(any(EnquiryRequest.class));
    }

    @Test
    void shouldFindAllEnquiries() throws Exception {
        EnquiryResponse response = createResponse(1L, "Ravi Kumar", EnquirySource.WALK_IN, EnquiryStatus.NEW);

        when(enquiryService.findAll()).thenReturn(List.of(response));

        mockMvc.perform(get("/enquiries"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(1));

        verify(enquiryService).findAll();
    }

    @Test
    void shouldFindByStatus() throws Exception {
        EnquiryResponse response = createResponse(1L, "Ravi Kumar", EnquirySource.WALK_IN, EnquiryStatus.NEW);

        when(enquiryService.findByStatus(EnquiryStatus.NEW)).thenReturn(List.of(response));

        mockMvc.perform(get("/enquiries").param("status", "NEW"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(1));

        verify(enquiryService).findByStatus(EnquiryStatus.NEW);
    }

    @Test
    void shouldFindBySource() throws Exception {
        EnquiryResponse response = createResponse(1L, "Ravi Kumar", EnquirySource.WALK_IN, EnquiryStatus.NEW);

        when(enquiryService.findBySource(EnquirySource.WALK_IN)).thenReturn(List.of(response));

        mockMvc.perform(get("/enquiries").param("source", "WALK_IN"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(1));

        verify(enquiryService).findBySource(EnquirySource.WALK_IN);
    }

    @Test
    void shouldFindById() throws Exception {
        EnquiryResponse response = createResponse(1L, "Ravi Kumar", EnquirySource.WALK_IN, EnquiryStatus.NEW);

        when(enquiryService.findById(1L)).thenReturn(response);

        mockMvc.perform(get("/enquiries/1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(1))
            .andExpect(jsonPath("$.name").value("Ravi Kumar"));

        verify(enquiryService).findById(1L);
    }

    @Test
    void shouldReturnNotFound() throws Exception {
        when(enquiryService.findById(999L))
            .thenThrow(new ResourceNotFoundException("Enquiry not found with id: 999"));

        mockMvc.perform(get("/enquiries/999"))
            .andExpect(status().isNotFound());

        verify(enquiryService).findById(999L);
    }

    @Test
    void shouldUpdateEnquiry() throws Exception {
        EnquiryRequest request = new EnquiryRequest(
            "Ravi Kumar Updated", "ravi@email.com", "9876543210", 1L,
            LocalDate.of(2024, 6, 20), EnquirySource.PHONE, EnquiryStatus.CONTACTED,
            null, "Staff", "Called back", new BigDecimal("45000.00")
        );

        EnquiryResponse response = createResponse(1L, "Ravi Kumar Updated", EnquirySource.PHONE, EnquiryStatus.CONTACTED);

        when(enquiryService.update(eq(1L), any(EnquiryRequest.class))).thenReturn(response);

        mockMvc.perform(put("/enquiries/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.name").value("Ravi Kumar Updated"));

        verify(enquiryService).update(eq(1L), any(EnquiryRequest.class));
    }

    @Test
    void shouldConvertToStudent() throws Exception {
        EnquiryResponse response = new EnquiryResponse(
            1L, "Ravi Kumar", "ravi@email.com", "9876543210",
            1L, "B.Tech CS", LocalDate.of(2024, 6, 15),
            EnquirySource.WALK_IN, EnquiryStatus.CONVERTED,
            null, null, "Admin", "Converted", new BigDecimal("50000.00"),
            10L, Instant.now(), Instant.now()
        );

        when(enquiryService.convertToStudent(1L, 10L)).thenReturn(response);

        mockMvc.perform(put("/enquiries/1/convert").param("studentId", "10"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("CONVERTED"))
            .andExpect(jsonPath("$.convertedStudentId").value(10));

        verify(enquiryService).convertToStudent(1L, 10L);
    }

    @Test
    void shouldDeleteEnquiry() throws Exception {
        doNothing().when(enquiryService).delete(1L);

        mockMvc.perform(delete("/enquiries/1"))
            .andExpect(status().isNoContent());

        verify(enquiryService).delete(1L);
    }

    @Test
    void shouldReturnNotFoundWhenDeleting() throws Exception {
        doThrow(new ResourceNotFoundException("Enquiry not found with id: 999"))
            .when(enquiryService).delete(999L);

        mockMvc.perform(delete("/enquiries/999"))
            .andExpect(status().isNotFound());

        verify(enquiryService).delete(999L);
    }

    private EnquiryResponse createResponse(Long id, String name, EnquirySource source, EnquiryStatus status) {
        Instant now = Instant.now();
        return new EnquiryResponse(
            id, name, "ravi@email.com", "9876543210",
            1L, "B.Tech CS", LocalDate.of(2024, 6, 15),
            source, status, null, null, "Admin", "Remarks",
            new BigDecimal("50000.00"), null, now, now
        );
    }
}
