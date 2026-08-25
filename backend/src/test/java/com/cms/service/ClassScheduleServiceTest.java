package com.cms.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
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

import com.cms.dto.ClassScheduleRequest;
import com.cms.dto.ClassScheduleResponse;
import com.cms.dto.ScheduleConflictResponse;
import com.cms.exception.ResourceNotFoundException;
import com.cms.model.AcademicYear;
import com.cms.model.ClassSchedule;
import com.cms.model.DesignationMaster;
import com.cms.model.Faculty;
import com.cms.model.Speciality;
import com.cms.model.Lab;
import com.cms.model.Period;
import com.cms.model.Program;
import com.cms.model.Subject;
import com.cms.model.TermInstance;
import com.cms.model.enums.ClassScheduleStatus;
import com.cms.model.enums.ClassSessionType;
import com.cms.model.enums.DayOfWeek;
import com.cms.model.enums.FacultyStatus;
import com.cms.model.enums.LabStatus;
import com.cms.model.enums.TermInstanceStatus;
import com.cms.model.enums.TermType;
import com.cms.repository.BatchRepository;
import com.cms.repository.ClassScheduleRepository;
import com.cms.repository.ClassroomRepository;
import com.cms.repository.ClinicalVenueRepository;
import com.cms.repository.CourseOfferingRepository;
import com.cms.repository.FacultyRepository;
import com.cms.repository.LabRepository;
import com.cms.repository.PeriodRepository;
import com.cms.repository.SubjectRepository;
import com.cms.repository.TermInstanceRepository;

@ExtendWith(MockitoExtension.class)
class ClassScheduleServiceTest {

    @Mock private ClassScheduleRepository classScheduleRepository;
    @Mock private LabRepository labRepository;
    @Mock private SubjectRepository subjectRepository;
    @Mock private FacultyRepository facultyRepository;
    @Mock private TermInstanceRepository termInstanceRepository;
    @Mock private BatchRepository batchRepository;
    @Mock private ClassroomRepository classroomRepository;
    @Mock private PeriodRepository periodRepository;
    @Mock private ClinicalVenueRepository clinicalVenueRepository;
    @Mock private CourseOfferingRepository courseOfferingRepository;

    private ClassScheduleService classScheduleService;

    private Lab testLab;
    private Subject testCourse;
    private Faculty testFaculty;
    private Period testPeriod;
    private TermInstance testTermInstance;
    private Speciality testSpeciality;
    private Program testProgram;

    @BeforeEach
    void setUp() {
        classScheduleService = new ClassScheduleService(
            classScheduleRepository, labRepository, subjectRepository,
            facultyRepository, termInstanceRepository, batchRepository,
            classroomRepository, periodRepository, clinicalVenueRepository, courseOfferingRepository
        );

        testSpeciality = new Speciality("Computer Science", "CS", "CS Dept", null, null);
        testSpeciality.setId(1L);

        testProgram = new Program();
        testProgram.setId(1L);
        testProgram.setName("B.Tech CS");

        testLab = new Lab("Lab 1", com.cms.model.enums.LabType.COMPUTER, testSpeciality,
            "Main Building", "L001", 30, LabStatus.ACTIVE);
        testLab.setId(1L);

        testCourse = new Subject("Data Structures Lab", "CS201L", 3, 0, 3, null, 3);
        testCourse.setId(1L);

        DesignationMaster asstProf = new DesignationMaster("Assistant Professor", "ASSISTANT_PROFESSOR", null);
        asstProf.setId(2L);
        testFaculty = new Faculty("EMP001", "John", "Doe", "john@college.edu", "1234567890",
            testSpeciality, asstProf, "Computer Science", "Programming", null, FacultyStatus.ACTIVE);
        testFaculty.setId(1L);

        testPeriod = new Period("Slot 1", LocalTime.of(9, 0), LocalTime.of(10, 30), 1);
        testPeriod.setId(1L);
        testPeriod.setDurationMinutes(90);

        AcademicYear ay = new AcademicYear("2024-2025",
            LocalDate.of(2024, 6, 1), LocalDate.of(2025, 5, 31), false);
        ay.setId(1L);
        ay.setCreatedAt(Instant.now());
        ay.setUpdatedAt(Instant.now());

        testTermInstance = new TermInstance(ay, TermType.ODD,
            LocalDate.of(2024, 6, 1), LocalDate.of(2024, 11, 30), TermInstanceStatus.OPEN);
        testTermInstance.setId(1L);
        testTermInstance.setCreatedAt(Instant.now());
        testTermInstance.setUpdatedAt(Instant.now());
    }

    private ClassScheduleRequest labRequest(Long labId, Long subjectId, Long facultyId, Long periodId,
                                             String batchName, DayOfWeek dayOfWeek, Long termInstanceId) {
        return new ClassScheduleRequest(
            ClassSessionType.LAB, labId, subjectId, facultyId, batchName, null,
            dayOfWeek, termInstanceId, true, null, periodId, null, null
        );
    }

    @Test
    void shouldCreateLabSession() {
        ClassScheduleRequest request = labRequest(1L, 1L, 1L, 1L, "Batch-A", DayOfWeek.MONDAY, 1L);
        ClassSchedule saved = createLabSchedule(1L, testLab, testCourse, testFaculty,
            testPeriod, "Batch-A", DayOfWeek.MONDAY, testTermInstance, true);

        when(labRepository.findById(1L)).thenReturn(Optional.of(testLab));
        when(subjectRepository.findById(1L)).thenReturn(Optional.of(testCourse));
        when(facultyRepository.findById(1L)).thenReturn(Optional.of(testFaculty));
        when(periodRepository.findById(1L)).thenReturn(Optional.of(testPeriod));
        when(termInstanceRepository.findById(1L)).thenReturn(Optional.of(testTermInstance));
        when(classScheduleRepository.save(any(ClassSchedule.class))).thenReturn(saved);

        ClassScheduleResponse response = classScheduleService.create(request);

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.sessionType()).isEqualTo(ClassSessionType.LAB);
        assertThat(response.batchName()).isEqualTo("Batch-A");
        assertThat(response.dayOfWeek()).isEqualTo(DayOfWeek.MONDAY);
        assertThat(response.termInstanceId()).isEqualTo(1L);
        assertThat(response.termInstanceLabel()).isEqualTo("2024-2025 ODD");
    }

    @Test
    void shouldThrowExceptionWhenLabNotFound() {
        ClassScheduleRequest request = labRequest(999L, 1L, 1L, 1L, "Batch-A", DayOfWeek.MONDAY, 1L);
        when(subjectRepository.findById(1L)).thenReturn(Optional.of(testCourse));
        when(facultyRepository.findById(1L)).thenReturn(Optional.of(testFaculty));
        when(termInstanceRepository.findById(1L)).thenReturn(Optional.of(testTermInstance));
        when(periodRepository.findById(1L)).thenReturn(Optional.of(testPeriod));
        when(labRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> classScheduleService.create(request))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessage("Lab not found with id: 999");
    }

    @Test
    void shouldThrowExceptionWhenCourseNotFound() {
        ClassScheduleRequest request = labRequest(1L, 999L, 1L, 1L, "Batch-A", DayOfWeek.MONDAY, 1L);
        when(subjectRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> classScheduleService.create(request))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessage("Subject not found with id: 999");
    }

    @Test
    void shouldThrowExceptionWhenFacultyNotFound() {
        ClassScheduleRequest request = labRequest(1L, 1L, 999L, 1L, "Batch-A", DayOfWeek.MONDAY, 1L);
        when(subjectRepository.findById(1L)).thenReturn(Optional.of(testCourse));
        when(facultyRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> classScheduleService.create(request))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessage("Faculty not found with id: 999");
    }

    @Test
    void shouldThrowExceptionWhenPeriodNotFound() {
        ClassScheduleRequest request = labRequest(1L, 1L, 1L, 999L, "Batch-A", DayOfWeek.MONDAY, 1L);
        when(subjectRepository.findById(1L)).thenReturn(Optional.of(testCourse));
        when(facultyRepository.findById(1L)).thenReturn(Optional.of(testFaculty));
        when(termInstanceRepository.findById(1L)).thenReturn(Optional.of(testTermInstance));
        when(periodRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> classScheduleService.create(request))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessage("Period not found with id: 999");
    }

    @Test
    void shouldThrowExceptionWhenTermInstanceNotFound() {
        ClassScheduleRequest request = labRequest(1L, 1L, 1L, 1L, "Batch-A", DayOfWeek.MONDAY, 999L);
        when(subjectRepository.findById(1L)).thenReturn(Optional.of(testCourse));
        when(facultyRepository.findById(1L)).thenReturn(Optional.of(testFaculty));
        when(termInstanceRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> classScheduleService.create(request))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessage("Term instance not found with id: 999");
    }

    @Test
    void shouldFindAllClassSchedules() {
        ClassSchedule schedule = createLabSchedule(1L, testLab, testCourse, testFaculty,
            testPeriod, "Batch-A", DayOfWeek.MONDAY, testTermInstance, true);
        when(classScheduleRepository.findAll()).thenReturn(List.of(schedule));

        List<ClassScheduleResponse> responses = classScheduleService.findAll();

        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).batchName()).isEqualTo("Batch-A");
    }

    @Test
    void shouldFindClassScheduleById() {
        ClassSchedule schedule = createLabSchedule(1L, testLab, testCourse, testFaculty,
            testPeriod, "Batch-A", DayOfWeek.MONDAY, testTermInstance, true);
        when(classScheduleRepository.findById(1L)).thenReturn(Optional.of(schedule));

        ClassScheduleResponse response = classScheduleService.findById(1L);

        assertThat(response.id()).isEqualTo(1L);
    }

    @Test
    void shouldThrowExceptionWhenClassScheduleNotFoundById() {
        when(classScheduleRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> classScheduleService.findById(999L))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessage("Class schedule not found with id: 999");
    }

    @Test
    void shouldFindByLabId() {
        ClassSchedule schedule = createLabSchedule(1L, testLab, testCourse, testFaculty,
            testPeriod, "Batch-A", DayOfWeek.MONDAY, testTermInstance, true);
        when(labRepository.existsById(1L)).thenReturn(true);
        when(classScheduleRepository.findByLabId(1L)).thenReturn(List.of(schedule));

        List<ClassScheduleResponse> responses = classScheduleService.findByLabId(1L);

        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).labId()).isEqualTo(1L);
    }

    @Test
    void shouldThrowExceptionWhenFindByLabIdWithNonExistentLab() {
        when(labRepository.existsById(999L)).thenReturn(false);

        assertThatThrownBy(() -> classScheduleService.findByLabId(999L))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessage("Lab not found with id: 999");
    }

    @Test
    void shouldFindByFacultyId() {
        ClassSchedule schedule = createLabSchedule(1L, testLab, testCourse, testFaculty,
            testPeriod, "Batch-A", DayOfWeek.MONDAY, testTermInstance, true);
        when(facultyRepository.existsById(1L)).thenReturn(true);
        when(classScheduleRepository.findByFacultyId(1L)).thenReturn(List.of(schedule));

        List<ClassScheduleResponse> responses = classScheduleService.findByFacultyId(1L);

        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).facultyId()).isEqualTo(1L);
    }

    @Test
    void shouldFindByBatchName() {
        ClassSchedule schedule = createLabSchedule(1L, testLab, testCourse, testFaculty,
            testPeriod, "Batch-A", DayOfWeek.MONDAY, testTermInstance, true);
        when(classScheduleRepository.findByBatchName("Batch-A")).thenReturn(List.of(schedule));

        List<ClassScheduleResponse> responses = classScheduleService.findByBatchName("Batch-A");

        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).batchName()).isEqualTo("Batch-A");
    }

    @Test
    void shouldFindByDayOfWeek() {
        ClassSchedule schedule = createLabSchedule(1L, testLab, testCourse, testFaculty,
            testPeriod, "Batch-A", DayOfWeek.MONDAY, testTermInstance, true);
        when(classScheduleRepository.findByDayOfWeek(DayOfWeek.MONDAY)).thenReturn(List.of(schedule));

        List<ClassScheduleResponse> responses = classScheduleService.findByDayOfWeek(DayOfWeek.MONDAY);

        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).dayOfWeek()).isEqualTo(DayOfWeek.MONDAY);
    }

    @Test
    void shouldCheckConflictsAndFindNone() {
        ClassScheduleRequest request = labRequest(1L, 1L, 1L, 1L, "Batch-A", DayOfWeek.MONDAY, 1L);
        when(periodRepository.findById(1L)).thenReturn(Optional.of(testPeriod));
        when(classScheduleRepository.findOverlapping(any(), anyLong(), any(), any(), any(), any()))
            .thenReturn(Collections.emptyList());

        ScheduleConflictResponse response = classScheduleService.checkConflicts(request);

        assertThat(response.hasConflict()).isFalse();
        assertThat(response.roomConflicts()).isEmpty();
        assertThat(response.facultyConflicts()).isEmpty();
        assertThat(response.audienceConflicts()).isEmpty();
    }

    @Test
    void shouldCheckConflictsAndFindRoomConflict() {
        ClassScheduleRequest request = labRequest(1L, 1L, 1L, 1L, "Batch-A", DayOfWeek.MONDAY, 1L);
        ClassSchedule conflict = createLabSchedule(2L, testLab, testCourse, testFaculty,
            testPeriod, "Batch-B", DayOfWeek.MONDAY, testTermInstance, true);
        Faculty otherFaculty = new Faculty("EMP002", "Jane", "Roe", "jane@college.edu", "9876543210",
            testSpeciality, testFaculty.getDesignation(), "Computer Science", "Programming", null, FacultyStatus.ACTIVE);
        otherFaculty.setId(2L);
        conflict.setFaculty(otherFaculty);

        when(periodRepository.findById(1L)).thenReturn(Optional.of(testPeriod));
        when(classScheduleRepository.findOverlapping(any(), anyLong(), any(), any(), any(), any()))
            .thenReturn(List.of(conflict));

        ScheduleConflictResponse response = classScheduleService.checkConflicts(request);

        assertThat(response.hasConflict()).isTrue();
        assertThat(response.roomConflicts()).hasSize(1);
    }

    @Test
    void shouldRejectCreateWhenRoomConflictExists() {
        ClassScheduleRequest request = labRequest(1L, 1L, 1L, 1L, "Batch-A", DayOfWeek.MONDAY, 1L);
        ClassSchedule conflict = createLabSchedule(2L, testLab, testCourse, testFaculty,
            testPeriod, "Batch-B", DayOfWeek.MONDAY, testTermInstance, true);

        when(labRepository.findById(1L)).thenReturn(Optional.of(testLab));
        when(subjectRepository.findById(1L)).thenReturn(Optional.of(testCourse));
        when(facultyRepository.findById(1L)).thenReturn(Optional.of(testFaculty));
        when(periodRepository.findById(1L)).thenReturn(Optional.of(testPeriod));
        when(termInstanceRepository.findById(1L)).thenReturn(Optional.of(testTermInstance));
        when(classScheduleRepository.findOverlapping(any(), anyLong(), any(), any(), any(), any()))
            .thenReturn(List.of(conflict));

        assertThatThrownBy(() -> classScheduleService.create(request))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("Room is already scheduled");

        verify(classScheduleRepository, never()).save(any());
    }

    @Test
    void shouldUpdateClassSchedule() {
        ClassSchedule existing = createLabSchedule(1L, testLab, testCourse, testFaculty,
            testPeriod, "Batch-A", DayOfWeek.MONDAY, testTermInstance, true);
        ClassScheduleRequest updateRequest = labRequest(1L, 1L, 1L, 1L, "Batch-B", DayOfWeek.TUESDAY, 1L);
        ClassSchedule updated = createLabSchedule(1L, testLab, testCourse, testFaculty,
            testPeriod, "Batch-B", DayOfWeek.TUESDAY, testTermInstance, true);

        when(classScheduleRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(labRepository.findById(1L)).thenReturn(Optional.of(testLab));
        when(subjectRepository.findById(1L)).thenReturn(Optional.of(testCourse));
        when(facultyRepository.findById(1L)).thenReturn(Optional.of(testFaculty));
        when(periodRepository.findById(1L)).thenReturn(Optional.of(testPeriod));
        when(termInstanceRepository.findById(1L)).thenReturn(Optional.of(testTermInstance));
        when(classScheduleRepository.save(any(ClassSchedule.class))).thenReturn(updated);

        ClassScheduleResponse response = classScheduleService.update(1L, updateRequest);

        assertThat(response.batchName()).isEqualTo("Batch-B");
        assertThat(response.dayOfWeek()).isEqualTo(DayOfWeek.TUESDAY);
    }

    @Test
    void shouldBlockFacultyFromADifferentSpecialityThanTheSubject() {
        Speciality nursingSpeciality = new Speciality("Nursing", "NUR", "Nursing Dept", null, null);
        nursingSpeciality.setId(2L);
        Subject nursingSubject = new Subject("Nursing Foundations", "NF101", 4, 3, 1, nursingSpeciality, 1);
        nursingSubject.setId(2L);

        ClassScheduleRequest request = labRequest(1L, 2L, 1L, 1L, "Batch-A", DayOfWeek.MONDAY, 1L);
        when(subjectRepository.findById(2L)).thenReturn(Optional.of(nursingSubject));
        when(facultyRepository.findById(1L)).thenReturn(Optional.of(testFaculty));

        assertThatThrownBy(() -> classScheduleService.create(request))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("not eligible to teach");

        verify(classScheduleRepository, never()).save(any());
    }

    @Test
    void shouldAllowFacultyExplicitlyOnTheSubjectsEligibleFacultyListDespiteSpecialityMismatch() {
        Speciality nursingSpeciality = new Speciality("Nursing", "NUR", "Nursing Dept", null, null);
        nursingSpeciality.setId(2L);
        Subject nursingSubject = new Subject("Nursing Foundations", "NF101", 4, 3, 1, nursingSpeciality, 1);
        nursingSubject.setId(2L);
        nursingSubject.setEligibleFaculty(new java.util.HashSet<>(java.util.Set.of(testFaculty)));

        ClassScheduleRequest request = labRequest(1L, 2L, 1L, 1L, "Batch-A", DayOfWeek.MONDAY, 1L);
        ClassSchedule saved = createLabSchedule(1L, testLab, nursingSubject, testFaculty,
            testPeriod, "Batch-A", DayOfWeek.MONDAY, testTermInstance, true);

        when(labRepository.findById(1L)).thenReturn(Optional.of(testLab));
        when(subjectRepository.findById(2L)).thenReturn(Optional.of(nursingSubject));
        when(facultyRepository.findById(1L)).thenReturn(Optional.of(testFaculty));
        when(periodRepository.findById(1L)).thenReturn(Optional.of(testPeriod));
        when(termInstanceRepository.findById(1L)).thenReturn(Optional.of(testTermInstance));
        when(classScheduleRepository.save(any(ClassSchedule.class))).thenReturn(saved);

        ClassScheduleResponse response = classScheduleService.create(request);

        assertThat(response.id()).isEqualTo(1L);
    }

    @Test
    void shouldAllowFacultyFromTheSameSpecialityAsTheSubject() {
        Subject csSubject = new Subject("Algorithms", "CS301", 3, 3, 0, testSpeciality, 3);
        csSubject.setId(3L);
        ClassScheduleRequest request = labRequest(1L, 3L, 1L, 1L, "Batch-A", DayOfWeek.MONDAY, 1L);
        ClassSchedule saved = createLabSchedule(1L, testLab, csSubject, testFaculty,
            testPeriod, "Batch-A", DayOfWeek.MONDAY, testTermInstance, true);

        when(labRepository.findById(1L)).thenReturn(Optional.of(testLab));
        when(subjectRepository.findById(3L)).thenReturn(Optional.of(csSubject));
        when(facultyRepository.findById(1L)).thenReturn(Optional.of(testFaculty));
        when(periodRepository.findById(1L)).thenReturn(Optional.of(testPeriod));
        when(termInstanceRepository.findById(1L)).thenReturn(Optional.of(testTermInstance));
        when(classScheduleRepository.save(any(ClassSchedule.class))).thenReturn(saved);

        ClassScheduleResponse response = classScheduleService.create(request);

        assertThat(response.id()).isEqualTo(1L);
    }

    @Test
    void shouldGrandfatherAnUnchangedMismatchedFacultyOnUpdate() {
        // A row saved before this eligibility rule existed (or via direct DB edit) may already
        // carry a mismatched faculty-subject pairing. Resubmitting the SAME faculty on an
        // otherwise-unrelated edit (e.g. changing the day) must not suddenly start failing.
        Speciality nursingSpeciality = new Speciality("Nursing", "NUR", "Nursing Dept", null, null);
        nursingSpeciality.setId(2L);
        Subject nursingSubject = new Subject("Nursing Foundations", "NF101", 4, 3, 1, nursingSpeciality, 1);
        nursingSubject.setId(2L);

        ClassSchedule existing = createLabSchedule(1L, testLab, nursingSubject, testFaculty,
            testPeriod, "Batch-A", DayOfWeek.MONDAY, testTermInstance, true);
        ClassScheduleRequest updateRequest = labRequest(1L, 2L, 1L, 1L, "Batch-A", DayOfWeek.TUESDAY, 1L);

        when(classScheduleRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(labRepository.findById(1L)).thenReturn(Optional.of(testLab));
        when(subjectRepository.findById(2L)).thenReturn(Optional.of(nursingSubject));
        when(facultyRepository.findById(1L)).thenReturn(Optional.of(testFaculty));
        when(periodRepository.findById(1L)).thenReturn(Optional.of(testPeriod));
        when(termInstanceRepository.findById(1L)).thenReturn(Optional.of(testTermInstance));
        when(classScheduleRepository.save(any(ClassSchedule.class))).thenReturn(existing);

        ClassScheduleResponse response = classScheduleService.update(1L, updateRequest);

        assertThat(response.id()).isEqualTo(1L);
    }

    @Test
    void shouldDeleteClassSchedule() {
        when(classScheduleRepository.existsById(1L)).thenReturn(true);

        classScheduleService.delete(1L);

        verify(classScheduleRepository).deleteById(1L);
    }

    @Test
    void shouldThrowExceptionWhenDeletingNonExistentClassSchedule() {
        when(classScheduleRepository.existsById(999L)).thenReturn(false);

        assertThatThrownBy(() -> classScheduleService.delete(999L))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessage("Class schedule not found with id: 999");

        verify(classScheduleRepository, never()).deleteById(any());
    }

    private ClassSchedule createLabSchedule(Long id, Lab lab, Subject course, Faculty faculty,
                                           Period period, String batchName, DayOfWeek dayOfWeek,
                                           TermInstance termInstance, Boolean isActive) {
        ClassSchedule schedule = new ClassSchedule(lab, course, faculty, period, batchName, dayOfWeek, termInstance, isActive);
        schedule.setId(id);
        schedule.setSessionType(ClassSessionType.LAB);
        schedule.setStatus(ClassScheduleStatus.PUBLISHED);
        Instant now = Instant.now();
        schedule.setCreatedAt(now);
        schedule.setUpdatedAt(now);
        return schedule;
    }

    @Test
    void getScheduleWorkloadSumsRealHoursPerDay_countingBothPublishedAndDraft() {
        Period period60 = new Period("Slot 2", LocalTime.of(11, 0), LocalTime.of(12, 0), 2);
        period60.setId(2L);
        period60.setDurationMinutes(60);

        ClassSchedule monday90 = createLabSchedule(10L, testLab, testCourse, testFaculty,
            testPeriod, "Batch-A", DayOfWeek.MONDAY, testTermInstance, true); // 90 min = 1.5h, PUBLISHED

        ClassSchedule monday60Draft = createLabSchedule(11L, testLab, testCourse, testFaculty,
            period60, "Batch-B", DayOfWeek.MONDAY, testTermInstance, true);
        monday60Draft.setStatus(ClassScheduleStatus.DRAFT); // still counts -- same convention as the workload-cap gate

        ClassSchedule wednesday90 = createLabSchedule(12L, testLab, testCourse, testFaculty,
            testPeriod, "Batch-A", DayOfWeek.WEDNESDAY, testTermInstance, true); // 90 min = 1.5h

        when(facultyRepository.existsById(1L)).thenReturn(true);
        when(classScheduleRepository.findByTermInstanceIdAndFacultyIdAndStatusIn(1L, 1L,
            List.of(ClassScheduleStatus.PUBLISHED, ClassScheduleStatus.DRAFT)))
            .thenReturn(List.of(monday90, monday60Draft, wednesday90));

        var result = classScheduleService.getScheduleWorkload(1L, 1L);

        assertThat(result.byDay()).hasSize(6); // every DayOfWeek present, even 0h days
        assertThat(result.byDay()).filteredOn(d -> d.dayOfWeek().equals("MONDAY"))
            .extracting(com.cms.dto.FacultyScheduleWorkload.DayHours::hours).containsExactly(2.5);
        assertThat(result.byDay()).filteredOn(d -> d.dayOfWeek().equals("WEDNESDAY"))
            .extracting(com.cms.dto.FacultyScheduleWorkload.DayHours::hours).containsExactly(1.5);
        assertThat(result.byDay()).filteredOn(d -> d.dayOfWeek().equals("TUESDAY"))
            .extracting(com.cms.dto.FacultyScheduleWorkload.DayHours::hours).containsExactly(0.0);
        assertThat(result.weeklyTotalHours()).isEqualTo(4.0);
    }

    @Test
    void findByFacultyIdAndTermInstanceId_throwsWhenFacultyNotFound() {
        when(facultyRepository.existsById(999L)).thenReturn(false);
        assertThatThrownBy(() -> classScheduleService.findByFacultyIdAndTermInstanceId(999L, 1L))
            .isInstanceOf(ResourceNotFoundException.class);
    }
}
