package com.cms.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.cms.dto.AcademicQualificationRequest;
import com.cms.dto.AcademicQualificationResponse;
import com.cms.dto.AdmissionDocumentResponse;
import com.cms.dto.AdmissionRequest;
import com.cms.dto.AdmissionResponse;
import com.cms.exception.ResourceNotFoundException;
import com.cms.model.enums.AdmissionStatus;
import com.cms.model.enums.DocumentType;
import com.cms.model.enums.DocumentVerificationStatus;
import com.cms.model.enums.QualificationType;
import com.cms.service.AcademicQualificationService;
import com.cms.service.AdmissionDocumentService;
import com.cms.service.AdmissionService;
import com.fasterxml.jackson.databind.ObjectMapper;

@WebMvcTest(controllers = AdmissionController.class)
@AutoConfigureMockMvc(addFilters = false)
class AdmissionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AdmissionService admissionService;

    @MockitoBean
    private AcademicQualificationService academicQualificationService;

    @MockitoBean
    private AdmissionDocumentService admissionDocumentService;

    private AdmissionResponse createAdmissionResponse(Long id) {
        return new AdmissionResponse(
            id, 1L, "John Doe", 2024, 2025,
            LocalDate.of(2024, 1, 15), AdmissionStatus.DRAFT,
            "Chennai", LocalDate.of(2024, 1, 15), true, true,
            Instant.now(), Instant.now()
        );
    }

    @Test
    void shouldCreateAdmission() throws Exception {
        AdmissionRequest request = new AdmissionRequest(
            1L, 2024, 2025, LocalDate.of(2024, 1, 15),
            AdmissionStatus.DRAFT, "Chennai", LocalDate.of(2024, 1, 15), true, true
        );
        AdmissionResponse response = createAdmissionResponse(1L);
        when(admissionService.create(any(AdmissionRequest.class))).thenReturn(response);

        mockMvc.perform(post("/admissions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").value(1))
            .andExpect(jsonPath("$.studentName").value("John Doe"));
        verify(admissionService).create(any(AdmissionRequest.class));
    }

    @Test
    void shouldReturnBadRequestWhenStudentIdIsNull() throws Exception {
        String json = """
            {"academicYearFrom": 2024, "academicYearTo": 2025, "applicationDate": "2024-01-15"}
            """;
        mockMvc.perform(post("/admissions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
            .andExpect(status().isBadRequest());
    }

    @Test
    void shouldFindAllAdmissions() throws Exception {
        when(admissionService.findAll()).thenReturn(List.of(createAdmissionResponse(1L)));
        mockMvc.perform(get("/admissions"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(1));
        verify(admissionService).findAll();
    }

    @Test
    void shouldReturnEmptyListWhenNoAdmissions() throws Exception {
        when(admissionService.findAll()).thenReturn(List.of());
        mockMvc.perform(get("/admissions"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void shouldFindAdmissionById() throws Exception {
        when(admissionService.findById(1L)).thenReturn(createAdmissionResponse(1L));
        mockMvc.perform(get("/admissions/1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(1));
        verify(admissionService).findById(1L);
    }

    @Test
    void shouldReturnNotFoundWhenAdmissionNotExists() throws Exception {
        when(admissionService.findById(999L))
            .thenThrow(new ResourceNotFoundException("Admission not found with id: 999"));
        mockMvc.perform(get("/admissions/999"))
            .andExpect(status().isNotFound());
    }

    @Test
    void shouldFindAdmissionByStudentId() throws Exception {
        when(admissionService.findByStudentId(1L)).thenReturn(createAdmissionResponse(1L));
        mockMvc.perform(get("/admissions/student/1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.studentId").value(1));
        verify(admissionService).findByStudentId(1L);
    }

    @Test
    void shouldReturnNotFoundWhenStudentHasNoAdmission() throws Exception {
        when(admissionService.findByStudentId(999L))
            .thenThrow(new ResourceNotFoundException("Admission not found for student id: 999"));
        mockMvc.perform(get("/admissions/student/999"))
            .andExpect(status().isNotFound());
    }

    @Test
    void shouldUpdateAdmission() throws Exception {
        AdmissionRequest request = new AdmissionRequest(
            1L, 2024, 2025, LocalDate.of(2024, 1, 15),
            AdmissionStatus.SUBMITTED, "Mumbai", LocalDate.of(2024, 1, 15), true, true
        );
        AdmissionResponse response = createAdmissionResponse(1L);
        when(admissionService.update(eq(1L), any(AdmissionRequest.class))).thenReturn(response);

        mockMvc.perform(put("/admissions/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(1));
        verify(admissionService).update(eq(1L), any(AdmissionRequest.class));
    }

    @Test
    void shouldUpdateAdmissionStatus() throws Exception {
        AdmissionResponse response = createAdmissionResponse(1L);
        when(admissionService.updateStatus(1L, AdmissionStatus.SUBMITTED)).thenReturn(response);
        mockMvc.perform(patch("/admissions/1/status")
                .param("status", "SUBMITTED"))
            .andExpect(status().isOk());
        verify(admissionService).updateStatus(1L, AdmissionStatus.SUBMITTED);
    }

    @Test
    void shouldDeleteAdmission() throws Exception {
        doNothing().when(admissionService).delete(1L);
        mockMvc.perform(delete("/admissions/1"))
            .andExpect(status().isNoContent());
        verify(admissionService).delete(1L);
    }

    @Test
    void shouldReturnNotFoundWhenDeletingNonExistentAdmission() throws Exception {
        doThrow(new ResourceNotFoundException("Admission not found with id: 999"))
            .when(admissionService).delete(999L);
        mockMvc.perform(delete("/admissions/999"))
            .andExpect(status().isNotFound());
    }

    @Test
    void shouldAddQualification() throws Exception {
        AcademicQualificationRequest request = new AcademicQualificationRequest(
            QualificationType.HSC, "ABC School", "Science", 500, null, "May 2022", "State Board"
        );
        AcademicQualificationResponse response = new AcademicQualificationResponse(
            1L, 1L, QualificationType.HSC, "ABC School", "Science", 500, null,
            "May 2022", "State Board", Instant.now(), Instant.now()
        );
        when(academicQualificationService.addQualification(eq(1L), any(AcademicQualificationRequest.class)))
            .thenReturn(response);

        mockMvc.perform(post("/admissions/1/qualifications")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").value(1))
            .andExpect(jsonPath("$.qualificationType").value("HSC"));
        verify(academicQualificationService).addQualification(eq(1L), any(AcademicQualificationRequest.class));
    }

    @Test
    void shouldReturnBadRequestWhenQualificationTypeIsNull() throws Exception {
        String json = """
            {"schoolName": "ABC School"}
            """;
        mockMvc.perform(post("/admissions/1/qualifications")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
            .andExpect(status().isBadRequest());
    }

    @Test
    void shouldFindQualificationsByAdmissionId() throws Exception {
        AcademicQualificationResponse response = new AcademicQualificationResponse(
            1L, 1L, QualificationType.HSC, "ABC School", "Science", 500, null,
            "May 2022", "State Board", Instant.now(), Instant.now()
        );
        when(academicQualificationService.findByAdmissionId(1L)).thenReturn(List.of(response));
        mockMvc.perform(get("/admissions/1/qualifications"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(1));
        verify(academicQualificationService).findByAdmissionId(1L);
    }

    @Test
    void shouldDeleteQualification() throws Exception {
        doNothing().when(academicQualificationService).delete(1L);
        mockMvc.perform(delete("/admissions/qualifications/1"))
            .andExpect(status().isNoContent());
        verify(academicQualificationService).delete(1L);
    }

    @Test
    void shouldReturnNotFoundWhenDeletingNonExistentQualification() throws Exception {
        doThrow(new ResourceNotFoundException("Academic qualification not found with id: 999"))
            .when(academicQualificationService).delete(999L);
        mockMvc.perform(delete("/admissions/qualifications/999"))
            .andExpect(status().isNotFound());
    }

    @Test
    void shouldFindDocumentsByAdmissionId() throws Exception {
        AdmissionDocumentResponse docResponse = new AdmissionDocumentResponse(
            1L, 1L, DocumentType.AADHAR_CARD, "aadhar.pdf", "key123",
            null, true, null, null, DocumentVerificationStatus.UPLOADED,
            Instant.now(), Instant.now()
        );
        when(admissionDocumentService.findByAdmissionId(1L)).thenReturn(List.of(docResponse));
        mockMvc.perform(get("/admissions/1/documents"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(1));
        verify(admissionDocumentService).findByAdmissionId(1L);
    }

    @Test
    void shouldUpdateDocumentVerification() throws Exception {
        AdmissionDocumentResponse docResponse = new AdmissionDocumentResponse(
            1L, 1L, DocumentType.AADHAR_CARD, "aadhar.pdf", "key123",
            null, true, "admin", null, DocumentVerificationStatus.VERIFIED,
            Instant.now(), Instant.now()
        );
        when(admissionDocumentService.updateVerification(1L, DocumentVerificationStatus.VERIFIED, "admin"))
            .thenReturn(docResponse);
        mockMvc.perform(patch("/admissions/documents/1/verify")
                .param("status", "VERIFIED")
                .param("verifiedBy", "admin"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.verificationStatus").value("VERIFIED"));
        verify(admissionDocumentService).updateVerification(1L, DocumentVerificationStatus.VERIFIED, "admin");
    }

    @Test
    void shouldGetDocumentChecklist() throws Exception {
        Map<DocumentType, DocumentVerificationStatus> checklist = Map.of(
            DocumentType.AADHAR_CARD, DocumentVerificationStatus.UPLOADED
        );
        when(admissionDocumentService.getChecklist(1L)).thenReturn(checklist);
        mockMvc.perform(get("/admissions/1/documents/checklist"))
            .andExpect(status().isOk());
        verify(admissionDocumentService).getChecklist(1L);
    }
}
