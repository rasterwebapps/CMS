package com.cms.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.cms.dto.ClassScheduleOccurrenceResponse;
import com.cms.dto.ClassScheduleResponse;
import com.cms.model.ClassSchedule;
import com.cms.model.Faculty;
import com.cms.model.SessionOccurrence;
import com.cms.model.enums.ClassScheduleStatus;
import com.cms.model.enums.ClassSessionType;
import com.cms.model.enums.DayOfWeek;
import com.cms.model.enums.OccurrenceStatus;
import com.cms.repository.ClassScheduleRepository;
import com.cms.repository.SessionOccurrenceRepository;

@ExtendWith(MockitoExtension.class)
class TimetableOccurrenceServiceTest {

    @Mock private ClassScheduleRepository classScheduleRepository;
    @Mock private ClassScheduleService classScheduleService;
    @Mock private ClassScheduleOccurrenceService occurrenceService;
    @Mock private PersonalTimetableService personalTimetableService;
    @Mock private SessionOccurrenceRepository sessionOccurrenceRepository;
    @Mock private TimetableSkeletonService timetableSkeletonService;

    private TimetableOccurrenceService service;
    private ClassSchedule schedule;
    private ClassScheduleResponse response;

    @BeforeEach
    void setUp() {
        service = new TimetableOccurrenceService(classScheduleRepository, classScheduleService,
            occurrenceService, personalTimetableService, sessionOccurrenceRepository, timetableSkeletonService);

        schedule = new ClassSchedule();
        schedule.setId(100L);

        response = new ClassScheduleResponse(100L, ClassSessionType.THEORY,
            ClassScheduleStatus.PUBLISHED, null, null, 1L, "Anatomy", "ANAT101", 1L, "John Doe",
            1L, "1st Period", LocalTime.of(9, 0), LocalTime.of(10, 0), null, null, 1L, null, "Room 101",
            1L, null, DayOfWeek.MONDAY, 10L, "ODD 2026", true, Instant.now(), Instant.now());

        when(sessionOccurrenceRepository.findByClassSchedule_TermInstance_IdAndClassSchedule_Status(
            10L, ClassScheduleStatus.PUBLISHED)).thenReturn(List.of());
    }

    @Test
    void shouldMergeHeldAndCancelledOccurrencesSortedByDate() {
        when(classScheduleRepository.findByTermInstanceIdAndStatusAndIsActiveTrue(10L, ClassScheduleStatus.PUBLISHED))
            .thenReturn(List.of(schedule));
        when(occurrenceService.occurrenceDatesForSchedules(List.of(schedule), LocalDate.of(2024, 8, 1), LocalDate.of(2024, 8, 31)))
            .thenReturn(Map.of(100L, List.of(LocalDate.of(2024, 8, 5), LocalDate.of(2024, 8, 19))));
        when(occurrenceService.cancelledDatesForSchedules(List.of(schedule), LocalDate.of(2024, 8, 1), LocalDate.of(2024, 8, 31)))
            .thenReturn(Map.of(100L, List.of(new ClassScheduleOccurrenceService.CancelledOccurrence(
                LocalDate.of(2024, 8, 12), "Independence Day"))));
        when(classScheduleService.toResponseList(List.of(schedule))).thenReturn(List.of(response));

        List<ClassScheduleOccurrenceResponse> result = service.findOccurrences(
            null, 10L, LocalDate.of(2024, 8, 1), LocalDate.of(2024, 8, 31), "browse");

        assertThat(result).hasSize(3);
        assertThat(result).extracting(ClassScheduleOccurrenceResponse::date).containsExactly(
            LocalDate.of(2024, 8, 5), LocalDate.of(2024, 8, 12), LocalDate.of(2024, 8, 19));
        assertThat(result.get(0).occurrenceStatus()).isEqualTo(OccurrenceStatus.HELD);
        assertThat(result.get(0).cancelReason()).isNull();
        assertThat(result.get(1).occurrenceStatus()).isEqualTo(OccurrenceStatus.CANCELLED);
        assertThat(result.get(1).cancelReason()).isEqualTo("Independence Day");
        assertThat(result.get(2).occurrenceStatus()).isEqualTo(OccurrenceStatus.HELD);
    }

    @Test
    void shouldReturnOnlyHeldWhenNothingIsCancelled() {
        when(classScheduleRepository.findByTermInstanceIdAndStatusAndIsActiveTrue(10L, ClassScheduleStatus.PUBLISHED))
            .thenReturn(List.of(schedule));
        when(occurrenceService.occurrenceDatesForSchedules(List.of(schedule), LocalDate.of(2024, 8, 1), LocalDate.of(2024, 8, 31)))
            .thenReturn(Map.of(100L, List.of(LocalDate.of(2024, 8, 5))));
        when(occurrenceService.cancelledDatesForSchedules(List.of(schedule), LocalDate.of(2024, 8, 1), LocalDate.of(2024, 8, 31)))
            .thenReturn(Map.of(100L, List.of()));
        when(classScheduleService.toResponseList(List.of(schedule))).thenReturn(List.of(response));

        List<ClassScheduleOccurrenceResponse> result = service.findOccurrences(
            null, 10L, LocalDate.of(2024, 8, 1), LocalDate.of(2024, 8, 31), "browse");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).occurrenceStatus()).isEqualTo(OccurrenceStatus.HELD);
    }

    @Test
    void shouldOverrideFacultyOnSubstitutedDateOnly() {
        Faculty substitute = new Faculty();
        substitute.setId(9L);
        substitute.setFirstName("Jane");
        substitute.setLastName("Sub");

        SessionOccurrence substituted = new SessionOccurrence(schedule, LocalDate.of(2024, 8, 19));
        substituted.setOccurrenceStatus(OccurrenceStatus.SUBSTITUTED);
        substituted.setEffectiveFaculty(substitute);

        when(classScheduleRepository.findByTermInstanceIdAndStatusAndIsActiveTrue(10L, ClassScheduleStatus.PUBLISHED))
            .thenReturn(List.of(schedule));
        when(occurrenceService.occurrenceDatesForSchedules(List.of(schedule), LocalDate.of(2024, 8, 1), LocalDate.of(2024, 8, 31)))
            .thenReturn(Map.of(100L, List.of(LocalDate.of(2024, 8, 5), LocalDate.of(2024, 8, 19))));
        when(occurrenceService.cancelledDatesForSchedules(List.of(schedule), LocalDate.of(2024, 8, 1), LocalDate.of(2024, 8, 31)))
            .thenReturn(Map.of(100L, List.of()));
        when(classScheduleService.toResponseList(List.of(schedule))).thenReturn(List.of(response));
        when(sessionOccurrenceRepository.findByClassSchedule_TermInstance_IdAndClassSchedule_Status(
            10L, ClassScheduleStatus.PUBLISHED)).thenReturn(List.of(substituted));

        List<ClassScheduleOccurrenceResponse> result = service.findOccurrences(
            null, 10L, LocalDate.of(2024, 8, 1), LocalDate.of(2024, 8, 31), "browse");

        assertThat(result).hasSize(2);
        assertThat(result.get(0).date()).isEqualTo(LocalDate.of(2024, 8, 5));
        assertThat(result.get(0).occurrenceStatus()).isEqualTo(OccurrenceStatus.HELD);
        assertThat(result.get(0).session().facultyName()).isEqualTo("John Doe");

        assertThat(result.get(1).date()).isEqualTo(LocalDate.of(2024, 8, 19));
        assertThat(result.get(1).occurrenceStatus()).isEqualTo(OccurrenceStatus.SUBSTITUTED);
        assertThat(result.get(1).session().facultyId()).isEqualTo(9L);
        assertThat(result.get(1).session().facultyName()).isEqualTo("Jane Sub");
        // Non-faculty fields must pass through unchanged from the original response.
        assertThat(result.get(1).session().subjectName()).isEqualTo("Anatomy");
        assertThat(result.get(1).session().roomName()).isEqualTo("Room 101");
    }

    /** CLINICAL hours are delivered off-grid via ClinicalShiftGroup/Batch and never produce a real
     *  ClassSchedule row -- without exploding TimetableSkeletonService's synthetic weekly template
     *  onto real calendar dates too, this endpoint (unlike the published/draft list) silently drops
     *  every CLINICAL session from the Date-wise/Day calendar views. */
    @Test
    void shouldExplodeClinicalShiftTemplatesOntoMatchingCalendarDates() {
        when(classScheduleRepository.findByTermInstanceIdAndStatusAndIsActiveTrue(10L, ClassScheduleStatus.PUBLISHED))
            .thenReturn(List.of());
        when(occurrenceService.occurrenceDatesForSchedules(List.of(), LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 6)))
            .thenReturn(Map.of());
        when(occurrenceService.cancelledDatesForSchedules(List.of(), LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 6)))
            .thenReturn(Map.of());
        when(classScheduleService.toResponseList(List.of())).thenReturn(List.of());

        ClassScheduleResponse clinicalTemplate = new ClassScheduleResponse(-1000068L, ClassSessionType.CLINICAL,
            ClassScheduleStatus.PUBLISHED, null, null, 39L, "Adult Health Nursing I — Off-campus Clinical Shift",
            "N-AHN-I-215", 37L, "Sneha Rao", null, "Shift A - AHN", LocalTime.of(6, 0), LocalTime.of(14, 10),
            "Clinical - Section 1", 68L, null, 1L, "Ward 1 - Medical", 68L, null, DayOfWeek.MONDAY, 10L, null, true, null, null);
        when(timetableSkeletonService.findClinicalShiftGridEntries(10L, ClassScheduleStatus.PUBLISHED, null))
            .thenReturn(List.of(clinicalTemplate));

        // 2026-10-01 is a Thursday; the only Monday in this Mon-Sat window is 2026-10-05.
        List<ClassScheduleOccurrenceResponse> result = service.findOccurrences(
            null, 10L, LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 6), "browse");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).date()).isEqualTo(LocalDate.of(2026, 10, 5));
        assertThat(result.get(0).occurrenceStatus()).isEqualTo(OccurrenceStatus.HELD);
        assertThat(result.get(0).session().sessionType()).isEqualTo(ClassSessionType.CLINICAL);
        assertThat(result.get(0).session().id()).isEqualTo(-1000068L);
    }

    @Test
    void shouldNotExplodeClinicalShiftTemplatesForPersonalScope() {
        when(personalTimetableService.findPublishedSchedules(null, 10L)).thenReturn(List.of());
        when(occurrenceService.occurrenceDatesForSchedules(List.of(), LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 6)))
            .thenReturn(Map.of());
        when(occurrenceService.cancelledDatesForSchedules(List.of(), LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 6)))
            .thenReturn(Map.of());
        when(classScheduleService.toResponseList(List.of())).thenReturn(List.of());

        List<ClassScheduleOccurrenceResponse> result = service.findOccurrences(
            null, 10L, LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 6), "personal");

        assertThat(result).isEmpty();
    }
}
