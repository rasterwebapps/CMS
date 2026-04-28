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
@RequestMapping("/api/course-offerings")
public class CourseOfferingController {

    private final CourseOfferingService courseOfferingService;

    public CourseOfferingController(CourseOfferingService courseOfferingService) {
        this.courseOfferingService = courseOfferingService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ROLE_ADMIN','ROLE_COLLEGE_ADMIN','ROLE_FRONT_OFFICE','ROLE_FACULTY')")
    public ResponseEntity<List<CourseOfferingDto>> getOfferings(
            @RequestParam Long termInstanceId,
            @RequestParam(required = false) Integer semesterNumber) {
        if (semesterNumber != null) {
            return ResponseEntity.ok(
                courseOfferingService.getOfferingsByTermInstanceAndSemester(termInstanceId, semesterNumber));
        }
        return ResponseEntity.ok(courseOfferingService.getOfferingsByTermInstance(termInstanceId));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN','ROLE_COLLEGE_ADMIN','ROLE_FRONT_OFFICE','ROLE_FACULTY')")
    public ResponseEntity<CourseOfferingDto> getById(@PathVariable Long id) {
        return ResponseEntity.ok(courseOfferingService.getById(id));
    }

    @PostMapping("/generate")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN','ROLE_COLLEGE_ADMIN')")
    public ResponseEntity<Map<String, Integer>> generate(@RequestParam Long termInstanceId) {
        int count = courseOfferingService.generateOfferingsForTermInstance(termInstanceId);
        return ResponseEntity.ok(Map.of("offeringsCreated", count));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN','ROLE_COLLEGE_ADMIN')")
    public ResponseEntity<CourseOfferingDto> update(
            @PathVariable Long id,
            @RequestBody CourseOfferingUpdateRequest request) {
        return ResponseEntity.ok(
            courseOfferingService.updateOffering(id, request.facultyId(), request.sectionLabel()));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN','ROLE_COLLEGE_ADMIN')")
    public ResponseEntity<Void> deactivate(@PathVariable Long id) {
        courseOfferingService.deactivateOffering(id);
        return ResponseEntity.noContent().build();
    }
}
