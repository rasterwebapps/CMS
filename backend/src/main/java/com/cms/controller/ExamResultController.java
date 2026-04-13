package com.cms.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.cms.dto.ExamResultRequest;
import com.cms.dto.ExamResultResponse;
import com.cms.service.ExamResultService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/exam-results")
public class ExamResultController {

    private final ExamResultService examResultService;

    public ExamResultController(ExamResultService examResultService) {
        this.examResultService = examResultService;
    }

    @PostMapping
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_FACULTY')")
    public ResponseEntity<ExamResultResponse> create(@Valid @RequestBody ExamResultRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(examResultService.create(request));
    }

    @GetMapping("/examination/{examinationId}")
    public ResponseEntity<List<ExamResultResponse>> findByExaminationId(@PathVariable Long examinationId) {
        return ResponseEntity.ok(examResultService.findByExaminationId(examinationId));
    }

    @GetMapping("/student/{studentId}")
    public ResponseEntity<List<ExamResultResponse>> findByStudentId(@PathVariable Long studentId) {
        return ResponseEntity.ok(examResultService.findByStudentId(studentId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ExamResultResponse> findById(@PathVariable Long id) {
        return ResponseEntity.ok(examResultService.findById(id));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_FACULTY')")
    public ResponseEntity<ExamResultResponse> update(@PathVariable Long id,
                                                      @Valid @RequestBody ExamResultRequest request) {
        return ResponseEntity.ok(examResultService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        examResultService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
