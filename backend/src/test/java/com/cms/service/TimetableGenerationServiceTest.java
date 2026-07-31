package com.cms.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
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

import com.cms.dto.TimetableActionResponse;
import com.cms.dto.TimetableGenerationResponse;
import com.cms.exception.LifecycleConflictException;
import com.cms.exception.ResourceNotFoundException;
import com.cms.model.AcademicYear;
import com.cms.model.Batch;
import com.cms.model.ClassSchedule;
import com.cms.model.Classroom;
import com.cms.model.CourseOffering;
import com.cms.model.CurriculumSemesterCourse;
import com.cms.model.DesignationMaster;
import com.cms.model.Faculty;
import com.cms.model.Period;
import com.cms.model.Speciality;
import com.cms.model.Subject;
import com.cms.model.TermInstance;
import com.cms.model.enums.ClassScheduleStatus;
import com.cms.model.enums.FacultyStatus;
import com.cms.model.enums.TermInstanceStatus;
import com.cms.model.enums.TermType;
import com.cms.repository.BatchRepository;
import com.cms.repository.ClassScheduleRepository;
import com.cms.repository.ClassroomRepository;
import com.cms.repository.CourseOfferingRepository;
import com.cms.repository.FacultyAvailabilityRepository;
import com.cms.repository.FacultyRepository;
import com.cms.repository.LabAttendanceRepository;
import com.cms.repository.LabRepository;
import com.cms.repository.PeriodRepository;
import com.cms.repository.TermInstanceRepository;

@ExtendWith(MockitoExtension.class)
class TimetableGenerationServiceTest {

    @Mock private ClassScheduleRepository classScheduleRepository;
    @Mock private CourseOfferingRepository courseOfferingRepository;
    @Mock private TermInstanceRepository termInstanceRepository;
    @Mock private BatchRepository batchRepository;
    @Mock private FacultyRepository facultyRepository;
    @Mock private ClassroomRepository classroomRepository;
    @Mock private PeriodRepository periodRepository;
    @Mock private LabRepository labRepository;
    @Mock private FacultyAvailabilityRepository facultyAvailabilityRepository;
    @Mock private LabAttendanceRepository labAttendanceRepository;

    private TimetableGenerationService service;

    private TermInstance termInstance;
    private Faculty faculty;
    private Subject subject;
    private Classroom classroom;
    private Period period;

    @BeforeEach
    void setUp() {
        service = new TimetableGenerationService(classScheduleRepository, courseOfferingRepository,
            termInstanceRepository, batchRepository, facultyRepository, classroomRepository,
            periodRepository, labRepository, facultyAvailabilityRepository,
            labAttendanceRepository);

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

        subject = new Subject("Nursing Foundations", "NF101", 4, 3, 1, speciality, 1);
        subject.setId(1L);

        classroom = new Classroom("Room 101", "Main Block", "101", 60);
        classroom.setId(1L);

        period = new Period("1st Period", LocalTime.of(9, 0), LocalTime.of(10, 0), 1);
        period.setId(1L);
        period.setDurationMinutes(60);
    }

    private CourseOffering offeringWithHours(Long id, int theoryHours, int labHours, int clinicalHours, Long facultyId) {
        CourseOffering offering = new CourseOffering();
        offering.setId(id);
        offering.setTermInstance(termInstance);
        offering.setSubject(subject);
        offering.setSemesterNumber(1);
        offering.setFacultyId(facultyId);
        offering.setIsActive(true);

        CurriculumSemesterCourse csc = new CurriculumSemesterCourse();
        csc.setTheoryHours(theoryHours);
        csc.setLabHours(labHours);
        csc.setClinicalHours(clinicalHours);
        csc.setIsElective(false);
        offering.setCurriculumSemesterCourse(csc);
        return offering;
    }

    @Test
    void shouldBlockGenerationWhenTimetableAlreadyPublished() {
        when(classScheduleRepository.existsByTermInstanceIdAndStatus(10L, ClassScheduleStatus.PUBLISHED)).thenReturn(true);

        assertThatThrownBy(() -> service.generate(10L))
            .isInstanceOf(LifecycleConflictException.class);

        verify(termInstanceRepository, never()).findById(any());
    }

    @Test
    void shouldThrowWhenTermInstanceNotFound() {
        when(classScheduleRepository.existsByTermInstanceIdAndStatus(10L, ClassScheduleStatus.PUBLISHED)).thenReturn(false);
        when(termInstanceRepository.findById(10L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.generate(10L))
            .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void shouldPlaceATheorySessionAndReturnGeneratedCount() {
        CourseOffering offering = offeringWithHours(100L, 1, 0, 0, faculty.getId());

        when(classScheduleRepository.existsByTermInstanceIdAndStatus(10L, ClassScheduleStatus.PUBLISHED)).thenReturn(false);
        when(termInstanceRepository.findById(10L)).thenReturn(Optional.of(termInstance));
        when(courseOfferingRepository.findByTermInstanceIdAndIsActiveTrue(10L)).thenReturn(List.of(offering));
        when(periodRepository.findByIsActiveTrueOrderByPeriodOrderAsc()).thenReturn(List.of(period));
        when(classroomRepository.findByIsActiveTrueOrderByNameAsc()).thenReturn(List.of(classroom));
        when(labRepository.findAll()).thenReturn(Collections.emptyList());
        when(facultyRepository.findById(faculty.getId())).thenReturn(Optional.of(faculty));
        when(classScheduleRepository.save(any(ClassSchedule.class))).thenAnswer(inv -> inv.getArgument(0));

        TimetableGenerationResponse response = service.generate(10L);

        assertThat(response.generatedCount()).isEqualTo(1);
        assertThat(response.unplaceable()).isEmpty();
        verify(classScheduleRepository, times(1)).save(any(ClassSchedule.class));
    }

    @Test
    void shouldSkipSlotsBlockedByFacultyAvailability() {
        // Only one period/classroom/day combination is even possible to isolate the effect:
        // a single availability block covering that period's exact time window on every day
        // must make the whole thing unplaceable, since there is nowhere else to place it.
        CourseOffering offering = offeringWithHours(100L, 1, 0, 0, faculty.getId());

        when(classScheduleRepository.existsByTermInstanceIdAndStatus(10L, ClassScheduleStatus.PUBLISHED)).thenReturn(false);
        when(termInstanceRepository.findById(10L)).thenReturn(Optional.of(termInstance));
        when(courseOfferingRepository.findByTermInstanceIdAndIsActiveTrue(10L)).thenReturn(List.of(offering));
        when(periodRepository.findByIsActiveTrueOrderByPeriodOrderAsc()).thenReturn(List.of(period));
        when(classroomRepository.findByIsActiveTrueOrderByNameAsc()).thenReturn(List.of(classroom));
        when(labRepository.findAll()).thenReturn(Collections.emptyList());
        when(facultyRepository.findById(faculty.getId())).thenReturn(Optional.of(faculty));
        when(facultyAvailabilityRepository.findByFacultyIdOrderByDayOfWeekAscStartTimeAsc(faculty.getId()))
            .thenReturn(java.util.stream.Stream.of(com.cms.model.enums.DayOfWeek.values())
                .map(day -> {
                    com.cms.model.FacultyAvailability b = new com.cms.model.FacultyAvailability();
                    b.setFaculty(faculty);
                    b.setDayOfWeek(day);
                    b.setStartTime(period.getStartTime());
                    b.setEndTime(period.getEndTime());
                    return b;
                }).toList());

        TimetableGenerationResponse response = service.generate(10L);

        assertThat(response.generatedCount()).isEqualTo(0);
        assertThat(response.unplaceable()).hasSize(1);
        assertThat(response.unplaceable().get(0)).contains("placed 0/1");
        verify(classScheduleRepository, never()).save(any());
    }

    @Test
    void shouldDeleteExistingDraftRowsBeforeRegenerating() {
        // Generate is also the in-place Regenerate action: it must wipe DRAFT rows up front
        // (discarding any manual Swap tweaks) rather than refusing to run, as long as the term
        // isn't already published.
        CourseOffering offering = offeringWithHours(100L, 1, 0, 0, faculty.getId());

        when(classScheduleRepository.existsByTermInstanceIdAndStatus(10L, ClassScheduleStatus.PUBLISHED)).thenReturn(false);
        when(termInstanceRepository.findById(10L)).thenReturn(Optional.of(termInstance));
        when(courseOfferingRepository.findByTermInstanceIdAndIsActiveTrue(10L)).thenReturn(List.of(offering));
        when(periodRepository.findByIsActiveTrueOrderByPeriodOrderAsc()).thenReturn(List.of(period));
        when(classroomRepository.findByIsActiveTrueOrderByNameAsc()).thenReturn(List.of(classroom));
        when(labRepository.findAll()).thenReturn(Collections.emptyList());
        when(facultyRepository.findById(faculty.getId())).thenReturn(Optional.of(faculty));
        when(classScheduleRepository.save(any(ClassSchedule.class))).thenAnswer(inv -> inv.getArgument(0));

        service.generate(10L);

        verify(classScheduleRepository, times(1))
            .deleteByTermInstanceIdAndStatus(10L, ClassScheduleStatus.DRAFT);
    }

    @Test
    void shouldPlaceWeeklyRecurringSessionsInsteadOfOneRowPerTermHour() {
        // The fixture term (2024-06-01 to 2024-11-30) spans 183 days = 27 whole weeks.
        // 120 theory hours should place ceil(120/27) = 5 weekly recurring sessions,
        // not 120 individual rows (one per raw curriculum hour, the old buggy behavior).
        CourseOffering offering = offeringWithHours(100L, 120, 0, 0, faculty.getId());

        when(classScheduleRepository.existsByTermInstanceIdAndStatus(10L, ClassScheduleStatus.PUBLISHED)).thenReturn(false);
        when(termInstanceRepository.findById(10L)).thenReturn(Optional.of(termInstance));
        when(courseOfferingRepository.findByTermInstanceIdAndIsActiveTrue(10L)).thenReturn(List.of(offering));
        when(periodRepository.findByIsActiveTrueOrderByPeriodOrderAsc()).thenReturn(List.of(period));
        when(classroomRepository.findByIsActiveTrueOrderByNameAsc()).thenReturn(List.of(classroom));
        when(labRepository.findAll()).thenReturn(Collections.emptyList());
        when(facultyRepository.findById(faculty.getId())).thenReturn(Optional.of(faculty));
        when(classScheduleRepository.save(any(ClassSchedule.class))).thenAnswer(inv -> inv.getArgument(0));

        TimetableGenerationResponse response = service.generate(10L);

        assertThat(response.generatedCount()).isEqualTo(5);
        assertThat(response.unplaceable()).isEmpty();
        verify(classScheduleRepository, times(5)).save(any(ClassSchedule.class));
    }

    @Test
    void shouldAccountForPeriodDurationShorterThanSixtyMinutes() {
        // 54 theory CLOCK hours over the fixture's 27-week term. The old (buggy) 1-period=1-hour
        // math would give ceil(54/27) = 2 weekly sessions; a 50-minute period actually needs
        // ceil((54*60/50)/27) = ceil(64.8/27) = 3 -- proving the conversion, not just that a
        // number came out (2 and 3 are different enough to not coincide by rounding luck).
        CourseOffering offering = offeringWithHours(100L, 54, 0, 0, faculty.getId());
        Period shortPeriod = new Period("1st Period", LocalTime.of(9, 0), LocalTime.of(9, 50), 1);
        shortPeriod.setId(1L);
        shortPeriod.setDurationMinutes(50);

        when(classScheduleRepository.existsByTermInstanceIdAndStatus(10L, ClassScheduleStatus.PUBLISHED)).thenReturn(false);
        when(termInstanceRepository.findById(10L)).thenReturn(Optional.of(termInstance));
        when(courseOfferingRepository.findByTermInstanceIdAndIsActiveTrue(10L)).thenReturn(List.of(offering));
        when(periodRepository.findByIsActiveTrueOrderByPeriodOrderAsc()).thenReturn(List.of(shortPeriod));
        when(classroomRepository.findByIsActiveTrueOrderByNameAsc()).thenReturn(List.of(classroom));
        when(labRepository.findAll()).thenReturn(Collections.emptyList());
        when(facultyRepository.findById(faculty.getId())).thenReturn(Optional.of(faculty));
        when(classScheduleRepository.save(any(ClassSchedule.class))).thenAnswer(inv -> inv.getArgument(0));

        TimetableGenerationResponse response = service.generate(10L);

        assertThat(response.generatedCount()).isEqualTo(3);
        assertThat(response.unplaceable()).isEmpty();
        verify(classScheduleRepository, times(3)).save(any(ClassSchedule.class));
    }

    @Test
    void shouldAccountForLabSlotDurationLongerThanSixtyMinutes() {
        // 100 lab/clinical CLOCK hours over the 27-week fixture term, placed via a 2-hour period
        // (Theory and Lab share the one Period pool since V331 merged LabSlot into it). Old
        // (buggy) 1-slot=1-hour math: ceil(100/27) = 4 weekly sessions. Correct: each slot
        // delivers 2 clock-hours, so ceil((100*60/120)/27) = ceil(50/27) = 2 -- fewer sessions
        // needed, proving the longer slot is credited properly rather than undercounted.
        CourseOffering offering = offeringWithHours(100L, 0, 100, 0, faculty.getId());
        Batch batch = new Batch(offering, "Batch A", 20, termInstance);
        batch.setId(1L);
        Period twoHourPeriod = new Period("Lab Slot 1", LocalTime.of(9, 0), LocalTime.of(11, 0), 1);
        twoHourPeriod.setId(1L);
        twoHourPeriod.setDurationMinutes(120);
        com.cms.model.Lab lab = new com.cms.model.Lab("Skills Lab", com.cms.model.enums.LabType.OTHER,
            null, "Main Block", "L1", 30, com.cms.model.enums.LabStatus.ACTIVE);
        lab.setId(1L);

        when(classScheduleRepository.existsByTermInstanceIdAndStatus(10L, ClassScheduleStatus.PUBLISHED)).thenReturn(false);
        when(termInstanceRepository.findById(10L)).thenReturn(Optional.of(termInstance));
        when(courseOfferingRepository.findByTermInstanceIdAndIsActiveTrue(10L)).thenReturn(List.of(offering));
        when(periodRepository.findByIsActiveTrueOrderByPeriodOrderAsc()).thenReturn(List.of(twoHourPeriod));
        when(classroomRepository.findByIsActiveTrueOrderByNameAsc()).thenReturn(Collections.emptyList());
        when(labRepository.findAll()).thenReturn(List.of(lab));
        when(batchRepository.findByCourseOfferingId(offering.getId())).thenReturn(List.of(batch));
        when(facultyRepository.findById(faculty.getId())).thenReturn(Optional.of(faculty));
        when(classScheduleRepository.save(any(ClassSchedule.class))).thenAnswer(inv -> inv.getArgument(0));

        TimetableGenerationResponse response = service.generate(10L);

        assertThat(response.generatedCount()).isEqualTo(2);
        assertThat(response.unplaceable()).isEmpty();
        verify(classScheduleRepository, times(2)).save(any(ClassSchedule.class));
    }

    @Test
    void shouldNotPlaceTheSameSubjectTwiceOnTheSameDay() {
        // 2 periods, 1 classroom, 6 usable days (MON-SAT). Without the same-day cap, once all
        // 6 days are used under period 1, the algorithm would fall through to period 2 and
        // double-book a day. With the cap, placement stops once every day is used once, even
        // though more sessions are still needed.
        CourseOffering offering = offeringWithHours(100L, 200, 0, 0, faculty.getId());

        Period p1 = new Period("1st Period", LocalTime.of(9, 0), LocalTime.of(10, 0), 1);
        p1.setId(1L);
        p1.setDurationMinutes(60);
        Period p2 = new Period("2nd Period", LocalTime.of(10, 0), LocalTime.of(11, 0), 2);
        p2.setId(2L);
        p2.setDurationMinutes(60);

        when(classScheduleRepository.existsByTermInstanceIdAndStatus(10L, ClassScheduleStatus.PUBLISHED)).thenReturn(false);
        when(termInstanceRepository.findById(10L)).thenReturn(Optional.of(termInstance));
        when(courseOfferingRepository.findByTermInstanceIdAndIsActiveTrue(10L)).thenReturn(List.of(offering));
        when(periodRepository.findByIsActiveTrueOrderByPeriodOrderAsc()).thenReturn(List.of(p1, p2));
        when(classroomRepository.findByIsActiveTrueOrderByNameAsc()).thenReturn(List.of(classroom));
        when(labRepository.findAll()).thenReturn(Collections.emptyList());
        when(facultyRepository.findById(faculty.getId())).thenReturn(Optional.of(faculty));
        when(classScheduleRepository.save(any(ClassSchedule.class))).thenAnswer(inv -> inv.getArgument(0));

        TimetableGenerationResponse response = service.generate(10L);

        // ceil(200/27) = 8 sessions/week needed, but the same-day cap allows at most 1 per day
        // across 6 days, so only 6 get placed and the rest is reported unplaceable.
        assertThat(response.generatedCount()).isEqualTo(6);
        assertThat(response.unplaceable()).hasSize(1);
        assertThat(response.unplaceable().get(0)).contains("placed 6/8 weekly theory session(s)");
        verify(classScheduleRepository, times(6)).save(any(ClassSchedule.class));
    }

    @Test
    void shouldReportUnplaceableWhenOfferingHasNoFaculty() {
        CourseOffering offering = offeringWithHours(100L, 1, 0, 0, null);

        when(classScheduleRepository.existsByTermInstanceIdAndStatus(10L, ClassScheduleStatus.PUBLISHED)).thenReturn(false);
        when(termInstanceRepository.findById(10L)).thenReturn(Optional.of(termInstance));
        when(courseOfferingRepository.findByTermInstanceIdAndIsActiveTrue(10L)).thenReturn(List.of(offering));
        when(periodRepository.findByIsActiveTrueOrderByPeriodOrderAsc()).thenReturn(List.of(period));
        when(classroomRepository.findByIsActiveTrueOrderByNameAsc()).thenReturn(List.of(classroom));
        when(labRepository.findAll()).thenReturn(Collections.emptyList());

        TimetableGenerationResponse response = service.generate(10L);

        assertThat(response.generatedCount()).isEqualTo(0);
        assertThat(response.unplaceable()).hasSize(1);
        assertThat(response.unplaceable().get(0)).contains("no faculty assigned");
        verify(classScheduleRepository, never()).save(any());
    }

    @Test
    void shouldSkipElectiveOfferings() {
        CourseOffering offering = offeringWithHours(100L, 1, 0, 0, faculty.getId());
        offering.getCurriculumSemesterCourse().setIsElective(true);

        when(classScheduleRepository.existsByTermInstanceIdAndStatus(10L, ClassScheduleStatus.PUBLISHED)).thenReturn(false);
        when(termInstanceRepository.findById(10L)).thenReturn(Optional.of(termInstance));
        when(courseOfferingRepository.findByTermInstanceIdAndIsActiveTrue(10L)).thenReturn(List.of(offering));
        when(periodRepository.findByIsActiveTrueOrderByPeriodOrderAsc()).thenReturn(List.of(period));
        when(classroomRepository.findByIsActiveTrueOrderByNameAsc()).thenReturn(List.of(classroom));
        when(labRepository.findAll()).thenReturn(Collections.emptyList());

        TimetableGenerationResponse response = service.generate(10L);

        assertThat(response.generatedCount()).isEqualTo(0);
        assertThat(response.unplaceable()).isEmpty();
        verify(classScheduleRepository, never()).save(any());
    }

    @Test
    void shouldClearAllRowsForTerm() {
        ClassSchedule row = new ClassSchedule();
        row.setId(1L);
        when(termInstanceRepository.existsById(10L)).thenReturn(true);
        when(classScheduleRepository.findByTermInstanceId(10L)).thenReturn(List.of(row));

        TimetableActionResponse response = service.clear(10L);

        assertThat(response.affectedCount()).isEqualTo(1);
        verify(classScheduleRepository).deleteByTermInstanceId(10L);
    }

    @Test
    void shouldThrowWhenClearingNonExistentTerm() {
        when(termInstanceRepository.existsById(999L)).thenReturn(false);

        assertThatThrownBy(() -> service.clear(999L))
            .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void shouldApproveAllDraftRows() {
        ClassSchedule draft1 = new ClassSchedule();
        draft1.setId(1L);
        draft1.setStatus(ClassScheduleStatus.DRAFT);
        draft1.setFaculty(faculty);
        ClassSchedule draft2 = new ClassSchedule();
        draft2.setId(2L);
        draft2.setStatus(ClassScheduleStatus.DRAFT);
        draft2.setFaculty(faculty);

        when(classScheduleRepository.findByTermInstanceIdAndStatus(10L, ClassScheduleStatus.DRAFT))
            .thenReturn(List.of(draft1, draft2));
        when(classScheduleRepository.save(any(ClassSchedule.class))).thenAnswer(inv -> inv.getArgument(0));

        TimetableActionResponse response = service.approve(10L);

        assertThat(response.affectedCount()).isEqualTo(2);
        assertThat(draft1.getStatus()).isEqualTo(ClassScheduleStatus.PUBLISHED);
        assertThat(draft2.getStatus()).isEqualTo(ClassScheduleStatus.PUBLISHED);
    }

    @Test
    void shouldBlockApproveWhenAnyDraftRowIsUnstaffed() {
        // R3 Phase 5: an unstaffed skeleton cell (no faculty yet) must be rejected with a clear
        // actionable error here, not left to fail as a raw chk_class_schedule_session_shape
        // violation the moment its status flips to PUBLISHED.
        ClassSchedule staffed = new ClassSchedule();
        staffed.setId(1L);
        staffed.setStatus(ClassScheduleStatus.DRAFT);
        staffed.setFaculty(faculty);
        ClassSchedule unstaffed = new ClassSchedule();
        unstaffed.setId(2L);
        unstaffed.setStatus(ClassScheduleStatus.DRAFT);

        when(classScheduleRepository.findByTermInstanceIdAndStatus(10L, ClassScheduleStatus.DRAFT))
            .thenReturn(List.of(staffed, unstaffed));

        assertThatThrownBy(() -> service.approve(10L))
            .isInstanceOf(LifecycleConflictException.class);

        verify(classScheduleRepository, never()).save(any());
    }

    @Test
    void shouldThrowWhenApprovingWithNoDrafts() {
        when(classScheduleRepository.findByTermInstanceIdAndStatus(10L, ClassScheduleStatus.DRAFT))
            .thenReturn(Collections.emptyList());

        assertThatThrownBy(() -> service.approve(10L))
            .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void shouldRevertAllPublishedRowsToDraft() {
        ClassSchedule published1 = new ClassSchedule();
        published1.setId(1L);
        published1.setStatus(ClassScheduleStatus.PUBLISHED);
        ClassSchedule published2 = new ClassSchedule();
        published2.setId(2L);
        published2.setStatus(ClassScheduleStatus.PUBLISHED);

        when(classScheduleRepository.findByTermInstanceIdAndStatus(10L, ClassScheduleStatus.PUBLISHED))
            .thenReturn(List.of(published1, published2));
        when(labAttendanceRepository.existsByLabScheduleTermInstanceId(10L)).thenReturn(false);
        when(classScheduleRepository.save(any(ClassSchedule.class))).thenAnswer(inv -> inv.getArgument(0));

        TimetableActionResponse response = service.revertToDraft(10L);

        assertThat(response.affectedCount()).isEqualTo(2);
        assertThat(published1.getStatus()).isEqualTo(ClassScheduleStatus.DRAFT);
        assertThat(published2.getStatus()).isEqualTo(ClassScheduleStatus.DRAFT);
    }

    @Test
    void shouldThrowWhenRevertingWithNoPublishedRows() {
        when(classScheduleRepository.findByTermInstanceIdAndStatus(10L, ClassScheduleStatus.PUBLISHED))
            .thenReturn(Collections.emptyList());

        assertThatThrownBy(() -> service.revertToDraft(10L))
            .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void shouldBlockRevertWhenAttendanceAlreadyRecorded() {
        ClassSchedule published1 = new ClassSchedule();
        published1.setId(1L);
        published1.setStatus(ClassScheduleStatus.PUBLISHED);

        when(classScheduleRepository.findByTermInstanceIdAndStatus(10L, ClassScheduleStatus.PUBLISHED))
            .thenReturn(List.of(published1));
        when(labAttendanceRepository.existsByLabScheduleTermInstanceId(10L)).thenReturn(true);

        assertThatThrownBy(() -> service.revertToDraft(10L))
            .isInstanceOf(LifecycleConflictException.class);

        verify(classScheduleRepository, never()).save(any());
    }
}
