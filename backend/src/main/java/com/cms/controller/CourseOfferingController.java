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
import com.cms.dto.ClinicalShiftConfigUpdateRequest;
import com.cms.dto.CourseOfferingDto;
import com.cms.dto.CourseOfferingFacultySummaryDto;
import com.cms.dto.CourseOfferingSectionFacultyResponse;
import com.cms.dto.EligibleFacultyCandidateDto;
import com.cms.dto.FacultyCapacityCheckResult;
import com.cms.dto.FacultyPoolUpdateRequest;
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

    /** Every eligible (Speciality match OR the subject's Eligible Faculty list) active faculty for
     *  this offering, each annotated with real remaining term capacity and sorted most-free-first —
     *  backs the Assign Faculty picker's primary Faculty field. */
    @GetMapping("/{id}/eligible-faculty")
    @PreAuthorize("@perm.has('COURSE_MANAGE')")
    public ResponseEntity<List<EligibleFacultyCandidateDto>> getEligibleFaculty(@PathVariable Long id) {
        return ResponseEntity.ok(timetableGlobalAutoScheduleService.getEligibleFacultyForOffering(id));
    }

    /** Section-scoped counterpart of {@link #getEligibleFaculty} — backs the per-section picker in
     *  the Section Faculty sub-widget, projecting each candidate's load against just this section's
     *  own Theory hours rather than the whole offering's. */
    @GetMapping("/{id}/sections/{cohortSectionId}/eligible-faculty")
    @PreAuthorize("@perm.has('SECTION_FACULTY_VIEW')")
    public ResponseEntity<List<EligibleFacultyCandidateDto>> getEligibleFacultyForSection(
            @PathVariable Long id, @PathVariable Long cohortSectionId) {
        return ResponseEntity.ok(timetableGlobalAutoScheduleService.getEligibleFacultyForSection(id, cohortSectionId));
    }

    /** Cohort-scoped counterpart of {@link #getEligibleFacultyForSection} — for a cohort with no
     *  active section split, projecting each candidate's load against this cohort's whole
     *  theory+lab+clinical hours rather than one section's theory hours. */
    @GetMapping("/{id}/cohorts/{cohortId}/eligible-faculty")
    @PreAuthorize("@perm.has('SECTION_FACULTY_VIEW')")
    public ResponseEntity<List<EligibleFacultyCandidateDto>> getEligibleFacultyForCohort(
            @PathVariable Long id, @PathVariable Long cohortId) {
        return ResponseEntity.ok(timetableGlobalAutoScheduleService.getEligibleFacultyForCohort(id, cohortId));
    }

    /** Replaces this offering's admin-curated faculty pool wholesale — the primary/section pickers
     *  are then scoped to just this pool rather than the full eligible list. */
    @PutMapping("/{id}/faculty-pool")
    @PreAuthorize("@perm.has('COURSE_MANAGE')")
    public ResponseEntity<List<EligibleFacultyCandidateDto>> updateFacultyPool(
            @PathVariable Long id, @RequestBody FacultyPoolUpdateRequest request) {
        return ResponseEntity.ok(courseOfferingService.updateFacultyPool(id, request.facultyIds()));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("@perm.has('COURSE_MANAGE')")
    public ResponseEntity<ActiveStatusUpdateResponse> updateStatus(
            @PathVariable Long id,
            @Valid @RequestBody ActiveStatusUpdateRequest request) {
        return ResponseEntity.ok(courseOfferingService.updateStatus(id, request));
    }

    @PutMapping("/{id}/clinical-shift-config")
    @PreAuthorize("@perm.has('COURSE_MANAGE')")
    public ResponseEntity<CourseOfferingDto> updateClinicalShiftConfig(
            @PathVariable Long id,
            @Valid @RequestBody ClinicalShiftConfigUpdateRequest request) {
        return ResponseEntity.ok(courseOfferingService.updateClinicalShiftConfig(id, request));
    }

    /** Live, pre-save check for a whole-cohort assignment (no section split) — same math {@link
     *  #upsertCohortFaculty} hard-blocks on, surfaced early so the admin sees it before saving. */
    @GetMapping("/{id}/cohort-faculty-capacity-check")
    @PreAuthorize("@perm.has('SECTION_FACULTY_VIEW')")
    public ResponseEntity<FacultyCapacityCheckResult> checkFacultyCapacityForCohort(
            @PathVariable Long id,
            @RequestParam Long cohortId,
            @RequestParam Long facultyId) {
        return ResponseEntity.ok(timetableGlobalAutoScheduleService.checkFacultyCapacityForCohort(id, cohortId, facultyId));
    }

    /** Roll-up of every offering's currently-assigned faculty in a term instance, in one call --
     *  backs the Assign Faculty list table's Faculty column. See {@link
     *  CourseOfferingSectionFacultyService#getAssignmentSummaryForTermInstance}. */
    @GetMapping("/faculty-assignment-summary")
    @PreAuthorize("@perm.has('SECTION_FACULTY_VIEW')")
    public ResponseEntity<List<CourseOfferingFacultySummaryDto>> getFacultyAssignmentSummary(
            @RequestParam Long termInstanceId) {
        return ResponseEntity.ok(sectionFacultyService.getAssignmentSummaryForTermInstance(termInstanceId));
    }

    /** Every cohort using this offering, each with one row per active section if its Theory
     *  delivery has split, or exactly one whole-cohort row if it hasn't -- authoritative for
     *  placement, not just accounting (Global Auto-Schedule and Staffing's auto-assign both
     *  resolve a Theory row's faculty from here). */
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

    /** Whole-cohort counterpart of {@link #upsertSectionFaculty} -- for a cohort with no active
     *  section split. */
    @PutMapping("/{id}/cohort-faculty/{cohortId}")
    @PreAuthorize("@perm.has('SECTION_FACULTY_MANAGE')")
    public ResponseEntity<SectionFacultyAssignment> upsertCohortFaculty(
            @PathVariable Long id,
            @PathVariable Long cohortId,
            @RequestBody SectionFacultyUpsertRequest request) {
        return ResponseEntity.ok(sectionFacultyService.upsertForCohort(id, cohortId, request.facultyId()));
    }
}
