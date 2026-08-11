package com.cms.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalTime;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.cms.dto.AutoStaffResult;
import com.cms.dto.StaffingAssignmentRequest;
import com.cms.dto.UnstaffedCellResponse;
import com.cms.exception.TimetableConstraintViolationException;
import com.cms.model.ClassSchedule;
import com.cms.model.Faculty;
import com.cms.model.enums.ClassSessionType;
import com.cms.model.enums.DayOfWeek;
import com.cms.model.enums.FacultyStatus;
import com.cms.repository.ClassScheduleRepository;
import com.cms.repository.FacultyRepository;

@ExtendWith(MockitoExtension.class)
class TimetableStaffingAutoAssignServiceTest {

    @Mock private TimetableStaffingService timetableStaffingService;
    @Mock private FacultyRepository facultyRepository;
    @Mock private ClassScheduleRepository classScheduleRepository;

    private TimetableStaffingAutoAssignService service;

    @BeforeEach
    void setUp() {
        service = new TimetableStaffingAutoAssignService(timetableStaffingService, facultyRepository, classScheduleRepository);
    }

    private UnstaffedCellResponse cell(Long id, Long offeringId, String subjectName, Long specialityId, Long venueId, boolean elective) {
        return new UnstaffedCellResponse(id, offeringId, subjectName, subjectName.substring(0, 4).toUpperCase(),
            specialityId, "Nursing", ClassSessionType.THEORY, DayOfWeek.MONDAY, 1L, "1st Period",
            LocalTime.of(9, 0), LocalTime.of(9, 50), null, 40, venueId, "Room 101", 60, elective, List.of());
    }

    private Faculty faculty(Long id) {
        Faculty f = new Faculty();
        f.setId(id);
        return f;
    }

    @Test
    void shouldStaffASimpleCell() {
        when(timetableStaffingService.getUnstaffedCells(10L))
            .thenReturn(List.of(cell(1L, 100L, "Anatomy", 5L, 9L, false)));
        when(facultyRepository.findBySpecialityIdAndStatus(5L, FacultyStatus.ACTIVE))
            .thenReturn(List.of(faculty(50L)));
        when(classScheduleRepository.findByCourseOfferingId(100L)).thenReturn(Collections.emptyList());

        AutoStaffResult result = service.autoStaff(10L);

        assertThat(result.staffedCount()).isEqualTo(1);
        assertThat(result.unplaced()).isEmpty();
        verify(timetableStaffingService).staffCell(1L, new StaffingAssignmentRequest(50L, null));
    }

    @Test
    void shouldSkipElectiveCells() {
        when(timetableStaffingService.getUnstaffedCells(10L))
            .thenReturn(List.of(cell(1L, 100L, "Elective Subject", 5L, null, true)));

        AutoStaffResult result = service.autoStaff(10L);

        assertThat(result.staffedCount()).isZero();
        assertThat(result.unplaced()).isEmpty();
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
        when(classScheduleRepository.findByCourseOfferingId(100L)).thenReturn(List.of(existingByFifty));

        service.autoStaff(10L);

        verify(timetableStaffingService).staffCell(1L, new StaffingAssignmentRequest(50L, null));
    }

    @Test
    void shouldTryTheNextCandidateWhenTheFirstIsRejected() {
        when(timetableStaffingService.getUnstaffedCells(10L))
            .thenReturn(List.of(cell(1L, 100L, "Anatomy", 5L, 9L, false)));
        when(facultyRepository.findBySpecialityIdAndStatus(5L, FacultyStatus.ACTIVE))
            .thenReturn(List.of(faculty(50L), faculty(51L)));
        when(classScheduleRepository.findByCourseOfferingId(100L)).thenReturn(Collections.emptyList());
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
        when(classScheduleRepository.findByCourseOfferingId(100L)).thenReturn(Collections.emptyList());
        when(timetableStaffingService.staffCell(1L, new StaffingAssignmentRequest(50L, null)))
            .thenThrow(new TimetableConstraintViolationException(
                List.of(new com.cms.dto.ConstraintViolation("STAFFING_FACULTY_CONFLICT", "busy"))));

        AutoStaffResult result = service.autoStaff(10L);

        assertThat(result.staffedCount()).isZero();
        assertThat(result.unplaced()).hasSize(1);
        assertThat(result.unplaced().get(0).subjectName()).isEqualTo("Anatomy");
    }
}
