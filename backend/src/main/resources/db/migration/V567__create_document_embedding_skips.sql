-- Tracks Admission documents the RAG ingestion job (OC-277, DocumentEmbeddingIngestionService)
-- has permanently given up on, so the @Scheduled sweep doesn't re-attempt them every run
-- forever. Only genuinely permanent outcomes are recorded here:
--   NO_TEXT            -- PDF extracted cleanly but has no selectable text (scanned/image PDF;
--                         OCR is an explicit phase-2, not retried until that ships)
--   EXTRACTION_FAILED  -- PDFBox could not read the file at all (corrupt/encrypted/not a PDF)
-- Transient failures (storage temporarily unreachable, Ollama embedding call timing out) are
-- deliberately NOT recorded here -- those documents simply have no row in either this table or
-- document_embeddings yet, so the next run retries them naturally.

CREATE TABLE document_embedding_skips (
    id            BIGSERIAL PRIMARY KEY,
    source_entity VARCHAR(30)  NOT NULL,
    source_id     BIGINT       NOT NULL,
    reason        VARCHAR(30)  NOT NULL,
    detail        TEXT,
    created_at    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_document_embedding_skips UNIQUE (source_entity, source_id)
);
