package com.cms.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.cms.model.EnquiryDocument;
import com.cms.model.enums.DocumentType;

public interface EnquiryDocumentRepository extends JpaRepository<EnquiryDocument, Long> {

    List<EnquiryDocument> findByEnquiryId(Long enquiryId);

    List<EnquiryDocument> findByAdmission_Id(Long admissionId);

    /**
     * Admission-scoped documents (non-null {@code admission_id} -- see V124's "unify applicant
     * documents" migration, which made this table canonical for both Enquiry and Admission
     * document APIs) the RAG ingestion job (OC-277) hasn't embedded yet
     * and hasn't permanently given up on -- see document_embeddings/document_embedding_skips
     * (DocumentEmbeddingIngestionService). Restricted to VERIFIED documents only (don't index
     * content staff haven't signed off on, or have actively rejected) and {@code .pdf} files --
     * OCR for scanned-image uploads is an explicit phase-2, not v1.
     */
    @Query(value = """
        SELECT ed.* FROM enquiry_documents ed
        WHERE ed.admission_id IS NOT NULL
          AND ed.status = 'VERIFIED'
          AND ed.storage_key IS NOT NULL
          AND ed.file_name ILIKE '%.pdf'
          AND NOT EXISTS (
              SELECT 1 FROM document_embeddings de
              WHERE de.source_entity = 'ADMISSION_DOCUMENT' AND de.source_id = ed.id
          )
          AND NOT EXISTS (
              SELECT 1 FROM document_embedding_skips s
              WHERE s.source_entity = 'ADMISSION_DOCUMENT' AND s.source_id = ed.id
          )
        ORDER BY ed.id
        LIMIT :batchSize
        """, nativeQuery = true)
    List<EnquiryDocument> findPendingEmbeddingBatch(@Param("batchSize") int batchSize);

    /**
     * Fetch-joins Admission and Student so RAG search results (OC-277) can show which student
     * each snippet belongs to without a LazyInitializationException -- {@code open-in-view} is
     * disabled in this codebase, so that join must happen inside this one query/transaction.
     */
    @Query("SELECT ed FROM EnquiryDocument ed "
        + "LEFT JOIN FETCH ed.admission a "
        + "LEFT JOIN FETCH a.student "
        + "WHERE ed.id IN :ids")
    List<EnquiryDocument> findByIdInWithAdmissionAndStudent(@Param("ids") List<Long> ids);
}
