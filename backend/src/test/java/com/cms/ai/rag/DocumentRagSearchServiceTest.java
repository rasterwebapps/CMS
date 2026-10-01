package com.cms.ai.rag;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.cms.ai.OllamaClient;
import com.cms.ai.rag.DocumentEmbeddingRepository.SimilarChunk;
import com.cms.dto.AdmissionDocumentRagResult;
import com.cms.model.Admission;
import com.cms.model.EnquiryDocument;
import com.cms.model.Student;
import com.cms.model.enums.DocumentType;
import com.cms.repository.EnquiryDocumentRepository;
import com.cms.util.CurrentUserResolver;

@ExtendWith(MockitoExtension.class)
class DocumentRagSearchServiceTest {

    private static final double THRESHOLD = 0.5;

    @Mock
    private OllamaClient ollamaClient;
    @Mock
    private DocumentEmbeddingRepository documentEmbeddingRepository;
    @Mock
    private EnquiryDocumentRepository enquiryDocumentRepository;
    @Mock
    private CurrentUserResolver currentUserResolver;

    private DocumentRagSearchService service;

    @BeforeEach
    void setUp() {
        service = new DocumentRagSearchService(
            ollamaClient, documentEmbeddingRepository, enquiryDocumentRepository, currentUserResolver, 5, THRESHOLD);
        when(ollamaClient.embed(any())).thenReturn(new float[] {0.1f});
        when(currentUserResolver.resolve()).thenReturn("staff.member");
    }

    private EnquiryDocument documentWithStudent(Long id, String firstName, String lastName, String admissionNumber) {
        Student student = new Student();
        student.setFirstName(firstName);
        student.setLastName(lastName);
        student.setAdmissionNumber(admissionNumber);
        Admission admission = new Admission();
        admission.setStudent(student);
        EnquiryDocument document = new EnquiryDocument(null, DocumentType.TRANSFER_CERTIFICATE, null);
        document.setAdmission(admission);
        document.setId(id);
        document.setFileName("tc.pdf");
        return document;
    }

    @Test
    void noCandidatesAtAll_returnsEmptyList_butStillLogsTheQuery() {
        when(documentEmbeddingRepository.findSimilarChunks(eq("ADMISSION_DOCUMENT"), any(), anyInt())).thenReturn(List.of());

        List<AdmissionDocumentRagResult> results = service.search("what school did Jane attend?");

        assertThat(results).isEmpty();
        verify(documentEmbeddingRepository).logQuery(eq("staff.member"), eq("what school did Jane attend?"), eq(List.of()), eq(List.of()));
        verify(enquiryDocumentRepository, never()).findByIdInWithAdmissionAndStudent(any());
    }

    @Test
    void allCandidatesBelowThreshold_returnsEmptyList_withoutGuessingAnAnswer() {
        SimilarChunk belowThreshold = new SimilarChunk(1L, 10L, "unrelated text", THRESHOLD - 0.1);
        when(documentEmbeddingRepository.findSimilarChunks(any(), any(), anyInt())).thenReturn(List.of(belowThreshold));

        List<AdmissionDocumentRagResult> results = service.search("irrelevant question");

        assertThat(results).isEmpty();
        verify(enquiryDocumentRepository, never()).findByIdInWithAdmissionAndStudent(any());
        // the low score is still logged for future eval/threshold-calibration work
        verify(documentEmbeddingRepository).logQuery(any(), any(), eq(List.of(1L)), eq(List.of(THRESHOLD - 0.1)));
    }

    @Test
    void candidateAboveThreshold_resolvesToAFullyAttributedResult() {
        SimilarChunk strongMatch = new SimilarChunk(2L, 20L, "Transfer certificate issued to Jane Doe", 0.87);
        when(documentEmbeddingRepository.findSimilarChunks(any(), any(), anyInt())).thenReturn(List.of(strongMatch));
        when(enquiryDocumentRepository.findByIdInWithAdmissionAndStudent(List.of(20L)))
            .thenReturn(List.of(documentWithStudent(20L, "Jane", "Doe", "ADM-001")));

        List<AdmissionDocumentRagResult> results = service.search("where did Jane study before?");

        assertThat(results).hasSize(1);
        AdmissionDocumentRagResult result = results.get(0);
        assertThat(result.admissionDocumentId()).isEqualTo(20L);
        assertThat(result.studentName()).isEqualTo("Jane Doe");
        assertThat(result.admissionNumber()).isEqualTo("ADM-001");
        assertThat(result.chunkText()).isEqualTo("Transfer certificate issued to Jane Doe");
        assertThat(result.similarity()).isEqualTo(0.87);
    }

    @Test
    void matchedDocumentNoLongerExists_isSilentlyDroppedRatherThanCrashing() {
        SimilarChunk orphanedChunk = new SimilarChunk(3L, 30L, "stale chunk for a deleted document", 0.9);
        when(documentEmbeddingRepository.findSimilarChunks(any(), any(), anyInt())).thenReturn(List.of(orphanedChunk));
        when(enquiryDocumentRepository.findByIdInWithAdmissionAndStudent(List.of(30L))).thenReturn(List.of());

        List<AdmissionDocumentRagResult> results = service.search("anything");

        assertThat(results).isEmpty();
    }
}
