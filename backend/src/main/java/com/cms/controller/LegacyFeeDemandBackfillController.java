package com.cms.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.cms.dto.LegacyFeeDemandBackfillApplyResult;
import com.cms.dto.LegacyFeeDemandBackfillSummary;
import com.cms.dto.LegacyTermOverrideApplyResult;
import com.cms.dto.LegacyTermOverrideSummary;
import com.cms.service.LegacyFeeDemandBackfillService;

/**
 * One-time migration operation (see the legacy-to-FeeDemand billing backfill plan) -- no nav
 * entry, no frontend UI. Invoked directly by an admin once per term instance being backfilled.
 */
@RestController
@RequestMapping("/finance/legacy-backfill")
public class LegacyFeeDemandBackfillController {

    private final LegacyFeeDemandBackfillService backfillService;

    public LegacyFeeDemandBackfillController(LegacyFeeDemandBackfillService backfillService) {
        this.backfillService = backfillService;
    }

    @GetMapping("/audit")
    @PreAuthorize("@perm.has('FINANCE_LEGACY_BACKFILL_RUN')")
    public ResponseEntity<LegacyFeeDemandBackfillSummary> audit(@RequestParam Long termInstanceId) {
        return ResponseEntity.ok(backfillService.auditCandidates(termInstanceId));
    }

    @PostMapping("/apply")
    @PreAuthorize("@perm.has('FINANCE_LEGACY_BACKFILL_RUN')")
    public ResponseEntity<LegacyFeeDemandBackfillApplyResult> apply(@RequestParam Long termInstanceId) {
        return ResponseEntity.ok(backfillService.applyBackfill(termInstanceId));
    }

    @GetMapping("/audit-term-overrides")
    @PreAuthorize("@perm.has('FINANCE_LEGACY_BACKFILL_RUN')")
    public ResponseEntity<LegacyTermOverrideSummary> auditTermOverrides(@RequestParam Long termInstanceId) {
        return ResponseEntity.ok(backfillService.auditFutureTermOverrides(termInstanceId));
    }

    @PostMapping("/apply-term-overrides")
    @PreAuthorize("@perm.has('FINANCE_LEGACY_BACKFILL_RUN')")
    public ResponseEntity<LegacyTermOverrideApplyResult> applyTermOverrides(@RequestParam Long termInstanceId) {
        return ResponseEntity.ok(backfillService.applyFutureTermOverrides(termInstanceId));
    }
}
