package com.cms.controller;

import java.util.List;
import java.util.Map;

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

import com.cms.dto.CourseOfferingDto;
import com.cms.dto.CourseOfferingUpdateRequest;
import com.cms.service.CourseOfferingService;

@RestController
@RequestMapping("/course-offerings")
public class CourseOfferingController {

    private final CourseOfferingService courseOfferingService;

    public CourseOfferingController(CourseOfferingService courseOfferingService) {
        this.courseOfferingService = courseOfferingService;
    }

    @GetMapping
    @PreAuthorize("@perm.has('COURSE_VIEW')")
    public ResponseEntity<List<CourseOfferingDto>> getOfferings(
            @RequestParam Long termInstanceId,
            @RequestParam(required = false) Integer termNumber,
            @RequestParam(required = false) Long cohortId) {
        // cohortId takes precedence — it's the only filter that also pins curriculumVersion, so it
        // never mixes in another cohort/program's offerings sharing the same term+semesterNumber
        // (see CourseOfferingServiceImpl#getOfferingsByTermInstanceAndCohort).
        if (cohortId != null) {
            return ResponseEntity.ok(courseOfferingService.getOfferingsByTermInstanceAndCohort(termInstanceId, cohortId));
        }
        if (termNumber != null) {
            return ResponseEntity.ok(
                courseOfferingService.getOfferingsByTermInstanceAndSemester(termInstanceId, termNumber));
        }
        return ResponseEntity.ok(courseOfferingService.getOfferingsByTermInstance(termInstanceId));
    }

    @GetMapping("/{id}")
    @PreAuthorize("@perm.has('COURSE_VIEW')")
    public ResponseEntity<CourseOfferingDto> getById(@PathVariable Long id) {
        return ResponseEntity.ok(courseOfferingService.getById(id));
    }

    @GetMapping("/elective-options")
    @PreAuthorize("@perm.has('COURSE_VIEW')")
    public ResponseEntity<List<CourseOfferingDto>> getElectiveOptions(
            @RequestParam Long termInstanceId,
            @RequestParam Long electiveGroupId) {
        return ResponseEntity.ok(
            courseOfferingService.getOfferingsByTermInstanceAndElectiveGroup(termInstanceId, electiveGroupId));
    }

    @PostMapping("/generate")
    @PreAuthorize("@perm.has('COURSE_MANAGE')")
    public ResponseEntity<Map<String, Integer>> generate(@RequestParam Long termInstanceId) {
        int count = courseOfferingService.generateOfferingsForTermInstance(termInstanceId);
        return ResponseEntity.ok(Map.of("offeringsCreated", count));
    }

    @PutMapping("/{id}")
    @PreAuthorize("@perm.has('COURSE_MANAGE')")
    public ResponseEntity<CourseOfferingDto> update(
            @PathVariable Long id,
            @RequestBody CourseOfferingUpdateRequest request) {
        return ResponseEntity.ok(
            courseOfferingService.updateOffering(id, request.facultyId(), request.secondaryFacultyId(), request.sectionLabel()));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@perm.has('COURSE_MANAGE')")
    public ResponseEntity<Void> deactivate(@PathVariable Long id) {
        courseOfferingService.deactivateOffering(id);
        return ResponseEntity.noContent().build();
    }
}
