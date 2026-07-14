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
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.cms.dto.AdmissionDocumentResponse;
import com.cms.dto.DocumentChecklistResponse;
import com.cms.exception.ResourceNotFoundException;
import com.cms.model.Admission;
import com.cms.model.AcademicYear;
import com.cms.model.EnquiryDocument;
import com.cms.model.Program;
import com.cms.model.ProgramDocumentRequirement;
import com.cms.model.Student;
import com.cms.model.enums.DocumentType;
import com.cms.model.enums.DocumentVerificationStatus;
import com.cms.model.enums.ProgramDocumentCategory;
import com.cms.model.enums.StudentStatus;
import com.cms.repository.AdmissionRepository;
import com.cms.repository.EnquiryDocumentRepository;

@ExtendWith(MockitoExtension.class)
class AdmissionDocumentServiceTest {

    @Mock
    private EnquiryDocumentRepository enquiryDocumentRepository;

    @Mock
    private com.cms.repository.EnquiryDocumentHistoryRepository historyRepository;

    @Mock
    private AdmissionRepository admissionRepository;

    @Mock
    private com.cms.util.CurrentUserResolver currentUserResolver;

    @Mock
    private com.cms.config.PermSecurityBean permSecurityBean;

    @Mock
    private StorageService storageService;

    private AdmissionDocumentService admissionDocumentService;

    @BeforeEach
    void setUp() {
        admissionDocumentService = new AdmissionDocumentService(enquiryDocumentRepository, historyRepository,
            admissionRepository, currentUserResolver, permSecurityBean, storageService);
    }

    private Admission createAdmission(Long id) {
        return createAdmission(id, null);
    }

    private Admission createAdmission(Long id, Set<DocumentType> mandatoryTypes) {
        Program program = null;
        if (mandatoryTypes != null) {
            program = new Program("Bachelor", "BACHELOR", 4);
            Set<ProgramDocumentRequirement> reqs = new java.util.HashSet<>();
            for (DocumentType t : mandatoryTypes) {
                reqs.add(new ProgramDocumentRequirement(t, ProgramDocumentCategory.MANDATORY));
            }
            program.setDocumentRequirements(reqs);
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
    void shouldReturnEmptyChecklistWhenProgramHasNoDocumentConfig() {
        Admission admission = createAdmission(1L);
        when(admissionRepository.findById(1L)).thenReturn(Optional.of(admission));
        when(enquiryDocumentRepository.findByAdmission_Id(1L)).thenReturn(List.of());

        DocumentChecklistResponse checklist = admissionDocumentService.getChecklist(1L);

        assertThat(checklist.mandatory()).isEmpty();
        assertThat(checklist.optional()).isEmpty();
    }

    @Test
    void shouldGetChecklistForConfiguredMandatoryTypes() {
        Set<DocumentType> mandatory = Set.of(
            DocumentType.AADHAR_CARD,
            DocumentType.TENTH_MARKSHEET,
            DocumentType.PASSPORT_PHOTO
        );
        Admission admission = createAdmission(1L, mandatory);
        EnquiryDocument doc = createDocument(1L, admission, DocumentType.AADHAR_CARD);
        doc.setStatus(DocumentVerificationStatus.VERIFIED);
        when(admissionRepository.findById(1L)).thenReturn(Optional.of(admission));
        when(enquiryDocumentRepository.findByAdmission_Id(1L)).thenReturn(List.of(doc));

        DocumentChecklistResponse checklist = admissionDocumentService.getChecklist(1L);

        assertThat(checklist.mandatory()).hasSize(3);
        assertThat(checklist.optional()).isEmpty();
        assertThat(checklist.mandatory().get(DocumentType.AADHAR_CARD.name())).isEqualTo(DocumentVerificationStatus.VERIFIED.name());
        assertThat(checklist.mandatory().get(DocumentType.TENTH_MARKSHEET.name())).isEqualTo(DocumentVerificationStatus.NOT_UPLOADED.name());
        assertThat(checklist.mandatory().get(DocumentType.PASSPORT_PHOTO.name())).isEqualTo(DocumentVerificationStatus.NOT_UPLOADED.name());
    }

    @Test
    void shouldThrowWhenAdmissionNotFoundForChecklist() {
        when(admissionRepository.findById(999L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> admissionDocumentService.getChecklist(999L))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessage("Admission not found with id: 999");
    }
}
