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
import com.cms.repository.BatchRepository;
import com.cms.repository.ClassScheduleRepository;
import com.cms.repository.ClassroomRepository;
import com.cms.repository.ClinicalShiftGroupRepository;
import com.cms.repository.ClinicalVenueRepository;
import com.cms.repository.DayMappingOverrideRepository;
import com.cms.repository.FacultyRepository;
import com.cms.repository.LabRepository;

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

    private ResourceGridService service;
    private Faculty faculty1;
    private Faculty faculty2;

    @BeforeEach
    void setUp() {
        service = new ResourceGridService(classScheduleRepository, classScheduleService,
            facultyRepository, classroomRepository, labRepository, clinicalVenueRepository,
            dayMappingOverrideRepository, clinicalShiftGroupRepository, batchRepository);
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
            1L, DayOfWeek.MONDAY, 10L, "ODD 2026", true, Instant.now(), Instant.now());

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
            1L, DayOfWeek.MONDAY, 10L, "ODD 2026", true, Instant.now(), Instant.now());

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
            1L, DayOfWeek.MONDAY, 10L, "ODD 2026", true, Instant.now(), Instant.now());

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
}
