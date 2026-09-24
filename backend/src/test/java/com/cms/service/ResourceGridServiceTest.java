package com.cms.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.LocalTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.cms.dto.ClassScheduleResponse;
import com.cms.dto.ResourceGridRowResponse;
import com.cms.model.Classroom;
import com.cms.model.ClinicalVenue;
import com.cms.model.DesignationMaster;
import com.cms.model.Faculty;
import com.cms.model.Speciality;
import com.cms.model.enums.ClassScheduleStatus;
import com.cms.model.enums.ClassSessionType;
import com.cms.model.enums.DayOfWeek;
import com.cms.model.enums.FacultyStatus;
import com.cms.model.enums.RoomKind;
import com.cms.repository.BatchRepository;
import com.cms.repository.ClassScheduleRepository;
import com.cms.repository.ClassroomRepository;
import com.cms.repository.ClinicalShiftGroupRepository;
import com.cms.repository.ClinicalVenueRepository;
import com.cms.repository.DayMappingOverrideRepository;
import com.cms.repository.FacultyRepository;
import com.cms.repository.LabRepository;
import com.cms.repository.StudentTermEnrollmentRepository;

@ExtendWith(MockitoExtension.class)
class ResourceGridServiceTest {

    @Mock private ClassScheduleRepository classScheduleRepository;
    @Mock private ClassScheduleService classScheduleService;
    @Mock private FacultyRepository facultyRepository;
    @Mock private ClassroomRepository classroomRepository;
    @Mock private LabRepository labRepository;
    @Mock private ClinicalVenueRepository clinicalVenueRepository;
    @Mock private DayMappingOverrideRepository dayMappingOverrideRepository;
    @Mock private ClinicalShiftGroupRepository clinicalShiftGroupRepository;
    @Mock private BatchRepository batchRepository;
    @Mock private StudentTermEnrollmentRepository studentTermEnrollmentRepository;

    private ResourceGridService service;
    private Faculty faculty1;
    private Faculty faculty2;

    @BeforeEach
    void setUp() {
        service = new ResourceGridService(classScheduleRepository, classScheduleService,
            facultyRepository, classroomRepository, labRepository, clinicalVenueRepository,
            dayMappingOverrideRepository, clinicalShiftGroupRepository, batchRepository,
            studentTermEnrollmentRepository);
        lenient().when(clinicalShiftGroupRepository.findByTermInstanceIdAndIsActiveTrue(any()))
            .thenReturn(List.of());

        Speciality speciality = new Speciality("Nursing", "NUR", "Nursing Dept", null, null);
        speciality.setId(1L);
        DesignationMaster designation = new DesignationMaster("Assistant Professor", "ASSISTANT_PROFESSOR", null);
        designation.setId(1L);

        faculty1 = new Faculty("EMP001", "John", "Doe", "john@college.edu", "1234567890",
            speciality, designation, "Nursing", null, null, FacultyStatus.ACTIVE);
        faculty1.setId(1L);
        faculty2 = new Faculty("EMP002", "Jane", "Roe", "jane@college.edu", "1234567891",
            speciality, designation, "Nursing", null, null, FacultyStatus.ACTIVE);
        faculty2.setId(2L);
    }

    @Test
    void shouldBuildOneRowPerActiveFacultyWithOnlyThatFacultysSessions() {
        com.cms.model.ClassSchedule cs1 = new com.cms.model.ClassSchedule();
        cs1.setId(100L);
        cs1.setFaculty(faculty1);

        ClassScheduleResponse response1 = new ClassScheduleResponse(100L, ClassSessionType.THEORY,
            ClassScheduleStatus.PUBLISHED, null, null, 1L, "Nursing Foundations", "NF101", 1L, "John Doe",
            1L, "1st Period", LocalTime.of(9, 0), LocalTime.of(10, 0), null, null, 1L, null, "Room 101",
            1L, null, DayOfWeek.MONDAY, 10L, "ODD 2026", true, Instant.now(), Instant.now());

        when(classScheduleRepository.findByTermInstanceIdAndStatusAndDayOfWeek(10L, ClassScheduleStatus.PUBLISHED, DayOfWeek.MONDAY))
            .thenReturn(List.of(cs1));
        when(classScheduleService.toResponseList(List.of(cs1))).thenReturn(List.of(response1));
        when(facultyRepository.findByStatus(FacultyStatus.ACTIVE)).thenReturn(List.of(faculty1, faculty2));

        List<ResourceGridRowResponse> rows = service.getResourceGrid(
            ResourceGridService.ResourceType.FACULTY, 10L, DayOfWeek.MONDAY, null);

        assertThat(rows).hasSize(2);
        ResourceGridRowResponse row1 = rows.stream().filter(r -> r.resourceId().equals(1L)).findFirst().orElseThrow();
        ResourceGridRowResponse row2 = rows.stream().filter(r -> r.resourceId().equals(2L)).findFirst().orElseThrow();
        assertThat(row1.sessions()).hasSize(1);
        assertThat(row1.sessions().get(0).sessionId()).isEqualTo(100L);
        assertThat(row2.sessions()).isEmpty();
    }

    @Test
    void shouldExcludeInactiveAndUnderMaintenanceLabsFromClassroomGrid() {
        Classroom classroom = new Classroom("Room 101", "Main Block", "101", 60);
        classroom.setId(1L);

        com.cms.model.Lab activeLab = new com.cms.model.Lab("Skills Lab", com.cms.model.enums.LabType.OTHER,
            null, "Main Block", "L1", 30, com.cms.model.enums.LabStatus.ACTIVE);
        activeLab.setId(1L);
        com.cms.model.Lab maintenanceLab = new com.cms.model.Lab("Broken Lab", com.cms.model.enums.LabType.OTHER,
            null, "Main Block", "L2", 30, com.cms.model.enums.LabStatus.UNDER_MAINTENANCE);
        maintenanceLab.setId(2L);

        when(classScheduleRepository.findByTermInstanceIdAndStatusAndDayOfWeek(10L, ClassScheduleStatus.PUBLISHED, DayOfWeek.MONDAY))
            .thenReturn(List.of());
        when(classScheduleService.toResponseList(List.of())).thenReturn(List.of());
        when(classroomRepository.findByIsActiveTrueOrderByNameAsc()).thenReturn(List.of(classroom));
        when(labRepository.findAll()).thenReturn(List.of(activeLab, maintenanceLab));
        when(clinicalVenueRepository.findByIsActiveTrueOrderByNameAsc()).thenReturn(List.of());

        List<ResourceGridRowResponse> rows = service.getResourceGrid(
            ResourceGridService.ResourceType.CLASSROOM, 10L, DayOfWeek.MONDAY, null);

        assertThat(rows).hasSize(2); // classroom + activeLab, maintenanceLab excluded
        assertThat(rows).noneMatch(r -> r.resourceId().equals(2L));
    }

    @Test
    void shouldIncludeClinicalVenuesInTheRoomGrid() {
        // R3 Phase 6 regression fix: a CLINICAL session lives in ClinicalVenue, never
        // Classroom/Lab -- without this it silently never appeared in this grid at all.
        ClinicalVenue venue = new ClinicalVenue("Ward 3", "Government General Hospital", "OBG");
        venue.setId(1L);

        com.cms.model.ClassSchedule clinicalSession = new com.cms.model.ClassSchedule();
        clinicalSession.setId(100L);
        clinicalSession.setClinicalVenue(venue);

        ClassScheduleResponse response = new ClassScheduleResponse(100L, ClassSessionType.CLINICAL,
            ClassScheduleStatus.PUBLISHED, null, null, 1L, "Community Health Nursing", "CHN101", 1L, "John Doe",
            1L, "1st Period", LocalTime.of(9, 0), LocalTime.of(10, 0), "Batch A", 1L, null, 1L, "Ward 3",
            1L, null, DayOfWeek.MONDAY, 10L, "ODD 2026", true, Instant.now(), Instant.now());

        when(classScheduleRepository.findByTermInstanceIdAndStatusAndDayOfWeek(10L, ClassScheduleStatus.PUBLISHED, DayOfWeek.MONDAY))
            .thenReturn(List.of(clinicalSession));
        when(classScheduleService.toResponseList(List.of(clinicalSession))).thenReturn(List.of(response));
        when(classroomRepository.findByIsActiveTrueOrderByNameAsc()).thenReturn(List.of());
        when(labRepository.findAll()).thenReturn(List.of());
        when(clinicalVenueRepository.findByIsActiveTrueOrderByNameAsc()).thenReturn(List.of(venue));

        List<ResourceGridRowResponse> rows = service.getResourceGrid(
            ResourceGridService.ResourceType.CLASSROOM, 10L, DayOfWeek.MONDAY, null);

        assertThat(rows).hasSize(1);
        assertThat(rows.get(0).resourceId()).isEqualTo(1L);
        assertThat(rows.get(0).sessions()).hasSize(1);
        assertThat(rows.get(0).sessions().get(0).sessionId()).isEqualTo(100L);
    }

    @Test
    void libraryCellWithNoCourseOfferingShouldFallBackToTheCohortsRealTermNumber() {
        // LIBRARY sessions never have a CourseOffering (see TimetableSkeletonService's Library
        // filler paths), so ClassScheduleResponse#termNumber is always null for them -- this grid
        // must fall back to the cell's own CohortSection -> cohort's real StudentTermEnrollment for
        // this term instance instead, the same source Global Auto-Schedule itself uses to resolve
        // "which term is this cohort actually in."
        Classroom libraryHall = new Classroom("Library Hall", "Main Block", "L01", 60);
        libraryHall.setId(1L);

        com.cms.model.Cohort cohort = new com.cms.model.Cohort();
        cohort.setId(5L);
        com.cms.model.CohortRoomAllocation allocation = new com.cms.model.CohortRoomAllocation();
        allocation.setId(9L);
        allocation.setCohort(cohort);
        com.cms.model.CohortSection section = new com.cms.model.CohortSection();
        section.setId(20L);
        section.setCohortRoomAllocation(allocation);

        com.cms.model.ClassSchedule librarySession = new com.cms.model.ClassSchedule();
        librarySession.setId(100L);
        librarySession.setClassroom(libraryHall);
        librarySession.setCohortSection(section);

        ClassScheduleResponse response = new ClassScheduleResponse(100L, ClassSessionType.LIBRARY,
            ClassScheduleStatus.PUBLISHED, null, null, null, "Library", "LIB", null, null,
            1L, "1st Period", LocalTime.of(9, 0), LocalTime.of(10, 0), "Section 1 — Whole Section", null, 1L, null,
            "Library Hall", null, null, DayOfWeek.MONDAY, 10L, "ODD 2026", true, Instant.now(), Instant.now());

        com.cms.model.StudentTermEnrollment enrollment = new com.cms.model.StudentTermEnrollment();
        enrollment.setSemesterNumber(3);

        when(classScheduleRepository.findByTermInstanceIdAndStatusAndDayOfWeek(10L, ClassScheduleStatus.PUBLISHED, DayOfWeek.MONDAY))
            .thenReturn(List.of(librarySession));
        when(classScheduleService.toResponseList(List.of(librarySession))).thenReturn(List.of(response));
        when(classroomRepository.findByIsActiveTrueOrderByNameAsc()).thenReturn(List.of(libraryHall));
        when(labRepository.findAll()).thenReturn(List.of());
        when(clinicalVenueRepository.findByIsActiveTrueOrderByNameAsc()).thenReturn(List.of());
        when(studentTermEnrollmentRepository.findFirstByTermInstanceIdAndCohortIdAndStatus(
            10L, 5L, com.cms.model.enums.EnrollmentStatus.ENROLLED)).thenReturn(java.util.Optional.of(enrollment));

        List<ResourceGridRowResponse> rows = service.getResourceGrid(
            ResourceGridService.ResourceType.CLASSROOM, 10L, DayOfWeek.MONDAY, null);

        assertThat(rows).hasSize(1);
        assertThat(rows.get(0).sessions()).hasSize(1);
        assertThat(rows.get(0).sessions().get(0).termNumber()).isEqualTo(3);
    }

    @Test
    void requestingByAMappedDateShouldResolveTheBorrowedWeekdaysSchedules() {
        // 2024-08-10 is a Saturday mapped to run Monday's schedule.
        java.time.LocalDate mappedSaturday = java.time.LocalDate.of(2024, 8, 10);
        com.cms.model.DayMappingOverride mapping = new com.cms.model.DayMappingOverride();
        mapping.setBorrowedDayOfWeek(DayOfWeek.MONDAY);
        when(dayMappingOverrideRepository.findByMappedDate(mappedSaturday)).thenReturn(java.util.Optional.of(mapping));

        com.cms.model.ClassSchedule cs1 = new com.cms.model.ClassSchedule();
        cs1.setId(100L);
        cs1.setFaculty(faculty1);

        ClassScheduleResponse response1 = new ClassScheduleResponse(100L, ClassSessionType.THEORY,
            ClassScheduleStatus.PUBLISHED, null, null, 1L, "Nursing Foundations", "NF101", 1L, "John Doe",
            1L, "1st Period", LocalTime.of(9, 0), LocalTime.of(10, 0), null, null, 1L, null, "Room 101",
            1L, null, DayOfWeek.MONDAY, 10L, "ODD 2026", true, Instant.now(), Instant.now());

        when(classScheduleRepository.findByTermInstanceIdAndStatusAndDayOfWeek(10L, ClassScheduleStatus.PUBLISHED, DayOfWeek.MONDAY))
            .thenReturn(List.of(cs1));
        when(classScheduleService.toResponseList(List.of(cs1))).thenReturn(List.of(response1));
        when(facultyRepository.findByStatus(FacultyStatus.ACTIVE)).thenReturn(List.of(faculty1, faculty2));

        List<ResourceGridRowResponse> rows = service.getResourceGrid(
            ResourceGridService.ResourceType.FACULTY, 10L, null, mappedSaturday);

        ResourceGridRowResponse row1 = rows.stream().filter(r -> r.resourceId().equals(1L)).findFirst().orElseThrow();
        assertThat(row1.sessions()).hasSize(1);
        assertThat(row1.sessions().get(0).sessionId()).isEqualTo(100L);
    }

    @Test
    void weekGridWeekdayModeShouldReturnOnlyThisFacultysSessionsAcrossAllSixDays() {
        com.cms.model.ClassSchedule mondaySession = new com.cms.model.ClassSchedule();
        mondaySession.setId(100L);
        mondaySession.setFaculty(faculty1);
        mondaySession.setDayOfWeek(DayOfWeek.MONDAY);

        com.cms.model.ClassSchedule otherFacultySession = new com.cms.model.ClassSchedule();
        otherFacultySession.setId(101L);
        otherFacultySession.setFaculty(faculty2);
        otherFacultySession.setDayOfWeek(DayOfWeek.MONDAY);

        ClassScheduleResponse mondayResponse = new ClassScheduleResponse(100L, ClassSessionType.THEORY,
            ClassScheduleStatus.PUBLISHED, null, null, 1L, "Nursing Foundations", "NF101", 1L, "John Doe",
            1L, "1st Period", LocalTime.of(9, 0), LocalTime.of(10, 0), null, null, 1L, null, "Room 101",
            1L, null, DayOfWeek.MONDAY, 10L, "ODD 2026", true, Instant.now(), Instant.now());
        ClassScheduleResponse otherResponse = new ClassScheduleResponse(101L, ClassSessionType.THEORY,
            ClassScheduleStatus.PUBLISHED, null, null, 1L, "Nursing Foundations", "NF101", 2L, "Jane Roe",
            1L, "1st Period", LocalTime.of(9, 0), LocalTime.of(10, 0), null, null, 1L, null, "Room 101",
            1L, null, DayOfWeek.MONDAY, 10L, "ODD 2026", true, Instant.now(), Instant.now());

        List<com.cms.model.ClassSchedule> published = List.of(mondaySession, otherFacultySession);
        when(classScheduleRepository.findByTermInstanceIdAndStatus(10L, ClassScheduleStatus.PUBLISHED))
            .thenReturn(published);
        when(classScheduleService.toResponseList(published)).thenReturn(List.of(mondayResponse, otherResponse));

        List<com.cms.dto.ResourceGridCellResponse> cells = service.getResourceWeekGrid(
            ResourceGridService.ResourceType.FACULTY, 1L, 10L, null, null);

        assertThat(cells).hasSize(1);
        assertThat(cells.get(0).sessionId()).isEqualTo(100L);
        assertThat(cells.get(0).dayOfWeek()).isEqualTo(DayOfWeek.MONDAY);
    }

    @Test
    void weekGridDateModeShouldDisplayABorrowedSessionUnderItsRealCalendarColumn() {
        // Saturday 2024-08-10 of the displayed week borrows Monday's schedule.
        java.time.LocalDate weekStart = java.time.LocalDate.of(2024, 8, 5); // a Monday
        java.time.LocalDate borrowedSaturday = weekStart.plusDays(5);
        com.cms.model.DayMappingOverride mapping = new com.cms.model.DayMappingOverride();
        mapping.setBorrowedDayOfWeek(DayOfWeek.MONDAY);
        // The other 5 real weekday lookups (Mon-Fri) hit no override -> fall back to their own actual weekday.
        lenient().when(dayMappingOverrideRepository.findByMappedDate(any())).thenReturn(java.util.Optional.empty());
        when(dayMappingOverrideRepository.findByMappedDate(borrowedSaturday)).thenReturn(java.util.Optional.of(mapping));

        com.cms.model.ClassSchedule mondaySession = new com.cms.model.ClassSchedule();
        mondaySession.setId(100L);
        mondaySession.setFaculty(faculty1);
        mondaySession.setDayOfWeek(DayOfWeek.MONDAY);

        ClassScheduleResponse mondayResponse = new ClassScheduleResponse(100L, ClassSessionType.THEORY,
            ClassScheduleStatus.PUBLISHED, null, null, 1L, "Nursing Foundations", "NF101", 1L, "John Doe",
            1L, "1st Period", LocalTime.of(9, 0), LocalTime.of(10, 0), null, null, 1L, null, "Room 101",
            1L, null, DayOfWeek.MONDAY, 10L, "ODD 2026", true, Instant.now(), Instant.now());

        List<com.cms.model.ClassSchedule> published = List.of(mondaySession);
        when(classScheduleRepository.findByTermInstanceIdAndStatus(10L, ClassScheduleStatus.PUBLISHED))
            .thenReturn(published);
        when(classScheduleService.toResponseList(published)).thenReturn(List.of(mondayResponse));

        List<com.cms.dto.ResourceGridCellResponse> cells = service.getResourceWeekGrid(
            ResourceGridService.ResourceType.FACULTY, 1L, 10L, weekStart, null);

        // The real Monday column shows it under MONDAY (no override there), and the borrowed
        // Saturday column shows the *same* Monday-template row again, but under SATURDAY.
        assertThat(cells).hasSize(2);
        assertThat(cells).extracting(c -> c.dayOfWeek()).containsExactlyInAnyOrder(DayOfWeek.MONDAY, DayOfWeek.SATURDAY);
    }

    @Test
    void weekGridShouldNotLeakSessionsFromAnotherRoomTableWhoseIdCoincides() {
        // Classroom and ClinicalVenue are separate tables, each with their own auto-increment id --
        // both landing on id 13 here is exactly the real-world collision reported against "Library
        // Hall" (Classroom 13) silently pulling in "CHC 2" (ClinicalVenue 13)'s own Clinical Shift.
        Classroom libraryHall = new Classroom("Library Hall", "Main Block", "L01", 60);
        libraryHall.setId(13L);
        ClinicalVenue chc2 = new ClinicalVenue("CHC 2", "Community Health Centre", "CHN");
        chc2.setId(13L);

        com.cms.model.ClassSchedule classroomSession = new com.cms.model.ClassSchedule();
        classroomSession.setId(200L);
        classroomSession.setClassroom(libraryHall);
        classroomSession.setDayOfWeek(DayOfWeek.MONDAY);

        com.cms.model.ClassSchedule clinicalVenueSession = new com.cms.model.ClassSchedule();
        clinicalVenueSession.setId(201L);
        clinicalVenueSession.setClinicalVenue(chc2);
        clinicalVenueSession.setDayOfWeek(DayOfWeek.MONDAY);

        ClassScheduleResponse classroomResponse = new ClassScheduleResponse(200L, ClassSessionType.LIBRARY,
            ClassScheduleStatus.PUBLISHED, null, null, null, "Library", "LIB", null, null,
            1L, "1st Period", LocalTime.of(9, 0), LocalTime.of(10, 0), null, null, 13L, null, "Library Hall",
            null, null, DayOfWeek.MONDAY, 10L, "ODD 2026", true, Instant.now(), Instant.now());
        ClassScheduleResponse clinicalVenueResponse = new ClassScheduleResponse(201L, ClassSessionType.CLINICAL,
            ClassScheduleStatus.PUBLISHED, null, null, 1L, "Community Health Nursing", "CHN101", 1L, "John Doe",
            1L, "1st Period", LocalTime.of(9, 0), LocalTime.of(10, 0), "Batch A", 1L, null, 13L, "CHC 2",
            1L, 5, DayOfWeek.MONDAY, 10L, "ODD 2026", true, Instant.now(), Instant.now());

        List<com.cms.model.ClassSchedule> published = List.of(classroomSession, clinicalVenueSession);
        when(classScheduleRepository.findByTermInstanceIdAndStatus(10L, ClassScheduleStatus.PUBLISHED))
            .thenReturn(published);
        when(classScheduleService.toResponseList(published))
            .thenReturn(List.of(classroomResponse, clinicalVenueResponse));

        List<com.cms.dto.ResourceGridCellResponse> cells = service.getResourceWeekGrid(
            ResourceGridService.ResourceType.CLASSROOM, 13L, 10L, null, RoomKind.CLASSROOM);

        assertThat(cells).hasSize(1);
        assertThat(cells.get(0).sessionId()).isEqualTo(200L);
    }

    @Test
    void weekGridShouldRejectAMissingRoomKindForClassroomType() {
        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class,
            () -> service.getResourceWeekGrid(ResourceGridService.ResourceType.CLASSROOM, 13L, 10L, null, null));
    }
}
