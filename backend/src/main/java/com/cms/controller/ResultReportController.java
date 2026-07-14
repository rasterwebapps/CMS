package com.cms.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.cms.dto.CourseStatsDto;
import com.cms.dto.SemesterSummaryDto;
import com.cms.dto.StudentResultSheetDto;
import com.cms.service.ResultReportService;

@RestController
@RequestMapping("/result-reports")
public class ResultReportController {

    private final ResultReportService resultReportService;

    public ResultReportController(ResultReportService resultReportService) {
        this.resultReportService = resultReportService;
    }

    @GetMapping("/result-sheet/{enrollmentId}")
    @PreAuthorize("@perm.has('EXAM_RESULT_VIEW')")
    public ResponseEntity<StudentResultSheetDto> getResultSheet(@PathVariable Long enrollmentId) {
        return ResponseEntity.ok(resultReportService.getResultSheet(enrollmentId));
    }

    @GetMapping("/summary")
    @PreAuthorize("@perm.has('EXAM_RESULT_MANAGE')")
    public ResponseEntity<List<SemesterSummaryDto>> getSummary(@RequestParam Long termInstanceId) {
        return ResponseEntity.ok(resultReportService.getSummaryByTermInstance(termInstanceId));
    }

    @GetMapping("/course-stats")
    @PreAuthorize("@perm.has('EXAM_RESULT_MANAGE')")
    public ResponseEntity<List<CourseStatsDto>> getCourseStats(@RequestParam Long termInstanceId) {
        return ResponseEntity.ok(resultReportService.getCourseStatsByTermInstance(termInstanceId));
    }
}
