package com.cms.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
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
import com.cms.dto.UnstaffedCellResponse;
import com.cms.exception.LifecycleConflictException;
import com.cms.exception.ResourceNotFoundException;
import com.cms.model.AcademicYear;
import com.cms.model.Classroom;
import com.cms.model.ClassSchedule;
import com.cms.model.DesignationMaster;
import com.cms.model.Faculty;
import com.cms.model.Period;
import com.cms.model.Speciality;
import com.cms.model.Subject;
import com.cms.model.TermInstance;
import com.cms.model.enums.ClassScheduleStatus;
import com.cms.model.enums.ClassSessionType;
import com.cms.model.enums.DayOfWeek;
import com.cms.model.enums.FacultyStatus;
import com.cms.model.enums.TermInstanceStatus;
import com.cms.model.enums.TermType;
import com.cms.repository.ClassScheduleRepository;
import com.cms.repository.ClassroomRepository;
import com.cms.repository.ClinicalVenueRepository;
import com.cms.repository.FacultyRepository;
import com.cms.repository.LabRepository;

@ExtendWith(MockitoExtension.class)
class TimetableStaffingServiceTest {

    @Mock private ClassScheduleRepository classScheduleRepository;
    @Mock private FacultyRepository facultyRepository;
    @Mock private ClassroomRepository classroomRepository;
    @Mock private LabRepository labRepository;
    @Mock private ClinicalVenueRepository clinicalVenueRepository;

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
            classroomRepository, labRepository, clinicalVenueRepository);

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
        StaffingAssignmentRequest request = new StaffingAssignmentRequest(1L, 1L, null, null);
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

        StaffingAssignmentRequest request = new StaffingAssignmentRequest(2L, 1L, null, null);
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

        StaffingAssignmentRequest request = new StaffingAssignmentRequest(1L, 1L, null, null);
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

        StaffingAssignmentRequest request = new StaffingAssignmentRequest(1L, 1L, null, null);
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
        StaffingAssignmentRequest request = new StaffingAssignmentRequest(1L, null, null, null);
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
        StaffingAssignmentRequest request = new StaffingAssignmentRequest(1L, 1L, null, null);
        when(classScheduleRepository.findById(100L)).thenReturn(Optional.of(cell));

        assertThatThrownBy(() -> service.staffCell(100L, request))
            .isInstanceOf(LifecycleConflictException.class);
    }

    @Test
    void shouldThrowWhenCellNotFound() {
        when(classScheduleRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.staffCell(999L, new StaffingAssignmentRequest(1L, 1L, null, null)))
            .isInstanceOf(ResourceNotFoundException.class);
    }
}
