package com.cms.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.cms.dto.CourseOfferingSectionFacultyResponse;
import com.cms.dto.FacultyCapacityCheckResult;
import com.cms.dto.SectionFacultyAssignment;
import com.cms.exception.ResourceNotFoundException;
import com.cms.model.Classroom;
import com.cms.model.Cohort;
import com.cms.model.CohortRoomAllocation;
import com.cms.model.CohortSection;
import com.cms.model.Course;
import com.cms.model.CourseOffering;
import com.cms.model.CourseOfferingSectionFaculty;
import com.cms.model.CurriculumVersion;
import com.cms.model.DesignationMaster;
import com.cms.model.Faculty;
import com.cms.model.Program;
import com.cms.model.Speciality;
import com.cms.model.StudentTermEnrollment;
import com.cms.model.Subject;
import com.cms.model.TermInstance;
import com.cms.model.enums.EnrollmentStatus;
import com.cms.model.enums.FacultyStatus;
import com.cms.repository.BatchRepository;
import com.cms.repository.CohortRepository;
import com.cms.repository.CourseOfferingRepository;
import com.cms.repository.CourseOfferingSectionFacultyRepository;
import com.cms.repository.FacultyRepository;
import com.cms.repository.StudentTermEnrollmentRepository;

@ExtendWith(MockitoExtension.class)
class CourseOfferingSectionFacultyServiceTest {

    @Mock private CourseOfferingRepository courseOfferingRepository;
    @Mock private CourseOfferingSectionFacultyRepository sectionFacultyRepository;
    @Mock private CohortRepository cohortRepository;
    @Mock private StudentTermEnrollmentRepository studentTermEnrollmentRepository;
    @Mock private FacultyRepository facultyRepository;
    @Mock private BatchRepository batchRepository;
    @Mock private TimetableSkeletonService timetableSkeletonService;
    @Mock private TimetableGlobalAutoScheduleService timetableGlobalAutoScheduleService;
    @Mock private BatchService batchService;

    private CourseOfferingSectionFacultyService service;

    private Program program;
    private Course course;
    private Subject subject;
    private TermInstance termInstance;
    private CourseOffering offering;

    @BeforeEach
    void setUp() {
        service = new CourseOfferingSectionFacultyService(courseOfferingRepository, sectionFacultyRepository,
            cohortRepository, studentTermEnrollmentRepository, facultyRepository, batchRepository,
            timetableSkeletonService, timetableGlobalAutoScheduleService, batchService);

        program = new Program("BSc Nursing", "BSCN", 4);
        program.setId(1L);
        course = new Course("BSc Nursing", "BSCN", null, program);
        course.setId(1L);

        Speciality speciality = new Speciality("General Nursing", "GN", "dept", null, null);
        speciality.setId(1L);
        subject = new Subject();
        subject.setId(1L);
        subject.setName("Nursing Foundation");
        subject.setSpeciality(speciality);

        CurriculumVersion curriculumVersion = new CurriculumVersion(program, course, "v1", null, null);
        curriculumVersion.setId(1L);

        termInstance = new TermInstance();
        termInstance.setId(10L);

        offering = new CourseOffering();
        offering.setId(100L);
        offering.setTermInstance(termInstance);
        offering.setCurriculumVersion(curriculumVersion);
        offering.setSubject(subject);
        offering.setSemesterNumber(3);
    }

    private Cohort cohort(Long id, String displayName) {
        Cohort cohort = new Cohort();
        cohort.setId(id);
        cohort.setCourse(course);
        cohort.setDisplayName(displayName);
        return cohort;
    }

    private StudentTermEnrollment enrollmentAtSemester(Integer semesterNumber) {
        StudentTermEnrollment enrollment = new StudentTermEnrollment();
        enrollment.setSemesterNumber(semesterNumber);
        return enrollment;
    }

    private CohortSection section(Long id, Cohort cohort, String sectionLabel) {
        CohortRoomAllocation allocation = new CohortRoomAllocation();
        allocation.setCohort(cohort);
        Classroom classroom = new Classroom("Room 1", "Block A", "101", 60);
        CohortSection section = new CohortSection(allocation, termInstance, sectionLabel, classroom, 60);
        section.setId(id);
        return section;
    }

    private FacultyCapacityCheckResult fitsWithinCapacity() {
        return new FacultyCapacityCheckResult(false, 0, 0, 0, 100, 5, "NONE", 100, 0, List.of());
    }

    private Faculty faculty(Long id, Speciality speciality) {
        DesignationMaster designation = new DesignationMaster("Professor", "PROFESSOR", null);
        designation.setId(1L);
        Faculty faculty = new Faculty("EMP00" + id, "Divya", "Krishnan", "divya" + id + "@test.com",
            "9999999999", speciality, designation, "Medical-Surgical", "Skills Lab", null, FacultyStatus.ACTIVE);
        faculty.setId(id);
        return faculty;
    }

    @Test
    void getForOffering_throwsWhenOfferingNotFound() {
        when(courseOfferingRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getForOffering(999L))
            .isInstanceOf(ResourceNotFoundException.class);
    }

    /** Regression test for the OC-163 bug: resolveCohorts() must key on (program, course) AND the
     *  offering's own semesterNumber -- a cohort sharing the same program/course but enrolled at a
     *  different semester this term must not be pulled in. */
    @Test
    void getForOffering_onlyIncludesCohortsEnrolledAtOfferingsOwnSemester() {
        Cohort matchingCohort = cohort(1L, "2023-2027 Batch");
        Cohort otherSemesterCohort = cohort(2L, "2024-2028 Batch");
        when(courseOfferingRepository.findById(100L)).thenReturn(Optional.of(offering));
        when(studentTermEnrollmentRepository.findDistinctCohortIdsByTermInstanceId(10L, EnrollmentStatus.ENROLLED))
            .thenReturn(Set.of(1L, 2L));
        when(cohortRepository.findById(1L)).thenReturn(Optional.of(matchingCohort));
        when(cohortRepository.findById(2L)).thenReturn(Optional.of(otherSemesterCohort));
        when(studentTermEnrollmentRepository.findByTermInstanceIdAndCohortId(10L, 1L))
            .thenReturn(List.of(enrollmentAtSemester(3)));
        when(studentTermEnrollmentRepository.findByTermInstanceIdAndCohortId(10L, 2L))
            .thenReturn(List.of(enrollmentAtSemester(5)));
        when(sectionFacultyRepository.findByCourseOfferingId(100L)).thenReturn(List.of());
        CohortSection matchingSection = section(201L, matchingCohort, "A");
        when(timetableSkeletonService.resolveActiveSections(1L, 10L)).thenReturn(List.of(matchingSection));

        CourseOfferingSectionFacultyResponse response = service.getForOffering(100L);

        assertThat(response.applicable()).isTrue();
        assertThat(response.sections()).extracting(SectionFacultyAssignment::cohortName)
            .containsExactly("2023-2027 Batch");
    }

    @Test
    void getForOffering_notApplicableWhenNoCohortEnrolledAtOfferingsSemester() {
        Cohort otherSemesterCohort = cohort(2L, "2024-2028 Batch");
        when(courseOfferingRepository.findById(100L)).thenReturn(Optional.of(offering));
        when(studentTermEnrollmentRepository.findDistinctCohortIdsByTermInstanceId(10L, EnrollmentStatus.ENROLLED))
            .thenReturn(Set.of(2L));
        when(cohortRepository.findById(2L)).thenReturn(Optional.of(otherSemesterCohort));
        when(studentTermEnrollmentRepository.findByTermInstanceIdAndCohortId(10L, 2L))
            .thenReturn(List.of(enrollmentAtSemester(5)));

        CourseOfferingSectionFacultyResponse response = service.getForOffering(100L);

        assertThat(response.applicable()).isFalse();
        assertThat(response.sections()).isEmpty();
    }

    @Test
    void getAssignmentSummaryForTermInstance_notApplicableWhenNothingToAssign() {
        when(courseOfferingRepository.findByTermInstanceId(10L)).thenReturn(List.of(offering));
        when(batchRepository.findByTermInstanceIdAndIsActiveTrue(10L)).thenReturn(List.of());
        when(studentTermEnrollmentRepository.findDistinctCohortIdsByTermInstanceId(10L, EnrollmentStatus.ENROLLED))
            .thenReturn(Set.of());

        List<com.cms.dto.CourseOfferingFacultySummaryDto> summaries = service.getAssignmentSummaryForTermInstance(10L);

        assertThat(summaries).hasSize(1);
        assertThat(summaries.get(0).assignmentStatus()).isEqualTo(com.cms.model.enums.OfferingAssignmentStatus.NOT_APPLICABLE);
        assertThat(summaries.get(0).assignedFacultyNames()).isEmpty();
    }

    @Test
    void getAssignmentSummaryForTermInstance_fullWhenEverySectionAndBatchIsAssigned() {
        Cohort matchingCohort = cohort(1L, "2023-2027 Batch");
        CohortSection matchingSection = section(201L, matchingCohort, "A");
        Faculty theoryFaculty = faculty(7L, subject.getSpeciality());
        CourseOfferingSectionFaculty theoryRow = new CourseOfferingSectionFaculty(offering, matchingSection, theoryFaculty);

        com.cms.model.Batch batch = new com.cms.model.Batch(offering, "Batch 1", 20, termInstance);
        batch.setId(301L);
        batch.setCoordinatorFaculty(theoryFaculty);

        when(courseOfferingRepository.findByTermInstanceId(10L)).thenReturn(List.of(offering));
        when(courseOfferingRepository.findById(100L)).thenReturn(Optional.of(offering));
        when(batchRepository.findByTermInstanceIdAndIsActiveTrue(10L)).thenReturn(List.of(batch));
        when(studentTermEnrollmentRepository.findDistinctCohortIdsByTermInstanceId(10L, EnrollmentStatus.ENROLLED))
            .thenReturn(Set.of(1L));
        when(cohortRepository.findById(1L)).thenReturn(Optional.of(matchingCohort));
        when(studentTermEnrollmentRepository.findByTermInstanceIdAndCohortId(10L, 1L))
            .thenReturn(List.of(enrollmentAtSemester(3)));
        when(sectionFacultyRepository.findByCourseOfferingId(100L)).thenReturn(List.of(theoryRow));
        when(timetableSkeletonService.resolveActiveSections(1L, 10L)).thenReturn(List.of(matchingSection));

        List<com.cms.dto.CourseOfferingFacultySummaryDto> summaries = service.getAssignmentSummaryForTermInstance(10L);

        assertThat(summaries).hasSize(1);
        assertThat(summaries.get(0).assignmentStatus()).isEqualTo(com.cms.model.enums.OfferingAssignmentStatus.FULL);
        assertThat(summaries.get(0).assignedFacultyNames()).containsExactly(theoryFaculty.getFullName());
    }

    @Test
    void getAssignmentSummaryForTermInstance_partialWhenBatchCoordinatorMissing() {
        Cohort matchingCohort = cohort(1L, "2023-2027 Batch");
        CohortSection matchingSection = section(201L, matchingCohort, "A");
        Faculty theoryFaculty = faculty(7L, subject.getSpeciality());
        CourseOfferingSectionFaculty theoryRow = new CourseOfferingSectionFaculty(offering, matchingSection, theoryFaculty);

        com.cms.model.Batch unassignedBatch = new com.cms.model.Batch(offering, "Batch 1", 20, termInstance);
        unassignedBatch.setId(301L);

        when(courseOfferingRepository.findByTermInstanceId(10L)).thenReturn(List.of(offering));
        when(courseOfferingRepository.findById(100L)).thenReturn(Optional.of(offering));
        when(batchRepository.findByTermInstanceIdAndIsActiveTrue(10L)).thenReturn(List.of(unassignedBatch));
        when(studentTermEnrollmentRepository.findDistinctCohortIdsByTermInstanceId(10L, EnrollmentStatus.ENROLLED))
            .thenReturn(Set.of(1L));
        when(cohortRepository.findById(1L)).thenReturn(Optional.of(matchingCohort));
        when(studentTermEnrollmentRepository.findByTermInstanceIdAndCohortId(10L, 1L))
            .thenReturn(List.of(enrollmentAtSemester(3)));
        when(sectionFacultyRepository.findByCourseOfferingId(100L)).thenReturn(List.of(theoryRow));
        when(timetableSkeletonService.resolveActiveSections(1L, 10L)).thenReturn(List.of(matchingSection));

        List<com.cms.dto.CourseOfferingFacultySummaryDto> summaries = service.getAssignmentSummaryForTermInstance(10L);

        assertThat(summaries).hasSize(1);
        assertThat(summaries.get(0).assignmentStatus()).isEqualTo(com.cms.model.enums.OfferingAssignmentStatus.PARTIAL);
    }

    /** Regression test for the Assign Faculty list showing a real name in the FACULTY column next
     *  to a red "Unassigned" badge: a {@link CourseOfferingSectionFaculty} row can outlive the
     *  section it names (the section gets deactivated/relabeled on a Capacity Auto-Plan recommit,
     *  or a cohort's split status changes) with nothing ever deleting it. {@code
     *  assignedFacultyNames} must come from the same live-resolved {@link #getForOffering} view
     *  {@code assignmentStatus} already uses, not the raw persisted row -- so a stale row pointing
     *  at a since-deactivated section must not surface its faculty name here even though the row
     *  itself is still sitting in the table. */
    @Test
    void getAssignmentSummaryForTermInstance_omitsFacultyNameFromAStaleRowPointingAtADeactivatedSection() {
        Cohort matchingCohort = cohort(1L, "2023-2027 Batch");
        CohortSection currentlyActiveSection = section(201L, matchingCohort, "A");
        CohortSection staleDeactivatedSection = section(999L, matchingCohort, "A");
        Faculty theoryFaculty = faculty(7L, subject.getSpeciality());
        // The persisted row still points at the OLD section id -- simulating a recommit that
        // replaced/deactivated it without cleaning up this row.
        CourseOfferingSectionFaculty staleRow = new CourseOfferingSectionFaculty(offering, staleDeactivatedSection, theoryFaculty);

        when(courseOfferingRepository.findByTermInstanceId(10L)).thenReturn(List.of(offering));
        when(courseOfferingRepository.findById(100L)).thenReturn(Optional.of(offering));
        when(batchRepository.findByTermInstanceIdAndIsActiveTrue(10L)).thenReturn(List.of());
        when(studentTermEnrollmentRepository.findDistinctCohortIdsByTermInstanceId(10L, EnrollmentStatus.ENROLLED))
            .thenReturn(Set.of(1L));
        when(cohortRepository.findById(1L)).thenReturn(Optional.of(matchingCohort));
        when(studentTermEnrollmentRepository.findByTermInstanceIdAndCohortId(10L, 1L))
            .thenReturn(List.of(enrollmentAtSemester(3)));
        when(sectionFacultyRepository.findByCourseOfferingId(100L)).thenReturn(List.of(staleRow));
        when(timetableSkeletonService.resolveActiveSections(1L, 10L)).thenReturn(List.of(currentlyActiveSection));

        List<com.cms.dto.CourseOfferingFacultySummaryDto> summaries = service.getAssignmentSummaryForTermInstance(10L);

        assertThat(summaries).hasSize(1);
        assertThat(summaries.get(0).assignedFacultyNames()).isEmpty();
        assertThat(summaries.get(0).assignmentStatus()).isEqualTo(com.cms.model.enums.OfferingAssignmentStatus.NONE);
    }

    @Test
    void upsert_throwsWhenSectionNotAmongResolvedCohortsSections() {
        Cohort matchingCohort = cohort(1L, "2023-2027 Batch");
        when(courseOfferingRepository.findById(100L)).thenReturn(Optional.of(offering));
        when(studentTermEnrollmentRepository.findDistinctCohortIdsByTermInstanceId(10L, EnrollmentStatus.ENROLLED))
            .thenReturn(Set.of(1L));
        when(cohortRepository.findById(1L)).thenReturn(Optional.of(matchingCohort));
        when(studentTermEnrollmentRepository.findByTermInstanceIdAndCohortId(10L, 1L))
            .thenReturn(List.of(enrollmentAtSemester(3)));
        when(timetableSkeletonService.resolveActiveSections(1L, 10L))
            .thenReturn(List.of(section(201L, matchingCohort, "A")));

        assertThatThrownBy(() -> service.upsert(100L, 999L, 1L, null))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void upsert_rejectsFacultyOutsideSubjectsSpeciality() {
        Cohort matchingCohort = cohort(1L, "2023-2027 Batch");
        CohortSection targetSection = section(201L, matchingCohort, "A");
        when(courseOfferingRepository.findById(100L)).thenReturn(Optional.of(offering));
        when(studentTermEnrollmentRepository.findDistinctCohortIdsByTermInstanceId(10L, EnrollmentStatus.ENROLLED))
            .thenReturn(Set.of(1L));
        when(cohortRepository.findById(1L)).thenReturn(Optional.of(matchingCohort));
        when(studentTermEnrollmentRepository.findByTermInstanceIdAndCohortId(10L, 1L))
            .thenReturn(List.of(enrollmentAtSemester(3)));
        when(timetableSkeletonService.resolveActiveSections(1L, 10L)).thenReturn(List.of(targetSection));
        when(sectionFacultyRepository.findByCourseOfferingIdAndCohortSectionId(100L, 201L)).thenReturn(Optional.empty());
        Speciality otherSpeciality = new Speciality("Pediatrics", "PED", "dept", null, null);
        otherSpeciality.setId(2L);
        Faculty ineligibleFaculty = faculty(5L, otherSpeciality);
        when(facultyRepository.findById(5L)).thenReturn(Optional.of(ineligibleFaculty));

        assertThatThrownBy(() -> service.upsert(100L, 201L, 5L, null))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void upsert_allowsFacultyExplicitlyOnTheSubjectsEligibleFacultyListDespiteSpecialityMismatch() {
        Cohort matchingCohort = cohort(1L, "2023-2027 Batch");
        CohortSection targetSection = section(201L, matchingCohort, "A");
        when(courseOfferingRepository.findById(100L)).thenReturn(Optional.of(offering));
        when(studentTermEnrollmentRepository.findDistinctCohortIdsByTermInstanceId(10L, EnrollmentStatus.ENROLLED))
            .thenReturn(Set.of(1L));
        when(cohortRepository.findById(1L)).thenReturn(Optional.of(matchingCohort));
        when(studentTermEnrollmentRepository.findByTermInstanceIdAndCohortId(10L, 1L))
            .thenReturn(List.of(enrollmentAtSemester(3)));
        when(timetableSkeletonService.resolveActiveSections(1L, 10L)).thenReturn(List.of(targetSection));
        when(sectionFacultyRepository.findByCourseOfferingIdAndCohortSectionId(100L, 201L)).thenReturn(Optional.empty());
        Speciality otherSpeciality = new Speciality("Pediatrics", "PED", "dept", null, null);
        otherSpeciality.setId(2L);
        Faculty widenedFaculty = faculty(5L, otherSpeciality);
        subject.setEligibleFaculty(new java.util.HashSet<>(java.util.Set.of(widenedFaculty)));
        when(facultyRepository.findById(5L)).thenReturn(Optional.of(widenedFaculty));
        when(sectionFacultyRepository.save(any(CourseOfferingSectionFaculty.class))).thenAnswer(inv -> inv.getArgument(0));
        when(timetableGlobalAutoScheduleService.checkFacultyCapacityForSection(100L, 201L, 5L)).thenReturn(fitsWithinCapacity());

        SectionFacultyAssignment result = service.upsert(100L, 201L, 5L, null);

        assertThat(result.facultyId()).isEqualTo(5L);
    }

    /** Regression test for the elective-group-faculty-conflict incident (see
     *  {@code CourseOfferingSectionFacultyService#requireNoElectiveGroupFacultyConflict}): binding
     *  the same faculty to two options within one elective group is a structural impossibility
     *  (every option must run at one shared simultaneous slot), so it must be rejected right here at
     *  assignment time rather than surfacing months later as an unexplained "no day/period found"
     *  from Global Auto-Schedule. */
    @Test
    void upsert_rejectsFacultyAlreadyBoundToASiblingOfferingInTheSameElectiveGroup() {
        Cohort matchingCohort = cohort(1L, "2023-2027 Batch");
        CohortSection targetSection = section(201L, matchingCohort, "A");

        com.cms.model.CurriculumElectiveGroup electiveGroup = new com.cms.model.CurriculumElectiveGroup();
        electiveGroup.setId(11L);
        electiveGroup.setGroupName("Elective I (Sem III-IV)");
        com.cms.model.CurriculumSemesterCourse csc = new com.cms.model.CurriculumSemesterCourse();
        csc.setElectiveGroup(electiveGroup);
        offering.setCurriculumSemesterCourse(csc);

        Subject siblingSubject = new Subject();
        siblingSubject.setId(2L);
        siblingSubject.setName("Elective: Soft Skills");
        CourseOffering siblingOffering = new CourseOffering();
        siblingOffering.setId(101L);
        siblingOffering.setSubject(siblingSubject);

        when(courseOfferingRepository.findById(100L)).thenReturn(Optional.of(offering));
        when(studentTermEnrollmentRepository.findDistinctCohortIdsByTermInstanceId(10L, EnrollmentStatus.ENROLLED))
            .thenReturn(Set.of(1L));
        when(cohortRepository.findById(1L)).thenReturn(Optional.of(matchingCohort));
        when(studentTermEnrollmentRepository.findByTermInstanceIdAndCohortId(10L, 1L))
            .thenReturn(List.of(enrollmentAtSemester(3)));
        when(timetableSkeletonService.resolveActiveSections(1L, 10L)).thenReturn(List.of(targetSection));
        when(sectionFacultyRepository.findByCourseOfferingIdAndCohortSectionId(100L, 201L)).thenReturn(Optional.empty());
        when(courseOfferingRepository.findByTermInstanceIdAndCurriculumSemesterCourse_ElectiveGroupId(10L, 11L))
            .thenReturn(List.of(offering, siblingOffering));
        Faculty faculty = faculty(6L, subject.getSpeciality());
        when(facultyRepository.findById(6L)).thenReturn(Optional.of(faculty));
        when(sectionFacultyRepository.findByCourseOfferingId(101L)).thenReturn(
            List.of(new CourseOfferingSectionFaculty(siblingOffering, matchingCohort, faculty)));

        assertThatThrownBy(() -> service.upsert(100L, 201L, 6L, null))
            .isInstanceOf(com.cms.exception.TimetableConstraintViolationException.class)
            .hasMessageContaining("Elective: Soft Skills");

        verify(sectionFacultyRepository, never()).save(any());
    }

    @Test
    void upsert_savesEligibleFacultyOverride() {
        Cohort matchingCohort = cohort(1L, "2023-2027 Batch");
        CohortSection targetSection = section(201L, matchingCohort, "A");
        when(courseOfferingRepository.findById(100L)).thenReturn(Optional.of(offering));
        when(studentTermEnrollmentRepository.findDistinctCohortIdsByTermInstanceId(10L, EnrollmentStatus.ENROLLED))
            .thenReturn(Set.of(1L));
        when(cohortRepository.findById(1L)).thenReturn(Optional.of(matchingCohort));
        when(studentTermEnrollmentRepository.findByTermInstanceIdAndCohortId(10L, 1L))
            .thenReturn(List.of(enrollmentAtSemester(3)));
        when(timetableSkeletonService.resolveActiveSections(1L, 10L)).thenReturn(List.of(targetSection));
        when(sectionFacultyRepository.findByCourseOfferingIdAndCohortSectionId(100L, 201L)).thenReturn(Optional.empty());
        Faculty eligibleFaculty = faculty(6L, subject.getSpeciality());
        when(facultyRepository.findById(6L)).thenReturn(Optional.of(eligibleFaculty));
        when(sectionFacultyRepository.save(any(CourseOfferingSectionFaculty.class))).thenAnswer(inv -> inv.getArgument(0));
        when(timetableGlobalAutoScheduleService.checkFacultyCapacityForSection(100L, 201L, 6L)).thenReturn(fitsWithinCapacity());

        SectionFacultyAssignment result = service.upsert(100L, 201L, 6L, null);

        assertThat(result.facultyId()).isEqualTo(6L);
        assertThat(result.cohortName()).isEqualTo("2023-2027 Batch");
    }

    @Test
    void upsert_blocksAnOverCapacityFacultyAssignment() {
        Cohort matchingCohort = cohort(1L, "2023-2027 Batch");
        CohortSection targetSection = section(201L, matchingCohort, "A");
        when(courseOfferingRepository.findById(100L)).thenReturn(Optional.of(offering));
        when(studentTermEnrollmentRepository.findDistinctCohortIdsByTermInstanceId(10L, EnrollmentStatus.ENROLLED))
            .thenReturn(Set.of(1L));
        when(cohortRepository.findById(1L)).thenReturn(Optional.of(matchingCohort));
        when(studentTermEnrollmentRepository.findByTermInstanceIdAndCohortId(10L, 1L))
            .thenReturn(List.of(enrollmentAtSemester(3)));
        when(timetableSkeletonService.resolveActiveSections(1L, 10L)).thenReturn(List.of(targetSection));
        when(sectionFacultyRepository.findByCourseOfferingIdAndCohortSectionId(100L, 201L)).thenReturn(Optional.empty());
        Faculty overCapacityFaculty = faculty(6L, subject.getSpeciality());
        when(facultyRepository.findById(6L)).thenReturn(Optional.of(overCapacityFaculty));
        when(timetableGlobalAutoScheduleService.checkFacultyCapacityForSection(100L, 201L, 6L))
            .thenReturn(new FacultyCapacityCheckResult(true, 90, 40, 130, 100, 5, "FACULTY_OVERRIDE", 100, 2, List.of()));

        assertThatThrownBy(() -> service.upsert(100L, 201L, 6L, null))
            .isInstanceOf(com.cms.exception.TimetableConstraintViolationException.class);

        verify(sectionFacultyRepository, never()).save(any());
    }

    /** Regression test for the confirm-substitutions "changed by someone else" false positive: a
     *  section's own sessions can split across two different substitutes within one Global
     *  Auto-Schedule run (each session retries the fallback list independently against that day's
     *  availability), producing two tips that both list the same (cohortId, cohortSectionId) row.
     *  Confirming both in one batch must not roll back on the second tip just because the first tip
     *  (in the same call) already moved that row off the original faculty -- the first-ticked tip
     *  wins the row and the second tip's claim on it is skipped, not treated as an external conflict. */
    @Test
    void confirmSubstitutions_skipsASectionAlreadyWonByAnEarlierTipInTheSameBatchInsteadOfFailingTheWholeBatch() {
        Cohort matchingCohort = cohort(1L, "2023-2027 Batch");
        CohortSection targetSection = section(201L, matchingCohort, "A");

        Faculty originalFaculty = faculty(9L, subject.getSpeciality());
        Faculty substituteA = faculty(6L, subject.getSpeciality());
        CourseOfferingSectionFaculty rowOnOriginal = new CourseOfferingSectionFaculty(offering, targetSection, originalFaculty);
        CourseOfferingSectionFaculty rowOnSubstituteA = new CourseOfferingSectionFaculty(offering, targetSection, substituteA);

        when(courseOfferingRepository.findById(100L)).thenReturn(Optional.of(offering));
        when(studentTermEnrollmentRepository.findDistinctCohortIdsByTermInstanceId(10L, EnrollmentStatus.ENROLLED))
            .thenReturn(Set.of(1L));
        when(cohortRepository.findById(1L)).thenReturn(Optional.of(matchingCohort));
        when(studentTermEnrollmentRepository.findByTermInstanceIdAndCohortId(10L, 1L))
            .thenReturn(List.of(enrollmentAtSemester(3)));
        when(timetableSkeletonService.resolveActiveSections(1L, 10L)).thenReturn(List.of(targetSection));

        // getForOffering is re-read at the top of each item's loop iteration -- the second read must
        // see the first item's own write already applied (that's what makes this an in-batch, not an
        // external, conflict).
        when(sectionFacultyRepository.findByCourseOfferingId(100L))
            .thenReturn(List.of(rowOnOriginal), List.of(rowOnSubstituteA));
        when(sectionFacultyRepository.findByCourseOfferingIdAndCohortSectionId(100L, 201L))
            .thenReturn(Optional.of(rowOnOriginal));
        when(facultyRepository.findById(6L)).thenReturn(Optional.of(substituteA));
        when(sectionFacultyRepository.save(any(CourseOfferingSectionFaculty.class))).thenAnswer(inv -> inv.getArgument(0));
        when(timetableGlobalAutoScheduleService.checkFacultyCapacityForSection(100L, 201L, 6L)).thenReturn(fitsWithinCapacity());

        com.cms.dto.SubstitutionAffectedSection affected = new com.cms.dto.SubstitutionAffectedSection(1L, 201L, null);
        com.cms.dto.ConfirmFacultySubstitutionItem tipToSubstituteA =
            new com.cms.dto.ConfirmFacultySubstitutionItem(100L, 9L, 6L, List.of(affected));
        com.cms.dto.ConfirmFacultySubstitutionItem tipToSubstituteB =
            new com.cms.dto.ConfirmFacultySubstitutionItem(100L, 9L, 7L, List.of(affected));

        com.cms.dto.ConfirmFacultySubstitutionsResult result =
            service.confirmSubstitutions(List.of(tipToSubstituteA, tipToSubstituteB));

        assertThat(result.rowsReassigned()).isEqualTo(1);
        assertThat(result.sectionsSkipped()).isEqualTo(1);
        assertThat(result.offeringsUpdated()).isEqualTo(1);
        verify(facultyRepository, never()).findById(7L);
    }

    /** Regression test: a LAB/CLINICAL substitution tip's `originalFacultyId` comes from {@code
     *  Batch#coordinatorFaculty}, never from {@code CourseOfferingSectionFaculty} (the Theory-only
     *  table). Before the `batchId` field existed on {@code SubstitutionAffectedSection}, confirming
     *  one of these always fell through to the Theory row lookup, found no match (or a mismatched
     *  faculty), and threw "changed by someone else" -- even though nothing was ever wrong. Confirming
     *  must instead route a `batchId`-carrying affected section straight to {@link
     *  BatchService#reassignCoordinator}. */
    @Test
    void confirmSubstitutions_reassignsBatchCoordinatorForALabClinicalTip() {
        com.cms.model.Batch batch = new com.cms.model.Batch(offering, "Clinical - Section 1", 20, termInstance);
        batch.setId(501L);
        Faculty originalCoordinator = faculty(31L, subject.getSpeciality());
        Faculty substituteCoordinator = faculty(33L, subject.getSpeciality());
        batch.setCoordinatorFaculty(originalCoordinator);
        batch.setVersion(0L);

        when(courseOfferingRepository.findById(100L)).thenReturn(Optional.of(offering));
        when(batchRepository.findById(501L)).thenReturn(Optional.of(batch));

        com.cms.dto.SubstitutionAffectedSection affected = new com.cms.dto.SubstitutionAffectedSection(1L, null, 501L);
        com.cms.dto.ConfirmFacultySubstitutionItem tip =
            new com.cms.dto.ConfirmFacultySubstitutionItem(100L, 31L, 33L, List.of(affected));

        com.cms.dto.ConfirmFacultySubstitutionsResult result = service.confirmSubstitutions(List.of(tip));

        assertThat(result.rowsReassigned()).isEqualTo(1);
        assertThat(result.sectionsSkipped()).isEqualTo(0);
        verify(batchService).reassignCoordinator(501L, 33L, 0L);
        verify(sectionFacultyRepository, never()).save(any());
    }

    @Test
    void upsert_clearsExistingOverrideWhenFacultyIdNull() {
        Cohort matchingCohort = cohort(1L, "2023-2027 Batch");
        CohortSection targetSection = section(201L, matchingCohort, "A");
        when(courseOfferingRepository.findById(100L)).thenReturn(Optional.of(offering));
        when(studentTermEnrollmentRepository.findDistinctCohortIdsByTermInstanceId(10L, EnrollmentStatus.ENROLLED))
            .thenReturn(Set.of(1L));
        when(cohortRepository.findById(1L)).thenReturn(Optional.of(matchingCohort));
        when(studentTermEnrollmentRepository.findByTermInstanceIdAndCohortId(10L, 1L))
            .thenReturn(List.of(enrollmentAtSemester(3)));
        when(timetableSkeletonService.resolveActiveSections(1L, 10L)).thenReturn(List.of(targetSection));
        Faculty existingFaculty = faculty(6L, subject.getSpeciality());
        CourseOfferingSectionFaculty existingRow = new CourseOfferingSectionFaculty(offering, targetSection, existingFaculty);
        when(sectionFacultyRepository.findByCourseOfferingIdAndCohortSectionId(100L, 201L)).thenReturn(Optional.of(existingRow));

        SectionFacultyAssignment result = service.upsert(100L, 201L, null, null);

        assertThat(result.facultyId()).isNull();
    }
}
