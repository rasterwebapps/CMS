package com.cms.ai.rag;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cms.ai.OllamaClient;
import com.cms.ai.rag.DocumentEmbeddingRepository.SimilarChunk;
import com.cms.dto.AdmissionDocumentRagResult;
import com.cms.model.Admission;
import com.cms.model.AdmissionDocument;
import com.cms.model.Student;
import com.cms.repository.AdmissionDocumentRepository;
import com.cms.util.CurrentUserResolver;

/**
 * Retrieval side of the Admission document RAG pipeline (OC-277). Answer mode is raw snippets,
 * not an LLM-synthesized answer (per the specialist round) -- this embeds the question, finds
 * the top-K nearest chunks, and returns them attributed to their source document. If nothing
 * clears the similarity threshold, it says so explicitly (an empty result) rather than letting
 * anything guess an answer.
 */
@Service
public class DocumentRagSearchService {

    private static final String SOURCE_ENTITY = "ADMISSION_DOCUMENT";

    private final OllamaClient ollamaClient;
    private final DocumentEmbeddingRepository documentEmbeddingRepository;
    private final AdmissionDocumentRepository admissionDocumentRepository;
    private final CurrentUserResolver currentUserResolver;
    private final int topK;
    private final double similarityThreshold;

    public DocumentRagSearchService(OllamaClient ollamaClient,
                                     DocumentEmbeddingRepository documentEmbeddingRepository,
                                     AdmissionDocumentRepository admissionDocumentRepository,
                                     CurrentUserResolver currentUserResolver,
                                     @Value("${cms.rag.retrieval-top-k:5}") int topK,
                                     @Value("${cms.rag.similarity-threshold:0.5}") double similarityThreshold) {
        this.ollamaClient = ollamaClient;
        this.documentEmbeddingRepository = documentEmbeddingRepository;
        this.admissionDocumentRepository = admissionDocumentRepository;
        this.currentUserResolver = currentUserResolver;
        this.topK = topK;
        this.similarityThreshold = similarityThreshold;
    }

    @Transactional(readOnly = true)
    public List<AdmissionDocumentRagResult> search(String query) {
        float[] queryEmbedding = ollamaClient.embed(query);
        List<SimilarChunk> candidates = documentEmbeddingRepository.findSimilarChunks(SOURCE_ENTITY, queryEmbedding, topK);

        // Log the full candidate set, not just what clears the threshold -- future eval work
        // (phase 5) needs real scores to calibrate that threshold, not a pre-filtered view of it.
        documentEmbeddingRepository.logQuery(
            currentUserResolver.resolve(),
            query,
            candidates.stream().map(SimilarChunk::chunkId).toList(),
            candidates.stream().map(SimilarChunk::similarity).toList()
        );

        List<SimilarChunk> aboveThreshold = candidates.stream()
            .filter(c -> c.similarity() >= similarityThreshold)
            .toList();
        if (aboveThreshold.isEmpty()) {
            return List.of();
        }

        List<Long> documentIds = aboveThreshold.stream().map(SimilarChunk::sourceId).distinct().toList();
        Map<Long, AdmissionDocument> documentsById = admissionDocumentRepository
            .findByIdInWithAdmissionAndStudent(documentIds)
            .stream()
            .collect(Collectors.toMap(AdmissionDocument::getId, Function.identity()));

        return aboveThreshold.stream()
            .map(chunk -> toResult(chunk, documentsById.get(chunk.sourceId())))
            .filter(Objects::nonNull) // the source document may have been deleted since embedding
            .toList();
    }

    private AdmissionDocumentRagResult toResult(SimilarChunk chunk, AdmissionDocument document) {
        if (document == null) {
            return null;
        }
        Admission admission = document.getAdmission();
        Student student = admission != null ? admission.getStudent() : null;
        String studentName = student == null ? null
            : (nullToEmpty(student.getFirstName()) + " " + nullToEmpty(student.getLastName())).strip();

        return new AdmissionDocumentRagResult(
            document.getId(),
            admission != null ? admission.getId() : null,
            document.getDocumentType(),
            document.getFileName(),
            studentName == null || studentName.isEmpty() ? null : studentName,
            student != null ? student.getAdmissionNumber() : null,
            chunk.chunkText(),
            chunk.similarity()
        );
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}
