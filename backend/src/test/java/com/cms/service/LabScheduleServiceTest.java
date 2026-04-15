package com.cms.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.LocalTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.cms.dto.LabScheduleRequest;
import com.cms.dto.LabScheduleResponse;
import com.cms.dto.ScheduleConflictResponse;
import com.cms.exception.ResourceNotFoundException;
import com.cms.model.Subject;
import com.cms.model.Department;
import com.cms.model.Faculty;
import com.cms.model.Lab;
import com.cms.model.LabSchedule;
import com.cms.model.LabSlot;
import com.cms.model.Program;
import com.cms.model.Semester;
import com.cms.model.enums.DayOfWeek;
import com.cms.model.enums.Designation;
import com.cms.model.enums.FacultyStatus;
import com.cms.model.enums.LabStatus;
import com.cms.repository.SubjectRepository;
import com.cms.repository.FacultyRepository;
import com.cms.repository.LabRepository;
import com.cms.repository.LabScheduleRepository;
import com.cms.repository.LabSlotRepository;
import com.cms.repository.SemesterRepository;

@ExtendWith(MockitoExtension.class)
class LabScheduleServiceTest {

    @Mock
    private LabScheduleRepository labScheduleRepository;
    @Mock
    private LabRepository labRepository;
    @Mock
    private SubjectRepository subjectRepository;
    @Mock
    private FacultyRepository facultyRepository;
    @Mock
    private LabSlotRepository labSlotRepository;
    @Mock
    private SemesterRepository semesterRepository;

    private LabScheduleService labScheduleService;

    private Lab testLab;
    private Subject testCourse;
    private Faculty testFaculty;
    private LabSlot testLabSlot;
    private Semester testSemester;
    private Department testDepartment;
    private Program testProgram;

    @BeforeEach
    void setUp() {
        labScheduleService = new LabScheduleService(
            labScheduleRepository, labRepository, subjectRepository,
            facultyRepository, labSlotRepository, semesterRepository
        );

        testDepartment = new Department("Computer Science", "CS", "CS Dept", "Dr. Smith");
        testDepartment.setId(1L);

        testProgram = new Program();
        testProgram.setId(1L);
        testProgram.setName("B.Tech CS");

        testLab = new Lab("Lab 1", com.cms.model.enums.LabType.COMPUTER, testDepartment, "Main Building",
            "L001", 30, LabStatus.ACTIVE);
        testLab.setId(1L);

        testCourse = new Subject("Data Structures Lab", "CS201L", 3, 0, 3, null, null, 3);
        testCourse.setId(1L);

        testFaculty = new Faculty("EMP001", "John", "Doe", "john@college.edu", "1234567890",
            testDepartment, Designation.ASSISTANT_PROFESSOR, "Computer Science", "Programming", null, FacultyStatus.ACTIVE);
        testFaculty.setId(1L);

        testLabSlot = new LabSlot("Slot 1", LocalTime.of(9, 0), LocalTime.of(10, 30), 1, true);
        testLabSlot.setId(1L);

        testSemester = new Semester();
        testSemester.setId(1L);
        testSemester.setName("Odd Semester 2024");
    }

    @Test
    void shouldCreateLabSchedule() {
        LabScheduleRequest request = new LabScheduleRequest(
            1L, 1L, 1L, 1L, "Batch-A", DayOfWeek.MONDAY, 1L, true
        );

        LabSchedule savedSchedule = createLabSchedule(1L, testLab, testCourse, testFaculty,
            testLabSlot, "Batch-A", DayOfWeek.MONDAY, testSemester, true);

        when(labRepository.findById(1L)).thenReturn(Optional.of(testLab));
        when(subjectRepository.findById(1L)).thenReturn(Optional.of(testCourse));
        when(facultyRepository.findById(1L)).thenReturn(Optional.of(testFaculty));
        when(labSlotRepository.findById(1L)).thenReturn(Optional.of(testLabSlot));
        when(semesterRepository.findById(1L)).thenReturn(Optional.of(testSemester));
        when(labScheduleRepository.save(any(LabSchedule.class))).thenReturn(savedSchedule);

        LabScheduleResponse response = labScheduleService.create(request);

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.batchName()).isEqualTo("Batch-A");
        assertThat(response.dayOfWeek()).isEqualTo(DayOfWeek.MONDAY);
    }

    @Test
    void shouldThrowExceptionWhenLabNotFound() {
        LabScheduleRequest request = new LabScheduleRequest(
            999L, 1L, 1L, 1L, "Batch-A", DayOfWeek.MONDAY, 1L, true
        );

        when(labRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> labScheduleService.create(request))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessage("Lab not found with id: 999");
    }

    @Test
    void shouldThrowExceptionWhenCourseNotFound() {
        LabScheduleRequest request = new LabScheduleRequest(
            1L, 999L, 1L, 1L, "Batch-A", DayOfWeek.MONDAY, 1L, true
        );

        when(labRepository.findById(1L)).thenReturn(Optional.of(testLab));
        when(subjectRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> labScheduleService.create(request))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessage("Subject not found with id: 999");
    }

    @Test
    void shouldThrowExceptionWhenFacultyNotFound() {
        LabScheduleRequest request = new LabScheduleRequest(
            1L, 1L, 999L, 1L, "Batch-A", DayOfWeek.MONDAY, 1L, true
        );

        when(labRepository.findById(1L)).thenReturn(Optional.of(testLab));
        when(subjectRepository.findById(1L)).thenReturn(Optional.of(testCourse));
        when(facultyRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> labScheduleService.create(request))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessage("Faculty not found with id: 999");
    }

    @Test
    void shouldThrowExceptionWhenLabSlotNotFound() {
        LabScheduleRequest request = new LabScheduleRequest(
            1L, 1L, 1L, 999L, "Batch-A", DayOfWeek.MONDAY, 1L, true
        );

        when(labRepository.findById(1L)).thenReturn(Optional.of(testLab));
        when(subjectRepository.findById(1L)).thenReturn(Optional.of(testCourse));
        when(facultyRepository.findById(1L)).thenReturn(Optional.of(testFaculty));
        when(labSlotRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> labScheduleService.create(request))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessage("Lab slot not found with id: 999");
    }

    @Test
    void shouldThrowExceptionWhenSemesterNotFound() {
        LabScheduleRequest request = new LabScheduleRequest(
            1L, 1L, 1L, 1L, "Batch-A", DayOfWeek.MONDAY, 999L, true
        );

        when(labRepository.findById(1L)).thenReturn(Optional.of(testLab));
        when(subjectRepository.findById(1L)).thenReturn(Optional.of(testCourse));
        when(facultyRepository.findById(1L)).thenReturn(Optional.of(testFaculty));
        when(labSlotRepository.findById(1L)).thenReturn(Optional.of(testLabSlot));
        when(semesterRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> labScheduleService.create(request))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessage("Semester not found with id: 999");
    }

    @Test
    void shouldFindAllLabSchedules() {
        LabSchedule schedule = createLabSchedule(1L, testLab, testCourse, testFaculty,
            testLabSlot, "Batch-A", DayOfWeek.MONDAY, testSemester, true);

        when(labScheduleRepository.findAll()).thenReturn(List.of(schedule));

        List<LabScheduleResponse> responses = labScheduleService.findAll();

        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).batchName()).isEqualTo("Batch-A");
    }

    @Test
    void shouldFindLabScheduleById() {
        LabSchedule schedule = createLabSchedule(1L, testLab, testCourse, testFaculty,
            testLabSlot, "Batch-A", DayOfWeek.MONDAY, testSemester, true);

        when(labScheduleRepository.findById(1L)).thenReturn(Optional.of(schedule));

        LabScheduleResponse response = labScheduleService.findById(1L);

        assertThat(response.id()).isEqualTo(1L);
    }

    @Test
    void shouldThrowExceptionWhenLabScheduleNotFoundById() {
        when(labScheduleRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> labScheduleService.findById(999L))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessage("Lab schedule not found with id: 999");
    }

    @Test
    void shouldFindByLabId() {
        LabSchedule schedule = createLabSchedule(1L, testLab, testCourse, testFaculty,
            testLabSlot, "Batch-A", DayOfWeek.MONDAY, testSemester, true);

        when(labRepository.existsById(1L)).thenReturn(true);
        when(labScheduleRepository.findByLabId(1L)).thenReturn(List.of(schedule));

        List<LabScheduleResponse> responses = labScheduleService.findByLabId(1L);

        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).labId()).isEqualTo(1L);
    }

    @Test
    void shouldThrowExceptionWhenFindByLabIdWithNonExistentLab() {
        when(labRepository.existsById(999L)).thenReturn(false);

        assertThatThrownBy(() -> labScheduleService.findByLabId(999L))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessage("Lab not found with id: 999");
    }

    @Test
    void shouldFindByFacultyId() {
        LabSchedule schedule = createLabSchedule(1L, testLab, testCourse, testFaculty,
            testLabSlot, "Batch-A", DayOfWeek.MONDAY, testSemester, true);

        when(facultyRepository.existsById(1L)).thenReturn(true);
        when(labScheduleRepository.findByFacultyId(1L)).thenReturn(List.of(schedule));

        List<LabScheduleResponse> responses = labScheduleService.findByFacultyId(1L);

        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).facultyId()).isEqualTo(1L);
    }

    @Test
    void shouldThrowExceptionWhenFindByFacultyIdWithNonExistentFaculty() {
        when(facultyRepository.existsById(999L)).thenReturn(false);

        assertThatThrownBy(() -> labScheduleService.findByFacultyId(999L))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessage("Faculty not found with id: 999");
    }

    @Test
    void shouldFindByBatchName() {
        LabSchedule schedule = createLabSchedule(1L, testLab, testCourse, testFaculty,
            testLabSlot, "Batch-A", DayOfWeek.MONDAY, testSemester, true);

        when(labScheduleRepository.findByBatchName("Batch-A")).thenReturn(List.of(schedule));

        List<LabScheduleResponse> responses = labScheduleService.findByBatchName("Batch-A");

        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).batchName()).isEqualTo("Batch-A");
    }

    @Test
    void shouldFindByDayOfWeek() {
        LabSchedule schedule = createLabSchedule(1L, testLab, testCourse, testFaculty,
            testLabSlot, "Batch-A", DayOfWeek.MONDAY, testSemester, true);

        when(labScheduleRepository.findByDayOfWeek(DayOfWeek.MONDAY)).thenReturn(List.of(schedule));

        List<LabScheduleResponse> responses = labScheduleService.findByDayOfWeek(DayOfWeek.MONDAY);

        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).dayOfWeek()).isEqualTo(DayOfWeek.MONDAY);
    }

    @Test
    void shouldCheckConflictsAndFindNone() {
        LabScheduleRequest request = new LabScheduleRequest(
            1L, 1L, 1L, 1L, "Batch-A", DayOfWeek.MONDAY, 1L, true
        );

        when(labScheduleRepository.findConflictingLabSchedules(1L, DayOfWeek.MONDAY, 1L))
            .thenReturn(Collections.emptyList());
        when(labScheduleRepository.findConflictingFacultySchedules(1L, DayOfWeek.MONDAY, 1L))
            .thenReturn(Collections.emptyList());
        when(labScheduleRepository.findConflictingBatchSchedules("Batch-A", DayOfWeek.MONDAY, 1L))
            .thenReturn(Collections.emptyList());

        ScheduleConflictResponse response = labScheduleService.checkConflicts(request);

        assertThat(response.hasConflict()).isFalse();
        assertThat(response.labConflicts()).isEmpty();
        assertThat(response.facultyConflicts()).isEmpty();
        assertThat(response.batchConflicts()).isEmpty();
    }

    @Test
    void shouldCheckConflictsAndFindLabConflict() {
        LabScheduleRequest request = new LabScheduleRequest(
            1L, 1L, 1L, 1L, "Batch-A", DayOfWeek.MONDAY, 1L, true
        );

        LabSchedule conflictingSchedule = createLabSchedule(2L, testLab, testCourse, testFaculty,
            testLabSlot, "Batch-B", DayOfWeek.MONDAY, testSemester, true);

        when(labScheduleRepository.findConflictingLabSchedules(1L, DayOfWeek.MONDAY, 1L))
            .thenReturn(List.of(conflictingSchedule));
        when(labScheduleRepository.findConflictingFacultySchedules(1L, DayOfWeek.MONDAY, 1L))
            .thenReturn(Collections.emptyList());
        when(labScheduleRepository.findConflictingBatchSchedules("Batch-A", DayOfWeek.MONDAY, 1L))
            .thenReturn(Collections.emptyList());

        ScheduleConflictResponse response = labScheduleService.checkConflicts(request);

        assertThat(response.hasConflict()).isTrue();
        assertThat(response.labConflicts()).hasSize(1);
    }

    @Test
    void shouldCheckConflictsAndFindFacultyConflict() {
        LabScheduleRequest request = new LabScheduleRequest(
            1L, 1L, 1L, 1L, "Batch-A", DayOfWeek.MONDAY, 1L, true
        );

        LabSchedule conflictingSchedule = createLabSchedule(2L, testLab, testCourse, testFaculty,
            testLabSlot, "Batch-B", DayOfWeek.MONDAY, testSemester, true);

        when(labScheduleRepository.findConflictingLabSchedules(1L, DayOfWeek.MONDAY, 1L))
            .thenReturn(Collections.emptyList());
        when(labScheduleRepository.findConflictingFacultySchedules(1L, DayOfWeek.MONDAY, 1L))
            .thenReturn(List.of(conflictingSchedule));
        when(labScheduleRepository.findConflictingBatchSchedules("Batch-A", DayOfWeek.MONDAY, 1L))
            .thenReturn(Collections.emptyList());

        ScheduleConflictResponse response = labScheduleService.checkConflicts(request);

        assertThat(response.hasConflict()).isTrue();
        assertThat(response.facultyConflicts()).hasSize(1);
    }

    @Test
    void shouldCheckConflictsAndFindBatchConflict() {
        LabScheduleRequest request = new LabScheduleRequest(
            1L, 1L, 1L, 1L, "Batch-A", DayOfWeek.MONDAY, 1L, true
        );

        LabSchedule conflictingSchedule = createLabSchedule(2L, testLab, testCourse, testFaculty,
            testLabSlot, "Batch-A", DayOfWeek.MONDAY, testSemester, true);

        when(labScheduleRepository.findConflictingLabSchedules(1L, DayOfWeek.MONDAY, 1L))
            .thenReturn(Collections.emptyList());
        when(labScheduleRepository.findConflictingFacultySchedules(1L, DayOfWeek.MONDAY, 1L))
            .thenReturn(Collections.emptyList());
        when(labScheduleRepository.findConflictingBatchSchedules("Batch-A", DayOfWeek.MONDAY, 1L))
            .thenReturn(List.of(conflictingSchedule));

        ScheduleConflictResponse response = labScheduleService.checkConflicts(request);

        assertThat(response.hasConflict()).isTrue();
        assertThat(response.batchConflicts()).hasSize(1);
    }

    @Test
    void shouldUpdateLabSchedule() {
        LabSchedule existingSchedule = createLabSchedule(1L, testLab, testCourse, testFaculty,
            testLabSlot, "Batch-A", DayOfWeek.MONDAY, testSemester, true);

        LabScheduleRequest updateRequest = new LabScheduleRequest(
            1L, 1L, 1L, 1L, "Batch-B", DayOfWeek.TUESDAY, 1L, true
        );

        LabSchedule updatedSchedule = createLabSchedule(1L, testLab, testCourse, testFaculty,
            testLabSlot, "Batch-B", DayOfWeek.TUESDAY, testSemester, true);

        when(labScheduleRepository.findById(1L)).thenReturn(Optional.of(existingSchedule));
        when(labRepository.findById(1L)).thenReturn(Optional.of(testLab));
        when(subjectRepository.findById(1L)).thenReturn(Optional.of(testCourse));
        when(facultyRepository.findById(1L)).thenReturn(Optional.of(testFaculty));
        when(labSlotRepository.findById(1L)).thenReturn(Optional.of(testLabSlot));
        when(semesterRepository.findById(1L)).thenReturn(Optional.of(testSemester));
        when(labScheduleRepository.save(any(LabSchedule.class))).thenReturn(updatedSchedule);

        LabScheduleResponse response = labScheduleService.update(1L, updateRequest);

        assertThat(response.batchName()).isEqualTo("Batch-B");
        assertThat(response.dayOfWeek()).isEqualTo(DayOfWeek.TUESDAY);
    }

    @Test
    void shouldDeleteLabSchedule() {
        when(labScheduleRepository.existsById(1L)).thenReturn(true);

        labScheduleService.delete(1L);

        verify(labScheduleRepository).deleteById(1L);
    }

    @Test
    void shouldThrowExceptionWhenDeletingNonExistentLabSchedule() {
        when(labScheduleRepository.existsById(999L)).thenReturn(false);

        assertThatThrownBy(() -> labScheduleService.delete(999L))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessage("Lab schedule not found with id: 999");

        verify(labScheduleRepository, never()).deleteById(any());
    }

    private LabSchedule createLabSchedule(Long id, Lab lab, Subject course, Faculty faculty,
                                           LabSlot labSlot, String batchName, DayOfWeek dayOfWeek,
                                           Semester semester, Boolean isActive) {
        LabSchedule schedule = new LabSchedule(lab, course, faculty, labSlot, batchName, dayOfWeek, semester, isActive);
        schedule.setId(id);
        Instant now = Instant.now();
        schedule.setCreatedAt(now);
        schedule.setUpdatedAt(now);
        return schedule;
    }
}
