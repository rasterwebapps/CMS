package com.cms.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.cms.dto.EnquiryDocumentRequest;
import com.cms.dto.EnquiryDocumentResponse;
import com.cms.exception.ResourceNotFoundException;
import com.cms.model.Enquiry;
import com.cms.model.EnquiryDocument;
import com.cms.model.ReferralType;
import com.cms.model.enums.DocumentType;
import com.cms.model.enums.DocumentVerificationStatus;
import com.cms.model.enums.EnquiryStatus;
import com.cms.repository.EnquiryDocumentRepository;
import com.cms.repository.EnquiryRepository;

@ExtendWith(MockitoExtension.class)
class EnquiryDocumentServiceTest {

    @Mock
    private EnquiryDocumentRepository documentRepository;
    @Mock
    private EnquiryRepository enquiryRepository;

    private EnquiryDocumentService documentService;

    private Enquiry testEnquiry;

    @BeforeEach
    void setUp() {
        documentService = new EnquiryDocumentService(documentRepository, enquiryRepository);

        testEnquiry = new Enquiry("Test", "test@email.com", "1234567890", null,
            java.time.LocalDate.now(), new ReferralType("Walk In", "WALK_IN", java.math.BigDecimal.ZERO, false, "Walk in", true), EnquiryStatus.FEES_PAID);
        testEnquiry.setId(1L);
        testEnquiry.setCreatedAt(Instant.now());
        testEnquiry.setUpdatedAt(Instant.now());
    }

    @Test
    void shouldAddDocument() {
        EnquiryDocumentRequest request = new EnquiryDocumentRequest(
            DocumentType.TENTH_MARKSHEET, null, "10th certificate"
        );

        EnquiryDocument saved = createDocument(1L, testEnquiry, DocumentType.TENTH_MARKSHEET);

        when(enquiryRepository.findById(1L)).thenReturn(Optional.of(testEnquiry));
        when(documentRepository.save(any(EnquiryDocument.class))).thenReturn(saved);

        EnquiryDocumentResponse response = documentService.addDocument(1L, request);

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.documentType()).isEqualTo(DocumentType.TENTH_MARKSHEET);
        assertThat(response.status()).isEqualTo(DocumentVerificationStatus.NOT_UPLOADED);
    }

    @Test
    void shouldNotAutoUpdateStatusToDocumentsSubmitted() {
        testEnquiry.setStatus(EnquiryStatus.FEES_PAID);

        EnquiryDocumentRequest request = new EnquiryDocumentRequest(
            DocumentType.TENTH_MARKSHEET, DocumentVerificationStatus.UPLOADED, null
        );

        EnquiryDocument saved = createDocument(1L, testEnquiry, DocumentType.TENTH_MARKSHEET);
        saved.setStatus(DocumentVerificationStatus.UPLOADED);

        when(enquiryRepository.findById(1L)).thenReturn(Optional.of(testEnquiry));
        when(documentRepository.save(any(EnquiryDocument.class))).thenReturn(saved);

        documentService.addDocument(1L, request);

        // Auto-transition was removed; enquiryRepository.save should never be called
        verify(enquiryRepository, never()).save(any(Enquiry.class));
    }

    @Test
    void shouldNotAutoUpdateStatusWhenNotFeesPaid() {
        testEnquiry.setStatus(EnquiryStatus.INTERESTED);

        EnquiryDocumentRequest request = new EnquiryDocumentRequest(
            DocumentType.TENTH_MARKSHEET, null, null
        );

        EnquiryDocument saved = createDocument(1L, testEnquiry, DocumentType.TENTH_MARKSHEET);

        when(enquiryRepository.findById(1L)).thenReturn(Optional.of(testEnquiry));
        when(documentRepository.save(any(EnquiryDocument.class))).thenReturn(saved);

        documentService.addDocument(1L, request);

        // enquiryRepository.save should not be called for status update when status is INTERESTED
        verify(enquiryRepository, never()).save(any(Enquiry.class));
    }

    @Test
    void shouldThrowWhenEnquiryNotFoundOnAdd() {
        EnquiryDocumentRequest request = new EnquiryDocumentRequest(
            DocumentType.TENTH_MARKSHEET, null, null
        );

        when(enquiryRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> documentService.addDocument(999L, request))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessage("Enquiry not found with id: 999");
    }

    @Test
    void shouldFindByEnquiryId() {
        EnquiryDocument doc = createDocument(1L, testEnquiry, DocumentType.TENTH_MARKSHEET);

        when(enquiryRepository.existsById(1L)).thenReturn(true);
        when(documentRepository.findByEnquiryId(1L)).thenReturn(List.of(doc));

        List<EnquiryDocumentResponse> responses = documentService.findByEnquiryId(1L);

        assertThat(responses).hasSize(1);
        verify(documentRepository).findByEnquiryId(1L);
    }

    @Test
    void shouldThrowWhenEnquiryNotFoundOnFind() {
        when(enquiryRepository.existsById(999L)).thenReturn(false);

        assertThatThrownBy(() -> documentService.findByEnquiryId(999L))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessage("Enquiry not found with id: 999");
    }

    @Test
    void shouldUpdateDocument() {
        EnquiryDocument existing = createDocument(1L, testEnquiry, DocumentType.TENTH_MARKSHEET);
        EnquiryDocumentRequest request = new EnquiryDocumentRequest(
            DocumentType.TWELFTH_MARKSHEET, DocumentVerificationStatus.VERIFIED, "Verified"
        );

        EnquiryDocument updated = createDocument(1L, testEnquiry, DocumentType.TWELFTH_MARKSHEET);
        updated.setStatus(DocumentVerificationStatus.VERIFIED);

        when(documentRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(documentRepository.save(any(EnquiryDocument.class))).thenReturn(updated);

        EnquiryDocumentResponse response = documentService.updateDocument(1L, request);

        assertThat(response.documentType()).isEqualTo(DocumentType.TWELFTH_MARKSHEET);
        assertThat(response.status()).isEqualTo(DocumentVerificationStatus.VERIFIED);
    }

    @Test
    void shouldThrowWhenDocumentNotFoundOnUpdate() {
        EnquiryDocumentRequest request = new EnquiryDocumentRequest(
            DocumentType.TENTH_MARKSHEET, null, null
        );

        when(documentRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> documentService.updateDocument(999L, request))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessage("Document not found with id: 999");
    }

    @Test
    void shouldDeleteDocument() {
        when(documentRepository.existsById(1L)).thenReturn(true);

        documentService.deleteDocument(1L);

        verify(documentRepository).deleteById(1L);
    }

    @Test
    void shouldThrowWhenDeletingNonExistent() {
        when(documentRepository.existsById(999L)).thenReturn(false);

        assertThatThrownBy(() -> documentService.deleteDocument(999L))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessage("Document not found with id: 999");

        verify(documentRepository, never()).deleteById(any());
    }

    @Test
    void shouldReturnAllSubmittedWhenMandatoryDocsPresent() {
        testEnquiry.setStatus(EnquiryStatus.FEES_PAID);

        EnquiryDocument doc1 = createDocument(1L, testEnquiry, com.cms.model.enums.DocumentType.TENTH_MARKSHEET);
        doc1.setStatus(DocumentVerificationStatus.UPLOADED);
        EnquiryDocument doc2 = createDocument(2L, testEnquiry, com.cms.model.enums.DocumentType.TWELFTH_MARKSHEET);
        doc2.setStatus(DocumentVerificationStatus.VERIFIED);
        EnquiryDocument doc3 = createDocument(3L, testEnquiry, com.cms.model.enums.DocumentType.TRANSFER_CERTIFICATE);
        doc3.setStatus(DocumentVerificationStatus.UPLOADED);
        EnquiryDocument doc4 = createDocument(4L, testEnquiry, com.cms.model.enums.DocumentType.AADHAR_CARD);
        doc4.setStatus(DocumentVerificationStatus.UPLOADED);
        EnquiryDocument doc5 = createDocument(5L, testEnquiry, com.cms.model.enums.DocumentType.PASSPORT_PHOTO);
        doc5.setStatus(DocumentVerificationStatus.UPLOADED);

        when(enquiryRepository.existsById(1L)).thenReturn(true);
        when(documentRepository.findByEnquiryId(1L)).thenReturn(List.of(doc1, doc2, doc3, doc4, doc5));

        com.cms.dto.MissingDocumentsResponse response = documentService.allMandatoryDocumentsSubmitted(1L);

        assertThat(response.allSubmitted()).isTrue();
        assertThat(response.missingDocumentTypes()).isEmpty();
    }

    @Test
    void shouldReturnMissingDocsWhenSomeMandatoryDocsAbsent() {
        when(enquiryRepository.existsById(1L)).thenReturn(true);
        when(documentRepository.findByEnquiryId(1L)).thenReturn(List.of());

        com.cms.dto.MissingDocumentsResponse response = documentService.allMandatoryDocumentsSubmitted(1L);

        assertThat(response.allSubmitted()).isFalse();
        assertThat(response.missingDocumentTypes()).hasSize(5);
    }

    @Test
    void shouldThrowWhenEnquiryNotFoundOnMandatoryCheck() {
        when(enquiryRepository.existsById(999L)).thenReturn(false);

        assertThatThrownBy(() -> documentService.allMandatoryDocumentsSubmitted(999L))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessage("Enquiry not found with id: 999");
    }

    private EnquiryDocument createDocument(Long id, Enquiry enquiry, DocumentType type) {
        EnquiryDocument doc = new EnquiryDocument(enquiry, type, DocumentVerificationStatus.NOT_UPLOADED);
        doc.setId(id);
        Instant now = Instant.now();
        doc.setCreatedAt(now);
        doc.setUpdatedAt(now);
        return doc;
    }
}
