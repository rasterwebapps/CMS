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
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.cms.dto.AdmissionDocumentResponse;
import com.cms.exception.ResourceNotFoundException;
import com.cms.model.Admission;
import com.cms.model.AcademicYear;
import com.cms.model.EnquiryDocument;
import com.cms.model.Program;
import com.cms.model.Student;
import com.cms.model.enums.DocumentType;
import com.cms.model.enums.DocumentVerificationStatus;
import com.cms.model.enums.StudentStatus;
import com.cms.repository.AdmissionRepository;
import com.cms.repository.EnquiryDocumentRepository;

@ExtendWith(MockitoExtension.class)
class AdmissionDocumentServiceTest {

    @Mock
    private EnquiryDocumentRepository enquiryDocumentRepository;

    @Mock
    private AdmissionRepository admissionRepository;

    private AdmissionDocumentService admissionDocumentService;

    @BeforeEach
    void setUp() {
        admissionDocumentService = new AdmissionDocumentService(enquiryDocumentRepository, admissionRepository);
    }

    private Admission createAdmission(Long id) {
        return createAdmission(id, null);
    }

    private Admission createAdmission(Long id, Set<DocumentType> programRequiredTypes) {
        Program program = null;
        if (programRequiredTypes != null) {
            program = new Program("Bachelor", "BACHELOR", 4);
            program.setRequiredDocumentTypes(programRequiredTypes);
        }
        Student student = new Student("ROLL001", "John", "Doe", "john@example.com",
            program, 1, LocalDate.of(2024, 1, 1), StudentStatus.ACTIVE);
        student.setId(1L);
        AcademicYear ay = new AcademicYear("2024-2025", LocalDate.of(2024, 6, 1), LocalDate.of(2025, 5, 31), true);
        ay.setId(100L);
        Admission admission = new Admission(student, ay, LocalDate.of(2024, 1, 15));
        admission.setId(id);
        admission.setCreatedAt(Instant.now());
        admission.setUpdatedAt(Instant.now());
        return admission;
    }

    private EnquiryDocument createDocument(Long id, Admission admission, DocumentType type) {
        EnquiryDocument doc = new EnquiryDocument(null, type, DocumentVerificationStatus.UPLOADED);
        doc.setId(id);
        doc.setAdmission(admission);
        doc.setFileName("file.pdf");
        doc.setStorageKey("key123");
        doc.setCreatedAt(Instant.now());
        doc.setUpdatedAt(Instant.now());
        return doc;
    }

    @Test
    void shouldFindDocumentsByAdmissionId() {
        Admission admission = createAdmission(1L);
        EnquiryDocument doc = createDocument(1L, admission, DocumentType.AADHAR_CARD);
        when(enquiryDocumentRepository.findByAdmission_Id(1L)).thenReturn(List.of(doc));

        List<AdmissionDocumentResponse> responses = admissionDocumentService.findByAdmissionId(1L);

        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).documentType()).isEqualTo(DocumentType.AADHAR_CARD);
        verify(enquiryDocumentRepository).findByAdmission_Id(1L);
    }

    @Test
    void shouldReturnEmptyListWhenNoDocuments() {
        when(enquiryDocumentRepository.findByAdmission_Id(1L)).thenReturn(List.of());
        List<AdmissionDocumentResponse> responses = admissionDocumentService.findByAdmissionId(1L);
        assertThat(responses).isEmpty();
    }

    @Test
    void shouldUpdateDocumentVerification() {
        Admission admission = createAdmission(1L);
        EnquiryDocument doc = createDocument(1L, admission, DocumentType.AADHAR_CARD);
        EnquiryDocument updated = createDocument(1L, admission, DocumentType.AADHAR_CARD);
        updated.setStatus(DocumentVerificationStatus.VERIFIED);
        updated.setVerifiedBy("admin");

        when(enquiryDocumentRepository.findById(1L)).thenReturn(Optional.of(doc));
        when(enquiryDocumentRepository.save(any(EnquiryDocument.class))).thenReturn(updated);

        AdmissionDocumentResponse response = admissionDocumentService.updateVerification(
            1L, DocumentVerificationStatus.VERIFIED, "admin");

        assertThat(response.verificationStatus()).isEqualTo(DocumentVerificationStatus.VERIFIED);
        assertThat(response.verifiedBy()).isEqualTo("admin");
        verify(enquiryDocumentRepository).findById(1L);
        verify(enquiryDocumentRepository).save(any(EnquiryDocument.class));
    }

    @Test
    void shouldThrowExceptionWhenDocumentNotFoundForVerification() {
        when(enquiryDocumentRepository.findById(999L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> admissionDocumentService.updateVerification(
                999L, DocumentVerificationStatus.VERIFIED, "admin"))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessage("Document not found with id: 999");
        verify(enquiryDocumentRepository, never()).save(any());
    }

    @Test
    void shouldGetChecklistWithAllDocumentTypesWhenProgramHasNoMapping() {
        Admission admission = createAdmission(1L);
        EnquiryDocument doc = createDocument(1L, admission, DocumentType.AADHAR_CARD);
        doc.setStatus(DocumentVerificationStatus.VERIFIED);
        when(admissionRepository.findById(1L)).thenReturn(Optional.of(admission));
        when(enquiryDocumentRepository.findByAdmission_Id(1L)).thenReturn(List.of(doc));

        Map<DocumentType, DocumentVerificationStatus> checklist = admissionDocumentService.getChecklist(1L);

        assertThat(checklist).containsKey(DocumentType.AADHAR_CARD);
        assertThat(checklist.get(DocumentType.AADHAR_CARD)).isEqualTo(DocumentVerificationStatus.VERIFIED);
        assertThat(checklist.get(DocumentType.TENTH_MARKSHEET)).isEqualTo(DocumentVerificationStatus.NOT_UPLOADED);
        assertThat(checklist).hasSize(DocumentType.values().length);
    }

    @Test
    void shouldReturnAllDocumentTypesAsNotUploadedWhenNoDocuments() {
        Admission admission = createAdmission(1L);
        when(admissionRepository.findById(1L)).thenReturn(Optional.of(admission));
        when(enquiryDocumentRepository.findByAdmission_Id(1L)).thenReturn(List.of());
        Map<DocumentType, DocumentVerificationStatus> checklist = admissionDocumentService.getChecklist(1L);
        assertThat(checklist).hasSize(DocumentType.values().length);
        checklist.values().forEach(status ->
            assertThat(status).isEqualTo(DocumentVerificationStatus.NOT_UPLOADED));
    }

    @Test
    void shouldGetChecklistOnlyForProgramRequiredTypesWhenConfigured() {
        Set<DocumentType> required = Set.of(
            DocumentType.AADHAR_CARD,
            DocumentType.TENTH_MARKSHEET,
            DocumentType.PASSPORT_PHOTO
        );
        Admission admission = createAdmission(1L, required);
        EnquiryDocument doc = createDocument(1L, admission, DocumentType.AADHAR_CARD);
        doc.setStatus(DocumentVerificationStatus.VERIFIED);
        when(admissionRepository.findById(1L)).thenReturn(Optional.of(admission));
        when(enquiryDocumentRepository.findByAdmission_Id(1L)).thenReturn(List.of(doc));

        Map<DocumentType, DocumentVerificationStatus> checklist = admissionDocumentService.getChecklist(1L);

        assertThat(checklist).hasSize(3);
        assertThat(checklist.keySet()).containsExactlyInAnyOrderElementsOf(required);
        assertThat(checklist.get(DocumentType.AADHAR_CARD)).isEqualTo(DocumentVerificationStatus.VERIFIED);
        assertThat(checklist.get(DocumentType.TENTH_MARKSHEET)).isEqualTo(DocumentVerificationStatus.NOT_UPLOADED);
        assertThat(checklist.get(DocumentType.PASSPORT_PHOTO)).isEqualTo(DocumentVerificationStatus.NOT_UPLOADED);
    }

    @Test
    void shouldThrowWhenAdmissionNotFoundForChecklist() {
        when(admissionRepository.findById(999L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> admissionDocumentService.getChecklist(999L))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessage("Admission not found with id: 999");
    }
}
