package com.cms.ai.rag;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.cms.ai.OllamaClient;
import com.cms.ai.OllamaUnavailableException;
import com.cms.model.Admission;
import com.cms.model.Enquiry;
import com.cms.model.EnquiryDocument;
import com.cms.repository.EnquiryDocumentRepository;
import com.cms.service.StorageService;

@ExtendWith(MockitoExtension.class)
class DocumentEmbeddingIngestionServiceTest {

    @Mock
    private EnquiryDocumentRepository enquiryDocumentRepository;
    @Mock
    private StorageService storageService;
    @Mock
    private OllamaClient ollamaClient;
    @Mock
    private DocumentEmbeddingRepository documentEmbeddingRepository;

    private DocumentEmbeddingIngestionService service;

    @BeforeEach
    void setUp() {
        service = new DocumentEmbeddingIngestionService(
            enquiryDocumentRepository, storageService, ollamaClient, documentEmbeddingRepository, 10);
    }

    private static EnquiryDocument documentWithId(Long id) {
        EnquiryDocument document = new EnquiryDocument(null, null, null);
        document.setAdmission(new Admission());
        document.setId(id);
        document.setStorageKey("admission/" + id + ".pdf");
        return document;
    }

    private static EnquiryDocument enquiryOnlyDocumentWithId(Long id) {
        EnquiryDocument document = new EnquiryDocument(new Enquiry(), null, null);
        document.setId(id);
        document.setStorageKey("enquiry/" + id + ".pdf");
        return document;
    }

    private static byte[] pdfWithText(String text) throws IOException {
        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage();
            document.addPage(page);
            try (PDPageContentStream content = new PDPageContentStream(document, page)) {
                content.beginText();
                content.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12);
                content.newLineAtOffset(50, 700);
                content.showText(text);
                content.endText();
            }
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            document.save(out);
            return out.toByteArray();
        }
    }

    private static byte[] blankPdf() throws IOException {
        try (PDDocument document = new PDDocument()) {
            document.addPage(new PDPage());
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            document.save(out);
            return out.toByteArray();
        }
    }

    @Test
    void successfulDocument_embedsAndSavesChunksWithoutRecordingASkip() throws IOException {
        EnquiryDocument document = documentWithId(1L);
        when(storageService.downloadBytes(document.getStorageKey())).thenReturn(pdfWithText("Transfer Certificate"));
        when(ollamaClient.embed(anyString())).thenReturn(new float[] {0.1f, 0.2f});

        service.processDocument(document);

        verify(documentEmbeddingRepository).saveChunks(eq("ADMISSION_DOCUMENT"), eq(1L), any(), any());
        verify(documentEmbeddingRepository, never()).recordSkip(any(), any(), any(), any());
    }

    @Test
    void successfulEnquiryStageDocument_isTaggedWithTheEnquirySourceEntity_notAdmission() throws IOException {
        EnquiryDocument document = enquiryOnlyDocumentWithId(7L);
        when(storageService.downloadBytes(document.getStorageKey())).thenReturn(pdfWithText("Tenth Marksheet"));
        when(ollamaClient.embed(anyString())).thenReturn(new float[] {0.4f});

        service.processDocument(document);

        verify(documentEmbeddingRepository).saveChunks(eq("ENQUIRY_DOCUMENT"), eq(7L), any(), any());
        verify(documentEmbeddingRepository, never()).recordSkip(any(), any(), any(), any());
    }

    @Test
    void scannedPdfWithNoText_recordsPermanentNoTextSkip_andNeverEmbeds() throws IOException {
        EnquiryDocument document = documentWithId(2L);
        when(storageService.downloadBytes(document.getStorageKey())).thenReturn(blankPdf());

        service.processDocument(document);

        verify(documentEmbeddingRepository).recordSkip("ADMISSION_DOCUMENT", 2L, "NO_TEXT", null);
        verify(documentEmbeddingRepository, never()).saveChunks(any(), any(), any(), any());
        verify(ollamaClient, never()).embed(anyString());
    }

    @Test
    void corruptFile_recordsPermanentExtractionFailedSkip_andNeverEmbeds() {
        EnquiryDocument document = documentWithId(3L);
        when(storageService.downloadBytes(document.getStorageKey())).thenReturn("not a real pdf".getBytes());

        service.processDocument(document);

        ArgumentCaptor<String> reasonCaptor = ArgumentCaptor.forClass(String.class);
        verify(documentEmbeddingRepository).recordSkip(eq("ADMISSION_DOCUMENT"), eq(3L), reasonCaptor.capture(), any());
        assertThat(reasonCaptor.getValue()).isEqualTo("EXTRACTION_FAILED");
        verify(documentEmbeddingRepository, never()).saveChunks(any(), any(), any(), any());
    }

    @Test
    void transientEmbeddingFailure_doesNotRecordASkip_soTheDocumentIsRetriedNextRun() throws IOException {
        EnquiryDocument document = documentWithId(4L);
        when(storageService.downloadBytes(document.getStorageKey())).thenReturn(pdfWithText("Degree Certificate"));
        when(ollamaClient.embed(anyString())).thenThrow(new OllamaUnavailableException("Ollama is down"));

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> service.processDocument(document))
            .isInstanceOf(OllamaUnavailableException.class);

        verify(documentEmbeddingRepository, never()).recordSkip(any(), any(), any(), any());
        verify(documentEmbeddingRepository, never()).saveChunks(any(), any(), any(), any());
    }

    @Test
    void oneFailingDocumentInABatch_doesNotBlockTheRestOfTheBatch() throws IOException {
        EnquiryDocument failing = documentWithId(5L);
        EnquiryDocument healthy = documentWithId(6L);
        when(enquiryDocumentRepository.findPendingEmbeddingBatch(10)).thenReturn(List.of(failing, healthy));
        when(storageService.downloadBytes(failing.getStorageKey())).thenThrow(new IllegalStateException("MinIO unreachable"));
        when(storageService.downloadBytes(healthy.getStorageKey())).thenReturn(pdfWithText("Eligibility Certificate"));
        when(ollamaClient.embed(anyString())).thenReturn(new float[] {0.3f});

        service.ingestPendingDocuments();

        verify(documentEmbeddingRepository).saveChunks(eq("ADMISSION_DOCUMENT"), eq(6L), any(), any());
        verify(documentEmbeddingRepository, never()).recordSkip(any(), eq(5L), any(), any());
        verify(documentEmbeddingRepository, times(1)).saveChunks(any(), any(), any(), any());
    }
}
