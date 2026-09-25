package com.cms.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.cms.dto.ApplyRescheduleRequest;
import com.cms.dto.RescheduleResponse;
import com.cms.dto.VenueCandidate;
import com.cms.exception.TimetableConstraintViolationException;
import com.cms.model.AcademicYear;
import com.cms.model.Classroom;
import com.cms.model.ClassSchedule;
import com.cms.model.DesignationMaster;
import com.cms.model.Faculty;
import com.cms.model.Period;
import com.cms.model.SessionOccurrence;
import com.cms.model.Speciality;
import com.cms.model.Subject;
import com.cms.model.TermInstance;
import com.cms.model.enums.ClassScheduleStatus;
import com.cms.model.enums.ClassSessionType;
import com.cms.model.enums.DayOfWeek;
import com.cms.model.enums.FacultyStatus;
import com.cms.model.enums.OccurrenceStatus;
import com.cms.model.enums.TermInstanceStatus;
import com.cms.model.enums.TermType;
import com.cms.repository.ClassScheduleRepository;
import com.cms.repository.ClassroomRepository;
import com.cms.repository.ClinicalVenueRepository;
import com.cms.repository.LabRepository;
import com.cms.repository.PeriodRepository;
import com.cms.repository.SessionOccurrenceRepository;

@ExtendWith(MockitoExtension.class)
class SessionRescheduleServiceTest {

    @Mock private ClassScheduleRepository classScheduleRepository;
    @Mock private ClassroomRepository classroomRepository;
    @Mock private LabRepository labRepository;
    @Mock private ClinicalVenueRepository clinicalVenueRepository;
    @Mock private PeriodRepository periodRepository;
    @Mock private SessionOccurrenceRepository sessionOccurrenceRepository;
    @Mock private ClassScheduleOccurrenceService occurrenceService;
    @Mock private TimetableStaffingService timetableStaffingService;
    @Mock private AuditLogService auditLogService;

    private SessionRescheduleService service;

    private TermInstance termInstance;
    private Faculty faculty;
    private ClassSchedule schedule;
    private Period sourcePeriod;
    private Period targetPeriod;
    private Classroom targetRoom;

    private final LocalDate sourceDate = LocalDate.of(2024, 8, 5); // Monday
    private final LocalDate targetDate = LocalDate.of(2024, 8, 7); // Wednesday, same week

    @BeforeEach
    void setUp() {
        service = new SessionRescheduleService(classScheduleRepository, classroomRepository, labRepository,
            clinicalVenueRepository, periodRepository, sessionOccurrenceRepository, occurrenceService,
            timetableStaffingService, auditLogService);

        AcademicYear ay = new AcademicYear("2024-2025", LocalDate.of(2024, 6, 1), LocalDate.of(2025, 5, 31), false);
        ay.setId(1L);
        ay.setCreatedAt(Instant.now());
        ay.setUpdatedAt(Instant.now());
        termInstance = new TermInstance(ay, TermType.ODD, LocalDate.of(2024, 6, 1), LocalDate.of(2024, 11, 30), TermInstanceStatus.OPEN);
        termInstance.setId(10L);
        termInstance.setCreatedAt(Instant.now());
        termInstance.setUpdatedAt(Instant.now());

        Speciality speciality = new Speciality("Nursing", "NUR", "Nursing Dept", null, null);
        speciality.setId(1L);
        DesignationMaster designation = new DesignationMaster("Assistant Professor", "ASSISTANT_PROFESSOR", null);
        designation.setId(1L);
        faculty = new Faculty("EMP001", "John", "Doe", "john@college.edu", "1234567890",
            speciality, designation, "Nursing", null, null, FacultyStatus.ACTIVE);
        faculty.setId(1L);

        Subject subject = new Subject("Nursing Foundations", "NF101", 4, 3, 1, speciality, 1);
        subject.setId(1L);

        sourcePeriod = new Period("1st Period", LocalTime.of(9, 0), LocalTime.of(10, 0), 1);
        sourcePeriod.setId(1L);
        targetPeriod = new Period("3rd Period", LocalTime.of(11, 0), LocalTime.of(12, 0), 3);
        targetPeriod.setId(3L);

        schedule = new ClassSchedule();
        schedule.setId(300L);
        schedule.setFaculty(faculty);
        schedule.setSubject(subject);
        schedule.setSessionType(ClassSessionType.THEORY);
        schedule.setDayOfWeek(DayOfWeek.MONDAY);
        schedule.setTermInstance(termInstance);
        schedule.setPeriod(sourcePeriod);
        schedule.setStatus(ClassScheduleStatus.PUBLISHED);

        targetRoom = new Classroom();
        targetRoom.setId(20L);
        targetRoom.setName("Room 101");
        targetRoom.setCapacity(60);

        lenient().when(classScheduleRepository.findById(300L)).thenReturn(Optional.of(schedule));
        lenient().when(periodRepository.findById(3L)).thenReturn(Optional.of(targetPeriod));
    }

    private void stubCleanConflictChecks() {
        lenient().when(occurrenceService.occurrenceDatesFor(schedule, sourceDate, sourceDate)).thenReturn(List.of(sourceDate));
        lenient().when(timetableStaffingService.checkFacultyAvailable(eq(1L), any(), any(), any(), eq(targetDate)))
            .thenReturn(Optional.empty());
        lenient().when(timetableStaffingService.checkFacultyAbsent(eq(1L), eq(targetDate))).thenReturn(Optional.empty());
        lenient().when(timetableStaffingService.checkFacultyFree(eq(1L), eq(10L), eq(300L), any(), any(), any()))
            .thenReturn(Optional.empty());
        lenient().when(timetableStaffingService.checkRoomFree(any(), any(), any(), eq(10L), eq(300L), any(), any(), any()))
            .thenReturn(Optional.empty());
        lenient().when(classScheduleRepository.findOverlapping(any(), eq(10L), any(), any(), eq(ClassScheduleStatus.PUBLISHED), eq(300L)))
            .thenReturn(List.of());
        lenient().when(sessionOccurrenceRepository.findByOccurrenceDate(targetDate)).thenReturn(List.of());
    }

    @Test
    void shouldListCandidateClassroomsFreeOfConflicts() {
        stubCleanConflictChecks();
        when(classroomRepository.findByIsActiveTrueOrderByNameAsc()).thenReturn(List.of(targetRoom));

        List<VenueCandidate> candidates = service.findCandidateVenues(300L, sourceDate, targetDate, 3L);

        assertThat(candidates).hasSize(1);
        assertThat(candidates.get(0).id()).isEqualTo(20L);
    }

    @Test
    void shouldExcludeCandidateRoomBlockedByRecurringConflict() {
        stubCleanConflictChecks();
        when(classroomRepository.findByIsActiveTrueOrderByNameAsc()).thenReturn(List.of(targetRoom));
        when(timetableStaffingService.checkRoomFree(any(), any(), any(), eq(10L), eq(300L), any(), any(), any()))
            .thenReturn(Optional.of(new com.cms.dto.ConstraintViolation("STAFFING_ROOM_CONFLICT", "Room busy")));

        List<VenueCandidate> candidates = service.findCandidateVenues(300L, sourceDate, targetDate, 3L);

        assertThat(candidates).isEmpty();
    }

    @Test
    void shouldRejectSourceDateThatIsNotARealOccurrence() {
        when(occurrenceService.occurrenceDatesFor(schedule, sourceDate, sourceDate)).thenReturn(List.of());

        assertThatThrownBy(() -> service.findCandidateVenues(300L, sourceDate, targetDate, 3L))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("not a real occurrence");
    }

    @Test
    void shouldRejectDraftSourceSession() {
        schedule.setStatus(ClassScheduleStatus.DRAFT);

        assertThatThrownBy(() -> service.findCandidateVenues(300L, sourceDate, targetDate, 3L))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("published");
    }

    @Test
    void shouldRejectSundayAsTargetDate() {
        when(occurrenceService.occurrenceDatesFor(schedule, sourceDate, sourceDate)).thenReturn(List.of(sourceDate));
        when(classroomRepository.findByIsActiveTrueOrderByNameAsc()).thenReturn(List.of(targetRoom));

        LocalDate sunday = LocalDate.of(2024, 8, 11);
        List<VenueCandidate> candidates = service.findCandidateVenues(300L, sourceDate, sunday, 3L);

        assertThat(candidates).isEmpty();
    }

    @Test
    void shouldApplyRescheduleCancellingOriginalAndCreatingTargetOccurrence() {
        stubCleanConflictChecks();
        when(classroomRepository.findById(20L)).thenReturn(Optional.of(targetRoom));
        when(sessionOccurrenceRepository.findByClassScheduleIdAndOccurrenceDate(300L, targetDate)).thenReturn(Optional.empty());
        when(sessionOccurrenceRepository.findByClassScheduleIdAndOccurrenceDate(300L, sourceDate)).thenReturn(Optional.empty());
        when(sessionOccurrenceRepository.save(any(SessionOccurrence.class))).thenAnswer(inv -> inv.getArgument(0));

        ApplyRescheduleRequest request = new ApplyRescheduleRequest(sourceDate, targetDate, 3L, 20L);
        RescheduleResponse response = service.apply(300L, request, "admin");

        assertThat(response.occurrenceStatus()).isEqualTo(OccurrenceStatus.RESCHEDULED);
        assertThat(response.date()).isEqualTo(sourceDate);
        assertThat(response.targetDate()).isEqualTo(targetDate);
        assertThat(response.venueName()).isEqualTo("Room 101");

        org.mockito.ArgumentCaptor<SessionOccurrence> captor = org.mockito.ArgumentCaptor.forClass(SessionOccurrence.class);
        org.mockito.Mockito.verify(sessionOccurrenceRepository, org.mockito.Mockito.times(2)).save(captor.capture());
        List<SessionOccurrence> saved = captor.getAllValues();
        SessionOccurrence originalSaved = saved.stream().filter(o -> o.getOccurrenceDate().equals(sourceDate)).findFirst().orElseThrow();
        SessionOccurrence targetSaved = saved.stream().filter(o -> o.getOccurrenceDate().equals(targetDate)).findFirst().orElseThrow();
        assertThat(originalSaved.getOccurrenceStatus()).isEqualTo(OccurrenceStatus.CANCELLED);
        assertThat(originalSaved.getRemarks()).contains(targetDate.toString());
        assertThat(targetSaved.getOccurrenceStatus()).isEqualTo(OccurrenceStatus.RESCHEDULED);
        assertThat(targetSaved.getPeriod()).isEqualTo(targetPeriod);
        assertThat(targetSaved.getClassroom()).isEqualTo(targetRoom);
    }

    @Test
    void shouldApplyRescheduleToADifferentPeriodOnTheSameDateAsOneRowUpdate() {
        when(occurrenceService.occurrenceDatesFor(schedule, sourceDate, sourceDate)).thenReturn(List.of(sourceDate));
        lenient().when(timetableStaffingService.checkFacultyAvailable(eq(1L), any(), any(), any(), eq(sourceDate)))
            .thenReturn(Optional.empty());
        lenient().when(timetableStaffingService.checkFacultyAbsent(eq(1L), eq(sourceDate))).thenReturn(Optional.empty());
        lenient().when(timetableStaffingService.checkFacultyFree(eq(1L), eq(10L), eq(300L), any(), any(), any()))
            .thenReturn(Optional.empty());
        when(timetableStaffingService.checkRoomFree(any(), any(), any(), eq(10L), eq(300L), any(), any(), any()))
            .thenReturn(Optional.empty());
        lenient().when(classScheduleRepository.findOverlapping(any(), eq(10L), any(), any(), eq(ClassScheduleStatus.PUBLISHED), eq(300L)))
            .thenReturn(List.of());
        when(sessionOccurrenceRepository.findByOccurrenceDate(sourceDate)).thenReturn(List.of());
        when(classroomRepository.findById(20L)).thenReturn(Optional.of(targetRoom));
        when(sessionOccurrenceRepository.findByClassScheduleIdAndOccurrenceDate(300L, sourceDate)).thenReturn(Optional.empty());
        when(sessionOccurrenceRepository.save(any(SessionOccurrence.class))).thenAnswer(inv -> inv.getArgument(0));

        ApplyRescheduleRequest request = new ApplyRescheduleRequest(sourceDate, sourceDate, 3L, 20L);
        RescheduleResponse response = service.apply(300L, request, "admin");

        assertThat(response.occurrenceStatus()).isEqualTo(OccurrenceStatus.RESCHEDULED);
        org.mockito.Mockito.verify(sessionOccurrenceRepository, org.mockito.Mockito.times(1)).save(any(SessionOccurrence.class));
    }

    @Test
    void shouldRejectApplyWhenTargetSlotHasAFacultyConflict() {
        stubCleanConflictChecks();
        SessionOccurrence otherOnTargetDate = new SessionOccurrence();
        ClassSchedule otherSchedule = new ClassSchedule();
        otherSchedule.setId(999L);
        otherSchedule.setFaculty(faculty);
        otherSchedule.setSessionType(ClassSessionType.THEORY);
        otherOnTargetDate.setClassSchedule(otherSchedule);
        otherOnTargetDate.setOccurrenceDate(targetDate);
        otherOnTargetDate.setPeriod(targetPeriod);
        when(sessionOccurrenceRepository.findByOccurrenceDate(targetDate)).thenReturn(List.of(otherOnTargetDate));
        when(classroomRepository.findById(20L)).thenReturn(Optional.of(targetRoom));

        ApplyRescheduleRequest request = new ApplyRescheduleRequest(sourceDate, targetDate, 3L, 20L);

        assertThatThrownBy(() -> service.apply(300L, request, "admin"))
            .isInstanceOf(TimetableConstraintViolationException.class)
            .hasMessageContaining("faculty");
    }

    @Test
    void shouldRevertRescheduleDeletingTargetRowAndRestoringOriginal() {
        SessionOccurrence targetRow = new SessionOccurrence(schedule, targetDate);
        targetRow.setOccurrenceStatus(OccurrenceStatus.RESCHEDULED);
        SessionOccurrence originalRow = new SessionOccurrence(schedule, sourceDate);
        originalRow.setOccurrenceStatus(OccurrenceStatus.CANCELLED);

        when(sessionOccurrenceRepository.findByClassScheduleIdAndOccurrenceDate(300L, targetDate)).thenReturn(Optional.of(targetRow));
        when(sessionOccurrenceRepository.findByClassScheduleIdAndOccurrenceDate(300L, sourceDate)).thenReturn(Optional.of(originalRow));

        RescheduleResponse response = service.revert(300L, sourceDate, targetDate, "admin");

        assertThat(response.occurrenceStatus()).isEqualTo(OccurrenceStatus.HELD);
        org.mockito.Mockito.verify(sessionOccurrenceRepository).delete(targetRow);
        assertThat(originalRow.getOccurrenceStatus()).isEqualTo(OccurrenceStatus.HELD);
    }

    @Test
    void shouldRejectRevertWhenTargetRowIsNotARescheduledOccurrence() {
        SessionOccurrence targetRow = new SessionOccurrence(schedule, targetDate);
        targetRow.setOccurrenceStatus(OccurrenceStatus.SUBSTITUTED);
        when(sessionOccurrenceRepository.findByClassScheduleIdAndOccurrenceDate(300L, targetDate)).thenReturn(Optional.of(targetRow));

        assertThatThrownBy(() -> service.revert(300L, sourceDate, targetDate, "admin"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("not a rescheduled occurrence");
    }
}
