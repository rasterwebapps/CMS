package com.cms.controller;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.cms.service.StorageMigrationService;
import com.cms.service.StorageMigrationService.MigrationReport;
import com.cms.service.StorageMigrationService.TableReport;

/**
 * Admin-only endpoint to migrate existing file_data blobs from PostgreSQL to MinIO.
 * Run once after deploying MinIO. Check /status first, then POST /run.
 *
 * All endpoints require the DEV_ADMIN role.
 */
@RestController
@RequestMapping("/admin/migrate-storage")
@PreAuthorize("hasRole('DEV_ADMIN')")
public class StorageMigrationController {

    private final StorageMigrationService migrationService;

    public StorageMigrationController(StorageMigrationService migrationService) {
        this.migrationService = migrationService;
    }

    /**
     * Returns a count of rows still pending migration — safe, read-only.
     * Use this to check progress or confirm migration is complete.
     */
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> status() {
        // Delegated to the service via raw JDBC for a lightweight count-only query.
        Map<String, Object> counts = migrationService.pendingCounts();
        return ResponseEntity.ok(counts);
    }

    /**
     * Runs the migration synchronously. Processes all pending rows in batches,
     * retrying each file up to 5 times before marking it as failed.
     * Returns a full report of successes, failures, and failed IDs.
     *
     * This call may take several minutes depending on the number of files.
     * It is safe to call multiple times — already-migrated rows are skipped.
     */
    @PostMapping("/run")
    public ResponseEntity<Map<String, Object>> run() {
        MigrationReport report = migrationService.run();
        return ResponseEntity.ok(toMap(report));
    }

    private Map<String, Object> toMap(MigrationReport report) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("enquiry_documents", tableMap(report.enquiryDocuments));
        result.put("faculty_documents", tableMap(report.facultyDocuments));
        result.put("profile_photos",    tableMap(report.profilePhotos));
        result.put("all_succeeded",     report.allSucceeded());
        return result;
    }

    private Map<String, Object> tableMap(TableReport r) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("table",      r.table);
        m.put("succeeded",  r.succeeded);
        m.put("failed",     r.failed);
        m.put("skipped",    r.skipped);
        m.put("failed_ids", r.failedIds);
        return m;
    }
}
