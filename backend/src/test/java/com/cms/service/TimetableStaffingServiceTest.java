package com.cms.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.cms.dto.StaffingAssignmentRequest;
import com.cms.dto.SystemConfigurationResponse;
import com.cms.dto.UnstaffedCellResponse;
import com.cms.exception.LifecycleConflictException;
import com.cms.exception.ResourceNotFoundException;
import com.cms.model.AcademicYear;
import com.cms.model.Batch;
import com.cms.model.BlockedPeriod;
import com.cms.model.Classroom;
import com.cms.model.ClassSchedule;
import com.cms.model.Cohort;
import com.cms.model.CohortRoomAllocation;
import com.cms.model.CohortSection;
import com.cms.model.CourseOffering;
import com.cms.model.CurriculumSemesterCourse;
import com.cms.model.DesignationMaster;
import com.cms.model.Faculty;
import com.cms.model.FacultyAvailability;
import com.cms.model.Lab;
import com.cms.model.Period;
import com.cms.model.Speciality;
import com.cms.model.StudentTermEnrollment;
import com.cms.model.Subject;
import com.cms.model.TermInstance;
import com.cms.model.enums.ClassScheduleStatus;
import com.cms.model.enums.ClassSessionType;
import com.cms.model.enums.CohortRoomAllocationStatus;
import com.cms.model.enums.ConfigDataType;
import com.cms.model.enums.DayOfWeek;
import com.cms.model.enums.FacultyStatus;
import com.cms.model.enums.LabStatus;
import com.cms.model.enums.PlanningBasis;
import com.cms.model.enums.BlockType;
import com.cms.model.enums.TermInstanceStatus;
import com.cms.model.enums.TermType;
import com.cms.repository.BatchRepository;
import com.cms.repository.BlockedPeriodRepository;
import com.cms.repository.ClassScheduleRepository;
import com.cms.repository.ClassroomRepository;
import com.cms.repository.CohortRoomAllocationRepository;
import com.cms.repository.CohortSectionRepository;
import com.cms.repository.CourseRegistrationRepository;
import com.cms.repository.FacultyAvailabilityRepository;
import com.cms.repository.FacultyRepository;
import com.cms.repository.StudentTermEnrollmentRepository;

@ExtendWith(MockitoExtension.class)
class TimetableStaffingServiceTest {

    @Mock private ClassScheduleRepository classScheduleRepository;
    @Mock private FacultyRepository facultyRepository;
    @Mock private ClassroomRepository classroomRepository;
    @Mock private BatchRepository batchRepository;
    @Mock private CourseRegistrationRepository courseRegistrationRepository;
    @Mock private StudentTermEnrollmentRepository studentTermEnrollmentRepository;
    @Mock private CohortRoomAllocationRepository cohortRoomAllocationRepository;
    @Mock private CohortSectionRepository cohortSectionRepository;
    @Mock private RotationResolverService rotationResolverService;
    @Mock private BlockedPeriodRepository blockedPeriodRepository;
    @Mock private FacultyAvailabilityRepository facultyAvailabilityRepository;
    @Mock private SystemConfigurationService systemConfigurationService;

    private TimetableStaffingService service;

    private TermInstance termInstance;
    private Speciality speciality;
    private Faculty eligibleFaculty;
    private Subject subject;
    private Period period;
    private Classroom classroom;
    private ClassSchedule cell;

    @BeforeEach
    void setUp() {
        service = new TimetableStaffingService(classScheduleRepository, facultyRepository,
            classroomRepository, batchRepository, courseRegistrationRepository,
            studentTermEnrollmentRepository, cohortRoomAllocationRepository, cohortSectionRepository,
            rotationResolverService, blockedPeriodRepository, facultyAvailabilityRepository,
            systemConfigurationService);

        AcademicYear ay = new AcademicYear("2024-2025", LocalDate.of(2024, 6, 1), LocalDate.of(2025, 5, 31), false);
        ay.setId(1L);
        termInstance = new TermInstance(ay, TermType.ODD, LocalDate.of(2024, 6, 1), LocalDate.of(2024, 11, 30), TermInstanceStatus.OPEN);
        termInstance.setId(10L);

        speciality = new Speciality("Nursing", "NUR", "Nursing Dept", null, null);
        speciality.setId(1L);

        DesignationMaster designation = new DesignationMaster("Assistant Professor", "ASSISTANT_PROFESSOR", null);
        designation.setId(1L);
        eligibleFaculty = new Faculty("EMP001", "John", "Doe", "john@college.edu", "1234567890",
            speciality, designation, "Nursing", null, null, FacultyStatus.ACTIVE);
        eligibleFaculty.setId(1L);

        subject = new Subject("Anatomy", "ANAT101", 4, 3, 1, speciality, 1);
        subject.setId(1L);

        period = new Period("1st Period", LocalTime.of(9, 0), LocalTime.of(9, 50), 1);
        period.setId(1L);
        period.setDurationMinutes(50);

        classroom = new Classroom("Room 101", "Main Block", "101", 60);
        classroom.setId(1L);

        cell = new ClassSchedule();
        cell.setId(100L);
        cell.setSessionType(ClassSessionType.THEORY);
        cell.setStatus(ClassScheduleStatus.DRAFT);
        cell.setSubject(subject);
        cell.setDayOfWeek(DayOfWeek.MONDAY);
        cell.setTermInstance(termInstance);
        cell.setPeriod(period);
        // Default fixture is an elective offering (free classroom pick) so every pre-existing
        // test below — none of which are about the non-elective Theory lock — keeps exercising
        // the same free-pick behavior it always has, without needing enrollment/allocation mocks.
        cell.setCourseOffering(electiveOffering());
    }

    private SystemConfigurationResponse configResponse(String key, String value) {
        return new SystemConfigurationResponse(1L, key, value, null, ConfigDataType.DECIMAL, "TIMETABLE", true, null, null);
    }

    private CourseOffering electiveOffering() {
        CurriculumSemesterCourse csc = new CurriculumSemesterCourse();
        csc.setIsElective(true);
        CourseOffering offering = new CourseOffering();
        offering.setTermInstance(termInstance);
        offering.setSemesterNumber(1);
        offering.setCurriculumSemesterCourse(csc);
        return offering;
    }

    @Test
    void shouldListOnlyUnstaffedDraftCells() {
        ClassSchedule staffed = new ClassSchedule();
        staffed.setSessionType(ClassSessionType.THEORY);
        staffed.setFaculty(eligibleFaculty);
        staffed.setSubject(subject);
        staffed.setPeriod(period);

        when(classScheduleRepository.findByTermInstanceIdAndStatus(10L, ClassScheduleStatus.DRAFT))
            .thenReturn(List.of(cell, staffed));

        List<UnstaffedCellResponse> result = service.getUnstaffedCells(10L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).id()).isEqualTo(100L);
    }

    @Test
    void shouldStaffATheoryCellWithFacultyAndClassroom() {
        StaffingAssignmentRequest request = new StaffingAssignmentRequest(1L, 1L);
        when(classScheduleRepository.findById(100L)).thenReturn(Optional.of(cell));
        when(facultyRepository.findById(1L)).thenReturn(Optional.of(eligibleFaculty));
        when(classroomRepository.findById(1L)).thenReturn(Optional.of(classroom));
        when(classScheduleRepository.findOverlapping(any(), any(), any(), any(), any(), any()))
            .thenReturn(Collections.emptyList());
        when(classScheduleRepository.save(any(ClassSchedule.class))).thenAnswer(inv -> inv.getArgument(0));

        service.staffCell(100L, request);

        assertThat(cell.getFaculty()).isEqualTo(eligibleFaculty);
        assertThat(cell.getClassroom()).isEqualTo(classroom);
    }

    @Test
    void shouldRejectAFacultyFromADifferentDepartment() {
        Speciality otherDept = new Speciality("Computer Science", "CS", "CS Dept", null, null);
        otherDept.setId(2L);
        DesignationMaster designation = new DesignationMaster("Lecturer", "LECTURER", null);
        designation.setId(2L);
        Faculty ineligible = new Faculty("EMP002", "Jane", "Roe", "jane@college.edu", "9876543210",
            otherDept, designation, "Computer Science", null, null, FacultyStatus.ACTIVE);
        ineligible.setId(2L);

        StaffingAssignmentRequest request = new StaffingAssignmentRequest(2L, 1L);
        when(classScheduleRepository.findById(100L)).thenReturn(Optional.of(cell));
        when(facultyRepository.findById(2L)).thenReturn(Optional.of(ineligible));

        assertThatThrownBy(() -> service.staffCell(100L, request))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("not eligible to teach");
    }

    @Test
    void shouldRejectWhenFacultyAlreadyCommittedElsewhereAtThatTime() {
        ClassSchedule alreadyBusyElsewhere = new ClassSchedule();
        alreadyBusyElsewhere.setId(200L);
        alreadyBusyElsewhere.setFaculty(eligibleFaculty);
        alreadyBusyElsewhere.setSessionType(ClassSessionType.LAB);

        StaffingAssignmentRequest request = new StaffingAssignmentRequest(1L, 1L);
        when(classScheduleRepository.findById(100L)).thenReturn(Optional.of(cell));
        when(facultyRepository.findById(1L)).thenReturn(Optional.of(eligibleFaculty));
        when(classScheduleRepository.findOverlapping(DayOfWeek.MONDAY, 10L, period.getStartTime(), period.getEndTime(),
            ClassScheduleStatus.PUBLISHED, 100L)).thenReturn(List.of(alreadyBusyElsewhere));

        assertThatThrownBy(() -> service.staffCell(100L, request))
            .isInstanceOf(LifecycleConflictException.class)
            .hasMessageContaining("already scheduled");
    }

    @Test
    void shouldRejectWhenRoomAlreadyOccupiedByAnotherStaffedDraftCell() {
        ClassSchedule otherDraftInSameRoom = new ClassSchedule();
        otherDraftInSameRoom.setId(300L);
        otherDraftInSameRoom.setSessionType(ClassSessionType.THEORY);
        otherDraftInSameRoom.setClassroom(classroom);

        StaffingAssignmentRequest request = new StaffingAssignmentRequest(1L, 1L);
        when(classScheduleRepository.findById(100L)).thenReturn(Optional.of(cell));
        when(facultyRepository.findById(1L)).thenReturn(Optional.of(eligibleFaculty));
        when(classroomRepository.findById(1L)).thenReturn(Optional.of(classroom));
        when(classScheduleRepository.findOverlapping(DayOfWeek.MONDAY, 10L, period.getStartTime(), period.getEndTime(),
            ClassScheduleStatus.PUBLISHED, 100L)).thenReturn(Collections.emptyList());
        when(classScheduleRepository.findOverlapping(DayOfWeek.MONDAY, 10L, period.getStartTime(), period.getEndTime(),
            ClassScheduleStatus.DRAFT, 100L)).thenReturn(List.of(otherDraftInSameRoom));

        assertThatThrownBy(() -> service.staffCell(100L, request))
            .isInstanceOf(LifecycleConflictException.class)
            .hasMessageContaining("already occupied");
    }

    @Test
    void shouldRequireClassroomForATheoryCell() {
        StaffingAssignmentRequest request = new StaffingAssignmentRequest(1L, null);
        when(classScheduleRepository.findById(100L)).thenReturn(Optional.of(cell));
        when(facultyRepository.findById(1L)).thenReturn(Optional.of(eligibleFaculty));
        when(classScheduleRepository.findOverlapping(any(), any(), any(), any(), any(), any()))
            .thenReturn(Collections.emptyList());

        assertThatThrownBy(() -> service.staffCell(100L, request))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("classroom is required");
    }

    @Test
    void shouldRefuseToStaffAnAlreadyPublishedRow() {
        cell.setStatus(ClassScheduleStatus.PUBLISHED);
        StaffingAssignmentRequest request = new StaffingAssignmentRequest(1L, 1L);
        when(classScheduleRepository.findById(100L)).thenReturn(Optional.of(cell));

        assertThatThrownBy(() -> service.staffCell(100L, request))
            .isInstanceOf(LifecycleConflictException.class);
    }

    @Test
    void shouldRejectWhenClassroomCapacityIsBelowRegisteredStrength() {
        CourseOffering offering = electiveOffering();
        offering.setId(50L);
        cell.setCourseOffering(offering);

        StaffingAssignmentRequest request = new StaffingAssignmentRequest(1L, 1L);
        when(classScheduleRepository.findById(100L)).thenReturn(Optional.of(cell));
        when(facultyRepository.findById(1L)).thenReturn(Optional.of(eligibleFaculty));
        when(classroomRepository.findById(1L)).thenReturn(Optional.of(classroom));
        when(classScheduleRepository.findOverlapping(any(), any(), any(), any(), any(), any()))
            .thenReturn(Collections.emptyList());
        when(courseRegistrationRepository.countByCourseOfferingIdAndStatus(50L, com.cms.model.enums.RegistrationStatus.REGISTERED))
            .thenReturn(75L);

        assertThatThrownBy(() -> service.staffCell(100L, request))
            .isInstanceOf(LifecycleConflictException.class)
            .hasMessageContaining("seats 60");
    }

    @Test
    void shouldAllowStaffingWhenRegisteredStrengthFitsClassroomCapacity() {
        CourseOffering offering = electiveOffering();
        offering.setId(50L);
        cell.setCourseOffering(offering);

        StaffingAssignmentRequest request = new StaffingAssignmentRequest(1L, 1L);
        when(classScheduleRepository.findById(100L)).thenReturn(Optional.of(cell));
        when(facultyRepository.findById(1L)).thenReturn(Optional.of(eligibleFaculty));
        when(classroomRepository.findById(1L)).thenReturn(Optional.of(classroom));
        when(classScheduleRepository.findOverlapping(any(), any(), any(), any(), any(), any()))
            .thenReturn(Collections.emptyList());
        when(courseRegistrationRepository.countByCourseOfferingIdAndStatus(50L, com.cms.model.enums.RegistrationStatus.REGISTERED))
            .thenReturn(45L);
        when(classScheduleRepository.save(any(ClassSchedule.class))).thenAnswer(inv -> inv.getArgument(0));

        service.staffCell(100L, request);

        assertThat(cell.getClassroom()).isEqualTo(classroom);
    }

    @Test
    void shouldThrowWhenCellNotFound() {
        when(classScheduleRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.staffCell(999L, new StaffingAssignmentRequest(1L, 1L)))
            .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void shouldStaffALabCellUsingItsBatchsCommittedVenueWithNoRoomInTheRequest() {
        Lab committedLab = new Lab("Lab A", null, speciality, "Main Block", "L1", 30, LabStatus.ACTIVE);
        committedLab.setId(5L);
        Batch batch = new Batch();
        batch.setLab(committedLab);

        cell.setSessionType(ClassSessionType.LAB);
        cell.setBatch(batch);

        StaffingAssignmentRequest request = new StaffingAssignmentRequest(1L, null);
        when(classScheduleRepository.findById(100L)).thenReturn(Optional.of(cell));
        when(facultyRepository.findById(1L)).thenReturn(Optional.of(eligibleFaculty));
        when(classScheduleRepository.findOverlapping(any(), any(), any(), any(), any(), any()))
            .thenReturn(Collections.emptyList());
        when(classScheduleRepository.save(any(ClassSchedule.class))).thenAnswer(inv -> inv.getArgument(0));

        service.staffCell(100L, request);

        assertThat(cell.getLab()).isEqualTo(committedLab);
    }

    @Test
    void shouldRejectStaffingALabCellWhoseBatchHasNoCommittedVenue() {
        Batch batchWithNoVenue = new Batch();

        cell.setSessionType(ClassSessionType.LAB);
        cell.setBatch(batchWithNoVenue);

        StaffingAssignmentRequest request = new StaffingAssignmentRequest(1L, null);
        when(classScheduleRepository.findById(100L)).thenReturn(Optional.of(cell));
        when(facultyRepository.findById(1L)).thenReturn(Optional.of(eligibleFaculty));
        when(classScheduleRepository.findOverlapping(any(), any(), any(), any(), any(), any()))
            .thenReturn(Collections.emptyList());

        assertThatThrownBy(() -> service.staffCell(100L, request))
            .isInstanceOf(LifecycleConflictException.class)
            .hasMessageContaining("no Lab committed");
    }

    @Test
    void shouldResolveNonElectiveTheoryClassroomFromCommittedCohortRoomAllocationWithNoRoomInTheRequest() {
        CurriculumSemesterCourse csc = new CurriculumSemesterCourse();
        csc.setIsElective(false);
        CourseOffering offering = new CourseOffering();
        offering.setTermInstance(termInstance);
        offering.setSemesterNumber(3);
        offering.setCurriculumSemesterCourse(csc);
        cell.setCourseOffering(offering);

        Cohort cohort = new Cohort();
        cohort.setId(7L);
        StudentTermEnrollment enrollment = new StudentTermEnrollment();
        enrollment.setCohort(cohort);
        when(studentTermEnrollmentRepository.findByTermInstanceIdAndSemesterNumber(10L, 3))
            .thenReturn(List.of(enrollment));

        Classroom committedClassroom = new Classroom("Room 202", "Main Block", "202", 60);
        committedClassroom.setId(9L);
        CohortRoomAllocation allocation = new CohortRoomAllocation(cohort, termInstance, PlanningBasis.ENROLLED, 55, "admin");
        allocation.setId(50L);
        when(cohortRoomAllocationRepository.findByCohortIdAndTermInstanceIdAndStatus(7L, 10L, CohortRoomAllocationStatus.COMMITTED))
            .thenReturn(Optional.of(allocation));
        CohortSection section = new CohortSection(allocation, termInstance, "Section 1", committedClassroom, 55);
        when(cohortSectionRepository.findByCohortRoomAllocationIdAndIsActiveTrue(50L))
            .thenReturn(List.of(section));

        StaffingAssignmentRequest request = new StaffingAssignmentRequest(1L, null);
        when(classScheduleRepository.findById(100L)).thenReturn(Optional.of(cell));
        when(facultyRepository.findById(1L)).thenReturn(Optional.of(eligibleFaculty));
        when(classScheduleRepository.findOverlapping(any(), any(), any(), any(), any(), any()))
            .thenReturn(Collections.emptyList());
        when(classScheduleRepository.save(any(ClassSchedule.class))).thenAnswer(inv -> inv.getArgument(0));

        service.staffCell(100L, request);

        assertThat(cell.getClassroom()).isEqualTo(committedClassroom);
    }

    @Test
    void shouldRejectNonElectiveTheoryCellWhoseCohortHasNoCommittedRoom() {
        CurriculumSemesterCourse csc = new CurriculumSemesterCourse();
        csc.setIsElective(false);
        CourseOffering offering = new CourseOffering();
        offering.setTermInstance(termInstance);
        offering.setSemesterNumber(3);
        offering.setCurriculumSemesterCourse(csc);
        cell.setCourseOffering(offering);

        Cohort cohort = new Cohort();
        cohort.setId(7L);
        StudentTermEnrollment enrollment = new StudentTermEnrollment();
        enrollment.setCohort(cohort);
        when(studentTermEnrollmentRepository.findByTermInstanceIdAndSemesterNumber(10L, 3))
            .thenReturn(List.of(enrollment));
        when(cohortRoomAllocationRepository.findByCohortIdAndTermInstanceIdAndStatus(7L, 10L, CohortRoomAllocationStatus.COMMITTED))
            .thenReturn(Optional.empty());

        StaffingAssignmentRequest request = new StaffingAssignmentRequest(1L, null);
        when(classScheduleRepository.findById(100L)).thenReturn(Optional.of(cell));
        when(facultyRepository.findById(1L)).thenReturn(Optional.of(eligibleFaculty));
        when(classScheduleRepository.findOverlapping(any(), any(), any(), any(), any(), any()))
            .thenReturn(Collections.emptyList());

        assertThatThrownBy(() -> service.staffCell(100L, request))
            .isInstanceOf(LifecycleConflictException.class)
            .hasMessageContaining("no Theory room committed");
    }

    @Test
    void shouldResolveTheoryClassroomDirectlyFromTheCellsOwnCohortSectionWithoutEnrollmentInference() {
        Classroom sectionClassroom = new Classroom("Room 303", "Main Block", "303", 40);
        sectionClassroom.setId(11L);
        CohortRoomAllocation allocation = new CohortRoomAllocation();
        allocation.setId(60L);
        CohortSection section = new CohortSection(allocation, termInstance, "Section A", sectionClassroom, 30);
        cell.setCohortSection(section);

        CurriculumSemesterCourse csc = new CurriculumSemesterCourse();
        csc.setIsElective(false);
        CourseOffering offering = new CourseOffering();
        offering.setTermInstance(termInstance);
        offering.setSemesterNumber(3);
        offering.setCurriculumSemesterCourse(csc);
        cell.setCourseOffering(offering);

        StaffingAssignmentRequest request = new StaffingAssignmentRequest(1L, null);
        when(classScheduleRepository.findById(100L)).thenReturn(Optional.of(cell));
        when(facultyRepository.findById(1L)).thenReturn(Optional.of(eligibleFaculty));
        when(classScheduleRepository.findOverlapping(any(), any(), any(), any(), any(), any()))
            .thenReturn(Collections.emptyList());
        when(classScheduleRepository.save(any(ClassSchedule.class))).thenAnswer(inv -> inv.getArgument(0));

        service.staffCell(100L, request);

        assertThat(cell.getClassroom()).isEqualTo(sectionClassroom);
        // The direct link on the cell resolves the room with no ambiguity -- the legacy
        // enrollment-inference chain (and its dependency on this repository) is never consulted.
        verifyNoInteractions(studentTermEnrollmentRepository);
    }

    @Test
    void shouldResolveTheoryClassroomForASectionedCohortWhenTheCellCarriesItsOwnSection() {
        // Regression test: today (without a direct link on the cell) a >1-section allocation
        // always fails staffing with STAFFING_VENUE_NOT_COMMITTED, because
        // resolveCommittedTheoryClassroom's enrollment-inference fallback refuses to guess which
        // of several sections a row belongs to -- reproduced here by stubbing the exact same
        // 2-active-section allocation the legacy path would choke on, then proving staffing still
        // succeeds because the cell's own direct CohortSection link is checked first and skips
        // that fallback entirely.
        Classroom sectionAClassroom = new Classroom("Room 202", "Main Block", "202", 30);
        sectionAClassroom.setId(9L);
        Classroom sectionBClassroom = new Classroom("Room 203", "Main Block", "203", 30);
        sectionBClassroom.setId(10L);
        CohortRoomAllocation allocation = new CohortRoomAllocation();
        allocation.setId(60L);
        CohortSection sectionA = new CohortSection(allocation, termInstance, "Section A", sectionAClassroom, 30);
        CohortSection sectionB = new CohortSection(allocation, termInstance, "Section B", sectionBClassroom, 30);
        cell.setCohortSection(sectionA);

        CurriculumSemesterCourse csc = new CurriculumSemesterCourse();
        csc.setIsElective(false);
        CourseOffering offering = new CourseOffering();
        offering.setTermInstance(termInstance);
        offering.setSemesterNumber(3);
        offering.setCurriculumSemesterCourse(csc);
        cell.setCourseOffering(offering);

        Cohort cohort = new Cohort();
        cohort.setId(7L);
        StudentTermEnrollment enrollment = new StudentTermEnrollment();
        enrollment.setCohort(cohort);
        // Stubbed leniently -- these back the legacy fallback path, which this test proves is
        // never reached (the direct cell.getCohortSection() link short-circuits before it).
        lenient().when(studentTermEnrollmentRepository.findByTermInstanceIdAndSemesterNumber(10L, 3))
            .thenReturn(List.of(enrollment));
        lenient().when(cohortRoomAllocationRepository.findByCohortIdAndTermInstanceIdAndStatus(7L, 10L, CohortRoomAllocationStatus.COMMITTED))
            .thenReturn(Optional.of(allocation));
        lenient().when(cohortSectionRepository.findByCohortRoomAllocationIdAndIsActiveTrue(60L))
            .thenReturn(List.of(sectionA, sectionB));

        StaffingAssignmentRequest request = new StaffingAssignmentRequest(1L, null);
        when(classScheduleRepository.findById(100L)).thenReturn(Optional.of(cell));
        when(facultyRepository.findById(1L)).thenReturn(Optional.of(eligibleFaculty));
        when(classScheduleRepository.findOverlapping(any(), any(), any(), any(), any(), any()))
            .thenReturn(Collections.emptyList());
        when(classScheduleRepository.save(any(ClassSchedule.class))).thenAnswer(inv -> inv.getArgument(0));

        service.staffCell(100L, request);

        assertThat(cell.getClassroom()).isEqualTo(sectionAClassroom);
        verifyNoInteractions(studentTermEnrollmentRepository);
    }

    @Test
    void shouldRejectADifferentClassroomThatSharesTheSamePhysicalRoomAsAnAlreadyOccupiedOne() {
        com.cms.model.Room physicalRoom = new com.cms.model.Room();
        physicalRoom.setId(99L);

        Classroom otherClassroomSameRoom = new Classroom("Room B", "Main Block", "102", 60);
        otherClassroomSameRoom.setId(2L);
        otherClassroomSameRoom.setRoom(physicalRoom);
        classroom.setRoom(physicalRoom);

        ClassSchedule otherDraftInSamePhysicalRoom = new ClassSchedule();
        otherDraftInSamePhysicalRoom.setId(300L);
        otherDraftInSamePhysicalRoom.setSessionType(ClassSessionType.THEORY);
        otherDraftInSamePhysicalRoom.setClassroom(otherClassroomSameRoom);

        StaffingAssignmentRequest request = new StaffingAssignmentRequest(1L, 1L);
        when(classScheduleRepository.findById(100L)).thenReturn(Optional.of(cell));
        when(facultyRepository.findById(1L)).thenReturn(Optional.of(eligibleFaculty));
        when(classroomRepository.findById(1L)).thenReturn(Optional.of(classroom));
        when(classScheduleRepository.findOverlapping(DayOfWeek.MONDAY, 10L, period.getStartTime(), period.getEndTime(),
            ClassScheduleStatus.PUBLISHED, 100L)).thenReturn(Collections.emptyList());
        when(classScheduleRepository.findOverlapping(DayOfWeek.MONDAY, 10L, period.getStartTime(), period.getEndTime(),
            ClassScheduleStatus.DRAFT, 100L)).thenReturn(List.of(otherDraftInSamePhysicalRoom));

        assertThatThrownBy(() -> service.staffCell(100L, request))
            .isInstanceOf(LifecycleConflictException.class)
            .hasMessageContaining("already occupied");
    }

    @Test
    void shouldRejectStaffingACellAtARecurringBlockedPeriod() {
        BlockedPeriod block = new BlockedPeriod();
        block.setBlockType(BlockType.RECURRING);
        block.setDayOfWeek(DayOfWeek.MONDAY);
        block.setRangeStartDate(LocalDate.of(2024, 6, 1));
        block.setRangeEndDate(LocalDate.of(2024, 11, 30));
        block.setReason("Staff meeting");

        StaffingAssignmentRequest request = new StaffingAssignmentRequest(1L, 1L);
        when(classScheduleRepository.findById(100L)).thenReturn(Optional.of(cell));
        when(blockedPeriodRepository.findOverlappingRecurringBlocks(
            DayOfWeek.MONDAY, 1L, termInstance.getStartDate(), termInstance.getEndDate()))
            .thenReturn(List.of(block));

        assertThatThrownBy(() -> service.staffCell(100L, request))
            .isInstanceOf(LifecycleConflictException.class)
            .hasMessageContaining("Staff meeting");
    }

    @Test
    void shouldRejectStaffingACellAtAHolidayDerivedOneOffBlock() {
        com.cms.model.CalendarEvent holiday = new com.cms.model.CalendarEvent();
        holiday.setTitle("Independence Day");
        BlockedPeriod block = new BlockedPeriod();
        block.setBlockType(BlockType.ONE_OFF);
        block.setSpecificDate(LocalDate.of(2024, 8, 5)); // a Monday
        block.setReason("Auto-blocked — Independence Day");
        block.setSourceCalendarEvent(holiday);

        StaffingAssignmentRequest request = new StaffingAssignmentRequest(1L, 1L);
        when(classScheduleRepository.findById(100L)).thenReturn(Optional.of(cell));
        when(blockedPeriodRepository.findHolidayOneOffBlocksInRange(
            1L, termInstance.getStartDate(), termInstance.getEndDate()))
            .thenReturn(List.of(block));

        assertThatThrownBy(() -> service.staffCell(100L, request))
            .isInstanceOf(LifecycleConflictException.class)
            .hasMessageContaining("Auto-blocked");
    }

    @Test
    void shouldRejectStaffingAFacultyMemberDuringTheirDeclaredUnavailableWindow() {
        FacultyAvailability unavailable = new FacultyAvailability();
        unavailable.setReason("On approved leave");

        StaffingAssignmentRequest request = new StaffingAssignmentRequest(1L, 1L);
        when(classScheduleRepository.findById(100L)).thenReturn(Optional.of(cell));
        when(facultyRepository.findById(1L)).thenReturn(Optional.of(eligibleFaculty));
        when(facultyAvailabilityRepository.findOverlapping(1L, DayOfWeek.MONDAY, period.getStartTime(), period.getEndTime()))
            .thenReturn(List.of(unavailable));

        assertThatThrownBy(() -> service.staffCell(100L, request))
            .isInstanceOf(LifecycleConflictException.class)
            .hasMessageContaining("On approved leave");
    }

    @Test
    void shouldRejectStaffingWhenDailyCapWouldBeExceeded() {
        Period newCellPeriod = new Period("2nd Period", LocalTime.of(10, 0), LocalTime.of(11, 0), 2);
        newCellPeriod.setId(2L);
        cell.setPeriod(newCellPeriod);

        ClassSchedule existingSameDay = new ClassSchedule();
        existingSameDay.setId(400L);
        existingSameDay.setFaculty(eligibleFaculty);
        existingSameDay.setDayOfWeek(DayOfWeek.MONDAY);
        Period existingPeriod = new Period("1st Period", LocalTime.of(9, 0), LocalTime.of(9, 30), 1);
        existingPeriod.setId(1L);
        existingSameDay.setPeriod(existingPeriod);

        StaffingAssignmentRequest request = new StaffingAssignmentRequest(1L, 1L);
        when(classScheduleRepository.findById(100L)).thenReturn(Optional.of(cell));
        when(facultyRepository.findById(1L)).thenReturn(Optional.of(eligibleFaculty));
        when(classScheduleRepository.findOverlapping(any(), any(), any(), any(), any(), any()))
            .thenReturn(Collections.emptyList());
        when(systemConfigurationService.findByKey("timetable.faculty_max_daily_hours"))
            .thenReturn(Optional.of(configResponse("timetable.faculty_max_daily_hours", "1")));
        when(classScheduleRepository.findByTermInstanceIdAndStatusAndFacultyId(10L, ClassScheduleStatus.PUBLISHED, 1L))
            .thenReturn(List.of(existingSameDay));
        when(classScheduleRepository.findByTermInstanceIdAndStatusAndFacultyId(10L, ClassScheduleStatus.DRAFT, 1L))
            .thenReturn(Collections.emptyList());

        assertThatThrownBy(() -> service.staffCell(100L, request))
            .isInstanceOf(LifecycleConflictException.class)
            .hasMessageContaining("daily cap");
    }

    @Test
    void shouldRejectStaffingWhenWeeklyCapWouldBeExceeded() {
        ClassSchedule existingDifferentDay = new ClassSchedule();
        existingDifferentDay.setId(400L);
        existingDifferentDay.setFaculty(eligibleFaculty);
        existingDifferentDay.setDayOfWeek(DayOfWeek.TUESDAY);
        Period existingPeriod = new Period("1st Period", LocalTime.of(9, 0), LocalTime.of(9, 30), 1);
        existingPeriod.setId(1L);
        existingDifferentDay.setPeriod(existingPeriod);

        StaffingAssignmentRequest request = new StaffingAssignmentRequest(1L, 1L);
        when(classScheduleRepository.findById(100L)).thenReturn(Optional.of(cell));
        when(facultyRepository.findById(1L)).thenReturn(Optional.of(eligibleFaculty));
        when(classScheduleRepository.findOverlapping(any(), any(), any(), any(), any(), any()))
            .thenReturn(Collections.emptyList());
        when(systemConfigurationService.findByKey("timetable.faculty_max_daily_hours"))
            .thenReturn(Optional.empty());
        when(systemConfigurationService.findByKey("timetable.faculty_max_weekly_hours"))
            .thenReturn(Optional.of(configResponse("timetable.faculty_max_weekly_hours", "1")));
        when(systemConfigurationService.findByKey("timetable.faculty_max_continuous_hours"))
            .thenReturn(Optional.empty());
        when(classScheduleRepository.findByTermInstanceIdAndStatusAndFacultyId(10L, ClassScheduleStatus.PUBLISHED, 1L))
            .thenReturn(List.of(existingDifferentDay));
        when(classScheduleRepository.findByTermInstanceIdAndStatusAndFacultyId(10L, ClassScheduleStatus.DRAFT, 1L))
            .thenReturn(Collections.emptyList());

        assertThatThrownBy(() -> service.staffCell(100L, request))
            .isInstanceOf(LifecycleConflictException.class)
            .hasMessageContaining("weekly cap");
    }

    @Test
    void shouldRejectStaffingWhenContinuousCapWouldBeExceeded() {
        Period newCellPeriod = new Period("2nd Period", LocalTime.of(10, 0), LocalTime.of(11, 0), 2);
        newCellPeriod.setId(2L);
        cell.setPeriod(newCellPeriod);

        ClassSchedule precedingSameDay = new ClassSchedule();
        precedingSameDay.setId(400L);
        precedingSameDay.setFaculty(eligibleFaculty);
        precedingSameDay.setDayOfWeek(DayOfWeek.MONDAY);
        Period precedingPeriod = new Period("1st Period", LocalTime.of(9, 0), LocalTime.of(10, 0), 1);
        precedingPeriod.setId(1L);
        precedingSameDay.setPeriod(precedingPeriod);

        StaffingAssignmentRequest request = new StaffingAssignmentRequest(1L, 1L);
        when(classScheduleRepository.findById(100L)).thenReturn(Optional.of(cell));
        when(facultyRepository.findById(1L)).thenReturn(Optional.of(eligibleFaculty));
        when(classScheduleRepository.findOverlapping(any(), any(), any(), any(), any(), any()))
            .thenReturn(Collections.emptyList());
        when(systemConfigurationService.findByKey("timetable.faculty_max_daily_hours"))
            .thenReturn(Optional.empty());
        when(systemConfigurationService.findByKey("timetable.faculty_max_weekly_hours"))
            .thenReturn(Optional.empty());
        when(systemConfigurationService.findByKey("timetable.faculty_max_continuous_hours"))
            .thenReturn(Optional.of(configResponse("timetable.faculty_max_continuous_hours", "1")));
        when(classScheduleRepository.findByTermInstanceIdAndStatusAndFacultyId(10L, ClassScheduleStatus.PUBLISHED, 1L))
            .thenReturn(List.of(precedingSameDay));
        when(classScheduleRepository.findByTermInstanceIdAndStatusAndFacultyId(10L, ClassScheduleStatus.DRAFT, 1L))
            .thenReturn(Collections.emptyList());

        assertThatThrownBy(() -> service.staffCell(100L, request))
            .isInstanceOf(LifecycleConflictException.class)
            .hasMessageContaining("continuous-hours cap");
    }

    @Test
    void shouldAllowStaffingExactlyAtTheCapBoundary() {
        Period newCellPeriod = new Period("2nd Period", LocalTime.of(9, 30), LocalTime.of(10, 0), 2);
        newCellPeriod.setId(2L);
        cell.setPeriod(newCellPeriod);

        ClassSchedule existingSameDay = new ClassSchedule();
        existingSameDay.setId(400L);
        existingSameDay.setFaculty(eligibleFaculty);
        existingSameDay.setDayOfWeek(DayOfWeek.MONDAY);
        Period existingPeriod = new Period("1st Period", LocalTime.of(9, 0), LocalTime.of(9, 30), 1);
        existingPeriod.setId(1L);
        existingSameDay.setPeriod(existingPeriod);

        StaffingAssignmentRequest request = new StaffingAssignmentRequest(1L, 1L);
        when(classScheduleRepository.findById(100L)).thenReturn(Optional.of(cell));
        when(facultyRepository.findById(1L)).thenReturn(Optional.of(eligibleFaculty));
        when(classroomRepository.findById(1L)).thenReturn(Optional.of(classroom));
        when(classScheduleRepository.findOverlapping(any(), any(), any(), any(), any(), any()))
            .thenReturn(Collections.emptyList());
        when(systemConfigurationService.findByKey("timetable.faculty_max_daily_hours"))
            .thenReturn(Optional.of(configResponse("timetable.faculty_max_daily_hours", "1")));
        when(classScheduleRepository.findByTermInstanceIdAndStatusAndFacultyId(10L, ClassScheduleStatus.PUBLISHED, 1L))
            .thenReturn(List.of(existingSameDay));
        when(classScheduleRepository.findByTermInstanceIdAndStatusAndFacultyId(10L, ClassScheduleStatus.DRAFT, 1L))
            .thenReturn(Collections.emptyList());
        when(classScheduleRepository.save(any(ClassSchedule.class))).thenAnswer(inv -> inv.getArgument(0));

        service.staffCell(100L, request);

        assertThat(cell.getFaculty()).isEqualTo(eligibleFaculty);
    }

    @Test
    void shouldIgnoreAnUnparseableWorkloadCapConfigValue() {
        // A malformed manual edit to the daily-cap config (unparseable as a number) must degrade
        // to "no cap" rather than crashing staffing -- both other caps stay unset too, so this
        // resolves to zero configured caps and requireWithinWorkloadCaps returns before ever
        // consulting the faculty's other sessions this term.
        StaffingAssignmentRequest request = new StaffingAssignmentRequest(1L, 1L);
        when(classScheduleRepository.findById(100L)).thenReturn(Optional.of(cell));
        when(facultyRepository.findById(1L)).thenReturn(Optional.of(eligibleFaculty));
        when(classroomRepository.findById(1L)).thenReturn(Optional.of(classroom));
        when(classScheduleRepository.findOverlapping(any(), any(), any(), any(), any(), any()))
            .thenReturn(Collections.emptyList());
        when(systemConfigurationService.findByKey("timetable.faculty_max_daily_hours"))
            .thenReturn(Optional.of(configResponse("timetable.faculty_max_daily_hours", "not-a-number")));
        when(classScheduleRepository.save(any(ClassSchedule.class))).thenAnswer(inv -> inv.getArgument(0));

        service.staffCell(100L, request);

        assertThat(cell.getFaculty()).isEqualTo(eligibleFaculty);
    }
}
