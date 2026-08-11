package com.cms.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
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

import com.cms.dto.StaffSwapCandidateResponse;
import com.cms.model.AcademicYear;
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
import com.cms.model.enums.TermInstanceStatus;
import com.cms.model.enums.TermType;
import com.cms.repository.ClassScheduleRepository;
import com.cms.repository.FacultyAvailabilityRepository;
import com.cms.repository.SessionOccurrenceRepository;

@ExtendWith(MockitoExtension.class)
class FacultySessionSwapServiceTest {

    @Mock private ClassScheduleRepository classScheduleRepository;
    @Mock private FacultyAvailabilityRepository facultyAvailabilityRepository;
    @Mock private SessionOccurrenceRepository sessionOccurrenceRepository;
    @Mock private ClassScheduleOccurrenceService occurrenceService;
    @Mock private AuditLogService auditLogService;

    private FacultySessionSwapService service;

    private TermInstance termInstance;
    private Faculty facultyA;
    private Faculty facultyB;
    private ClassSchedule sessionA;
    private ClassSchedule sessionB;
    private final LocalDate date = LocalDate.of(2024, 8, 5); // a Monday

    @BeforeEach
    void setUp() {
        service = new FacultySessionSwapService(classScheduleRepository, facultyAvailabilityRepository,
            sessionOccurrenceRepository, occurrenceService, auditLogService);

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

        facultyA = new Faculty("EMP001", "John", "Doe", "john@college.edu", "1234567890",
            speciality, designation, "Nursing", null, null, com.cms.model.enums.FacultyStatus.ACTIVE);
        facultyA.setId(1L);
        facultyB = new Faculty("EMP002", "Jane", "Roe", "jane@college.edu", "1234567891",
            speciality, designation, "Nursing", null, null, com.cms.model.enums.FacultyStatus.ACTIVE);
        facultyB.setId(2L);

        Subject subjectA = new Subject("Nursing Foundations", "NF101", 4, 3, 1, speciality, 1);
        subjectA.setId(1L);
        Subject subjectB = new Subject("Applied Sociology", "SOCI115", 4, 3, 1, speciality, 1);
        subjectB.setId(2L);

        Period period1 = new Period("1st Period", LocalTime.of(9, 0), LocalTime.of(10, 0), 1);
        period1.setId(1L);
        period1.setDurationMinutes(60);
        Period period2 = new Period("2nd Period", LocalTime.of(10, 0), LocalTime.of(11, 0), 2);
        period2.setId(2L);
        period2.setDurationMinutes(60);

        sessionA = new ClassSchedule();
        sessionA.setId(300L);
        sessionA.setFaculty(facultyA);
        sessionA.setSubject(subjectA);
        sessionA.setSessionType(ClassSessionType.THEORY);
        sessionA.setDayOfWeek(DayOfWeek.MONDAY);
        sessionA.setTermInstance(termInstance);
        sessionA.setPeriod(period1);
        sessionA.setStatus(ClassScheduleStatus.PUBLISHED);

        sessionB = new ClassSchedule();
        sessionB.setId(301L);
        sessionB.setFaculty(facultyB);
        sessionB.setSubject(subjectB);
        sessionB.setSessionType(ClassSessionType.THEORY);
        sessionB.setDayOfWeek(DayOfWeek.MONDAY);
        sessionB.setTermInstance(termInstance);
        sessionB.setPeriod(period2);
        sessionB.setStatus(ClassScheduleStatus.PUBLISHED);
    }

    @Test
    void shouldFindMutuallyAvailableCandidate() {
        when(classScheduleRepository.findById(300L)).thenReturn(Optional.of(sessionA));
        when(occurrenceService.occurrenceDatesFor(sessionA, date, date)).thenReturn(List.of(date));
        when(classScheduleRepository.findByTermInstanceIdAndStatusAndDayOfWeek(10L, ClassScheduleStatus.PUBLISHED, DayOfWeek.MONDAY))
            .thenReturn(List.of(sessionA, sessionB));
        when(facultyAvailabilityRepository.findOverlapping(1L, DayOfWeek.MONDAY, LocalTime.of(10, 0), LocalTime.of(11, 0)))
            .thenReturn(Collections.emptyList());
        when(facultyAvailabilityRepository.findOverlapping(2L, DayOfWeek.MONDAY, LocalTime.of(9, 0), LocalTime.of(10, 0)))
            .thenReturn(Collections.emptyList());
        when(classScheduleRepository.findOverlapping(DayOfWeek.MONDAY, 10L, LocalTime.of(10, 0), LocalTime.of(11, 0),
            ClassScheduleStatus.PUBLISHED, 300L)).thenReturn(Collections.emptyList());
        when(classScheduleRepository.findOverlapping(DayOfWeek.MONDAY, 10L, LocalTime.of(9, 0), LocalTime.of(10, 0),
            ClassScheduleStatus.PUBLISHED, 301L)).thenReturn(Collections.emptyList());

        List<StaffSwapCandidateResponse> candidates = service.findSwapCandidates(300L, date);

        assertThat(candidates).hasSize(1);
        assertThat(candidates.get(0).classScheduleId()).isEqualTo(301L);
        assertThat(candidates.get(0).facultyName()).isEqualTo("Jane Roe");
    }

    @Test
    void shouldExcludeCandidateBlockedByFacultyAvailabilityInEitherDirection() {
        when(classScheduleRepository.findById(300L)).thenReturn(Optional.of(sessionA));
        when(occurrenceService.occurrenceDatesFor(sessionA, date, date)).thenReturn(List.of(date));
        when(classScheduleRepository.findByTermInstanceIdAndStatusAndDayOfWeek(10L, ClassScheduleStatus.PUBLISHED, DayOfWeek.MONDAY))
            .thenReturn(List.of(sessionA, sessionB));
        // Faculty A is blocked at B's slot (10-11) -- mutual check fails.
        when(facultyAvailabilityRepository.findOverlapping(1L, DayOfWeek.MONDAY, LocalTime.of(10, 0), LocalTime.of(11, 0)))
            .thenReturn(List.of(new com.cms.model.FacultyAvailability()));

        List<StaffSwapCandidateResponse> candidates = service.findSwapCandidates(300L, date);

        assertThat(candidates).isEmpty();
    }

    @Test
    void shouldRejectDateThatIsNotARealOccurrence() {
        when(classScheduleRepository.findById(300L)).thenReturn(Optional.of(sessionA));
        when(occurrenceService.occurrenceDatesFor(sessionA, date, date)).thenReturn(List.of());

        assertThatThrownBy(() -> service.findSwapCandidates(300L, date))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("not a real occurrence");
    }

    @Test
    void shouldRejectDraftSession() {
        sessionA.setStatus(ClassScheduleStatus.DRAFT);
        when(classScheduleRepository.findById(300L)).thenReturn(Optional.of(sessionA));

        assertThatThrownBy(() -> service.findSwapCandidates(300L, date))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("published");
    }

    @Test
    void shouldApplySwapAndCreateLinkedOccurrencesWithoutMutatingClassSchedule() {
        when(classScheduleRepository.findById(300L)).thenReturn(Optional.of(sessionA));
        when(classScheduleRepository.findById(301L)).thenReturn(Optional.of(sessionB));
        when(occurrenceService.occurrenceDatesFor(sessionA, date, date)).thenReturn(List.of(date));
        when(occurrenceService.occurrenceDatesFor(sessionB, date, date)).thenReturn(List.of(date));
        when(classScheduleRepository.findByTermInstanceIdAndStatusAndDayOfWeek(10L, ClassScheduleStatus.PUBLISHED, DayOfWeek.MONDAY))
            .thenReturn(List.of(sessionA, sessionB));
        when(facultyAvailabilityRepository.findOverlapping(any(), any(), any(), any())).thenReturn(Collections.emptyList());
        when(classScheduleRepository.findOverlapping(any(), any(), any(), any(), any(), any())).thenReturn(Collections.emptyList());
        when(sessionOccurrenceRepository.findByClassScheduleIdAndOccurrenceDate(300L, date)).thenReturn(Optional.empty());
        when(sessionOccurrenceRepository.findByClassScheduleIdAndOccurrenceDate(301L, date)).thenReturn(Optional.empty());
        when(sessionOccurrenceRepository.save(any(SessionOccurrence.class))).thenAnswer(inv -> {
            SessionOccurrence occ = inv.getArgument(0);
            if (occ.getId() == null) occ.setId(occ.getClassSchedule().getId() + 1000);
            return occ;
        });

        service.applySwap(300L, 301L, date, "admin");

        assertThat(sessionA.getFaculty().getId()).isEqualTo(1L);
        assertThat(sessionB.getFaculty().getId()).isEqualTo(2L);
    }
}
