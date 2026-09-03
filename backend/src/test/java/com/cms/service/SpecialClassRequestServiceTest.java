package com.cms.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.cms.dto.SpecialClassRequest;
import com.cms.exception.TimetableConstraintViolationException;
import com.cms.model.Classroom;
import com.cms.model.CourseOffering;
import com.cms.model.Faculty;
import com.cms.model.Period;
import com.cms.model.SessionOccurrence;
import com.cms.model.Subject;
import com.cms.model.TermInstance;
import com.cms.model.enums.ClassSessionType;
import com.cms.model.enums.OccurrenceSource;
import com.cms.model.enums.RegistrationStatus;
import com.cms.repository.CalendarEventRepository;
import com.cms.repository.ClassScheduleRepository;
import com.cms.repository.ClassroomRepository;
import com.cms.repository.ClinicalVenueRepository;
import com.cms.repository.CohortSectionRepository;
import com.cms.repository.CourseOfferingRepository;
import com.cms.repository.CourseRegistrationRepository;
import com.cms.repository.FacultyRepository;
import com.cms.repository.LabRepository;
import com.cms.repository.PeriodRepository;
import com.cms.repository.SessionOccurrenceRepository;
import com.cms.repository.SubjectRepository;

/** Covers the concurrent capacity-pooled sharing rule for shared-flagged classrooms (large
 *  lecture/drawing halls) -- see Classroom.allowsConcurrentSharing and
 *  SpecialClassRequestService.checkConflicts. Every other conflict in this service (faculty
 *  availability/conflict, recurring-template room check, duplicate request) is exercised via the
 *  mocked TimetableStaffingService returning no violations, keeping these tests focused on the new
 *  room-sharing behavior. */
@ExtendWith(MockitoExtension.class)
class SpecialClassRequestServiceTest {

    @Mock private SessionOccurrenceRepository sessionOccurrenceRepository;
    @Mock private ClassScheduleRepository classScheduleRepository;
    @Mock private SubjectRepository subjectRepository;
    @Mock private CourseOfferingRepository courseOfferingRepository;
    @Mock private CohortSectionRepository cohortSectionRepository;
    @Mock private PeriodRepository periodRepository;
    @Mock private ClassroomRepository classroomRepository;
    @Mock private LabRepository labRepository;
    @Mock private ClinicalVenueRepository clinicalVenueRepository;
    @Mock private FacultyRepository facultyRepository;
    @Mock private CourseRegistrationRepository courseRegistrationRepository;
    @Mock private CalendarEventRepository calendarEventRepository;
    @Mock private TimetableStaffingService timetableStaffingService;
    @Mock private AuditLogService auditLogService;

    private SpecialClassRequestService service;

    private TermInstance term;
    private Period period;
    private Faculty faculty;
    private Faculty otherFaculty;
    private Classroom hall;

    /** A Sunday -- the simplest legitimate special-class date, needing no CalendarEventRepository
     *  stubbing (see SpecialClassRequestService#requireNonInstructionDay), so tests unrelated to
     *  the date/holiday rule itself don't need to set that up. */
    private static final LocalDate SUNDAY = LocalDate.of(2026, 9, 6);

    @BeforeEach
    void setUp() {
        service = new SpecialClassRequestService(sessionOccurrenceRepository, classScheduleRepository, subjectRepository,
            courseOfferingRepository, cohortSectionRepository, periodRepository, classroomRepository, labRepository,
            clinicalVenueRepository, facultyRepository, courseRegistrationRepository, calendarEventRepository,
            timetableStaffingService, auditLogService);

        term = new TermInstance();
        term.setId(1L);
        term.setStartDate(LocalDate.of(2026, 1, 1));
        term.setEndDate(LocalDate.of(2026, 12, 31));

        period = new Period();
        period.setId(10L);
        period.setName("Period 1");
        period.setStartTime(LocalTime.of(9, 0));
        period.setEndTime(LocalTime.of(10, 0));
        when(periodRepository.findByIsActiveTrueOrderByPeriodOrderAsc()).thenReturn(List.of(period));

        faculty = new Faculty();
        faculty.setId(100L);
        faculty.setFirstName("Req");
        faculty.setLastName("Faculty");
        lenient().when(facultyRepository.findById(100L)).thenReturn(Optional.of(faculty));

        otherFaculty = new Faculty();
        otherFaculty.setId(101L);
        otherFaculty.setFirstName("Other");
        otherFaculty.setLastName("Faculty");

        hall = new Classroom("Drawing Hall", null, null, 120);
        hall.setId(1L);

        // Faculty/recurring-template checks are out of scope for this test -- always clean.
        lenient().when(timetableStaffingService.checkFacultyAvailable(anyLong(), any(), any(), any(), any())).thenReturn(Optional.empty());
        lenient().when(timetableStaffingService.checkFacultyFree(anyLong(), anyLong(), any(), any(), any(), any())).thenReturn(Optional.empty());
        lenient().when(timetableStaffingService.checkRoomFree(any(), anyLong(), any(), anyLong(), any(), any(), any(), any()))
            .thenReturn(Optional.empty());
    }

    private CourseOffering offering(long id, TermInstance term) {
        CourseOffering offering = new CourseOffering();
        offering.setId(id);
        offering.setTermInstance(term);
        return offering;
    }

    private Subject subject(long id, String name) {
        Subject subject = new Subject();
        subject.setId(id);
        subject.setName(name);
        return subject;
    }

    private SpecialClassRequest requestFor(long subjectId, long courseOfferingId, long classroomId) {
        return new SpecialClassRequest(SUNDAY, List.of(10L), subjectId, courseOfferingId, null,
            ClassSessionType.THEORY, classroomId, null, null, 100L, "Training session");
    }

    private SessionOccurrence liveOtherOccurrence(CourseOffering otherOffering, Classroom classroom) {
        SessionOccurrence other = SessionOccurrence.forSpecialClass(OccurrenceSource.SPECIAL_CLASS,
            SUNDAY, subject(999L, "Other Subject"), otherOffering, null, period,
            ClassSessionType.THEORY, otherFaculty, otherFaculty, "Other booking");
        other.setClassroom(classroom);
        return other;
    }

    @Test
    void sharedRoom_combinedStrengthFits_secondRequestSucceeds() {
        hall.setAllowsConcurrentSharing(true);
        when(classroomRepository.findById(1L)).thenReturn(Optional.of(hall));

        CourseOffering cohortAOffering = offering(200L, term);
        CourseOffering cohortBOffering = offering(201L, term);
        when(courseOfferingRepository.findById(201L)).thenReturn(Optional.of(cohortBOffering));
        when(subjectRepository.findById(1L)).thenReturn(Optional.of(subject(1L, "Anatomy")));

        // Cohort A already booked the hall for 60 -- a live "other" occurrence at the same date+period.
        when(sessionOccurrenceRepository.findByOccurrenceSourceInAndOccurrenceDateAndPeriod_Id(any(), any(), any()))
            .thenReturn(List.of(liveOtherOccurrence(cohortAOffering, hall)));
        when(courseRegistrationRepository.countByCourseOfferingIdAndStatus(200L, RegistrationStatus.REGISTERED)).thenReturn(60L);
        when(courseRegistrationRepository.countByCourseOfferingIdAndStatus(201L, RegistrationStatus.REGISTERED)).thenReturn(60L);
        when(sessionOccurrenceRepository.saveAll(any())).thenAnswer(inv -> {
            List<SessionOccurrence> toSave = inv.getArgument(0);
            toSave.forEach(o -> o.setId(500L));
            return toSave;
        });

        // Cohort B (60) requesting the same 120-capacity hall at the same period -- 60+60=120 fits.
        assertThat(service.requestSingleSubject(requestFor(1L, 201L, 1L), 100L, "faculty")).hasSize(1);
    }

    @Test
    void sharedRoom_combinedStrengthExceedsCapacity_rejectedWithSharedCapacityViolation() {
        hall.setId(1L);
        hall.setCapacity(100);
        hall.setAllowsConcurrentSharing(true);
        when(classroomRepository.findById(1L)).thenReturn(Optional.of(hall));

        CourseOffering cohortAOffering = offering(200L, term);
        CourseOffering cohortBOffering = offering(201L, term);
        when(courseOfferingRepository.findById(201L)).thenReturn(Optional.of(cohortBOffering));
        when(subjectRepository.findById(1L)).thenReturn(Optional.of(subject(1L, "Anatomy")));

        when(sessionOccurrenceRepository.findByOccurrenceSourceInAndOccurrenceDateAndPeriod_Id(any(), any(), any()))
            .thenReturn(List.of(liveOtherOccurrence(cohortAOffering, hall)));
        when(courseRegistrationRepository.countByCourseOfferingIdAndStatus(200L, RegistrationStatus.REGISTERED)).thenReturn(70L);
        when(courseRegistrationRepository.countByCourseOfferingIdAndStatus(201L, RegistrationStatus.REGISTERED)).thenReturn(40L);

        // 70 (already booked) + 40 (this request) = 110 > 100 capacity.
        assertThatThrownBy(() -> service.requestSingleSubject(requestFor(1L, 201L, 1L), 100L, "faculty"))
            .isInstanceOf(TimetableConstraintViolationException.class)
            .satisfies(ex -> assertThat(((TimetableConstraintViolationException) ex).getViolations())
                .anyMatch(v -> v.code().equals("SPECIAL_CLASS_SHARED_CAPACITY_EXCEEDED")));
    }

    @Test
    void nonSharedRoom_secondRequestRejectedWithRoomConflict() {
        // allowsConcurrentSharing defaults to false -- today's exclusive behavior must be unchanged.
        hall.setId(1L);
        when(classroomRepository.findById(1L)).thenReturn(Optional.of(hall));

        CourseOffering cohortAOffering = offering(200L, term);
        CourseOffering cohortBOffering = offering(201L, term);
        when(courseOfferingRepository.findById(201L)).thenReturn(Optional.of(cohortBOffering));
        when(subjectRepository.findById(1L)).thenReturn(Optional.of(subject(1L, "Anatomy")));

        when(sessionOccurrenceRepository.findByOccurrenceSourceInAndOccurrenceDateAndPeriod_Id(any(), any(), any()))
            .thenReturn(List.of(liveOtherOccurrence(cohortAOffering, hall)));

        assertThatThrownBy(() -> service.requestSingleSubject(requestFor(1L, 201L, 1L), 100L, "faculty"))
            .isInstanceOf(TimetableConstraintViolationException.class)
            .satisfies(ex -> assertThat(((TimetableConstraintViolationException) ex).getViolations())
                .anyMatch(v -> v.code().equals("SPECIAL_CLASS_ROOM_CONFLICT")));
    }

    @Test
    void sharedRoomWithNoCapacityOnRecord_fallsBackToHardRoomConflict() {
        // allowsConcurrentSharing=true but capacity unknown -- can't safely pool an unknown number,
        // so this must behave exactly like a non-shared room.
        hall.setId(1L);
        hall.setCapacity(null);
        hall.setAllowsConcurrentSharing(true);
        when(classroomRepository.findById(1L)).thenReturn(Optional.of(hall));

        CourseOffering cohortAOffering = offering(200L, term);
        CourseOffering cohortBOffering = offering(201L, term);
        when(courseOfferingRepository.findById(201L)).thenReturn(Optional.of(cohortBOffering));
        when(subjectRepository.findById(1L)).thenReturn(Optional.of(subject(1L, "Anatomy")));

        when(sessionOccurrenceRepository.findByOccurrenceSourceInAndOccurrenceDateAndPeriod_Id(any(), any(), any()))
            .thenReturn(List.of(liveOtherOccurrence(cohortAOffering, hall)));

        assertThatThrownBy(() -> service.requestSingleSubject(requestFor(1L, 201L, 1L), 100L, "faculty"))
            .isInstanceOf(TimetableConstraintViolationException.class)
            .satisfies(ex -> assertThat(((TimetableConstraintViolationException) ex).getViolations())
                .anyMatch(v -> v.code().equals("SPECIAL_CLASS_ROOM_CONFLICT")));
    }

    // ── Non-instruction-day / future-date / within-term rules ───────────

    private SpecialClassRequest requestOn(LocalDate date, List<Long> periodIds) {
        return new SpecialClassRequest(date, periodIds, 1L, 201L, null,
            ClassSessionType.THEORY, 1L, null, null, 100L, "Training session");
    }

    private void stubOfferingAndSubject() {
        when(courseOfferingRepository.findById(201L)).thenReturn(Optional.of(offering(201L, term)));
        when(subjectRepository.findById(1L)).thenReturn(Optional.of(subject(1L, "Anatomy")));
        lenient().when(classroomRepository.findById(1L)).thenReturn(Optional.of(hall));
        lenient().when(sessionOccurrenceRepository.findByOccurrenceSourceInAndOccurrenceDateAndPeriod_Id(any(), any(), any()))
            .thenReturn(List.of());
    }

    /** {@code saveAll} stub shared by the tests below that expect the request to actually succeed --
     *  assigns a fake id to each row, same as JPA would on a real insert, so the audit-log line's
     *  {@code occurrences.get(0).getId()} has something to call {@code toString()} on. */
    private void stubSaveAllAssignsIds() {
        lenient().when(sessionOccurrenceRepository.saveAll(any())).thenAnswer(inv -> {
            List<SessionOccurrence> toSave = inv.getArgument(0);
            long id = 500L;
            for (SessionOccurrence o : toSave) {
                o.setId(id++);
            }
            return toSave;
        });
    }

    @Test
    void requestSingleSubject_rejectsARegularInstructionDay() {
        // A Monday with no declared holiday -- college has its own full regular timetable that day.
        stubOfferingAndSubject();
        com.cms.model.AcademicYear academicYear = new com.cms.model.AcademicYear();
        academicYear.setId(5L);
        term.setAcademicYear(academicYear);
        when(calendarEventRepository.findOverlapping(eq(5L), eq(com.cms.model.enums.CalendarEventType.HOLIDAY), any(), any()))
            .thenReturn(List.of());
        LocalDate monday = LocalDate.of(2026, 9, 7);

        assertThatThrownBy(() -> service.requestSingleSubject(requestOn(monday, List.of(10L)), 100L, "faculty"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("no regular instruction");
    }

    @Test
    void requestSingleSubject_allowsADeclaredHoliday() {
        stubOfferingAndSubject();
        com.cms.model.AcademicYear academicYear = new com.cms.model.AcademicYear();
        academicYear.setId(5L);
        term.setAcademicYear(academicYear);
        LocalDate holidayMonday = LocalDate.of(2026, 9, 7);
        when(calendarEventRepository.findOverlapping(eq(5L), eq(com.cms.model.enums.CalendarEventType.HOLIDAY),
            eq(holidayMonday), eq(holidayMonday))).thenReturn(List.of(new com.cms.model.CalendarEvent()));
        stubSaveAllAssignsIds();

        assertThat(service.requestSingleSubject(requestOn(holidayMonday, List.of(10L)), 100L, "faculty")).hasSize(1);
    }

    @Test
    void requestSingleSubject_rejectsAPastDate() {
        stubOfferingAndSubject();
        LocalDate yesterday = LocalDate.now().minusDays(1);

        assertThatThrownBy(() -> service.requestSingleSubject(requestOn(yesterday, List.of(10L)), 100L, "faculty"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("today or a future date");
    }

    @Test
    void requestSingleSubject_rejectsADateOutsideTheTerm() {
        stubOfferingAndSubject();
        LocalDate wayInTheFuture = term.getEndDate().plusMonths(1);

        assertThatThrownBy(() -> service.requestSingleSubject(requestOn(wayInTheFuture, List.of(10L)), 100L, "faculty"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("fall within this term");
    }

    // ── Multi-period consecutive-block rule ─────────────────────────────

    @Test
    void requestSingleSubject_allowsAConsecutiveTwoPeriodBlock() {
        stubOfferingAndSubject();
        Period period2 = new Period();
        period2.setId(11L);
        period2.setName("Period 2");
        period2.setStartTime(LocalTime.of(10, 0));
        period2.setEndTime(LocalTime.of(10, 50));
        when(periodRepository.findByIsActiveTrueOrderByPeriodOrderAsc()).thenReturn(List.of(period, period2));
        stubSaveAllAssignsIds();

        assertThat(service.requestSingleSubject(requestOn(SUNDAY, List.of(10L, 11L)), 100L, "faculty")).hasSize(2);
    }

    @Test
    void requestSingleSubject_rejectsNonConsecutivePeriods() {
        stubOfferingAndSubject();
        Period period2 = new Period();
        period2.setId(11L);
        period2.setName("Period 2");
        period2.setStartTime(LocalTime.of(10, 0));
        period2.setEndTime(LocalTime.of(10, 50));
        Period period3 = new Period();
        period3.setId(12L);
        period3.setName("Period 3");
        period3.setStartTime(LocalTime.of(11, 0));
        period3.setEndTime(LocalTime.of(11, 50));
        when(periodRepository.findByIsActiveTrueOrderByPeriodOrderAsc()).thenReturn(List.of(period, period2, period3));

        // Periods 1 and 3 requested, skipping 2 in between -- not a single consecutive block.
        assertThatThrownBy(() -> service.requestSingleSubject(requestOn(SUNDAY, List.of(10L, 12L)), 100L, "faculty"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("consecutive block");
    }

    @Test
    void requestSingleSubject_rejectsAdjacentPeriodsWithAClockTimeGap() {
        stubOfferingAndSubject();
        // List-adjacent to period 1, but a 10-minute recess sits between them in real clock time --
        // must not be silently spanned.
        Period period2 = new Period();
        period2.setId(11L);
        period2.setName("Period 2");
        period2.setStartTime(LocalTime.of(10, 10));
        period2.setEndTime(LocalTime.of(11, 0));
        when(periodRepository.findByIsActiveTrueOrderByPeriodOrderAsc()).thenReturn(List.of(period, period2));

        assertThatThrownBy(() -> service.requestSingleSubject(requestOn(SUNDAY, List.of(10L, 11L)), 100L, "faculty"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("back-to-back in time");
    }
}
