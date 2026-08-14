package com.cms.controller;

import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.cms.dto.CourseRegistrationDto;
import com.cms.dto.ElectiveAssignmentRequest;
import com.cms.dto.ElectiveBulkAssignmentRequest;
import com.cms.dto.ElectiveBulkAssignmentResponse;
import com.cms.service.CourseRegistrationService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/course-registrations")
public class CourseRegistrationController {

    private final CourseRegistrationService courseRegistrationService;

    public CourseRegistrationController(CourseRegistrationService courseRegistrationService) {
        this.courseRegistrationService = courseRegistrationService;
    }

    @GetMapping
    @PreAuthorize("@perm.hasAny('ADMISSION_VIEW', 'COURSE_REGISTRATION_ELECTIVE_ASSIGN')")
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
    @PreAuthorize("@perm.hasAny('ADMISSION_VIEW', 'COURSE_REGISTRATION_ELECTIVE_ASSIGN')")
    public ResponseEntity<CourseRegistrationDto> getById(@PathVariable Long id) {
        return ResponseEntity.ok(courseRegistrationService.getById(id));
    }

    @PostMapping("/generate")
    @PreAuthorize("@perm.has('ADMISSION_CREATE')")
    public ResponseEntity<Map<String, Integer>> generate(@RequestParam Long termInstanceId) {
        int count = courseRegistrationService.generateRegistrationsForTermInstance(termInstanceId);
        return ResponseEntity.ok(Map.of("registrationsCreated", count));
    }

    @PutMapping("/{id}/drop")
    @PreAuthorize("@perm.has('ADMISSION_CREATE')")
    public ResponseEntity<CourseRegistrationDto> drop(@PathVariable Long id) {
        return ResponseEntity.ok(courseRegistrationService.dropRegistration(id));
    }

    @PostMapping("/elective-assignment")
    @PreAuthorize("@perm.has('COURSE_REGISTRATION_ELECTIVE_ASSIGN')")
    public ResponseEntity<CourseRegistrationDto> assignElectiveChoice(
            @Valid @RequestBody ElectiveAssignmentRequest request) {
        return ResponseEntity.ok(
            courseRegistrationService.assignElectiveChoice(request.enrollmentId(), request.courseOfferingId()));
    }

    @PostMapping("/elective-assignment/bulk")
    @PreAuthorize("@perm.has('COURSE_REGISTRATION_ELECTIVE_ASSIGN')")
    public ResponseEntity<ElectiveBulkAssignmentResponse> bulkAssignElectiveChoice(
            @Valid @RequestBody ElectiveBulkAssignmentRequest request) {
        return ResponseEntity.ok(courseRegistrationService.bulkAssignElectiveChoice(
            request.termInstanceId(), request.electiveGroupId(), request.courseOfferingId()));
    }
}
