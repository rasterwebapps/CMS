package com.cms.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.cms.dto.StudentMarkDto;
import com.cms.dto.StudentMarkRequest;
import com.cms.service.StudentMarkService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/student-marks")
public class StudentMarkController {

    private final StudentMarkService studentMarkService;

    public StudentMarkController(StudentMarkService studentMarkService) {
        this.studentMarkService = studentMarkService;
    }

    @PostMapping
    @PreAuthorize("@perm.has('EXAM_RESULT_MANAGE')")
    public ResponseEntity<StudentMarkDto> upsert(@Valid @RequestBody StudentMarkRequest request) {
        return ResponseEntity.ok(studentMarkService.upsert(request));
    }

    @GetMapping("/{id}")
    @PreAuthorize("@perm.has('EXAM_RESULT_MANAGE')")
    public ResponseEntity<StudentMarkDto> getById(@PathVariable Long id) {
        return ResponseEntity.ok(studentMarkService.getById(id));
    }

    @GetMapping
    @PreAuthorize("@perm.has('EXAM_RESULT_MANAGE')")
    public ResponseEntity<List<StudentMarkDto>> getMarks(
            @RequestParam(required = false) Long examEventId,
            @RequestParam(required = false) Long enrollmentId) {
        if (examEventId != null) {
            return ResponseEntity.ok(studentMarkService.getByExamEvent(examEventId));
        }
        return ResponseEntity.ok(studentMarkService.getByEnrollment(enrollmentId));
    }
}
