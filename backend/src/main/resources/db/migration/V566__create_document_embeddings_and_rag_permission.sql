-- Local RAG pipeline for Admission document Q&A (R&D spike, local-only -- see AI Smart Search
-- precedent, STUDENT_AI_SEARCH_VIEW/V565). Scope is Admission documents only for v1; other
-- document-bearing entities (Enquiry/Faculty/Compliance) are explicitly deferred.
--
-- source_entity/source_id is a generic (entity_type, entity_id) pair rather than a hard FK to
-- admission_documents, so a later phase can add Faculty/Compliance documents without widening
-- this table's shape -- same pattern BR-60's FloorPlan uses for its generic entity attachment.

CREATE EXTENSION IF NOT EXISTS vector;

CREATE TABLE document_embeddings (
    id            BIGSERIAL PRIMARY KEY,
    source_entity VARCHAR(30)  NOT NULL,
    source_id     BIGINT       NOT NULL,
    chunk_index   INTEGER      NOT NULL,
    chunk_text    TEXT         NOT NULL,
    embedding     vector(768)  NOT NULL,
    created_at    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_document_embeddings_chunk UNIQUE (source_entity, source_id, chunk_index)
);

CREATE INDEX idx_document_embeddings_source ON document_embeddings (source_entity, source_id);

CREATE TABLE document_rag_query_log (
    id                BIGSERIAL PRIMARY KEY,
    queried_by        VARCHAR(255) NOT NULL,
    query_text        TEXT         NOT NULL,
    retrieved_chunk_ids BIGINT[]   NOT NULL,
    similarity_scores   DOUBLE PRECISION[] NOT NULL,
    created_at        TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- New dedicated permission for this feature, per the operation-wise permission mapping hard
-- gate -- not reused from DOCUMENT_SUBMISSION_MANAGE/DOCUMENT_VERIFICATION_MANAGE (V88).
-- Defaults to DOCUMENT_VERIFICATION_MANAGE's tier (closest match -- that permission already
-- implies reading document content for verification, not just managing submission metadata).
-- Who else gets it afterward is handled entirely via the DB-only Role Management module, not
-- hardcoded here.

INSERT INTO permissions (code, display_name, category, screen_label, created_at) VALUES
    ('ADMISSION_DOCUMENT_AI_SEARCH_VIEW', 'AI Document Search (Admission)', 'ADMISSION', 'AI Document Search', CURRENT_TIMESTAMP)
ON CONFLICT (code) DO NOTHING;

INSERT INTO role_permissions (role_id, permission_id)
SELECT rp.role_id, new_p.id
FROM role_permissions rp
JOIN permissions old_p ON rp.permission_id = old_p.id AND old_p.code = 'DOCUMENT_VERIFICATION_MANAGE'
CROSS JOIN (SELECT id FROM permissions WHERE code = 'ADMISSION_DOCUMENT_AI_SEARCH_VIEW') new_p
WHERE NOT EXISTS (
    SELECT 1 FROM role_permissions x WHERE x.role_id = rp.role_id AND x.permission_id = new_p.id
);

-- DEV_ADMIN / SUPPORT_ADMIN catch-all sync
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM app_roles r
CROSS JOIN permissions p
WHERE r.name IN ('DEV_ADMIN', 'SUPPORT_ADMIN')
  AND NOT EXISTS (
      SELECT 1 FROM role_permissions rp
      WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );
