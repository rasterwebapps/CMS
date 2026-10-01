package com.cms.ai.rag;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.cms.ai.OllamaClient;
import com.cms.model.EnquiryDocument;
import com.cms.repository.EnquiryDocumentRepository;
import com.cms.service.StorageService;

/**
 * RAG document-ingestion sweep (OC-277/OC-278): finds VERIFIED applicant documents (an {@code
 * enquiry_documents} row -- V124's "unify applicant documents" migration made that table
 * canonical for both Enquiry and Admission document APIs) with no embeddings yet, extracts text,
 * chunks it, embeds each chunk locally via Ollama, and stores the result. Covers both
 * Enquiry-stage documents (no Admission yet) and Admission-stage documents alike -- tagged with
 * different {@code source_entity} values (see {@link #sourceEntityFor}) so retrieval can
 * attribute each chunk back to the right kind of record, but otherwise processed identically.
 * Follows this codebase's only existing background-work idiom -- a {@code @Scheduled} polling
 * sweep (see {@code AcademicTermAlertService}) -- since there is no {@code @Async}/queue
 * infrastructure anywhere else to hook into.
 * <p>
 * Each document is processed in its own {@code REQUIRES_NEW} transaction so one bad document
 * (corrupt PDF, a transient Ollama/storage failure) can never roll back or block the rest of the
 * batch -- the same lesson the auto-schedule transaction-poisoning fix already established
 * elsewhere in this codebase.
 */
@Service
public class DocumentEmbeddingIngestionService {

    private static final Logger log = LoggerFactory.getLogger(DocumentEmbeddingIngestionService.class);
    static final String ADMISSION_SOURCE_ENTITY = "ADMISSION_DOCUMENT";
    static final String ENQUIRY_SOURCE_ENTITY = "ENQUIRY_DOCUMENT";

    private final EnquiryDocumentRepository enquiryDocumentRepository;
    private final StorageService storageService;
    private final OllamaClient ollamaClient;
    private final DocumentEmbeddingRepository documentEmbeddingRepository;
    private final int batchSize;

    public DocumentEmbeddingIngestionService(EnquiryDocumentRepository enquiryDocumentRepository,
                                              StorageService storageService,
                                              OllamaClient ollamaClient,
                                              DocumentEmbeddingRepository documentEmbeddingRepository,
                                              @Value("${cms.rag.ingestion-batch-size:10}") int batchSize) {
        this.enquiryDocumentRepository = enquiryDocumentRepository;
        this.storageService = storageService;
        this.ollamaClient = ollamaClient;
        this.documentEmbeddingRepository = documentEmbeddingRepository;
        this.batchSize = batchSize;
    }

    @Scheduled(fixedDelayString = "${cms.rag.ingestion-interval-ms:300000}")
    public void ingestPendingDocuments() {
        List<EnquiryDocument> pending = enquiryDocumentRepository.findPendingEmbeddingBatch(batchSize);
        for (EnquiryDocument document : pending) {
            try {
                processDocument(document);
            } catch (Exception e) {
                // Deliberately no skip row here -- this branch is for failures that may be
                // transient (storage unreachable, Ollama timing out), so the next sweep retries.
                log.warn("RAG ingestion: failed to process admission document {} this run, will retry: {}",
                    document.getId(), e.getMessage());
            }
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void processDocument(EnquiryDocument document) {
        String sourceEntity = sourceEntityFor(document);
        byte[] bytes = storageService.downloadBytes(document.getStorageKey());

        String text;
        try {
            text = PdfTextExtractor.extractText(bytes);
        } catch (IOException e) {
            log.warn("RAG ingestion: PDF extraction failed for document {} -- permanently skipping: {}",
                document.getId(), e.getMessage());
            documentEmbeddingRepository.recordSkip(sourceEntity, document.getId(), "EXTRACTION_FAILED", e.getMessage());
            return;
        }

        List<String> chunks = DocumentChunker.chunk(text);
        if (chunks.isEmpty()) {
            log.info("RAG ingestion: document {} has no extractable text (scanned/image PDF) -- "
                + "permanently skipping, OCR is phase 2", document.getId());
            documentEmbeddingRepository.recordSkip(sourceEntity, document.getId(), "NO_TEXT", null);
            return;
        }

        List<float[]> embeddings = new ArrayList<>(chunks.size());
        for (String chunk : chunks) {
            embeddings.add(ollamaClient.embed(chunk));
        }

        documentEmbeddingRepository.saveChunks(sourceEntity, document.getId(), chunks, embeddings);
        log.info("RAG ingestion: embedded document {} ({}) into {} chunk(s)", document.getId(), sourceEntity, chunks.size());
    }

    /** Admission-linked documents are tagged distinctly from still-Enquiry-stage ones (OC-278). */
    private static String sourceEntityFor(EnquiryDocument document) {
        return document.getAdmission() != null ? ADMISSION_SOURCE_ENTITY : ENQUIRY_SOURCE_ENTITY;
    }
}
