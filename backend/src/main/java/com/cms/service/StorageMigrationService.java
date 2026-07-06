package com.cms.service;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

/**
 * One-time migration: reads file_data / photo bytes from PostgreSQL, uploads
 * each file to MinIO, then writes the resulting storage_key back to the row.
 *
 * The file_data columns are NOT nulled out here — they are kept as a safety
 * fallback and can be cleared once the MinIO migration is verified in production.
 *
 * Processing is batched (BATCH_SIZE rows at a time). Each individual file is
 * retried up to MAX_RETRIES times before being counted as a failure.
 * The method never aborts early — it processes every row and reports at the end.
 */
@Service
public class StorageMigrationService {

    private static final Logger log = LoggerFactory.getLogger(StorageMigrationService.class);
    private static final int BATCH_SIZE  = 50;
    private static final int MAX_RETRIES = 5;
    private static final long RETRY_DELAY_MS = 2_000;

    private final JdbcTemplate jdbc;
    private final StorageService storageService;

    public StorageMigrationService(JdbcTemplate jdbc, StorageService storageService) {
        this.jdbc = jdbc;
        this.storageService = storageService;
    }

    // ── Public entry points ────────────────────────────────────────────────────

    public Map<String, Object> pendingCounts() {
        Long enquiryPending = jdbc.queryForObject(
            "SELECT COUNT(*) FROM enquiry_documents WHERE file_data IS NOT NULL AND storage_key IS NULL", Long.class);
        Long facultyPending = jdbc.queryForObject(
            "SELECT COUNT(*) FROM faculty_documents WHERE file_data IS NOT NULL AND storage_key IS NULL", Long.class);
        Long photoPending = jdbc.queryForObject(
            "SELECT COUNT(*) FROM app_users WHERE " +
            "(profile_photo IS NOT NULL AND profile_photo_key IS NULL) OR " +
            "(cover_photo   IS NOT NULL AND cover_photo_key   IS NULL)", Long.class);
        Long enquiryDone = jdbc.queryForObject(
            "SELECT COUNT(*) FROM enquiry_documents WHERE storage_key IS NOT NULL", Long.class);
        Long facultyDone = jdbc.queryForObject(
            "SELECT COUNT(*) FROM faculty_documents WHERE storage_key IS NOT NULL", Long.class);

        Map<String, Object> map = new LinkedHashMap<>();
        map.put("enquiry_documents_pending",  enquiryPending);
        map.put("enquiry_documents_migrated", enquiryDone);
        map.put("faculty_documents_pending",  facultyPending);
        map.put("faculty_documents_migrated", facultyDone);
        map.put("profile_photos_pending",     photoPending);
        return map;
    }

    public MigrationReport run() {
        MigrationReport report = new MigrationReport();
        report.enquiryDocuments = migrateEnquiryDocuments();
        report.facultyDocuments = migrateFacultyDocuments();
        report.profilePhotos    = migrateProfilePhotos();
        return report;
    }

    // ── enquiry_documents ──────────────────────────────────────────────────────

    private TableReport migrateEnquiryDocuments() {
        TableReport r = new TableReport("enquiry_documents");
        int offset = 0;

        while (true) {
            List<long[]> batch = jdbc.query(
                "SELECT id FROM enquiry_documents " +
                "WHERE file_data IS NOT NULL AND storage_key IS NULL " +
                "ORDER BY id LIMIT ? OFFSET ?",
                (rs, i) -> new long[]{ rs.getLong("id") },
                BATCH_SIZE, offset
            );
            if (batch.isEmpty()) break;

            for (long[] row : batch) {
                long id = row[0];
                migrateEnquiryDoc(id, r);
            }

            // If none in this batch had storage_key set yet (all failed),
            // advance offset so we don't loop forever.
            offset += batch.size();
            log.info("[enquiry_documents] processed offset {}: ok={} fail={}", offset, r.succeeded, r.failed);
        }
        return r;
    }

    private void migrateEnquiryDoc(long id, TableReport r) {
        for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {
            try {
                var rows = jdbc.query(
                    "SELECT file_data, file_name, content_type, document_type FROM enquiry_documents WHERE id = ?",
                    (rs, i) -> new Object[]{
                        rs.getBytes("file_data"),
                        rs.getString("file_name"),
                        rs.getString("content_type"),
                        rs.getString("document_type")
                    },
                    id
                );
                if (rows.isEmpty()) { r.failed++; return; }

                byte[] data        = (byte[]) rows.get(0)[0];
                String fileName    = (String) rows.get(0)[1];
                String contentType = (String) rows.get(0)[2];
                String docType     = (String) rows.get(0)[3];

                if (data == null || data.length == 0) { r.skipped++; return; }

                String folder = docType != null ? docType.toLowerCase() : "unknown";
                String key    = MinioStorageService.buildKey(folder, id, fileName);
                String ct     = contentType != null ? contentType : "application/octet-stream";

                storageService.upload(key, new ByteArrayInputStream(data), data.length, ct);
                jdbc.update("UPDATE enquiry_documents SET storage_key = ? WHERE id = ?", key, id);
                r.succeeded++;
                log.debug("[enquiry_documents] migrated id={} → {}", id, key);
                return;

            } catch (Exception e) {
                log.warn("[enquiry_documents] attempt {}/{} failed for id={}: {}", attempt, MAX_RETRIES, id, e.getMessage());
                if (attempt < MAX_RETRIES) sleep(RETRY_DELAY_MS);
            }
        }
        r.failed++;
        r.failedIds.add(id);
        log.error("[enquiry_documents] FAILED permanently for id={}", id);
    }

    // ── faculty_documents ──────────────────────────────────────────────────────

    private TableReport migrateFacultyDocuments() {
        TableReport r = new TableReport("faculty_documents");
        int offset = 0;

        while (true) {
            List<long[]> batch = jdbc.query(
                "SELECT id FROM faculty_documents " +
                "WHERE file_data IS NOT NULL AND storage_key IS NULL " +
                "ORDER BY id LIMIT ? OFFSET ?",
                (rs, i) -> new long[]{ rs.getLong("id") },
                BATCH_SIZE, offset
            );
            if (batch.isEmpty()) break;

            for (long[] row : batch) {
                migrateFacultyDoc(row[0], r);
            }
            offset += batch.size();
            log.info("[faculty_documents] processed offset {}: ok={} fail={}", offset, r.succeeded, r.failed);
        }
        return r;
    }

    private void migrateFacultyDoc(long id, TableReport r) {
        for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {
            try {
                var rows = jdbc.query(
                    "SELECT file_data, file_name, content_type, document_type FROM faculty_documents WHERE id = ?",
                    (rs, i) -> new Object[]{
                        rs.getBytes("file_data"),
                        rs.getString("file_name"),
                        rs.getString("content_type"),
                        rs.getString("document_type")
                    },
                    id
                );
                if (rows.isEmpty()) { r.failed++; return; }

                byte[] data        = (byte[]) rows.get(0)[0];
                String fileName    = (String) rows.get(0)[1];
                String contentType = (String) rows.get(0)[2];
                String docType     = (String) rows.get(0)[3];

                if (data == null || data.length == 0) { r.skipped++; return; }

                String folder = docType != null ? docType.toLowerCase() : "unknown";
                String key    = MinioStorageService.buildKey(folder, id, fileName);
                String ct     = contentType != null ? contentType : "application/octet-stream";

                storageService.upload(key, new ByteArrayInputStream(data), data.length, ct);
                jdbc.update("UPDATE faculty_documents SET storage_key = ? WHERE id = ?", key, id);
                r.succeeded++;
                log.debug("[faculty_documents] migrated id={} → {}", id, key);
                return;

            } catch (Exception e) {
                log.warn("[faculty_documents] attempt {}/{} failed for id={}: {}", attempt, MAX_RETRIES, id, e.getMessage());
                if (attempt < MAX_RETRIES) sleep(RETRY_DELAY_MS);
            }
        }
        r.failed++;
        r.failedIds.add(id);
        log.error("[faculty_documents] FAILED permanently for id={}", id);
    }

    // ── app_users (profile + cover photos) ────────────────────────────────────

    private TableReport migrateProfilePhotos() {
        TableReport r = new TableReport("app_users (photos)");
        int offset = 0;

        while (true) {
            List<long[]> batch = jdbc.query(
                "SELECT id FROM app_users " +
                "WHERE (profile_photo IS NOT NULL AND profile_photo_key IS NULL) " +
                "   OR (cover_photo   IS NOT NULL AND cover_photo_key   IS NULL) " +
                "ORDER BY id LIMIT ? OFFSET ?",
                (rs, i) -> new long[]{ rs.getLong("id") },
                BATCH_SIZE, offset
            );
            if (batch.isEmpty()) break;

            for (long[] row : batch) {
                migrateUserPhotos(row[0], r);
            }
            offset += batch.size();
        }
        return r;
    }

    private void migrateUserPhotos(long userId, TableReport r) {
        var rows = jdbc.query(
            "SELECT profile_photo, profile_photo_type, profile_photo_key, " +
            "       cover_photo,   cover_photo_type,   cover_photo_key " +
            "FROM app_users WHERE id = ?",
            (rs, i) -> new Object[]{
                rs.getBytes("profile_photo"),   rs.getString("profile_photo_type"), rs.getString("profile_photo_key"),
                rs.getBytes("cover_photo"),     rs.getString("cover_photo_type"),   rs.getString("cover_photo_key")
            },
            userId
        );
        if (rows.isEmpty()) return;

        byte[] profileData  = (byte[]) rows.get(0)[0];
        String profileType  = (String) rows.get(0)[1];
        String profileKey   = (String) rows.get(0)[2];
        byte[] coverData    = (byte[]) rows.get(0)[3];
        String coverType    = (String) rows.get(0)[4];
        String coverKey     = (String) rows.get(0)[5];

        if (profileData != null && profileData.length > 0 && (profileKey == null || profileKey.isBlank())) {
            migratePhoto(userId, profileData, profileType, "profile-photo", "profile_photo_key", r);
        }
        if (coverData != null && coverData.length > 0 && (coverKey == null || coverKey.isBlank())) {
            migratePhoto(userId, coverData, coverType, "cover-photo", "cover_photo_key", r);
        }
    }

    private void migratePhoto(long userId, byte[] data, String contentType,
                               String folder, String keyColumn, TableReport r) {
        String ext = "image/png".equals(contentType) ? ".png" : ".jpg";
        String key = folder + "/" + userId + "-"
            + java.util.UUID.randomUUID().toString().replace("-", "").substring(0, 8) + ext;
        String ct  = contentType != null ? contentType : "image/jpeg";

        for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {
            try {
                storageService.upload(key, new ByteArrayInputStream(data), data.length, ct);
                jdbc.update("UPDATE app_users SET " + keyColumn + " = ? WHERE id = ?", key, userId);
                r.succeeded++;
                log.debug("[app_users] migrated {} for userId={} → {}", keyColumn, userId, key);
                return;
            } catch (Exception e) {
                log.warn("[app_users] attempt {}/{} failed {} for userId={}: {}", attempt, MAX_RETRIES, keyColumn, userId, e.getMessage());
                if (attempt < MAX_RETRIES) sleep(RETRY_DELAY_MS);
            }
        }
        r.failed++;
        r.failedIds.add(userId);
        log.error("[app_users] FAILED permanently {} for userId={}", keyColumn, userId);
    }

    // ── Helpers ────────────────────────────────────────────────────────────────

    private void sleep(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }

    // ── Result types ───────────────────────────────────────────────────────────

    public static class TableReport {
        public final String table;
        public int succeeded = 0;
        public int failed    = 0;
        public int skipped   = 0;
        public final List<Long> failedIds = new ArrayList<>();

        public TableReport(String table) { this.table = table; }
    }

    public static class MigrationReport {
        public TableReport enquiryDocuments;
        public TableReport facultyDocuments;
        public TableReport profilePhotos;

        public boolean allSucceeded() {
            return enquiryDocuments.failed == 0
                && facultyDocuments.failed == 0
                && profilePhotos.failed == 0;
        }
    }
}
