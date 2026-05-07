package com.cms.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.cms.dto.SemesterResponse;
import com.cms.service.SemesterService;

@RestController
@RequestMapping("/semesters")
public class SemesterController {

    private final SemesterService semesterService;

    public SemesterController(SemesterService semesterService) {
        this.semesterService = semesterService;
    }

    @GetMapping
    public ResponseEntity<List<SemesterResponse>> findAll() {
        return ResponseEntity.ok(semesterService.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<SemesterResponse> findById(@PathVariable Long id) {
        return ResponseEntity.ok(semesterService.findById(id));
    }

    @GetMapping("/academic-year/{academicYearId}")
    public ResponseEntity<List<SemesterResponse>> findByAcademicYearId(@PathVariable Long academicYearId) {
        return ResponseEntity.ok(semesterService.findByAcademicYearId(academicYearId));
    }
}
