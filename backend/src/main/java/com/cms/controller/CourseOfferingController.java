package com.cms.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.cms.dto.ActiveStatusUpdateRequest;
import com.cms.dto.ActiveStatusUpdateResponse;
import com.cms.dto.CourseOfferingDto;
import com.cms.dto.CourseOfferingSectionFacultyResponse;
import com.cms.dto.CourseOfferingUpdateRequest;
import com.cms.dto.FacultyCapacityCheckResult;
import com.cms.dto.GenerateOfferingsResponse;
import com.cms.dto.SectionFacultyAssignment;
import com.cms.dto.SectionFacultyUpsertRequest;
import com.cms.service.CourseOfferingSectionFacultyService;
import com.cms.service.CourseOfferingService;
import com.cms.service.TimetableGlobalAutoScheduleService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/course-offerings")
public class CourseOfferingController {

    private final CourseOfferingService courseOfferingService;
    private final TimetableGlobalAutoScheduleService timetableGlobalAutoScheduleService;
    private final CourseOfferingSectionFacultyService sectionFacultyService;

    public CourseOfferingController(CourseOfferingService courseOfferingService,
                                     TimetableGlobalAutoScheduleService timetableGlobalAutoScheduleService,
                                     CourseOfferingSectionFacultyService sectionFacultyService) {
        this.courseOfferingService = courseOfferingService;
        this.timetableGlobalAutoScheduleService = timetableGlobalAutoScheduleService;
        this.sectionFacultyService = sectionFacultyService;
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
    public ResponseEntity<GenerateOfferingsResponse> generate(@RequestParam Long termInstanceId) {
        return ResponseEntity.ok(courseOfferingService.generateOfferingsForTermInstance(termInstanceId));
    }

    @PutMapping("/{id}")
    @PreAuthorize("@perm.has('COURSE_MANAGE')")
    public ResponseEntity<CourseOfferingDto> update(
            @PathVariable Long id,
            @RequestBody CourseOfferingUpdateRequest request) {
        return ResponseEntity.ok(
            courseOfferingService.updateOffering(id, request.facultyId(), request.secondaryFacultyId()));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("@perm.has('COURSE_MANAGE')")
    public ResponseEntity<ActiveStatusUpdateResponse> updateStatus(
            @PathVariable Long id,
            @Valid @RequestBody ActiveStatusUpdateRequest request) {
        return ResponseEntity.ok(courseOfferingService.updateStatus(id, request));
    }

    /** Live, pre-save check for the edit dialog's Faculty picker — same math {@link
     *  #update} hard-blocks on, surfaced early so the admin sees it before even clicking Save. */
    @GetMapping("/{id}/faculty-capacity-check")
    @PreAuthorize("@perm.has('COURSE_MANAGE')")
    public ResponseEntity<FacultyCapacityCheckResult> checkFacultyCapacity(
            @PathVariable Long id,
            @RequestParam Long facultyId,
            @RequestParam Long termInstanceId) {
        return ResponseEntity.ok(timetableGlobalAutoScheduleService.checkFacultyCapacityForOffering(termInstanceId, id, facultyId));
    }

    /** Per-section Theory faculty overrides for this offering's cohort — advisory/accounting-only,
     *  feeds the same capacity math above without touching Skeleton Builder placement or Staffing. */
    @GetMapping("/{id}/section-faculty")
    @PreAuthorize("@perm.has('SECTION_FACULTY_VIEW')")
    public ResponseEntity<CourseOfferingSectionFacultyResponse> getSectionFaculty(@PathVariable Long id) {
        return ResponseEntity.ok(sectionFacultyService.getForOffering(id));
    }

    @PutMapping("/{id}/section-faculty/{cohortSectionId}")
    @PreAuthorize("@perm.has('SECTION_FACULTY_MANAGE')")
    public ResponseEntity<SectionFacultyAssignment> upsertSectionFaculty(
            @PathVariable Long id,
            @PathVariable Long cohortSectionId,
            @RequestBody SectionFacultyUpsertRequest request) {
        return ResponseEntity.ok(sectionFacultyService.upsert(id, cohortSectionId, request.facultyId()));
    }
}
