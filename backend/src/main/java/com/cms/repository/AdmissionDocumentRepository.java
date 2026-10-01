package com.cms.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.cms.model.AdmissionDocument;
import com.cms.model.enums.DocumentType;
import com.cms.model.enums.DocumentVerificationStatus;

public interface AdmissionDocumentRepository extends JpaRepository<AdmissionDocument, Long> {

    List<AdmissionDocument> findByAdmissionId(Long admissionId);

    /**
     * Admission documents the RAG ingestion job (OC-277) hasn't embedded yet and hasn't
     * permanently given up on -- see document_embeddings/document_embedding_skips
     * (DocumentEmbeddingIngestionService). Restricted to {@code .pdf} files: OCR for
     * scanned-image uploads is an explicit phase-2, not v1.
     */
    @Query(value = """
        SELECT ad.* FROM admission_documents ad
        WHERE ad.storage_key IS NOT NULL
          AND ad.file_name ILIKE '%.pdf'
          AND NOT EXISTS (
              SELECT 1 FROM document_embeddings de
              WHERE de.source_entity = 'ADMISSION_DOCUMENT' AND de.source_id = ad.id
          )
          AND NOT EXISTS (
              SELECT 1 FROM document_embedding_skips s
              WHERE s.source_entity = 'ADMISSION_DOCUMENT' AND s.source_id = ad.id
          )
        ORDER BY ad.id
        LIMIT :batchSize
        """, nativeQuery = true)
    List<AdmissionDocument> findPendingEmbeddingBatch(@Param("batchSize") int batchSize);

    /**
     * Fetch-joins Admission and Student so RAG search results (OC-277) can show which student
     * each snippet belongs to without a LazyInitializationException -- {@code open-in-view} is
     * disabled in this codebase, so that join must happen inside this one query/transaction.
     */
    @Query("SELECT ad FROM AdmissionDocument ad "
        + "LEFT JOIN FETCH ad.admission a "
        + "LEFT JOIN FETCH a.student "
        + "WHERE ad.id IN :ids")
    List<AdmissionDocument> findByIdInWithAdmissionAndStudent(@Param("ids") List<Long> ids);

    Optional<AdmissionDocument> findByAdmissionIdAndDocumentType(Long admissionId, DocumentType documentType);

    List<AdmissionDocument> findByAdmissionIdAndVerificationStatus(Long admissionId, DocumentVerificationStatus verificationStatus);

    boolean existsByAdmissionIdAndDocumentType(Long admissionId, DocumentType documentType);

    void deleteByAdmissionId(Long admissionId);
}
