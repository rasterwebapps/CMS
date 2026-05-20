package com.cms.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import com.cms.dto.DocumentFileDownload;
import com.cms.dto.FacultyDocumentHistoryResponse;
import com.cms.dto.FacultyDocumentRequest;
import com.cms.dto.FacultyDocumentResponse;
import com.cms.exception.ResourceNotFoundException;
import com.cms.model.Faculty;
import com.cms.model.FacultyDocument;
import com.cms.model.FacultyDocumentHistory;
import com.cms.model.enums.DocumentType;
import com.cms.model.enums.DocumentVerificationStatus;
import com.cms.repository.FacultyDocumentHistoryRepository;
import com.cms.repository.FacultyDocumentRepository;
import com.cms.repository.FacultyRepository;
import com.cms.util.CurrentUserResolver;

@ExtendWith(MockitoExtension.class)
class FacultyDocumentServiceTest {

    @Mock
    private FacultyDocumentRepository documentRepository;

    @Mock
    private FacultyRepository facultyRepository;

    @Mock
    private FacultyDocumentHistoryRepository historyRepository;

    @Mock
    private CurrentUserResolver currentUserResolver;

    @InjectMocks
    private FacultyDocumentService service;

    @Test
    void findByFacultyIdShouldThrowWhenFacultyMissing() {
        when(facultyRepository.existsById(10L)).thenReturn(false);

        assertThatThrownBy(() -> service.findByFacultyId(10L))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessageContaining("Faculty not found with id: 10");
    }

    @Test
    void findByFacultyIdShouldMapDocuments() {
        when(facultyRepository.existsById(10L)).thenReturn(true);

        Faculty faculty = new Faculty();
        faculty.setId(10L);

        FacultyDocument doc = new FacultyDocument(faculty, DocumentType.PAN_CARD, DocumentVerificationStatus.UPLOADED);
        doc.setId(99L);
        doc.setFileName("pan.pdf");

        when(documentRepository.findByFacultyId(10L)).thenReturn(List.of(doc));

        List<FacultyDocumentResponse> out = service.findByFacultyId(10L);
        assertThat(out).hasSize(1);
        assertThat(out.getFirst().id()).isEqualTo(99L);
        assertThat(out.getFirst().facultyId()).isEqualTo(10L);
        assertThat(out.getFirst().hasFile()).isTrue();
    }

    @Test
    void addDocumentShouldDefaultStatusWhenNull() {
        Faculty faculty = new Faculty();
        faculty.setId(10L);

        when(facultyRepository.findById(10L)).thenReturn(Optional.of(faculty));
        when(documentRepository.save(any(FacultyDocument.class))).thenAnswer(inv -> inv.getArgument(0));

        FacultyDocumentRequest req = new FacultyDocumentRequest(DocumentType.UG_DEGREE, null, "remarks");
        FacultyDocumentResponse out = service.addDocument(10L, req);

        assertThat(out.documentType()).isEqualTo(DocumentType.UG_DEGREE);
        assertThat(out.status()).isEqualTo(DocumentVerificationStatus.NOT_UPLOADED);
        assertThat(out.remarks()).isEqualTo("remarks");
    }

    @Test
    void updateDocumentShouldNotOverwriteStatusWhenNull() {
        Faculty faculty = new Faculty();
        faculty.setId(10L);

        FacultyDocument existing = new FacultyDocument(faculty, DocumentType.UG_DEGREE, DocumentVerificationStatus.UPLOADED);
        existing.setId(1L);

        when(documentRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(documentRepository.save(any(FacultyDocument.class))).thenAnswer(inv -> inv.getArgument(0));

        FacultyDocumentRequest req = new FacultyDocumentRequest(DocumentType.PG_DEGREE, null, "updated");
        FacultyDocumentResponse out = service.updateDocument(1L, req);

        assertThat(out.documentType()).isEqualTo(DocumentType.PG_DEGREE);
        assertThat(out.status()).isEqualTo(DocumentVerificationStatus.UPLOADED);
        assertThat(out.remarks()).isEqualTo("updated");
    }

    @Test
    void updateDocumentShouldVerifyUploadedDocumentWithReviewerMetadata() {
        Faculty faculty = new Faculty();
        faculty.setId(10L);

        FacultyDocument existing = new FacultyDocument(faculty, DocumentType.UG_DEGREE, DocumentVerificationStatus.UPLOADED);
        existing.setId(1L);
        existing.setFileName("degree.pdf");

        when(documentRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(documentRepository.save(any(FacultyDocument.class))).thenAnswer(inv -> inv.getArgument(0));
        when(currentUserResolver.resolve()).thenReturn("reviewer@college.edu");

        FacultyDocumentRequest req = new FacultyDocumentRequest(DocumentType.UG_DEGREE, DocumentVerificationStatus.VERIFIED, null);
        FacultyDocumentResponse out = service.updateDocument(1L, req);

        assertThat(out.status()).isEqualTo(DocumentVerificationStatus.VERIFIED);
        assertThat(out.verifiedBy()).isEqualTo("reviewer@college.edu");
        assertThat(out.verifiedAt()).isNotNull();
        verify(historyRepository).save(any());
    }

    @Test
    void updateDocumentShouldRequireRejectionReason() {
        Faculty faculty = new Faculty();
        faculty.setId(10L);
        FacultyDocument existing = new FacultyDocument(faculty, DocumentType.UG_DEGREE, DocumentVerificationStatus.UPLOADED);
        existing.setId(1L);
        existing.setFileName("degree.pdf");

        when(documentRepository.findById(1L)).thenReturn(Optional.of(existing));

        FacultyDocumentRequest req = new FacultyDocumentRequest(DocumentType.UG_DEGREE, DocumentVerificationStatus.REJECTED, " ");

        assertThatThrownBy(() -> service.updateDocument(1L, req))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Rejection reason is required");
    }

    @Test
    void deleteDocumentShouldThrowWhenMissing() {
        when(documentRepository.existsById(123L)).thenReturn(false);

        assertThatThrownBy(() -> service.deleteDocument(123L))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessageContaining("Document not found with id: 123");
    }

    @Test
    void uploadFileShouldRejectInvalidInputs() {
        assertThatThrownBy(() -> service.uploadFile(1L, null, null, null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("documentType is required");

        MockMultipartFile empty = new MockMultipartFile("file", "a.txt", "text/plain", new byte[0]);
        assertThatThrownBy(() -> service.uploadFile(1L, DocumentType.PAN_CARD, null, empty))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("File is required");
    }

    @Test
    void uploadFileShouldCreateOrUpdateAndSanitizeFileName() {
        Faculty faculty = new Faculty();
        faculty.setId(10L);

        when(facultyRepository.findById(10L)).thenReturn(Optional.of(faculty));
        when(documentRepository.findByFacultyId(10L)).thenReturn(List.of());
        when(documentRepository.save(any(FacultyDocument.class))).thenAnswer(inv -> {
            FacultyDocument saved = inv.getArgument(0);
            saved.setId(55L);
            return saved;
        });

        MockMultipartFile file = new MockMultipartFile(
            "file",
            "C:/fakepath/pan.pdf",
            "application/pdf",
            "hello".getBytes()
        );

        FacultyDocumentResponse out = service.uploadFile(10L, DocumentType.PAN_CARD, "remark", file);

        assertThat(out.id()).isEqualTo(55L);
        assertThat(out.documentType()).isEqualTo(DocumentType.PAN_CARD);
        assertThat(out.status()).isEqualTo(DocumentVerificationStatus.UPLOADED);
        assertThat(out.fileName()).isEqualTo("pan.pdf");
        assertThat(out.contentType()).isEqualTo("application/pdf");
        assertThat(out.fileSize()).isEqualTo(5);
        assertThat(out.uploadedAt()).isNotNull();
        assertThat(out.hasFile()).isTrue();

        verify(documentRepository).save(any(FacultyDocument.class));
    }

    @Test
    void uploadFileShouldWrapIoErrors() throws IOException {
        Faculty faculty = new Faculty();
        faculty.setId(10L);

        when(facultyRepository.findById(10L)).thenReturn(Optional.of(faculty));
        when(documentRepository.findByFacultyId(10L)).thenReturn(List.of());

        MockMultipartFile file = new MockMultipartFile("file", "pan.pdf", "application/pdf", "x".getBytes()) {
            @Override
            public byte[] getBytes() throws IOException {
                throw new IOException("boom");
            }
        };

        assertThatThrownBy(() -> service.uploadFile(10L, DocumentType.PAN_CARD, null, file))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("Failed to read uploaded file");
    }

    @Test
    void getFileForDownloadShouldDefaultNameAndContentType() {
        Faculty faculty = new Faculty();
        faculty.setId(10L);

        FacultyDocument doc = new FacultyDocument(faculty, DocumentType.PAN_CARD, DocumentVerificationStatus.UPLOADED);
        doc.setId(77L);
        doc.setFileData("abc".getBytes());
        doc.setFileName(null);
        doc.setContentType(null);

        when(documentRepository.findById(77L)).thenReturn(Optional.of(doc));

        DocumentFileDownload dl = service.getFileForDownload(77L);
        assertThat(dl.fileName()).isEqualTo("PAN_CARD");
        assertThat(dl.contentType()).isEqualTo("application/octet-stream");
        assertThat(dl.data()).containsExactly("abc".getBytes());
    }

    @Test
    void getFileForDownloadShouldThrowWhenNoData() {
        Faculty faculty = new Faculty();
        faculty.setId(10L);

        FacultyDocument doc = new FacultyDocument(faculty, DocumentType.PAN_CARD, DocumentVerificationStatus.UPLOADED);
        doc.setId(77L);
        doc.setFileData(new byte[0]);

        when(documentRepository.findById(77L)).thenReturn(Optional.of(doc));

        assertThatThrownBy(() -> service.getFileForDownload(77L))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessageContaining("No file uploaded for document id: 77");
    }

    @Test
    void uploadFileShouldSetUploadedAtToNow() {
        Faculty faculty = new Faculty();
        faculty.setId(10L);

        FacultyDocument existing = new FacultyDocument(faculty, DocumentType.PAN_CARD, DocumentVerificationStatus.NOT_UPLOADED);
        existing.setId(11L);

        when(facultyRepository.findById(10L)).thenReturn(Optional.of(faculty));
        when(documentRepository.findByFacultyId(10L)).thenReturn(List.of(existing));
        when(documentRepository.save(any(FacultyDocument.class))).thenAnswer(inv -> inv.getArgument(0));

        Instant before = Instant.now();
        MockMultipartFile file = new MockMultipartFile("file", "pan.pdf", "application/pdf", "hello".getBytes());

        FacultyDocumentResponse out = service.uploadFile(10L, DocumentType.PAN_CARD, null, file);
        assertThat(out.uploadedAt()).isAfterOrEqualTo(before);
        assertThat(out.status()).isEqualTo(DocumentVerificationStatus.UPLOADED);
        assertThat(out.hasFile()).isTrue();

        verify(documentRepository).save(eq(existing));
    }

    @Test
    void shouldGetHistoryForDocument() {
        when(documentRepository.existsById(77L)).thenReturn(true);

        Faculty faculty = new Faculty();
        faculty.setId(10L);
        FacultyDocument doc = new FacultyDocument(faculty, DocumentType.PAN_CARD, DocumentVerificationStatus.UPLOADED);
        doc.setId(77L);

        FacultyDocumentHistory history = new FacultyDocumentHistory();
        history.setFacultyDocument(doc);
        history.setDocumentType(DocumentType.PAN_CARD);
        history.setPreviousStatus(DocumentVerificationStatus.NOT_UPLOADED);
        history.setNewStatus(DocumentVerificationStatus.UPLOADED);
        history.setFileName("pan.pdf");
        history.setChangedBy("admin");
        history.setChangedAt(java.time.Instant.now());

        when(historyRepository.findByFacultyDocumentIdOrderByChangedAtDesc(77L))
            .thenReturn(List.of(history));

        List<FacultyDocumentHistoryResponse> result = service.getHistory(77L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).documentType()).isEqualTo(DocumentType.PAN_CARD);
        assertThat(result.get(0).newStatus()).isEqualTo(DocumentVerificationStatus.UPLOADED);
        assertThat(result.get(0).changedBy()).isEqualTo("admin");
    }

    @Test
    void shouldThrowWhenGetHistoryForNonExistentDocument() {
        when(documentRepository.existsById(999L)).thenReturn(false);

        assertThatThrownBy(() -> service.getHistory(999L))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessageContaining("Document not found with id: 999");
    }

    @Test
    void uploadFileShouldRejectReplacementOfVerifiedDocument() {
        Faculty faculty = new Faculty();
        faculty.setId(10L);

        FacultyDocument existing = new FacultyDocument(faculty, DocumentType.PAN_CARD, DocumentVerificationStatus.VERIFIED);
        existing.setId(11L);
        existing.setFileName("pan.pdf");

        when(facultyRepository.findById(10L)).thenReturn(Optional.of(faculty));
        when(documentRepository.findByFacultyId(10L)).thenReturn(List.of(existing));

        MockMultipartFile file = new MockMultipartFile("file", "new-pan.pdf", "application/pdf", "hello".getBytes());

        assertThatThrownBy(() -> service.uploadFile(10L, DocumentType.PAN_CARD, null, file))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("Verified documents cannot be replaced");
    }
}
