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

import com.cms.dto.CurriculumFullViewDto;
import com.cms.dto.CurriculumSemesterCourseDto;
import com.cms.dto.CurriculumSemesterCourseRequest;
import com.cms.service.CurriculumSemesterCourseService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/curriculum-semester-courses")
public class CurriculumSemesterCourseController {

    private final CurriculumSemesterCourseService service;

    public CurriculumSemesterCourseController(CurriculumSemesterCourseService service) {
        this.service = service;
    }

    @PostMapping
    @PreAuthorize("@perm.has('CURRICULUM_MANAGE')")
    public ResponseEntity<CurriculumSemesterCourseDto> addCourse(
            @Valid @RequestBody CurriculumSemesterCourseRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.addCourseToSemester(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("@perm.has('CURRICULUM_MANAGE')")
    public ResponseEntity<CurriculumSemesterCourseDto> updateCourse(
            @PathVariable Long id,
            @Valid @RequestBody CurriculumSemesterCourseRequest request) {
        return ResponseEntity.ok(service.updateCourseDetails(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@perm.has('CURRICULUM_MANAGE')")
    public ResponseEntity<Void> removeCourse(@PathVariable Long id) {
        service.removeCourseFromSemester(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping
    public ResponseEntity<?> getCourses(
            @RequestParam Long curriculumVersionId,
            @RequestParam(required = false) Integer termNumber) {
        if (termNumber != null) {
            List<CurriculumSemesterCourseDto> courses =
                service.getCoursesBySemester(curriculumVersionId, termNumber);
            return ResponseEntity.ok(courses);
        }
        CurriculumFullViewDto fullView = service.getFullCurriculum(curriculumVersionId);
        return ResponseEntity.ok(fullView);
    }
}
