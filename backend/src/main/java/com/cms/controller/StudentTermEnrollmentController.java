package com.cms.controller;

import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.cms.dto.StudentTermEnrollmentDto;
import com.cms.service.StudentTermEnrollmentService;

@RestController
@RequestMapping("/student-term-enrollments")
public class StudentTermEnrollmentController {

    private final StudentTermEnrollmentService service;

    public StudentTermEnrollmentController(StudentTermEnrollmentService service) {
        this.service = service;
    }

    @GetMapping
    @PreAuthorize("@perm.hasAny('ADMISSION_VIEW', 'COURSE_REGISTRATION_ELECTIVE_ASSIGN')")
    public ResponseEntity<?> getEnrollments(
            @RequestParam(required = false) Long termInstanceId,
            @RequestParam(required = false) Long studentId,
            @RequestParam(required = false) Integer termNumber,
            @RequestParam(required = false) Long electiveGroupId) {
        // electiveGroupId takes precedence -- it's the only filter that also pins the group's own
        // curriculum version (via its course), so it never mixes in another program/course's
        // students sharing the same term+semesterNumber (see getEnrollmentsByElectiveGroup).
        if (termInstanceId != null && electiveGroupId != null) {
            return ResponseEntity.ok(service.getEnrollmentsByElectiveGroup(termInstanceId, electiveGroupId));
        } else if (termInstanceId != null && termNumber != null) {
            return ResponseEntity.ok(service.getEnrollmentsByTermInstanceAndSemester(termInstanceId, termNumber));
        } else if (termInstanceId != null) {
            return ResponseEntity.ok(service.getEnrollmentsByTermInstance(termInstanceId));
        } else if (studentId != null) {
            return ResponseEntity.ok(service.getEnrollmentsByStudent(studentId));
        }
        return ResponseEntity.badRequest().build();
    }

    @GetMapping("/{id}")
    @PreAuthorize("@perm.hasAny('ADMISSION_VIEW', 'COURSE_REGISTRATION_ELECTIVE_ASSIGN')")
    public ResponseEntity<StudentTermEnrollmentDto> getById(@PathVariable Long id) {
        return ResponseEntity.ok(service.getById(id));
    }

    @PostMapping("/generate")
    @PreAuthorize("@perm.has('ADMISSION_CREATE')")
    public ResponseEntity<Map<String, Integer>> generate(@RequestParam Long termInstanceId) {
        int count = service.generateEnrollmentsForTermInstance(termInstanceId);
        return ResponseEntity.ok(Map.of("enrollmentsCreated", count));
    }
}
