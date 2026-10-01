package com.cms.ai.rag;

import java.util.List;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * Plain-JDBC DAO for {@code document_embeddings}/{@code document_embedding_skips} (RAG
 * ingestion, OC-277). Not a Spring Data JPA repository: the {@code vector} column type isn't a
 * standard JPA/Hibernate mapping in this codebase, and hand-rolled JDBC for this one narrow
 * read/write shape is simpler than introducing a custom Hibernate UserType for it.
 */
@Repository
public class DocumentEmbeddingRepository {

    private final JdbcTemplate jdbcTemplate;

    public DocumentEmbeddingRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * Inserts one row per chunk. Idempotent: re-running ingestion for a document that already
     * has rows for a given chunk index is a no-op for that index, matching the unique
     * constraint on (source_entity, source_id, chunk_index).
     */
    public void saveChunks(String sourceEntity, Long sourceId, List<String> chunks, List<float[]> embeddings) {
        if (chunks.size() != embeddings.size()) {
            throw new IllegalArgumentException("chunks and embeddings must be the same size");
        }
        for (int i = 0; i < chunks.size(); i++) {
            jdbcTemplate.update(
                """
                INSERT INTO document_embeddings (source_entity, source_id, chunk_index, chunk_text, embedding)
                VALUES (?, ?, ?, ?, ?::vector)
                ON CONFLICT (source_entity, source_id, chunk_index) DO NOTHING
                """,
                sourceEntity, sourceId, i, chunks.get(i), toVectorLiteral(embeddings.get(i))
            );
        }
    }

    /**
     * Records a permanent skip so the ingestion sweep stops retrying this document. Upserts on
     * (source_entity, source_id) -- see V567 for which reasons belong here vs. which transient
     * failures are deliberately never recorded.
     */
    public void recordSkip(String sourceEntity, Long sourceId, String reason, String detail) {
        jdbcTemplate.update(
            """
            INSERT INTO document_embedding_skips (source_entity, source_id, reason, detail)
            VALUES (?, ?, ?, ?)
            ON CONFLICT (source_entity, source_id)
            DO UPDATE SET reason = EXCLUDED.reason, detail = EXCLUDED.detail, created_at = CURRENT_TIMESTAMP
            """,
            sourceEntity, sourceId, reason, detail
        );
    }

    static String toVectorLiteral(float[] embedding) {
        StringBuilder sb = new StringBuilder(embedding.length * 8 + 2);
        sb.append('[');
        for (int i = 0; i < embedding.length; i++) {
            if (i > 0) {
                sb.append(',');
            }
            sb.append(embedding[i]);
        }
        sb.append(']');
        return sb.toString();
    }
}
