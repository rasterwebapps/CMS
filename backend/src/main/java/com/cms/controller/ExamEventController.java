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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.cms.dto.ExamEventDto;
import com.cms.dto.ExamEventRequest;
import com.cms.service.ExamEventService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/exam-events")
public class ExamEventController {

    private final ExamEventService examEventService;

    public ExamEventController(ExamEventService examEventService) {
        this.examEventService = examEventService;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ROLE_ADMIN','ROLE_COLLEGE_ADMIN')")
    public ResponseEntity<ExamEventDto> create(@Valid @RequestBody ExamEventRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(examEventService.create(request));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN','ROLE_COLLEGE_ADMIN','ROLE_FACULTY')")
    public ResponseEntity<ExamEventDto> getById(@PathVariable Long id) {
        return ResponseEntity.ok(examEventService.getById(id));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ROLE_ADMIN','ROLE_COLLEGE_ADMIN','ROLE_FACULTY')")
    public ResponseEntity<List<ExamEventDto>> getEvents(
            @RequestParam(required = false) Long examSessionId,
            @RequestParam(required = false) Long termInstanceId) {
        if (examSessionId != null) {
            return ResponseEntity.ok(examEventService.getByExamSession(examSessionId));
        }
        return ResponseEntity.ok(examEventService.getByTermInstance(termInstanceId));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN','ROLE_COLLEGE_ADMIN')")
    public ResponseEntity<ExamEventDto> update(@PathVariable Long id,
                                               @Valid @RequestBody ExamEventRequest request) {
        return ResponseEntity.ok(examEventService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN','ROLE_COLLEGE_ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        examEventService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
