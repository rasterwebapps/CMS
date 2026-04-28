package com.cms.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.cms.dto.SemesterResultDto;
import com.cms.service.SemesterResultService;

@RestController
@RequestMapping("/api/semester-results")
public class SemesterResultController {

    private final SemesterResultService semesterResultService;

    public SemesterResultController(SemesterResultService semesterResultService) {
        this.semesterResultService = semesterResultService;
    }

    @PostMapping("/compute")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN','ROLE_COLLEGE_ADMIN')")
    public ResponseEntity<SemesterResultDto> computeForEnrollment(@RequestParam Long enrollmentId) {
        return ResponseEntity.ok(semesterResultService.computeForEnrollment(enrollmentId));
    }

    @PostMapping("/compute-term")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN','ROLE_COLLEGE_ADMIN')")
    public ResponseEntity<Void> computeForTermInstance(@RequestParam Long termInstanceId) {
        semesterResultService.computeResultsForTermInstance(termInstanceId);
        return ResponseEntity.ok().build();
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ROLE_ADMIN','ROLE_COLLEGE_ADMIN','ROLE_FACULTY')")
    public ResponseEntity<List<SemesterResultDto>> getResults(
            @RequestParam(required = false) Long termInstanceId,
            @RequestParam(required = false) Long studentId) {
        if (termInstanceId != null) {
            return ResponseEntity.ok(semesterResultService.getByTermInstance(termInstanceId));
        }
        return ResponseEntity.ok(semesterResultService.getByStudent(studentId));
    }

    @GetMapping("/enrollment/{enrollmentId}")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN','ROLE_COLLEGE_ADMIN','ROLE_FACULTY','ROLE_STUDENT')")
    public ResponseEntity<SemesterResultDto> getByEnrollment(@PathVariable Long enrollmentId) {
        return ResponseEntity.ok(semesterResultService.getByEnrollment(enrollmentId));
    }

    @PostMapping("/{id}/lock")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN','ROLE_COLLEGE_ADMIN')")
    public ResponseEntity<SemesterResultDto> lock(@PathVariable Long id) {
        return ResponseEntity.ok(semesterResultService.lockResult(id));
    }
}
