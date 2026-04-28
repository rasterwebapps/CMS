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
@RequestMapping("/api/student-term-enrollments")
public class StudentTermEnrollmentController {

    private final StudentTermEnrollmentService service;

    public StudentTermEnrollmentController(StudentTermEnrollmentService service) {
        this.service = service;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ROLE_ADMIN','ROLE_COLLEGE_ADMIN','ROLE_FRONT_OFFICE')")
    public ResponseEntity<?> getEnrollments(
            @RequestParam(required = false) Long termInstanceId,
            @RequestParam(required = false) Long studentId,
            @RequestParam(required = false) Integer semesterNumber) {
        if (termInstanceId != null && semesterNumber != null) {
            return ResponseEntity.ok(service.getEnrollmentsByTermInstanceAndSemester(termInstanceId, semesterNumber));
        } else if (termInstanceId != null) {
            return ResponseEntity.ok(service.getEnrollmentsByTermInstance(termInstanceId));
        } else if (studentId != null) {
            return ResponseEntity.ok(service.getEnrollmentsByStudent(studentId));
        }
        return ResponseEntity.badRequest().build();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN','ROLE_COLLEGE_ADMIN','ROLE_FRONT_OFFICE')")
    public ResponseEntity<StudentTermEnrollmentDto> getById(@PathVariable Long id) {
        return ResponseEntity.ok(service.getById(id));
    }

    @PostMapping("/generate")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN','ROLE_COLLEGE_ADMIN')")
    public ResponseEntity<Map<String, Integer>> generate(@RequestParam Long termInstanceId) {
        int count = service.generateEnrollmentsForTermInstance(termInstanceId);
        return ResponseEntity.ok(Map.of("enrollmentsCreated", count));
    }
}
