package com.cms.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
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
    @Mock private TimetableStaffingService timetableStaffingService;
    @Mock private AuditLogService auditLogService;

    private SpecialClassRequestService service;

    private TermInstance term;
    private Period period;
    private Faculty faculty;
    private Faculty otherFaculty;
    private Classroom hall;

    @BeforeEach
    void setUp() {
        service = new SpecialClassRequestService(sessionOccurrenceRepository, classScheduleRepository, subjectRepository,
            courseOfferingRepository, cohortSectionRepository, periodRepository, classroomRepository, labRepository,
            clinicalVenueRepository, facultyRepository, courseRegistrationRepository, timetableStaffingService, auditLogService);

        term = new TermInstance();
        term.setId(1L);

        period = new Period();
        period.setId(10L);
        period.setStartTime(LocalTime.of(9, 0));
        period.setEndTime(LocalTime.of(10, 0));
        when(periodRepository.findById(10L)).thenReturn(Optional.of(period));

        faculty = new Faculty();
        faculty.setId(100L);
        faculty.setFirstName("Req");
        faculty.setLastName("Faculty");
        when(facultyRepository.findById(100L)).thenReturn(Optional.of(faculty));

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
        return new SpecialClassRequest(LocalDate.of(2026, 9, 7), 10L, subjectId, courseOfferingId, null,
            ClassSessionType.THEORY, classroomId, null, null, 100L, "Training session");
    }

    private SessionOccurrence liveOtherOccurrence(CourseOffering otherOffering, Classroom classroom) {
        SessionOccurrence other = SessionOccurrence.forSpecialClass(OccurrenceSource.SPECIAL_CLASS,
            LocalDate.of(2026, 9, 7), subject(999L, "Other Subject"), otherOffering, null, period,
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
        when(sessionOccurrenceRepository.save(any())).thenAnswer(inv -> {
            SessionOccurrence toSave = inv.getArgument(0);
            toSave.setId(500L);
            return toSave;
        });

        // Cohort B (60) requesting the same 120-capacity hall at the same period -- 60+60=120 fits.
        assertThat(service.requestSingleSubject(requestFor(1L, 201L, 1L), 100L, "faculty")).isNotNull();
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
}
