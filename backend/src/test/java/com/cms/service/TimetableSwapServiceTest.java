package com.cms.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
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

import com.cms.dto.ConstraintViolation;
import com.cms.dto.SwapCandidateResponse;
import com.cms.dto.SwapRequest;
import com.cms.exception.LifecycleConflictException;
import com.cms.exception.TimetableConstraintViolationException;
import com.cms.model.ClassSchedule;
import com.cms.model.Classroom;
import com.cms.model.CourseOffering;
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
import com.cms.repository.ClassScheduleRepository;
import com.cms.repository.PeriodRepository;

@ExtendWith(MockitoExtension.class)
class TimetableSwapServiceTest {

    @Mock private ClassScheduleRepository classScheduleRepository;
    @Mock private PeriodRepository periodRepository;
    @Mock private TimetableBlockedPeriodChecker blockedPeriodChecker;
    @Mock private AuditLogService auditLogService;
    @Mock private TimetableStaffingService timetableStaffingService;

    private TimetableSwapService service;

    private TermInstance termInstance;
    private Faculty faculty;
    private Subject subject;
    private Classroom classroom;
    private CourseOffering offering;
    private Period p1;
    private Period p2;
    private ClassSchedule source;

    @BeforeEach
    void setUp() {
        service = new TimetableSwapService(classScheduleRepository, periodRepository,
            blockedPeriodChecker, auditLogService, timetableStaffingService);

        termInstance = new TermInstance();
        termInstance.setId(10L);
        termInstance.setStartDate(LocalDate.of(2024, 6, 1));
        termInstance.setEndDate(LocalDate.of(2024, 11, 30));

        Speciality speciality = new Speciality("Nursing", "NUR", "Nursing Dept", null, null);
        speciality.setId(1L);
        DesignationMaster designation = new DesignationMaster("Assistant Professor", "ASSISTANT_PROFESSOR", null);
        designation.setId(1L);
        faculty = new Faculty("EMP001", "John", "Doe", "john@college.edu", "1234567890",
            speciality, designation, "Nursing", null, null, FacultyStatus.ACTIVE);
        faculty.setId(1L);

        subject = new Subject("Nursing Foundations", "NF101", 4, 3, 1, speciality, 1);
        subject.setId(1L);

        classroom = new Classroom("Room 101", "Main Block", "101", 60);
        classroom.setId(1L);

        offering = new CourseOffering();
        offering.setId(500L);

        p1 = new Period("1st Period", LocalTime.of(9, 0), LocalTime.of(10, 0), 1);
        p1.setId(1L);
        p2 = new Period("2nd Period", LocalTime.of(10, 0), LocalTime.of(11, 0), 2);
        p2.setId(2L);

        source = new ClassSchedule();
        source.setId(100L);
        source.setSessionType(ClassSessionType.THEORY);
        source.setStatus(ClassScheduleStatus.DRAFT);
        source.setSubject(subject);
        source.setFaculty(faculty);
        source.setDayOfWeek(DayOfWeek.MONDAY);
        source.setTermInstance(termInstance);
        source.setClassroom(classroom);
        source.setPeriod(p1);
        source.setCourseOffering(offering);
    }

    @Test
    void shouldRejectSwapForNonDraftSession() {
        source.setStatus(ClassScheduleStatus.PUBLISHED);
        when(classScheduleRepository.findById(100L)).thenReturn(Optional.of(source));

        assertThatThrownBy(() -> service.findCandidates(10L, 100L))
            .isInstanceOf(LifecycleConflictException.class);
    }

    @Test
    void shouldOfferEmptySlotAsCandidateWhenNothingElseScheduled() {
        when(classScheduleRepository.findById(100L)).thenReturn(Optional.of(source));
        when(periodRepository.findByIsActiveTrueOrderByPeriodOrderAsc()).thenReturn(List.of(p1, p2));
        when(classScheduleRepository.findOverlapping(any(), eq(10L), any(), any(), any(), eq(100L)))
            .thenReturn(Collections.emptyList());

        List<SwapCandidateResponse> candidates = service.findCandidates(10L, 100L);

        assertThat(candidates).isNotEmpty();
        assertThat(candidates).allMatch(c -> !c.occupied());
        assertThat(candidates).noneMatch(c -> c.dayOfWeek() == DayOfWeek.MONDAY && c.periodId().equals(1L));
    }

    @Test
    void shouldExcludeSlotsBlockedByFacultyAvailability() {
        when(classScheduleRepository.findById(100L)).thenReturn(Optional.of(source));
        when(periodRepository.findByIsActiveTrueOrderByPeriodOrderAsc()).thenReturn(List.of(p1, p2));
        when(timetableStaffingService.checkFacultyAvailable(eq(1L), any(), any(), any()))
            .thenReturn(Optional.of(new ConstraintViolation("STAFFING_FACULTY_UNAVAILABLE", "On leave")));

        List<SwapCandidateResponse> candidates = service.findCandidates(10L, 100L);

        assertThat(candidates).isEmpty();
    }

    @Test
    void shouldExcludeSlotsThatWouldExceedAWorkloadCap() {
        when(classScheduleRepository.findById(100L)).thenReturn(Optional.of(source));
        when(periodRepository.findByIsActiveTrueOrderByPeriodOrderAsc()).thenReturn(List.of(p1, p2));
        when(timetableStaffingService.checkWithinWorkloadCaps(eq(faculty), eq(source), any(), any(), any()))
            .thenReturn(List.of(new ConstraintViolation("STAFFING_WORKLOAD_DAILY_CAP_EXCEEDED", "Over the daily cap")));

        List<SwapCandidateResponse> candidates = service.findCandidates(10L, 100L);

        assertThat(candidates).isEmpty();
    }

    @Test
    void shouldMoveSessionToAnEmptySlot() {
        when(classScheduleRepository.findById(100L)).thenReturn(Optional.of(source));
        when(periodRepository.findById(2L)).thenReturn(Optional.of(p2));
        when(classScheduleRepository.findOverlapping(eq(DayOfWeek.TUESDAY), eq(10L), any(), any(),
            any(), eq(100L))).thenReturn(Collections.emptyList());

        service.swap(10L, 100L, new SwapRequest(DayOfWeek.TUESDAY, 2L), "admin");

        assertThat(source.getDayOfWeek()).isEqualTo(DayOfWeek.TUESDAY);
        assertThat(source.getPeriod()).isEqualTo(p2);
        verify(classScheduleRepository).save(source);
    }

    @Test
    void shouldSwapWithOccupyingDraftSessionInSameRoom() {
        ClassSchedule occupant = new ClassSchedule();
        occupant.setId(200L);
        occupant.setSessionType(ClassSessionType.THEORY);
        occupant.setStatus(ClassScheduleStatus.DRAFT);
        occupant.setSubject(subject);
        Faculty otherFaculty = new Faculty();
        otherFaculty.setId(2L);
        occupant.setFaculty(otherFaculty);
        occupant.setDayOfWeek(DayOfWeek.TUESDAY);
        occupant.setTermInstance(termInstance);
        occupant.setClassroom(classroom);
        occupant.setPeriod(p2);
        CourseOffering otherOffering = new CourseOffering();
        otherOffering.setId(600L);
        occupant.setCourseOffering(otherOffering);

        when(classScheduleRepository.findById(100L)).thenReturn(Optional.of(source));
        when(periodRepository.findById(2L)).thenReturn(Optional.of(p2));
        when(classScheduleRepository.findOverlapping(eq(DayOfWeek.TUESDAY), eq(10L), eq(p2.getStartTime()), eq(p2.getEndTime()),
            eq(ClassScheduleStatus.PUBLISHED), eq(100L))).thenReturn(List.of());
        when(classScheduleRepository.findOverlapping(eq(DayOfWeek.TUESDAY), eq(10L), eq(p2.getStartTime()), eq(p2.getEndTime()),
            eq(ClassScheduleStatus.DRAFT), eq(100L))).thenReturn(List.of(occupant));
        when(classScheduleRepository.findOverlapping(eq(DayOfWeek.MONDAY), eq(10L), eq(p1.getStartTime()), eq(p1.getEndTime()),
            eq(ClassScheduleStatus.PUBLISHED), eq(200L))).thenReturn(List.of());
        when(classScheduleRepository.findOverlapping(eq(DayOfWeek.MONDAY), eq(10L), eq(p1.getStartTime()), eq(p1.getEndTime()),
            eq(ClassScheduleStatus.DRAFT), eq(200L))).thenReturn(List.of());

        service.swap(10L, 100L, new SwapRequest(DayOfWeek.TUESDAY, 2L), "admin");

        assertThat(source.getDayOfWeek()).isEqualTo(DayOfWeek.TUESDAY);
        assertThat(source.getPeriod()).isEqualTo(p2);
        assertThat(occupant.getDayOfWeek()).isEqualTo(DayOfWeek.MONDAY);
        assertThat(occupant.getPeriod()).isEqualTo(p1);
        verify(classScheduleRepository).save(source);
        verify(classScheduleRepository).save(occupant);
    }

    @Test
    void shouldRejectSwapOntoASlotOccupiedByAPublishedSession() {
        // Closes a real gap: this service only ever reschedules DRAFT rows, so a PUBLISHED
        // occupant must always be a hard conflict, never a swap-partner candidate.
        ClassSchedule publishedOccupant = new ClassSchedule();
        publishedOccupant.setId(200L);
        publishedOccupant.setSessionType(ClassSessionType.THEORY);
        publishedOccupant.setStatus(ClassScheduleStatus.PUBLISHED);
        Faculty otherFaculty = new Faculty();
        otherFaculty.setId(2L);
        publishedOccupant.setFaculty(otherFaculty);
        publishedOccupant.setClassroom(classroom);

        when(classScheduleRepository.findById(100L)).thenReturn(Optional.of(source));
        when(periodRepository.findById(2L)).thenReturn(Optional.of(p2));
        when(classScheduleRepository.findOverlapping(eq(DayOfWeek.TUESDAY), eq(10L), eq(p2.getStartTime()), eq(p2.getEndTime()),
            eq(ClassScheduleStatus.PUBLISHED), eq(100L))).thenReturn(List.of(publishedOccupant));
        lenient().when(classScheduleRepository.findOverlapping(eq(DayOfWeek.TUESDAY), eq(10L), eq(p2.getStartTime()), eq(p2.getEndTime()),
            eq(ClassScheduleStatus.DRAFT), eq(100L))).thenReturn(Collections.emptyList());

        assertThatThrownBy(() -> service.swap(10L, 100L, new SwapRequest(DayOfWeek.TUESDAY, 2L), "admin"))
            .isInstanceOf(TimetableConstraintViolationException.class)
            .hasMessageContaining("already occupied");
    }

    @Test
    void shouldExcludeARecurringBlockedPeriodFromCandidates() {
        when(classScheduleRepository.findById(100L)).thenReturn(Optional.of(source));
        when(periodRepository.findByIsActiveTrueOrderByPeriodOrderAsc()).thenReturn(List.of(p1, p2));
        when(classScheduleRepository.findOverlapping(any(), eq(10L), any(), any(), any(), eq(100L)))
            .thenReturn(Collections.emptyList());
        // Block every day for p2 specifically -- p1's only candidate day (MONDAY) is already
        // excluded as the session's own current slot, so this isolates the effect to p2's rows.
        // lenient(): findCandidates scans every (day, period) combination, most of which have
        // periodId=1L and would otherwise trip strict-stubbing's "unmatched invocation" guard.
        lenient().when(blockedPeriodChecker.blockReason(any(), eq(2L), eq(termInstance.getStartDate()), eq(termInstance.getEndDate())))
            .thenReturn(Optional.of("Staff meeting"));

        List<SwapCandidateResponse> candidates = service.findCandidates(10L, 100L);

        assertThat(candidates).isNotEmpty();
        assertThat(candidates).noneMatch(c -> c.periodId().equals(2L));
    }

    @Test
    void shouldRejectDirectSwapIntoABlockedPeriod() {
        when(classScheduleRepository.findById(100L)).thenReturn(Optional.of(source));
        when(periodRepository.findById(2L)).thenReturn(Optional.of(p2));
        when(blockedPeriodChecker.blockReason(
            DayOfWeek.TUESDAY, 2L, termInstance.getStartDate(), termInstance.getEndDate()))
            .thenReturn(Optional.of("Staff meeting"));

        assertThatThrownBy(() -> service.swap(10L, 100L, new SwapRequest(DayOfWeek.TUESDAY, 2L), "admin"))
            .isInstanceOf(TimetableConstraintViolationException.class)
            .hasMessageContaining("Staff meeting");
    }
}
