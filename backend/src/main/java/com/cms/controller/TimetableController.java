package com.cms.controller;

import java.time.LocalDate;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.cms.dto.ClassScheduleOccurrenceResponse;
import com.cms.dto.ClassScheduleResponse;
import com.cms.dto.MyTimetableResponse;
import com.cms.dto.ProfileIdentity;
import com.cms.dto.SwapCandidateResponse;
import com.cms.dto.SwapRequest;
import com.cms.dto.TimetableActionResponse;
import com.cms.dto.TimetableGenerationResponse;
import com.cms.model.enums.ClassScheduleStatus;
import com.cms.service.ClassScheduleService;
import com.cms.service.PersonalTimetableService;
import com.cms.service.ProfileService;
import com.cms.service.TimetableGenerationService;
import com.cms.service.TimetableOccurrenceService;
import com.cms.service.TimetableSwapService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/timetables")
public class TimetableController {

    private final TimetableGenerationService timetableGenerationService;
    private final TimetableSwapService timetableSwapService;
    private final ClassScheduleService classScheduleService;
    private final PersonalTimetableService personalTimetableService;
    private final ProfileService profileService;
    private final TimetableOccurrenceService timetableOccurrenceService;

    public TimetableController(TimetableGenerationService timetableGenerationService,
                                TimetableSwapService timetableSwapService,
                                ClassScheduleService classScheduleService,
                                PersonalTimetableService personalTimetableService,
                                ProfileService profileService,
                                TimetableOccurrenceService timetableOccurrenceService) {
        this.timetableGenerationService = timetableGenerationService;
        this.timetableSwapService = timetableSwapService;
        this.classScheduleService = classScheduleService;
        this.personalTimetableService = personalTimetableService;
        this.profileService = profileService;
        this.timetableOccurrenceService = timetableOccurrenceService;
    }

    @GetMapping("/me")
    @PreAuthorize("@perm.has('TIMETABLE_VIEW')")
    public ResponseEntity<MyTimetableResponse> findMyTimetable(
            @RequestParam Long termInstanceId,
            @RequestParam(required = false) LocalDate weekStart) {
        ProfileIdentity identity = profileService.resolveCurrentUser();
        return ResponseEntity.ok(personalTimetableService.findMyTimetable(identity, termInstanceId, weekStart));
    }

    @GetMapping("/occurrences")
    @PreAuthorize("@perm.has('TIMETABLE_VIEW')")
    public ResponseEntity<List<ClassScheduleOccurrenceResponse>> findOccurrences(
            @RequestParam Long termInstanceId,
            @RequestParam LocalDate from,
            @RequestParam LocalDate to,
            @RequestParam(defaultValue = "browse") String scope) {
        ProfileIdentity identity = profileService.resolveCurrentUser();
        return ResponseEntity.ok(
            timetableOccurrenceService.findOccurrences(identity, termInstanceId, from, to, scope));
    }

    @PostMapping("/generate")
    @PreAuthorize("@perm.has('TIMETABLE_GENERATE')")
    public ResponseEntity<TimetableGenerationResponse> generate(@RequestParam Long termInstanceId) {
        return ResponseEntity.ok(timetableGenerationService.generate(termInstanceId));
    }

    @GetMapping("/draft")
    @PreAuthorize("@perm.has('TIMETABLE_GENERATE') or @perm.has('TIMETABLE_MANAGE')")
    public ResponseEntity<List<ClassScheduleResponse>> findDraft(@RequestParam Long termInstanceId) {
        return ResponseEntity.ok(classScheduleService.findByTermInstanceIdAndStatus(termInstanceId, ClassScheduleStatus.DRAFT));
    }

    @GetMapping
    @PreAuthorize("@perm.has('TIMETABLE_VIEW')")
    public ResponseEntity<List<ClassScheduleResponse>> findPublished(@RequestParam Long termInstanceId) {
        return ResponseEntity.ok(classScheduleService.findByTermInstanceIdAndStatus(termInstanceId, ClassScheduleStatus.PUBLISHED));
    }

    @PostMapping("/{termInstanceId}/approve")
    @PreAuthorize("@perm.has('TIMETABLE_MANAGE')")
    public ResponseEntity<TimetableActionResponse> approve(@PathVariable Long termInstanceId) {
        return ResponseEntity.ok(timetableGenerationService.approve(termInstanceId));
    }

    @DeleteMapping("/{termInstanceId}")
    @PreAuthorize("@perm.has('TIMETABLE_MANAGE')")
    public ResponseEntity<TimetableActionResponse> clear(@PathVariable Long termInstanceId) {
        return ResponseEntity.ok(timetableGenerationService.clear(termInstanceId));
    }

    @PostMapping("/{termInstanceId}/revert-to-draft")
    @PreAuthorize("@perm.has('TIMETABLE_DISCARD_PUBLISHED')")
    public ResponseEntity<TimetableActionResponse> revertToDraft(@PathVariable Long termInstanceId) {
        return ResponseEntity.ok(timetableGenerationService.revertToDraft(termInstanceId));
    }

    @GetMapping("/{termInstanceId}/sessions/{sessionId}/swap-candidates")
    @PreAuthorize("@perm.has('TIMETABLE_SWAP')")
    public ResponseEntity<List<SwapCandidateResponse>> findSwapCandidates(
            @PathVariable Long termInstanceId, @PathVariable Long sessionId) {
        return ResponseEntity.ok(timetableSwapService.findCandidates(termInstanceId, sessionId));
    }

    @PostMapping("/{termInstanceId}/sessions/{sessionId}/swap")
    @PreAuthorize("@perm.has('TIMETABLE_SWAP')")
    public ResponseEntity<Void> swap(
            @PathVariable Long termInstanceId, @PathVariable Long sessionId, @Valid @RequestBody SwapRequest request) {
        timetableSwapService.swap(termInstanceId, sessionId, request);
        return ResponseEntity.noContent().build();
    }
}
