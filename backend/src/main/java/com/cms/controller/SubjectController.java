package com.cms.controller;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.cms.dto.ActiveStatusUpdateRequest;
import com.cms.dto.ActiveStatusUpdateResponse;
import com.cms.dto.AddEligibleVenueRequest;
import com.cms.dto.SubjectRequest;
import com.cms.dto.SubjectResponse;
import com.cms.service.SubjectService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/subjects")
public class SubjectController {

    private final SubjectService subjectService;

    public SubjectController(SubjectService subjectService) {
        this.subjectService = subjectService;
    }

    @PostMapping
    @PreAuthorize("@perm.has('SUBJECT_MANAGE')")
    public ResponseEntity<SubjectResponse> create(@Valid @RequestBody SubjectRequest request) {
        SubjectResponse response = subjectService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<List<SubjectResponse>> findAll(
            @RequestParam(required = false, defaultValue = "false") boolean activeOnly) {
        List<SubjectResponse> subjects = subjectService.findAll(activeOnly);
        return ResponseEntity.ok(subjects);
    }

    @GetMapping("/page")
    public ResponseEntity<Page<SubjectResponse>> findPage(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Long courseId,
            @PageableDefault(size = 25, sort = "name", direction = Sort.Direction.ASC) Pageable pageable) {
        return ResponseEntity.ok(subjectService.findPage(search, courseId, pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<SubjectResponse> findById(@PathVariable Long id) {
        SubjectResponse response = subjectService.findById(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/course/{courseId}")
    public ResponseEntity<List<SubjectResponse>> findByCourseId(@PathVariable Long courseId) {
        List<SubjectResponse> subjects = subjectService.findByCourseId(courseId);
        return ResponseEntity.ok(subjects);
    }

    @GetMapping("/speciality/{specialityId}")
    public ResponseEntity<List<SubjectResponse>> findBySpecialityId(@PathVariable Long specialityId) {
        List<SubjectResponse> subjects = subjectService.findBySpecialityId(specialityId);
        return ResponseEntity.ok(subjects);
    }

    @PutMapping("/{id}")
    @PreAuthorize("@perm.has('SUBJECT_MANAGE')")
    public ResponseEntity<SubjectResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody SubjectRequest request) {
        SubjectResponse response = subjectService.update(id, request);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/eligible-venues")
    @PreAuthorize("@perm.has('SUBJECT_MANAGE')")
    public ResponseEntity<Void> addEligibleVenue(@Valid @RequestBody AddEligibleVenueRequest request) {
        subjectService.addEligibleVenue(request);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("@perm.has('SUBJECT_MANAGE')")
    public ResponseEntity<ActiveStatusUpdateResponse> updateStatus(
            @PathVariable Long id,
            @Valid @RequestBody ActiveStatusUpdateRequest request) {
        return ResponseEntity.ok(subjectService.updateStatus(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@perm.has('SUBJECT_MANAGE')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        subjectService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/name-exists")
    @PreAuthorize("@perm.has('SUBJECT_MANAGE')")
    public ResponseEntity<Boolean> nameExists(
            @RequestParam String value,
            @RequestParam(required = false) Long excludeId) {
        return ResponseEntity.ok(subjectService.nameExists(value, excludeId));
    }

    @GetMapping("/code-exists")
    @PreAuthorize("@perm.has('SUBJECT_MANAGE')")
    public ResponseEntity<Boolean> codeExists(
            @RequestParam String value,
            @RequestParam(required = false) Long excludeId) {
        return ResponseEntity.ok(subjectService.codeExists(value, excludeId));
    }
}
