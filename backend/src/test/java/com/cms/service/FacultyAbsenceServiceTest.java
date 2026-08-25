package com.cms.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
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

import com.cms.dto.AffectedSessionResponse;
import com.cms.dto.FacultyAbsenceDto;
import com.cms.dto.FacultyAbsenceRequest;
import com.cms.dto.SubstituteCandidateResponse;
import com.cms.exception.ResourceNotFoundException;
import com.cms.model.AcademicYear;
import com.cms.model.ClassSchedule;
import com.cms.model.DesignationMaster;
import com.cms.model.Faculty;
import com.cms.model.FacultyAbsence;
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
import com.cms.repository.DayMappingOverrideRepository;
import com.cms.repository.FacultyAbsenceRepository;
import com.cms.repository.FacultyAvailabilityRepository;
import com.cms.repository.FacultyRepository;
import com.cms.repository.SessionOccurrenceRepository;

@ExtendWith(MockitoExtension.class)
class FacultyAbsenceServiceTest {

    @Mock private FacultyAbsenceRepository facultyAbsenceRepository;
    @Mock private ClassScheduleRepository classScheduleRepository;
    @Mock private FacultyRepository facultyRepository;
    @Mock private FacultyAvailabilityRepository facultyAvailabilityRepository;
    @Mock private SessionOccurrenceRepository sessionOccurrenceRepository;
    @Mock private AuditLogService auditLogService;
    @Mock private DayMappingOverrideRepository dayMappingOverrideRepository;

    private FacultyAbsenceService service;

    private Speciality speciality;
    private Faculty absentFaculty;
    private Faculty eligibleFaculty;
    private TermInstance termInstance;
    private ClassSchedule schedule;

    @BeforeEach
    void setUp() {
        service = new FacultyAbsenceService(facultyAbsenceRepository, classScheduleRepository,
            facultyRepository, facultyAvailabilityRepository, sessionOccurrenceRepository, auditLogService,
            dayMappingOverrideRepository);
        lenient().when(dayMappingOverrideRepository.findByMappedDate(any())).thenReturn(Optional.empty());

        speciality = new Speciality("Nursing", "NUR", "Nursing Dept", null, null);
        speciality.setId(1L);
        DesignationMaster designation = new DesignationMaster("Assistant Professor", "ASSISTANT_PROFESSOR", null);
        designation.setId(1L);

        absentFaculty = new Faculty("EMP001", "John", "Doe", "john@college.edu", "1234567890",
            speciality, designation, "Nursing", null, null, FacultyStatus.ACTIVE);
        absentFaculty.setId(1L);
        eligibleFaculty = new Faculty("EMP002", "Jane", "Roe", "jane@college.edu", "1234567891",
            speciality, designation, "Nursing", null, null, FacultyStatus.ACTIVE);
        eligibleFaculty.setId(2L);

        AcademicYear ay = new AcademicYear("2024-2025", LocalDate.of(2024, 6, 1), LocalDate.of(2025, 5, 31), false);
        ay.setId(1L);
        ay.setCreatedAt(Instant.now());
        ay.setUpdatedAt(Instant.now());
        termInstance = new TermInstance(ay, TermType.ODD, LocalDate.of(2024, 6, 1), LocalDate.of(2024, 11, 30), TermInstanceStatus.OPEN);
        termInstance.setId(10L);
        termInstance.setCreatedAt(Instant.now());
        termInstance.setUpdatedAt(Instant.now());

        Subject subject = new Subject("Nursing Foundations", "NF101", 4, 3, 1, speciality, 1);
        subject.setId(1L);

        Period period = new Period("1st Period", LocalTime.of(9, 0), LocalTime.of(10, 0), 1);
        period.setId(1L);
        period.setDurationMinutes(60);

        schedule = new ClassSchedule();
        schedule.setId(300L);
        schedule.setFaculty(absentFaculty);
        schedule.setSubject(subject);
        schedule.setSessionType(ClassSessionType.THEORY);
        schedule.setDayOfWeek(DayOfWeek.MONDAY);
        schedule.setTermInstance(termInstance);
        schedule.setPeriod(period);
        schedule.setStatus(ClassScheduleStatus.PUBLISHED);
    }

    @Test
    void shouldMarkAbsentAndCreateNewRecord() {
        FacultyAbsenceRequest request = new FacultyAbsenceRequest(1L, LocalDate.of(2024, 8, 5), "Sick leave");
        when(facultyRepository.findById(1L)).thenReturn(Optional.of(absentFaculty));
        when(facultyAbsenceRepository.findByFacultyIdAndAbsenceDate(1L, LocalDate.of(2024, 8, 5))).thenReturn(Optional.empty());
        when(facultyAbsenceRepository.save(any(FacultyAbsence.class))).thenAnswer(inv -> {
            FacultyAbsence a = inv.getArgument(0);
            a.setId(50L);
            return a;
        });

        FacultyAbsenceDto dto = service.markAbsent(request, "admin");

        assertThat(dto.id()).isEqualTo(50L);
        assertThat(dto.facultyId()).isEqualTo(1L);
        assertThat(dto.reason()).isEqualTo("Sick leave");
        assertThat(dto.recordedBy()).isEqualTo("admin");
    }

    @Test
    void shouldFindAffectedSessionsMatchingWeekdayAndWithinTermBounds() {
        FacultyAbsence absence = new FacultyAbsence(absentFaculty, LocalDate.of(2024, 8, 5), "Sick", "admin"); // a Monday
        absence.setId(50L);

        when(facultyAbsenceRepository.findById(50L)).thenReturn(Optional.of(absence));
        when(classScheduleRepository.findByFacultyIdAndStatusAndDayOfWeek(1L, ClassScheduleStatus.PUBLISHED, DayOfWeek.MONDAY))
            .thenReturn(List.of(schedule));
        when(sessionOccurrenceRepository.findByClassScheduleIdAndOccurrenceDate(300L, LocalDate.of(2024, 8, 5)))
            .thenReturn(Optional.empty());

        List<AffectedSessionResponse> result = service.findAffectedSessions(50L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).classScheduleId()).isEqualTo(300L);
        assertThat(result.get(0).subjectName()).isEqualTo("Nursing Foundations");
    }

    @Test
    void shouldResolveAffectedSessionsAgainstTheBorrowedWeekdayWhenAbsenceDateIsMapped() {
        // 2024-08-10 is a Saturday, mapped to run Monday's schedule -- the absent faculty's only
        // session is a MONDAY session, so it should be found even though the absence date itself
        // is a Saturday (which the faculty has no schedule on at all).
        LocalDate mappedSaturday = LocalDate.of(2024, 8, 10);
        FacultyAbsence absence = new FacultyAbsence(absentFaculty, mappedSaturday, "Sick", "admin");
        absence.setId(53L);

        com.cms.model.DayMappingOverride mapping = new com.cms.model.DayMappingOverride();
        mapping.setMappedDate(mappedSaturday);
        mapping.setBorrowedDayOfWeek(DayOfWeek.MONDAY);

        when(facultyAbsenceRepository.findById(53L)).thenReturn(Optional.of(absence));
        when(dayMappingOverrideRepository.findByMappedDate(mappedSaturday)).thenReturn(Optional.of(mapping));
        when(classScheduleRepository.findByFacultyIdAndStatusAndDayOfWeek(1L, ClassScheduleStatus.PUBLISHED, DayOfWeek.MONDAY))
            .thenReturn(List.of(schedule));
        when(sessionOccurrenceRepository.findByClassScheduleIdAndOccurrenceDate(300L, mappedSaturday))
            .thenReturn(Optional.empty());

        List<AffectedSessionResponse> result = service.findAffectedSessions(53L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).classScheduleId()).isEqualTo(300L);
    }

    @Test
    void shouldReturnNoAffectedSessionsWhenAbsenceDateIsOutsideTermBounds() {
        FacultyAbsence absence = new FacultyAbsence(absentFaculty, LocalDate.of(2026, 1, 5), "Sick", "admin"); // Monday, outside term
        absence.setId(51L);

        when(facultyAbsenceRepository.findById(51L)).thenReturn(Optional.of(absence));
        when(classScheduleRepository.findByFacultyIdAndStatusAndDayOfWeek(1L, ClassScheduleStatus.PUBLISHED, DayOfWeek.MONDAY))
            .thenReturn(List.of(schedule));

        List<AffectedSessionResponse> result = service.findAffectedSessions(51L);

        assertThat(result).isEmpty();
    }

    @Test
    void shouldReturnNoAffectedSessionsForSunday() {
        FacultyAbsence absence = new FacultyAbsence(absentFaculty, LocalDate.of(2024, 8, 4), "Sick", "admin"); // a Sunday
        absence.setId(52L);
        when(facultyAbsenceRepository.findById(52L)).thenReturn(Optional.of(absence));

        List<AffectedSessionResponse> result = service.findAffectedSessions(52L);

        assertThat(result).isEmpty();
    }

    @Test
    void shouldFindEligibleSubstituteWithMatchingSpecialityAndNoConflicts() {
        when(classScheduleRepository.findById(300L)).thenReturn(Optional.of(schedule));
        when(classScheduleRepository.findOverlapping(DayOfWeek.MONDAY, 10L, LocalTime.of(9, 0), LocalTime.of(10, 0),
            ClassScheduleStatus.PUBLISHED, 300L)).thenReturn(Collections.emptyList());
        when(facultyRepository.findBySpecialityIdAndStatus(1L, FacultyStatus.ACTIVE))
            .thenReturn(List.of(absentFaculty, eligibleFaculty));
        when(facultyAvailabilityRepository.findOverlapping(2L, DayOfWeek.MONDAY, LocalTime.of(9, 0), LocalTime.of(10, 0)))
            .thenReturn(Collections.emptyList());

        List<SubstituteCandidateResponse> candidates = service.findEligibleSubstitutes(300L, LocalDate.of(2024, 8, 5));

        assertThat(candidates).hasSize(1);
        assertThat(candidates.get(0).facultyId()).isEqualTo(2L);
    }

    @Test
    void shouldIncludeEverySameSpecialityFacultyAsAnEqualSubstituteCandidate() {
        // Every same-speciality active faculty is an equal, undistinguished candidate here -- there
        // is no separate "co-instructor" concept in this pool, just the speciality-based match.
        Faculty anotherEligibleFaculty = new Faculty("EMP003", "Sam", "Lee", "sam@college.edu", "1234567892",
            speciality, absentFaculty.getDesignation(), "Nursing", null, null, FacultyStatus.ACTIVE);
        anotherEligibleFaculty.setId(3L);

        when(classScheduleRepository.findById(300L)).thenReturn(Optional.of(schedule));
        when(classScheduleRepository.findOverlapping(DayOfWeek.MONDAY, 10L, LocalTime.of(9, 0), LocalTime.of(10, 0),
            ClassScheduleStatus.PUBLISHED, 300L)).thenReturn(Collections.emptyList());
        when(facultyRepository.findBySpecialityIdAndStatus(1L, FacultyStatus.ACTIVE))
            .thenReturn(List.of(absentFaculty, eligibleFaculty, anotherEligibleFaculty));
        when(facultyAvailabilityRepository.findOverlapping(2L, DayOfWeek.MONDAY, LocalTime.of(9, 0), LocalTime.of(10, 0)))
            .thenReturn(Collections.emptyList());
        when(facultyAvailabilityRepository.findOverlapping(3L, DayOfWeek.MONDAY, LocalTime.of(9, 0), LocalTime.of(10, 0)))
            .thenReturn(Collections.emptyList());

        List<SubstituteCandidateResponse> candidates = service.findEligibleSubstitutes(300L, LocalDate.of(2024, 8, 5));

        assertThat(candidates).extracting(SubstituteCandidateResponse::facultyId).containsExactlyInAnyOrder(2L, 3L);
    }

    @Test
    void shouldRejectFindingSubstitutesForAnUnstaffedSkeletonCell() {
        // R3 Phase 6 regression fix: an unstaffed skeleton cell (R3 Phase 4) has no faculty, so
        // "find a substitute for the absent teacher" is meaningless and must not NPE.
        ClassSchedule unstaffedSkeletonCell = new ClassSchedule();
        unstaffedSkeletonCell.setId(400L);
        unstaffedSkeletonCell.setStatus(ClassScheduleStatus.DRAFT);
        when(classScheduleRepository.findById(400L)).thenReturn(Optional.of(unstaffedSkeletonCell));

        assertThatThrownBy(() -> service.findEligibleSubstitutes(400L, LocalDate.of(2024, 8, 5)))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void shouldExcludeCandidateBlockedByFacultyAvailability() {
        when(classScheduleRepository.findById(300L)).thenReturn(Optional.of(schedule));
        when(classScheduleRepository.findOverlapping(DayOfWeek.MONDAY, 10L, LocalTime.of(9, 0), LocalTime.of(10, 0),
            ClassScheduleStatus.PUBLISHED, 300L)).thenReturn(Collections.emptyList());
        when(facultyRepository.findBySpecialityIdAndStatus(1L, FacultyStatus.ACTIVE))
            .thenReturn(List.of(absentFaculty, eligibleFaculty));
        com.cms.model.FacultyAvailability block = new com.cms.model.FacultyAvailability();
        when(facultyAvailabilityRepository.findOverlapping(2L, DayOfWeek.MONDAY, LocalTime.of(9, 0), LocalTime.of(10, 0)))
            .thenReturn(List.of(block));

        List<SubstituteCandidateResponse> candidates = service.findEligibleSubstitutes(300L, LocalDate.of(2024, 8, 5));

        assertThat(candidates).isEmpty();
    }

    @Test
    void shouldExcludeCandidateAlreadyTeachingAtThatTime() {
        ClassSchedule conflicting = new ClassSchedule();
        conflicting.setFaculty(eligibleFaculty);

        when(classScheduleRepository.findById(300L)).thenReturn(Optional.of(schedule));
        when(classScheduleRepository.findOverlapping(DayOfWeek.MONDAY, 10L, LocalTime.of(9, 0), LocalTime.of(10, 0),
            ClassScheduleStatus.PUBLISHED, 300L)).thenReturn(List.of(conflicting));
        when(facultyRepository.findBySpecialityIdAndStatus(1L, FacultyStatus.ACTIVE))
            .thenReturn(List.of(absentFaculty, eligibleFaculty));

        List<SubstituteCandidateResponse> candidates = service.findEligibleSubstitutes(300L, LocalDate.of(2024, 8, 5));

        assertThat(candidates).isEmpty();
    }

    @Test
    void shouldApplySubstituteAndRecordOnSessionOccurrenceWithoutMutatingClassSchedule() {
        FacultyAbsence absence = new FacultyAbsence(absentFaculty, LocalDate.of(2024, 8, 5), "Sick", "admin");
        absence.setId(50L);

        when(facultyAbsenceRepository.findById(50L)).thenReturn(Optional.of(absence));
        when(classScheduleRepository.findById(300L)).thenReturn(Optional.of(schedule));
        when(classScheduleRepository.findOverlapping(DayOfWeek.MONDAY, 10L, LocalTime.of(9, 0), LocalTime.of(10, 0),
            ClassScheduleStatus.PUBLISHED, 300L)).thenReturn(Collections.emptyList());
        when(facultyRepository.findBySpecialityIdAndStatus(1L, FacultyStatus.ACTIVE))
            .thenReturn(List.of(absentFaculty, eligibleFaculty));
        when(facultyAvailabilityRepository.findOverlapping(2L, DayOfWeek.MONDAY, LocalTime.of(9, 0), LocalTime.of(10, 0)))
            .thenReturn(Collections.emptyList());
        when(facultyRepository.findById(2L)).thenReturn(Optional.of(eligibleFaculty));
        when(sessionOccurrenceRepository.findByClassScheduleIdAndOccurrenceDate(300L, LocalDate.of(2024, 8, 5)))
            .thenReturn(Optional.empty());
        when(sessionOccurrenceRepository.save(any(com.cms.model.SessionOccurrence.class)))
            .thenAnswer(inv -> inv.getArgument(0));

        AffectedSessionResponse response = service.applySubstitute(50L, 300L, 2L, "admin");

        assertThat(response.substituteFacultyName()).isEqualTo("Jane Roe");
        assertThat(response.occurrenceStatus()).isEqualTo(com.cms.model.enums.OccurrenceStatus.SUBSTITUTED);
        // ClassSchedule.faculty must remain the originally-absent faculty -- never mutated.
        assertThat(schedule.getFaculty().getId()).isEqualTo(1L);
    }

    @Test
    void shouldRejectApplyingAnIneligibleSubstitute() {
        FacultyAbsence absence = new FacultyAbsence(absentFaculty, LocalDate.of(2024, 8, 5), "Sick", "admin");
        absence.setId(50L);

        when(facultyAbsenceRepository.findById(50L)).thenReturn(Optional.of(absence));
        when(classScheduleRepository.findById(300L)).thenReturn(Optional.of(schedule));
        when(classScheduleRepository.findOverlapping(DayOfWeek.MONDAY, 10L, LocalTime.of(9, 0), LocalTime.of(10, 0),
            ClassScheduleStatus.PUBLISHED, 300L)).thenReturn(Collections.emptyList());
        when(facultyRepository.findBySpecialityIdAndStatus(1L, FacultyStatus.ACTIVE))
            .thenReturn(List.of(absentFaculty)); // eligibleFaculty (id=2) not in the pool at all

        assertThatThrownBy(() -> service.applySubstitute(50L, 300L, 2L, "admin"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("no longer eligible");
    }

    @Test
    void shouldThrowWhenAbsenceNotFound() {
        when(facultyAbsenceRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findAffectedSessions(999L))
            .isInstanceOf(ResourceNotFoundException.class);
    }
}
