package com.cms.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.cms.model.AcademicYear;
import com.cms.model.BlockedPeriod;
import com.cms.model.ClassSchedule;
import com.cms.model.Period;
import com.cms.model.TermInstance;
import com.cms.model.enums.BlockType;
import com.cms.model.enums.DayOfWeek;
import com.cms.model.enums.TermInstanceStatus;
import com.cms.model.enums.TermType;
import com.cms.repository.BlockedPeriodRepository;

@ExtendWith(MockitoExtension.class)
class ClassScheduleOccurrenceServiceTest {

    @Mock
    private BlockedPeriodRepository blockedPeriodRepository;

    private ClassScheduleOccurrenceService service;
    private AcademicYear academicYear;
    private TermInstance termInstance;
    private Period period;

    @BeforeEach
    void setUp() {
        service = new ClassScheduleOccurrenceService(blockedPeriodRepository);

        academicYear = new AcademicYear("2024-2025", LocalDate.of(2024, 6, 1), LocalDate.of(2025, 5, 31), false);
        academicYear.setId(1L);
        academicYear.setCreatedAt(Instant.now());
        academicYear.setUpdatedAt(Instant.now());

        // Monday 2024-08-05 through Monday 2024-08-26 -- 4 Mondays inclusive.
        termInstance = new TermInstance(academicYear, TermType.ODD,
            LocalDate.of(2024, 8, 5), LocalDate.of(2024, 8, 26), TermInstanceStatus.OPEN);
        termInstance.setId(10L);
        termInstance.setCreatedAt(Instant.now());
        termInstance.setUpdatedAt(Instant.now());

        period = new Period();
        period.setId(50L);
    }

    private ClassSchedule mondaySchedule() {
        ClassSchedule schedule = new ClassSchedule();
        schedule.setId(100L);
        schedule.setTermInstance(termInstance);
        schedule.setDayOfWeek(DayOfWeek.MONDAY);
        schedule.setPeriod(period);
        return schedule;
    }

    @Test
    void shouldReturnEveryMatchingWeekdayWithinTermBounds() {
        when(blockedPeriodRepository.findApplicableForPeriodInRange(anyLong(), any(), any()))
            .thenReturn(Collections.emptyList());

        List<LocalDate> dates = service.occurrenceDatesFor(
            mondaySchedule(), LocalDate.of(2024, 1, 1), LocalDate.of(2024, 12, 31));

        assertThat(dates).containsExactly(
            LocalDate.of(2024, 8, 5), LocalDate.of(2024, 8, 12),
            LocalDate.of(2024, 8, 19), LocalDate.of(2024, 8, 26));
    }

    @Test
    void shouldClampToCallerSuppliedWindow() {
        when(blockedPeriodRepository.findApplicableForPeriodInRange(anyLong(), any(), any()))
            .thenReturn(Collections.emptyList());

        List<LocalDate> dates = service.occurrenceDatesFor(
            mondaySchedule(), LocalDate.of(2024, 8, 10), LocalDate.of(2024, 8, 20));

        assertThat(dates).containsExactly(LocalDate.of(2024, 8, 12), LocalDate.of(2024, 8, 19));
    }

    @Test
    void shouldExcludeDatesCoveredByAOneOffBlock() {
        BlockedPeriod independenceDayHoliday = new BlockedPeriod();
        independenceDayHoliday.setPeriod(period);
        independenceDayHoliday.setBlockType(BlockType.ONE_OFF);
        independenceDayHoliday.setSpecificDate(LocalDate.of(2024, 8, 12));
        when(blockedPeriodRepository.findApplicableForPeriodInRange(
                50L, LocalDate.of(2024, 8, 5), LocalDate.of(2024, 8, 26)))
            .thenReturn(List.of(independenceDayHoliday));

        List<LocalDate> dates = service.occurrenceDatesFor(
            mondaySchedule(), LocalDate.of(2024, 1, 1), LocalDate.of(2024, 12, 31));

        assertThat(dates).containsExactly(
            LocalDate.of(2024, 8, 5), LocalDate.of(2024, 8, 19), LocalDate.of(2024, 8, 26));
    }

    @Test
    void shouldExcludeDatesCoveredByARecurringBlock() {
        BlockedPeriod standingMeeting = new BlockedPeriod();
        standingMeeting.setPeriod(period);
        standingMeeting.setBlockType(BlockType.RECURRING);
        standingMeeting.setDayOfWeek(DayOfWeek.MONDAY);
        standingMeeting.setRangeStartDate(LocalDate.of(2024, 8, 19));
        standingMeeting.setRangeEndDate(LocalDate.of(2024, 8, 19));
        when(blockedPeriodRepository.findApplicableForPeriodInRange(
                50L, LocalDate.of(2024, 8, 5), LocalDate.of(2024, 8, 26)))
            .thenReturn(List.of(standingMeeting));

        List<LocalDate> dates = service.occurrenceDatesFor(
            mondaySchedule(), LocalDate.of(2024, 1, 1), LocalDate.of(2024, 12, 31));

        assertThat(dates).containsExactly(
            LocalDate.of(2024, 8, 5), LocalDate.of(2024, 8, 12), LocalDate.of(2024, 8, 26));
    }

    @Test
    void shouldReturnEmptyWhenWindowIsBeforeTermStarts() {
        List<LocalDate> dates = service.occurrenceDatesFor(
            mondaySchedule(), LocalDate.of(2024, 1, 1), LocalDate.of(2024, 1, 31));

        assertThat(dates).isEmpty();
    }

    @Test
    void batchedVariantShouldReuseBlockLookupPerPeriod() {
        when(blockedPeriodRepository.findApplicableForPeriodInRange(anyLong(), any(), any()))
            .thenReturn(Collections.emptyList());

        ClassSchedule first = mondaySchedule();
        ClassSchedule second = mondaySchedule();
        second.setId(200L);

        Map<Long, List<LocalDate>> result = service.occurrenceDatesForSchedules(
            List.of(first, second), LocalDate.of(2024, 8, 1), LocalDate.of(2024, 8, 31));

        assertThat(result.get(100L)).hasSize(4);
        assertThat(result.get(200L)).hasSize(4);
        org.mockito.Mockito.verify(blockedPeriodRepository, org.mockito.Mockito.times(1))
            .findApplicableForPeriodInRange(anyLong(), any(), any());
    }

    // ── cancelledDatesForSchedules (purely additive -- occurrenceDatesFor/occurrenceDatesForSchedules above are untouched) ──

    @Test
    void cancelledDatesShouldReturnTheBlockedDateWithItsReason() {
        BlockedPeriod independenceDayHoliday = new BlockedPeriod();
        independenceDayHoliday.setPeriod(period);
        independenceDayHoliday.setBlockType(BlockType.ONE_OFF);
        independenceDayHoliday.setSpecificDate(LocalDate.of(2024, 8, 12));
        independenceDayHoliday.setReason("Independence Day");
        when(blockedPeriodRepository.findApplicableForPeriodInRange(anyLong(), any(), any()))
            .thenReturn(List.of(independenceDayHoliday));

        Map<Long, List<ClassScheduleOccurrenceService.CancelledOccurrence>> result =
            service.cancelledDatesForSchedules(List.of(mondaySchedule()), LocalDate.of(2024, 1, 1), LocalDate.of(2024, 12, 31));

        assertThat(result.get(100L)).hasSize(1);
        assertThat(result.get(100L).get(0).date()).isEqualTo(LocalDate.of(2024, 8, 12));
        assertThat(result.get(100L).get(0).reason()).isEqualTo("Independence Day");
    }

    @Test
    void cancelledDatesShouldBeEmptyWhenNothingIsBlocked() {
        when(blockedPeriodRepository.findApplicableForPeriodInRange(anyLong(), any(), any()))
            .thenReturn(Collections.emptyList());

        Map<Long, List<ClassScheduleOccurrenceService.CancelledOccurrence>> result =
            service.cancelledDatesForSchedules(List.of(mondaySchedule()), LocalDate.of(2024, 1, 1), LocalDate.of(2024, 12, 31));

        assertThat(result.get(100L)).isEmpty();
    }

    @Test
    void cancelledAndHeldDatesShouldBeComplementarySets() {
        BlockedPeriod standingMeeting = new BlockedPeriod();
        standingMeeting.setPeriod(period);
        standingMeeting.setBlockType(BlockType.RECURRING);
        standingMeeting.setDayOfWeek(DayOfWeek.MONDAY);
        standingMeeting.setRangeStartDate(LocalDate.of(2024, 8, 19));
        standingMeeting.setRangeEndDate(LocalDate.of(2024, 8, 19));
        standingMeeting.setReason("Staff meeting");
        when(blockedPeriodRepository.findApplicableForPeriodInRange(anyLong(), any(), any()))
            .thenReturn(List.of(standingMeeting));

        List<LocalDate> held = service.occurrenceDatesFor(
            mondaySchedule(), LocalDate.of(2024, 1, 1), LocalDate.of(2024, 12, 31));
        Map<Long, List<ClassScheduleOccurrenceService.CancelledOccurrence>> cancelled =
            service.cancelledDatesForSchedules(List.of(mondaySchedule()), LocalDate.of(2024, 1, 1), LocalDate.of(2024, 12, 31));

        assertThat(held).containsExactly(
            LocalDate.of(2024, 8, 5), LocalDate.of(2024, 8, 12), LocalDate.of(2024, 8, 26));
        assertThat(cancelled.get(100L)).extracting(ClassScheduleOccurrenceService.CancelledOccurrence::date)
            .containsExactly(LocalDate.of(2024, 8, 19));
    }
}
