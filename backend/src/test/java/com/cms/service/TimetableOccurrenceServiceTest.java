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

    private TimetableOccurrenceService service;
    private ClassSchedule schedule;
    private ClassScheduleResponse response;

    @BeforeEach
    void setUp() {
        service = new TimetableOccurrenceService(classScheduleRepository, classScheduleService,
            occurrenceService, personalTimetableService, sessionOccurrenceRepository);

        schedule = new ClassSchedule();
        schedule.setId(100L);

        response = new ClassScheduleResponse(100L, ClassSessionType.THEORY,
            ClassScheduleStatus.PUBLISHED, null, null, 1L, "Anatomy", "ANAT101", 1L, "John Doe",
            1L, "1st Period", LocalTime.of(9, 0), LocalTime.of(10, 0), null, null, 1L, null, "Room 101",
            1L, DayOfWeek.MONDAY, 10L, "ODD 2026", true, Instant.now(), Instant.now());

        when(sessionOccurrenceRepository.findByClassSchedule_TermInstance_IdAndClassSchedule_Status(
            10L, ClassScheduleStatus.PUBLISHED)).thenReturn(List.of());
    }

    @Test
    void shouldMergeHeldAndCancelledOccurrencesSortedByDate() {
        when(classScheduleRepository.findByTermInstanceIdAndStatus(10L, ClassScheduleStatus.PUBLISHED))
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
        when(classScheduleRepository.findByTermInstanceIdAndStatus(10L, ClassScheduleStatus.PUBLISHED))
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

        when(classScheduleRepository.findByTermInstanceIdAndStatus(10L, ClassScheduleStatus.PUBLISHED))
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
}
