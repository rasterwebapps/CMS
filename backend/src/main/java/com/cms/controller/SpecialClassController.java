package com.cms.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.GetMapping;

import com.cms.dto.DayRepeatRequest;
import com.cms.dto.DayRepeatResult;
import com.cms.dto.ProfileIdentity;
import com.cms.dto.SpecialClassOccurrenceDto;
import com.cms.dto.SpecialClassRejectionRequest;
import com.cms.dto.SpecialClassRequest;
import com.cms.service.ProfileService;
import com.cms.service.SpecialClassRequestService;

import jakarta.validation.Valid;

/** BR-55 — Special/Remedial Class Scheduler. Faculty request, Admin approves/rejects. */
@RestController
@RequestMapping("/timetables/special-classes")
public class SpecialClassController {

    private final SpecialClassRequestService specialClassRequestService;
    private final ProfileService profileService;

    public SpecialClassController(SpecialClassRequestService specialClassRequestService, ProfileService profileService) {
        this.specialClassRequestService = specialClassRequestService;
        this.profileService = profileService;
    }

    @PostMapping("/single-subject")
    @PreAuthorize("@perm.has('TIMETABLE_SPECIAL_CLASS_REQUEST')")
    public ResponseEntity<List<SpecialClassOccurrenceDto>> requestSingleSubject(@Valid @RequestBody SpecialClassRequest request) {
        ProfileIdentity identity = requireFacultyIdentity();
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(specialClassRequestService.requestSingleSubject(request, identity.entityId(), identity.displayName()));
    }

    @PostMapping("/day-repeat")
    @PreAuthorize("@perm.has('TIMETABLE_SPECIAL_CLASS_REQUEST')")
    public ResponseEntity<DayRepeatResult> requestDayRepeat(@Valid @RequestBody DayRepeatRequest request) {
        ProfileIdentity identity = requireFacultyIdentity();
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(specialClassRequestService.requestDayRepeat(request, identity.entityId(), identity.displayName()));
    }

    @GetMapping("/my-requests")
    @PreAuthorize("@perm.has('TIMETABLE_SPECIAL_CLASS_VIEW')")
    public ResponseEntity<List<SpecialClassOccurrenceDto>> myRequests() {
        ProfileIdentity identity = requireFacultyIdentity();
        return ResponseEntity.ok(specialClassRequestService.listMyRequests(identity.entityId()));
    }

    @GetMapping("/approval-queue")
    @PreAuthorize("@perm.has('TIMETABLE_SPECIAL_CLASS_APPROVE')")
    public ResponseEntity<List<SpecialClassOccurrenceDto>> approvalQueue() {
        return ResponseEntity.ok(specialClassRequestService.listApprovalQueue());
    }

    @PutMapping("/{id}/approve")
    @PreAuthorize("@perm.has('TIMETABLE_SPECIAL_CLASS_APPROVE')")
    public ResponseEntity<SpecialClassOccurrenceDto> approve(@PathVariable Long id) {
        ProfileIdentity identity = profileService.resolveCurrentUser();
        return ResponseEntity.ok(specialClassRequestService.approve(id, identity.displayName()));
    }

    @PutMapping("/batches/{requestBatchId}/approve")
    @PreAuthorize("@perm.has('TIMETABLE_SPECIAL_CLASS_APPROVE')")
    public ResponseEntity<List<SpecialClassOccurrenceDto>> approveBatch(@PathVariable UUID requestBatchId) {
        ProfileIdentity identity = profileService.resolveCurrentUser();
        return ResponseEntity.ok(specialClassRequestService.approveBatch(requestBatchId, identity.displayName()));
    }

    @PutMapping("/{id}/reject")
    @PreAuthorize("@perm.has('TIMETABLE_SPECIAL_CLASS_APPROVE')")
    public ResponseEntity<SpecialClassOccurrenceDto> reject(@PathVariable Long id,
            @Valid @RequestBody SpecialClassRejectionRequest request) {
        ProfileIdentity identity = profileService.resolveCurrentUser();
        return ResponseEntity.ok(specialClassRequestService.reject(id, request.rejectionReason(), identity.displayName()));
    }

    @PutMapping("/batches/{requestBatchId}/reject")
    @PreAuthorize("@perm.has('TIMETABLE_SPECIAL_CLASS_APPROVE')")
    public ResponseEntity<List<SpecialClassOccurrenceDto>> rejectBatch(@PathVariable UUID requestBatchId,
            @Valid @RequestBody SpecialClassRejectionRequest request) {
        ProfileIdentity identity = profileService.resolveCurrentUser();
        return ResponseEntity.ok(specialClassRequestService.rejectBatch(requestBatchId, request.rejectionReason(), identity.displayName()));
    }

    @PutMapping("/{id}/cancel")
    @PreAuthorize("@perm.has('TIMETABLE_SPECIAL_CLASS_CANCEL')")
    public ResponseEntity<SpecialClassOccurrenceDto> cancel(@PathVariable Long id) {
        ProfileIdentity identity = profileService.resolveCurrentUser();
        return ResponseEntity.ok(specialClassRequestService.cancel(id, identity.displayName()));
    }

    private ProfileIdentity requireFacultyIdentity() {
        ProfileIdentity identity = profileService.resolveCurrentUser();
        if (!"FACULTY".equals(identity.entityType())) {
            throw new IllegalArgumentException("Only a faculty member can request a special class.");
        }
        return identity;
    }
}
