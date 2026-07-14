package com.cms.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.cms.dto.ExamSessionDto;
import com.cms.dto.ExamSessionRequest;
import com.cms.service.ExamSessionService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/exam-sessions")
public class ExamSessionController {

    private final ExamSessionService examSessionService;

    public ExamSessionController(ExamSessionService examSessionService) {
        this.examSessionService = examSessionService;
    }

    @PostMapping
    @PreAuthorize("@perm.has('EXAMINATION_MANAGE')")
    public ResponseEntity<ExamSessionDto> create(@Valid @RequestBody ExamSessionRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(examSessionService.create(request));
    }

    @GetMapping("/{id}")
    @PreAuthorize("@perm.has('EXAMINATION_VIEW')")
    public ResponseEntity<ExamSessionDto> getById(@PathVariable Long id) {
        return ResponseEntity.ok(examSessionService.getById(id));
    }

    @GetMapping
    @PreAuthorize("@perm.has('EXAMINATION_VIEW')")
    public ResponseEntity<List<ExamSessionDto>> getByTermInstance(@RequestParam Long termInstanceId) {
        return ResponseEntity.ok(examSessionService.getByTermInstance(termInstanceId));
    }

    @PostMapping("/{id}/publish")
    @PreAuthorize("@perm.has('EXAMINATION_MANAGE')")
    public ResponseEntity<ExamSessionDto> publish(@PathVariable Long id) {
        return ResponseEntity.ok(examSessionService.publish(id));
    }

    @PostMapping("/{id}/lock")
    @PreAuthorize("@perm.has('EXAMINATION_MANAGE')")
    public ResponseEntity<ExamSessionDto> lock(@PathVariable Long id) {
        return ResponseEntity.ok(examSessionService.lock(id));
    }
}
