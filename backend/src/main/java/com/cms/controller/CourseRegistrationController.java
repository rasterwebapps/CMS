package com.cms.controller;

import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.cms.dto.CourseRegistrationDto;
import com.cms.service.CourseRegistrationService;

@RestController
@RequestMapping("/api/course-registrations")
public class CourseRegistrationController {

    private final CourseRegistrationService courseRegistrationService;

    public CourseRegistrationController(CourseRegistrationService courseRegistrationService) {
        this.courseRegistrationService = courseRegistrationService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ROLE_ADMIN','ROLE_COLLEGE_ADMIN','ROLE_FRONT_OFFICE','ROLE_FACULTY')")
    public ResponseEntity<?> getRegistrations(
            @RequestParam(required = false) Long enrollmentId,
            @RequestParam(required = false) Long courseOfferingId) {
        if (enrollmentId != null) {
            return ResponseEntity.ok(courseRegistrationService.getRegistrationsByEnrollment(enrollmentId));
        } else if (courseOfferingId != null) {
            return ResponseEntity.ok(courseRegistrationService.getRegistrationsByCourseOffering(courseOfferingId));
        }
        return ResponseEntity.badRequest().build();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN','ROLE_COLLEGE_ADMIN','ROLE_FRONT_OFFICE','ROLE_FACULTY')")
    public ResponseEntity<CourseRegistrationDto> getById(@PathVariable Long id) {
        return ResponseEntity.ok(courseRegistrationService.getById(id));
    }

    @PostMapping("/generate")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN','ROLE_COLLEGE_ADMIN')")
    public ResponseEntity<Map<String, Integer>> generate(@RequestParam Long termInstanceId) {
        int count = courseRegistrationService.generateRegistrationsForTermInstance(termInstanceId);
        return ResponseEntity.ok(Map.of("registrationsCreated", count));
    }

    @PutMapping("/{id}/drop")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN','ROLE_COLLEGE_ADMIN')")
    public ResponseEntity<CourseRegistrationDto> drop(@PathVariable Long id) {
        return ResponseEntity.ok(courseRegistrationService.dropRegistration(id));
    }
}
