package com.cms.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.cms.dto.AdmissionDocumentResponse;
import com.cms.exception.ResourceNotFoundException;
import com.cms.model.Admission;
import com.cms.model.AdmissionDocument;
import com.cms.model.Student;
import com.cms.model.enums.AdmissionStatus;
import com.cms.model.enums.DocumentType;
import com.cms.model.enums.DocumentVerificationStatus;
import com.cms.model.enums.StudentStatus;
import com.cms.repository.AdmissionDocumentRepository;

@ExtendWith(MockitoExtension.class)
class AdmissionDocumentServiceTest {

    @Mock
    private AdmissionDocumentRepository admissionDocumentRepository;

    private AdmissionDocumentService admissionDocumentService;

    @BeforeEach
    void setUp() {
        admissionDocumentService = new AdmissionDocumentService(admissionDocumentRepository);
    }

    private Admission createAdmission(Long id) {
        Student student = new Student("ROLL001", "John", "Doe", "john@example.com",
            null, 1, LocalDate.of(2024, 1, 1), StudentStatus.ACTIVE);
        student.setId(1L);
        Admission admission = new Admission(student, 2024, 2025, LocalDate.of(2024, 1, 15), AdmissionStatus.DRAFT);
        admission.setId(id);
        admission.setCreatedAt(Instant.now());
        admission.setUpdatedAt(Instant.now());
        return admission;
    }

    private AdmissionDocument createDocument(Long id, Admission admission, DocumentType type) {
        AdmissionDocument doc = new AdmissionDocument(admission, type, DocumentVerificationStatus.UPLOADED);
        doc.setId(id);
        doc.setFileName("file.pdf");
        doc.setStorageKey("key123");
        doc.setCreatedAt(Instant.now());
        doc.setUpdatedAt(Instant.now());
        return doc;
    }

    @Test
    void shouldFindDocumentsByAdmissionId() {
        Admission admission = createAdmission(1L);
        AdmissionDocument doc = createDocument(1L, admission, DocumentType.AADHAR_CARD);
        when(admissionDocumentRepository.findByAdmissionId(1L)).thenReturn(List.of(doc));

        List<AdmissionDocumentResponse> responses = admissionDocumentService.findByAdmissionId(1L);

        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).documentType()).isEqualTo(DocumentType.AADHAR_CARD);
        verify(admissionDocumentRepository).findByAdmissionId(1L);
    }

    @Test
    void shouldReturnEmptyListWhenNoDocuments() {
        when(admissionDocumentRepository.findByAdmissionId(1L)).thenReturn(List.of());
        List<AdmissionDocumentResponse> responses = admissionDocumentService.findByAdmissionId(1L);
        assertThat(responses).isEmpty();
    }

    @Test
    void shouldUpdateDocumentVerification() {
        Admission admission = createAdmission(1L);
        AdmissionDocument doc = createDocument(1L, admission, DocumentType.AADHAR_CARD);
        AdmissionDocument updated = createDocument(1L, admission, DocumentType.AADHAR_CARD);
        updated.setVerificationStatus(DocumentVerificationStatus.VERIFIED);
        updated.setVerifiedBy("admin");

        when(admissionDocumentRepository.findById(1L)).thenReturn(Optional.of(doc));
        when(admissionDocumentRepository.save(any(AdmissionDocument.class))).thenReturn(updated);

        AdmissionDocumentResponse response = admissionDocumentService.updateVerification(
            1L, DocumentVerificationStatus.VERIFIED, "admin");

        assertThat(response.verificationStatus()).isEqualTo(DocumentVerificationStatus.VERIFIED);
        assertThat(response.verifiedBy()).isEqualTo("admin");
        verify(admissionDocumentRepository).findById(1L);
        verify(admissionDocumentRepository).save(any(AdmissionDocument.class));
    }

    @Test
    void shouldThrowExceptionWhenDocumentNotFoundForVerification() {
        when(admissionDocumentRepository.findById(999L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> admissionDocumentService.updateVerification(
                999L, DocumentVerificationStatus.VERIFIED, "admin"))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessage("Admission document not found with id: 999");
        verify(admissionDocumentRepository, never()).save(any());
    }

    @Test
    void shouldGetChecklistWithAllDocumentTypes() {
        Admission admission = createAdmission(1L);
        AdmissionDocument doc = createDocument(1L, admission, DocumentType.AADHAR_CARD);
        doc.setVerificationStatus(DocumentVerificationStatus.VERIFIED);
        when(admissionDocumentRepository.findByAdmissionId(1L)).thenReturn(List.of(doc));

        Map<DocumentType, DocumentVerificationStatus> checklist = admissionDocumentService.getChecklist(1L);

        assertThat(checklist).containsKey(DocumentType.AADHAR_CARD);
        assertThat(checklist.get(DocumentType.AADHAR_CARD)).isEqualTo(DocumentVerificationStatus.VERIFIED);
        assertThat(checklist.get(DocumentType.TENTH_MARKSHEET)).isEqualTo(DocumentVerificationStatus.NOT_UPLOADED);
        assertThat(checklist).hasSize(DocumentType.values().length);
    }

    @Test
    void shouldReturnAllDocumentTypesAsNotUploadedWhenNoDocuments() {
        when(admissionDocumentRepository.findByAdmissionId(1L)).thenReturn(List.of());
        Map<DocumentType, DocumentVerificationStatus> checklist = admissionDocumentService.getChecklist(1L);
        assertThat(checklist).hasSize(DocumentType.values().length);
        checklist.values().forEach(status ->
            assertThat(status).isEqualTo(DocumentVerificationStatus.NOT_UPLOADED));
    }
}
