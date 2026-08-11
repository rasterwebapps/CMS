package com.cms.controller;

import java.time.LocalDate;
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

import com.cms.dto.AffectedSessionResponse;
import com.cms.dto.ApplySubstituteRequest;
import com.cms.dto.FacultyAbsenceDto;
import com.cms.dto.FacultyAbsenceRequest;
import com.cms.dto.ProfileIdentity;
import com.cms.dto.SubstituteCandidateResponse;
import com.cms.service.FacultyAbsenceService;
import com.cms.service.ProfileService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/faculty-absences")
public class FacultyAbsenceController {

    private final FacultyAbsenceService facultyAbsenceService;
    private final ProfileService profileService;

    public FacultyAbsenceController(FacultyAbsenceService facultyAbsenceService, ProfileService profileService) {
        this.facultyAbsenceService = facultyAbsenceService;
        this.profileService = profileService;
    }

    @PostMapping
    @PreAuthorize("@perm.has('FACULTY_ABSENCE_MARK')")
    public ResponseEntity<FacultyAbsenceDto> markAbsent(@Valid @RequestBody FacultyAbsenceRequest request) {
        ProfileIdentity identity = profileService.resolveCurrentUser();
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(facultyAbsenceService.markAbsent(request, identity.displayName()));
    }

    @GetMapping("/{absenceId}")
    @PreAuthorize("@perm.has('FACULTY_ABSENCE_MARK') or @perm.has('FACULTY_ABSENCE_SUBSTITUTE_APPLY')")
    public ResponseEntity<FacultyAbsenceDto> getAbsence(@PathVariable Long absenceId) {
        return ResponseEntity.ok(facultyAbsenceService.getAbsence(absenceId));
    }

    @GetMapping("/{absenceId}/affected-sessions")
    @PreAuthorize("@perm.has('FACULTY_ABSENCE_MARK') or @perm.has('FACULTY_ABSENCE_SUBSTITUTE_APPLY')")
    public ResponseEntity<List<AffectedSessionResponse>> getAffectedSessions(@PathVariable Long absenceId) {
        return ResponseEntity.ok(facultyAbsenceService.findAffectedSessions(absenceId));
    }

    @GetMapping("/sessions/{classScheduleId}/substitute-candidates")
    @PreAuthorize("@perm.has('FACULTY_ABSENCE_SUBSTITUTE_APPLY')")
    public ResponseEntity<List<SubstituteCandidateResponse>> getSubstituteCandidates(
            @PathVariable Long classScheduleId, @RequestParam LocalDate date) {
        return ResponseEntity.ok(facultyAbsenceService.findEligibleSubstitutes(classScheduleId, date));
    }

    @PostMapping("/{absenceId}/sessions/{classScheduleId}/apply-substitute")
    @PreAuthorize("@perm.has('FACULTY_ABSENCE_SUBSTITUTE_APPLY')")
    public ResponseEntity<AffectedSessionResponse> applySubstitute(
            @PathVariable Long absenceId, @PathVariable Long classScheduleId,
            @Valid @RequestBody ApplySubstituteRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(
            facultyAbsenceService.applySubstitute(absenceId, classScheduleId, request.substituteFacultyId(),
                jwt != null ? jwt.getClaimAsString("preferred_username") : "system"));
    }
}
