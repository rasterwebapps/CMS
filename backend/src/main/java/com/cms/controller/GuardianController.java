package com.cms.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.cms.dto.GuardianRequest;
import com.cms.dto.GuardianResponse;
import com.cms.dto.WardSummaryResponse;
import com.cms.service.GuardianService;

import jakarta.validation.Valid;

@RestController
public class GuardianController {

    private final GuardianService guardianService;

    public GuardianController(GuardianService guardianService) {
        this.guardianService = guardianService;
    }

    @GetMapping("/guardians")
    @PreAuthorize("@perm.hasAny('GUARDIAN_VIEW', 'GUARDIAN_MANAGE')")
    public ResponseEntity<List<GuardianResponse>> findAll() {
        return ResponseEntity.ok(guardianService.findAll());
    }

    @PostMapping("/guardians")
    @PreAuthorize("@perm.has('GUARDIAN_MANAGE')")
    public ResponseEntity<GuardianResponse> create(@Valid @RequestBody GuardianRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(guardianService.create(request));
    }

    /** Async uniqueness check for the Guardian create form, matching this codebase's
     *  uniqueFieldValidator directive / {@code *-exists} endpoint pattern used by every other
     *  master form. */
    @GetMapping("/guardians/email-exists")
    @PreAuthorize("@perm.has('GUARDIAN_MANAGE')")
    public ResponseEntity<Boolean> emailExists(
            @RequestParam String value,
            @RequestParam(required = false) Long excludeId) {
        return ResponseEntity.ok(guardianService.emailExists(value, excludeId));
    }

    @PostMapping("/guardians/{guardianId}/wards/{studentId}")
    @PreAuthorize("@perm.has('GUARDIAN_MANAGE')")
    public ResponseEntity<Void> linkWard(@PathVariable Long guardianId, @PathVariable Long studentId,
                                          @RequestParam(defaultValue = "false") boolean isPrimary) {
        guardianService.linkToStudent(guardianId, studentId, isPrimary);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    /** Current authenticated guardian's own wards (parent self-service portal). */
    @GetMapping("/guardian/wards")
    @PreAuthorize("@perm.hasAny('MY_WARD_ATTENDANCE_VIEW', 'MY_WARD_EXAM_RESULT_VIEW')")
    public ResponseEntity<List<WardSummaryResponse>> myWards(@AuthenticationPrincipal Jwt jwt) {
        String username = jwt != null ? jwt.getClaimAsString("preferred_username") : "";
        return ResponseEntity.ok(guardianService.findMyWards(username));
    }
}
