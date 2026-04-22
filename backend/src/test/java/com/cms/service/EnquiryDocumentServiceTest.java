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

    @Test
    void shouldUploadFileAndCreateNewDocumentWhenNoneExists() {
        when(enquiryRepository.findById(1L)).thenReturn(Optional.of(testEnquiry));
        when(documentRepository.findByEnquiryId(1L)).thenReturn(List.of());
        when(documentRepository.save(any(EnquiryDocument.class))).thenAnswer(inv -> {
            EnquiryDocument d = inv.getArgument(0);
            d.setId(42L);
            d.setCreatedAt(Instant.now());
            d.setUpdatedAt(Instant.now());
            return d;
        });

        org.springframework.mock.web.MockMultipartFile file =
            new org.springframework.mock.web.MockMultipartFile(
                "file", "scan.pdf", "application/pdf", "PDF-CONTENT".getBytes()
            );

        EnquiryDocumentResponse response = documentService.uploadFile(
            1L, DocumentType.TENTH_MARKSHEET, "note", file
        );

        assertThat(response.id()).isEqualTo(42L);
        assertThat(response.fileName()).isEqualTo("scan.pdf");
        assertThat(response.contentType()).isEqualTo("application/pdf");
        assertThat(response.fileSize()).isEqualTo(11L);
        assertThat(response.hasFile()).isTrue();
        assertThat(response.status()).isEqualTo(DocumentVerificationStatus.UPLOADED);
        assertThat(response.remarks()).isEqualTo("note");
    }

    @Test
    void shouldUploadFileAndReplaceExistingDocumentOfSameType() {
        EnquiryDocument existing = createDocument(7L, testEnquiry, DocumentType.TENTH_MARKSHEET);
        existing.setRemarks("old remarks");
        when(enquiryRepository.findById(1L)).thenReturn(Optional.of(testEnquiry));
        when(documentRepository.findByEnquiryId(1L)).thenReturn(List.of(existing));
        when(documentRepository.save(any(EnquiryDocument.class))).thenAnswer(inv -> inv.getArgument(0));

        org.springframework.mock.web.MockMultipartFile file =
            new org.springframework.mock.web.MockMultipartFile(
                "file", "/uploads/new.png", "image/png", new byte[]{1, 2, 3}
            );

        // remarks=null means existing remarks should be preserved
        EnquiryDocumentResponse response = documentService.uploadFile(
            1L, DocumentType.TENTH_MARKSHEET, null, file
        );

        assertThat(response.id()).isEqualTo(7L);
        // Path components stripped from file name.
        assertThat(response.fileName()).isEqualTo("new.png");
        assertThat(response.contentType()).isEqualTo("image/png");
        assertThat(response.fileSize()).isEqualTo(3L);
        assertThat(response.status()).isEqualTo(DocumentVerificationStatus.UPLOADED);
        assertThat(response.remarks()).isEqualTo("old remarks");
    }

    @Test
    void shouldRejectEmptyFileOnUpload() {
        org.springframework.mock.web.MockMultipartFile empty =
            new org.springframework.mock.web.MockMultipartFile(
                "file", "empty.pdf", "application/pdf", new byte[0]
            );
        assertThatThrownBy(() ->
            documentService.uploadFile(1L, DocumentType.TENTH_MARKSHEET, null, empty))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void shouldRejectNullDocumentTypeOnUpload() {
        org.springframework.mock.web.MockMultipartFile file =
            new org.springframework.mock.web.MockMultipartFile(
                "file", "scan.pdf", "application/pdf", "X".getBytes()
            );
        assertThatThrownBy(() ->
            documentService.uploadFile(1L, null, null, file))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void shouldRejectOversizedFileOnUpload() {
        // Build a "large" file by mocking getSize() rather than allocating real memory.
        org.springframework.web.multipart.MultipartFile huge =
            org.mockito.Mockito.mock(org.springframework.web.multipart.MultipartFile.class);
        when(huge.isEmpty()).thenReturn(false);
        when(huge.getSize()).thenReturn(EnquiryDocumentService.MAX_FILE_SIZE_BYTES + 1);

        assertThatThrownBy(() ->
            documentService.uploadFile(1L, DocumentType.TENTH_MARKSHEET, null, huge))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("maximum");
    }

    @Test
    void shouldThrowWhenEnquiryNotFoundOnUpload() {
        org.springframework.mock.web.MockMultipartFile file =
            new org.springframework.mock.web.MockMultipartFile(
                "file", "scan.pdf", "application/pdf", "X".getBytes()
            );
        when(enquiryRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() ->
            documentService.uploadFile(999L, DocumentType.TENTH_MARKSHEET, null, file))
            .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void shouldDownloadFile() {
        EnquiryDocument doc = createDocument(5L, testEnquiry, DocumentType.TENTH_MARKSHEET);
        doc.setFileName("scan.pdf");
        doc.setContentType("application/pdf");
        doc.setFileData("PDF".getBytes());
        when(documentRepository.findById(5L)).thenReturn(Optional.of(doc));

        com.cms.dto.DocumentFileDownload download = documentService.getFileForDownload(5L);

        assertThat(download.fileName()).isEqualTo("scan.pdf");
        assertThat(download.contentType()).isEqualTo("application/pdf");
        assertThat(download.data()).isEqualTo("PDF".getBytes());
    }

    @Test
    void shouldDownloadFileWithFallbackMetadataWhenMissing() {
        EnquiryDocument doc = createDocument(5L, testEnquiry, DocumentType.TENTH_MARKSHEET);
        doc.setFileData("PDF".getBytes());
        // fileName and contentType intentionally null
        when(documentRepository.findById(5L)).thenReturn(Optional.of(doc));

        com.cms.dto.DocumentFileDownload download = documentService.getFileForDownload(5L);

        assertThat(download.fileName()).isEqualTo("TENTH_MARKSHEET");
        assertThat(download.contentType()).isEqualTo("application/octet-stream");
    }

    @Test
    void shouldThrowWhenDownloadingDocumentWithoutFile() {
        EnquiryDocument doc = createDocument(5L, testEnquiry, DocumentType.TENTH_MARKSHEET);
        // No file data set
        when(documentRepository.findById(5L)).thenReturn(Optional.of(doc));

        assertThatThrownBy(() -> documentService.getFileForDownload(5L))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessageContaining("No file");
    }

    @Test
    void shouldThrowWhenDownloadingNonExistentDocument() {
        when(documentRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> documentService.getFileForDownload(999L))
            .isInstanceOf(ResourceNotFoundException.class);
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
