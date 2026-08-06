package com.cms.controller;

import java.time.LocalDate;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.cms.dto.RotationCandidateSlotResponse;
import com.cms.dto.RotationEffectiveResponse;
import com.cms.dto.RotationGroupCreateRequest;
import com.cms.dto.RotationGroupResponse;
import com.cms.model.enums.DayOfWeek;
import com.cms.service.RotationGroupService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/timetables/rotation-groups")
public class RotationGroupController {

    private final RotationGroupService service;

    public RotationGroupController(RotationGroupService service) {
        this.service = service;
    }

    @GetMapping
    @PreAuthorize("@perm.has('TIMETABLE_ROTATION_VIEW')")
    public ResponseEntity<List<RotationGroupResponse>> list(@RequestParam Long termInstanceId) {
        return ResponseEntity.ok(service.list(termInstanceId));
    }

    @GetMapping("/candidate-slots")
    @PreAuthorize("@perm.has('TIMETABLE_ROTATION_VIEW')")
    public ResponseEntity<List<RotationCandidateSlotResponse>> candidateSlots(@RequestParam Long termInstanceId,
                                                                               @RequestParam DayOfWeek dayOfWeek,
                                                                               @RequestParam Long periodId) {
        return ResponseEntity.ok(service.candidateSlots(termInstanceId, dayOfWeek, periodId));
    }

    @GetMapping("/{id}/effective")
    @PreAuthorize("@perm.has('TIMETABLE_ROTATION_VIEW')")
    public ResponseEntity<RotationEffectiveResponse> effective(@PathVariable Long id,
                                                                 @RequestParam Long classScheduleId,
                                                                 @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ResponseEntity.ok(service.effective(id, classScheduleId, date));
    }

    @PostMapping
    @PreAuthorize("@perm.has('TIMETABLE_ROTATION_MANAGE')")
    public ResponseEntity<RotationGroupResponse> create(@Valid @RequestBody RotationGroupCreateRequest request,
                                                          @AuthenticationPrincipal Jwt jwt) {
        String createdBy = jwt != null ? jwt.getClaimAsString("preferred_username") : null;
        return ResponseEntity.ok(service.create(request, createdBy));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@perm.has('TIMETABLE_ROTATION_MANAGE')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
