package com.cms.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.cms.dto.AutoStaffResult;
import com.cms.dto.CourseOfferingSectionFacultyResponse;
import com.cms.dto.FacultyCapacityCheckResult;
import com.cms.dto.SectionFacultyAssignment;
import com.cms.dto.StaffingAssignmentRequest;
import com.cms.dto.UnstaffedCellResponse;
import com.cms.exception.TimetableConstraintViolationException;
import com.cms.model.ClassSchedule;
import com.cms.model.Classroom;
import com.cms.model.CourseOffering;
import com.cms.model.CourseOfferingSectionFaculty;
import com.cms.model.Faculty;
import com.cms.model.enums.ClassSessionType;
import com.cms.model.enums.DayOfWeek;
import com.cms.model.enums.FacultyStatus;
import com.cms.repository.ClassScheduleRepository;
import com.cms.repository.ClassroomRepository;
import com.cms.repository.CourseOfferingRepository;
import com.cms.repository.CourseOfferingSectionFacultyRepository;
import com.cms.repository.FacultyRepository;

@ExtendWith(MockitoExtension.class)
class TimetableStaffingAutoAssignServiceTest {

    @Mock private TimetableStaffingService timetableStaffingService;
    @Mock private FacultyRepository facultyRepository;
    @Mock private ClassScheduleRepository classScheduleRepository;
    @Mock private ClassroomRepository classroomRepository;
    @Mock private CourseOfferingRepository courseOfferingRepository;
    @Mock private CourseOfferingSectionFacultyRepository courseOfferingSectionFacultyRepository;
    @Mock private CourseOfferingSectionFacultyService courseOfferingSectionFacultyService;
    @Mock private TimetableGlobalAutoScheduleService timetableGlobalAutoScheduleService;

    private TimetableStaffingAutoAssignService service;

    @BeforeEach
    void setUp() {
        service = new TimetableStaffingAutoAssignService(timetableStaffingService, facultyRepository, classScheduleRepository,
            classroomRepository, courseOfferingRepository, courseOfferingSectionFacultyRepository,
            courseOfferingSectionFacultyService, timetableGlobalAutoScheduleService);
    }

    private Classroom classroom(Long id, Integer capacity) {
        Classroom c = new Classroom("Room " + id, null, null, capacity);
        c.setId(id);
        return c;
    }

    private UnstaffedCellResponse cell(Long id, Long offeringId, String subjectName, Long specialityId, Long venueId, boolean elective) {
        return new UnstaffedCellResponse(id, offeringId, subjectName, subjectName.substring(0, 4).toUpperCase(),
            specialityId, "Nursing", ClassSessionType.THEORY, DayOfWeek.MONDAY, 1L, "1st Period",
            LocalTime.of(9, 0), LocalTime.of(9, 50), null, 40, venueId, "Room 101", 60, elective, List.of(), null, null);
    }

    private UnstaffedCellResponse cellWithSection(Long id, Long offeringId, Long cohortSectionId, Long specialityId, Long venueId) {
        return new UnstaffedCellResponse(id, offeringId, "Anatomy", "ANAT",
            specialityId, "Nursing", ClassSessionType.THEORY, DayOfWeek.MONDAY, 1L, "1st Period",
            LocalTime.of(9, 0), LocalTime.of(9, 50), null, 40, venueId, "Room 101", 60, false, List.of(), null, cohortSectionId);
    }

    private FacultyCapacityCheckResult fitsWithinCapacity() {
        return new FacultyCapacityCheckResult(false, 0, 0, 0, 100, 5, "NONE", 100, 0, 0, List.of());
    }

    private FacultyCapacityCheckResult overCapacity() {
        return new FacultyCapacityCheckResult(true, 90, 40, 130, 100, 5, "FACULTY_OVERRIDE", 100, 2, 3, List.of());
    }

    private Faculty faculty(Long id) {
        Faculty f = new Faculty();
        f.setId(id);
        f.setStatus(FacultyStatus.ACTIVE);
        return f;
    }

    @Test
    void shouldStaffASimpleCell() {
        when(timetableStaffingService.getUnstaffedCells(10L))
            .thenReturn(List.of(cell(1L, 100L, "Anatomy", 5L, 9L, false)));
        when(facultyRepository.findBySpecialityIdAndStatus(5L, FacultyStatus.ACTIVE))
            .thenReturn(List.of(faculty(50L)));
        when(classScheduleRepository.findByCourseOfferingIdAndIsActiveTrue(100L)).thenReturn(Collections.emptyList());

        AutoStaffResult result = service.autoStaff(10L);

        assertThat(result.staffedCount()).isEqualTo(1);
        assertThat(result.unplaced()).isEmpty();
        verify(timetableStaffingService).staffCell(1L, new StaffingAssignmentRequest(50L, null));
    }

    @Test
    void shouldAutoStaffAnElectiveWithItsAssignedFacultyAndBestFitRoom() {
        // Electives carry no speciality (deliberately NULL for all 15 of them) so they're never
        // run through the department-pool branch -- faculty comes from whatever's already assigned
        // via Assign Faculty (mirrors TimetableGlobalAutoScheduleService#resolveElectiveMemberFacultyId),
        // and the room is picked tightest-fit-first since electives can never have one pre-committed
        // in Capacity Planner (no single owning cohort).
        when(timetableStaffingService.getUnstaffedCells(10L))
            .thenReturn(List.of(cell(1L, 100L, "Elective Subject", null, null, true)));
        when(courseOfferingSectionFacultyService.getForOffering(100L)).thenReturn(
            new CourseOfferingSectionFacultyResponse(true, null,
                List.of(new SectionFacultyAssignment(1L, null, "Cohort", null, 70L, "Dr. X", 1L))));
        when(classroomRepository.findByIsActiveTrueOrderByNameAsc())
            .thenReturn(List.of(classroom(9L, 100), classroom(8L, 45)));

        AutoStaffResult result = service.autoStaff(10L);

        assertThat(result.staffedCount()).isEqualTo(1);
        assertThat(result.unplaced()).isEmpty();
        // requiredStrength is 40 (see cell()) -- the 45-seat room is the tighter fit of the two.
        verify(timetableStaffingService).staffCell(1L, new StaffingAssignmentRequest(70L, 8L));
    }

    @Test
    void shouldReportElectiveUnplacedWhenNoSingleFacultyIsAssigned() {
        when(timetableStaffingService.getUnstaffedCells(10L))
            .thenReturn(List.of(cell(1L, 100L, "Elective Subject", null, null, true)));
        when(courseOfferingSectionFacultyService.getForOffering(100L))
            .thenReturn(new CourseOfferingSectionFacultyResponse(true, null, List.of()));

        AutoStaffResult result = service.autoStaff(10L);

        assertThat(result.staffedCount()).isZero();
        assertThat(result.unplaced()).hasSize(1);
        verify(timetableStaffingService, times(0)).staffCell(any(), any());
    }

    @Test
    void shouldReportUnplacedWhenSubjectHasNoSpeciality() {
        when(timetableStaffingService.getUnstaffedCells(10L))
            .thenReturn(List.of(cell(1L, 100L, "Anatomy", null, 9L, false)));

        AutoStaffResult result = service.autoStaff(10L);

        assertThat(result.staffedCount()).isZero();
        assertThat(result.unplaced()).hasSize(1);
        assertThat(result.unplaced().get(0).reason()).contains("no department");
        verify(timetableStaffingService, times(0)).staffCell(any(), any());
    }

    @Test
    void shouldReportUnplacedWhenNoRoomCommitted() {
        when(timetableStaffingService.getUnstaffedCells(10L))
            .thenReturn(List.of(cell(1L, 100L, "Anatomy", 5L, null, false)));

        AutoStaffResult result = service.autoStaff(10L);

        assertThat(result.staffedCount()).isZero();
        assertThat(result.unplaced()).hasSize(1);
        assertThat(result.unplaced().get(0).reason()).contains("no room committed");
        verify(timetableStaffingService, times(0)).staffCell(any(), any());
    }

    @Test
    void shouldPreferTheFacultyAlreadyTeachingOtherSessionsOfThisSubject() {
        when(timetableStaffingService.getUnstaffedCells(10L))
            .thenReturn(List.of(cell(1L, 100L, "Anatomy", 5L, 9L, false)));
        when(facultyRepository.findBySpecialityIdAndStatus(5L, FacultyStatus.ACTIVE))
            .thenReturn(List.of(faculty(50L), faculty(51L)));

        ClassSchedule existingByFifty = new ClassSchedule();
        existingByFifty.setFaculty(faculty(50L));
        when(classScheduleRepository.findByCourseOfferingIdAndIsActiveTrue(100L)).thenReturn(List.of(existingByFifty));

        service.autoStaff(10L);

        verify(timetableStaffingService).staffCell(1L, new StaffingAssignmentRequest(50L, null));
    }

    @Test
    void shouldTryTheNextCandidateWhenTheFirstIsRejected() {
        when(timetableStaffingService.getUnstaffedCells(10L))
            .thenReturn(List.of(cell(1L, 100L, "Anatomy", 5L, 9L, false)));
        when(facultyRepository.findBySpecialityIdAndStatus(5L, FacultyStatus.ACTIVE))
            .thenReturn(List.of(faculty(50L), faculty(51L)));
        when(classScheduleRepository.findByCourseOfferingIdAndIsActiveTrue(100L)).thenReturn(Collections.emptyList());
        when(timetableStaffingService.staffCell(1L, new StaffingAssignmentRequest(50L, null)))
            .thenThrow(new TimetableConstraintViolationException(
                List.of(new com.cms.dto.ConstraintViolation("STAFFING_FACULTY_CONFLICT", "busy"))));

        AutoStaffResult result = service.autoStaff(10L);

        assertThat(result.staffedCount()).isEqualTo(1);
        verify(timetableStaffingService).staffCell(1L, new StaffingAssignmentRequest(51L, null));
    }

    @Test
    void shouldReportUnplacedWhenEveryCandidateIsRejected() {
        when(timetableStaffingService.getUnstaffedCells(10L))
            .thenReturn(List.of(cell(1L, 100L, "Anatomy", 5L, 9L, false)));
        when(facultyRepository.findBySpecialityIdAndStatus(5L, FacultyStatus.ACTIVE))
            .thenReturn(List.of(faculty(50L)));
        when(classScheduleRepository.findByCourseOfferingIdAndIsActiveTrue(100L)).thenReturn(Collections.emptyList());
        when(timetableStaffingService.staffCell(1L, new StaffingAssignmentRequest(50L, null)))
            .thenThrow(new TimetableConstraintViolationException(
                List.of(new com.cms.dto.ConstraintViolation("STAFFING_FACULTY_CONFLICT", "busy"))));

        AutoStaffResult result = service.autoStaff(10L);

        assertThat(result.staffedCount()).isZero();
        assertThat(result.unplaced()).hasSize(1);
        assertThat(result.unplaced().get(0).subjectName()).isEqualTo("Anatomy");
    }

    @Test
    void shouldPreferASectionsFacultyOverrideWhenItHasCapacity() {
        CourseOffering offering = new CourseOffering();
        offering.setId(100L);
        when(courseOfferingRepository.findById(100L)).thenReturn(Optional.of(offering));
        when(timetableStaffingService.getUnstaffedCells(10L))
            .thenReturn(List.of(cellWithSection(1L, 100L, 20L, 5L, 9L)));
        CourseOfferingSectionFaculty override = new CourseOfferingSectionFaculty();
        override.setFaculty(faculty(60L));
        when(courseOfferingSectionFacultyRepository.findByCourseOfferingIdAndCohortSectionId(100L, 20L))
            .thenReturn(Optional.of(override));
        when(timetableGlobalAutoScheduleService.checkFacultyCapacityForSection(100L, 20L, 60L))
            .thenReturn(fitsWithinCapacity());

        AutoStaffResult result = service.autoStaff(10L);

        assertThat(result.staffedCount()).isEqualTo(1);
        verify(timetableStaffingService).staffCell(1L, new StaffingAssignmentRequest(60L, null));
        verify(facultyRepository, times(0)).findBySpecialityIdAndStatus(any(), any());
    }

    @Test
    void shouldFallBackToRankedPoolWhenSectionOverrideIsOverCapacity() {
        CourseOffering offering = new CourseOffering();
        offering.setId(100L);
        when(courseOfferingRepository.findById(100L)).thenReturn(Optional.of(offering));
        when(timetableStaffingService.getUnstaffedCells(10L))
            .thenReturn(List.of(cellWithSection(1L, 100L, 20L, 5L, 9L)));
        CourseOfferingSectionFaculty override = new CourseOfferingSectionFaculty();
        override.setFaculty(faculty(60L));
        when(courseOfferingSectionFacultyRepository.findByCourseOfferingIdAndCohortSectionId(100L, 20L))
            .thenReturn(Optional.of(override));
        when(timetableGlobalAutoScheduleService.checkFacultyCapacityForSection(100L, 20L, 60L))
            .thenReturn(overCapacity());
        when(facultyRepository.findBySpecialityIdAndStatus(5L, FacultyStatus.ACTIVE))
            .thenReturn(List.of(faculty(50L)));
        when(classScheduleRepository.findByCourseOfferingIdAndIsActiveTrue(100L)).thenReturn(Collections.emptyList());

        AutoStaffResult result = service.autoStaff(10L);

        assertThat(result.staffedCount()).isEqualTo(1);
        verify(timetableStaffingService).staffCell(1L, new StaffingAssignmentRequest(50L, null));
        verify(timetableStaffingService, times(0)).staffCell(1L, new StaffingAssignmentRequest(60L, null));
    }

    @Test
    void shouldFallBackToRankedPoolWhenSectionOverrideFacultyIsInactive() {
        // A section override pointing at a faculty who has since resigned/retired/gone on leave must
        // never be auto-staffed onto a cell just because a capacity check alone would allow it.
        CourseOffering offering = new CourseOffering();
        offering.setId(100L);
        when(courseOfferingRepository.findById(100L)).thenReturn(Optional.of(offering));
        when(timetableStaffingService.getUnstaffedCells(10L))
            .thenReturn(List.of(cellWithSection(1L, 100L, 20L, 5L, 9L)));
        Faculty inactiveOverrideFaculty = faculty(60L);
        inactiveOverrideFaculty.setStatus(FacultyStatus.RESIGNED);
        CourseOfferingSectionFaculty override = new CourseOfferingSectionFaculty();
        override.setFaculty(inactiveOverrideFaculty);
        when(courseOfferingSectionFacultyRepository.findByCourseOfferingIdAndCohortSectionId(100L, 20L))
            .thenReturn(Optional.of(override));
        when(facultyRepository.findBySpecialityIdAndStatus(5L, FacultyStatus.ACTIVE))
            .thenReturn(List.of(faculty(50L)));
        when(classScheduleRepository.findByCourseOfferingIdAndIsActiveTrue(100L)).thenReturn(Collections.emptyList());

        AutoStaffResult result = service.autoStaff(10L);

        assertThat(result.staffedCount()).isEqualTo(1);
        verify(timetableStaffingService).staffCell(1L, new StaffingAssignmentRequest(50L, null));
        verify(timetableStaffingService, times(0)).staffCell(1L, new StaffingAssignmentRequest(60L, null));
        verify(timetableGlobalAutoScheduleService, times(0)).checkFacultyCapacityForSection(any(), any(), any());
    }
}
