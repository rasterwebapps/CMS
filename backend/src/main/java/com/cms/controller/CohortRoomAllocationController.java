package com.cms.controller;

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

import com.cms.dto.CohortRoomAllocationCommitRequest;
import com.cms.dto.CohortRoomAllocationResponse;
import com.cms.service.CohortRoomAllocationService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/timetables/cohort-room-allocations")
public class CohortRoomAllocationController {

    private final CohortRoomAllocationService service;

    public CohortRoomAllocationController(CohortRoomAllocationService service) {
        this.service = service;
    }

    @GetMapping
    @PreAuthorize("@perm.has('TIMETABLE_COHORT_ROOM_ALLOCATION_VIEW')")
    public ResponseEntity<CohortRoomAllocationResponse> getCurrent(@RequestParam Long cohortId,
                                                                     @RequestParam Long termInstanceId) {
        CohortRoomAllocationResponse response = service.getCurrent(cohortId, termInstanceId);
        return response != null ? ResponseEntity.ok(response) : ResponseEntity.noContent().build();
    }

    @PostMapping("/commit")
    @PreAuthorize("@perm.has('TIMETABLE_COHORT_ROOM_ALLOCATION_MANAGE')")
    public ResponseEntity<CohortRoomAllocationResponse> commit(@Valid @RequestBody CohortRoomAllocationCommitRequest request,
                                                                 @AuthenticationPrincipal Jwt jwt) {
        String committedBy = jwt != null ? jwt.getClaimAsString("preferred_username") : null;
        return ResponseEntity.ok(service.commit(request, committedBy));
    }

    @PostMapping("/{id}/revert")
    @PreAuthorize("@perm.has('TIMETABLE_COHORT_ROOM_ALLOCATION_REVERT')")
    public ResponseEntity<CohortRoomAllocationResponse> revert(@PathVariable Long id, @AuthenticationPrincipal Jwt jwt) {
        String revertedBy = jwt != null ? jwt.getClaimAsString("preferred_username") : null;
        return ResponseEntity.ok(service.revert(id, revertedBy));
    }
}
