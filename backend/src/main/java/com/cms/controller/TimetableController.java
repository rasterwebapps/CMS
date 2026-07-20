package com.cms.controller;

import java.time.LocalDate;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.cms.dto.ClassScheduleResponse;
import com.cms.dto.MyTimetableResponse;
import com.cms.dto.ProfileIdentity;
import com.cms.dto.TimetableActionResponse;
import com.cms.dto.TimetableGenerationResponse;
import com.cms.model.enums.ClassScheduleStatus;
import com.cms.service.ClassScheduleService;
import com.cms.service.PersonalTimetableService;
import com.cms.service.ProfileService;
import com.cms.service.TimetableGenerationService;

@RestController
@RequestMapping("/timetables")
public class TimetableController {

    private final TimetableGenerationService timetableGenerationService;
    private final ClassScheduleService classScheduleService;
    private final PersonalTimetableService personalTimetableService;
    private final ProfileService profileService;

    public TimetableController(TimetableGenerationService timetableGenerationService,
                                ClassScheduleService classScheduleService,
                                PersonalTimetableService personalTimetableService,
                                ProfileService profileService) {
        this.timetableGenerationService = timetableGenerationService;
        this.classScheduleService = classScheduleService;
        this.personalTimetableService = personalTimetableService;
        this.profileService = profileService;
    }

    @GetMapping("/me")
    @PreAuthorize("@perm.has('TIMETABLE_VIEW')")
    public ResponseEntity<MyTimetableResponse> findMyTimetable(
            @RequestParam Long termInstanceId,
            @RequestParam(required = false) LocalDate weekStart) {
        ProfileIdentity identity = profileService.resolveCurrentUser();
        return ResponseEntity.ok(personalTimetableService.findMyTimetable(identity, termInstanceId, weekStart));
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
}
