package com.cms.service;

import static org.assertj.core.api.Assertions.assertThat;
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

import com.cms.model.BlockedPeriod;
import com.cms.model.CalendarEvent;
import com.cms.model.TermInstance;
import com.cms.model.enums.BlockType;
import com.cms.model.enums.DayOfWeek;
import com.cms.model.enums.WeekOfMonth;
import com.cms.repository.BlockedPeriodRepository;

@ExtendWith(MockitoExtension.class)
class TimetableBlockedPeriodCheckerTest {

    @Mock private BlockedPeriodRepository blockedPeriodRepository;

    private TimetableBlockedPeriodChecker checker;

    private final LocalDate termStart = LocalDate.of(2024, 6, 1);
    private final LocalDate termEnd = LocalDate.of(2024, 11, 30);
    private final LocalTime slotStart = LocalTime.of(9, 0);
    private final LocalTime slotEnd = LocalTime.of(10, 0);
    private TermInstance term;

    @BeforeEach
    void setUp() {
        checker = new TimetableBlockedPeriodChecker(blockedPeriodRepository);
        term = new TermInstance();
        term.setStartDate(termStart);
        term.setEndDate(termEnd);
        // Non-empty so the Saturday opt-in gate never interferes with these Mon/Tue tests.
        term.setWorkingSaturdayWeeks(java.util.Set.of(WeekOfMonth.FIRST));
    }

    @Test
    void shouldReturnEmptyWhenNothingBlocksThisSlot() {
        when(blockedPeriodRepository.findOverlappingRecurringBlocks(DayOfWeek.MONDAY, slotStart, slotEnd, termStart, termEnd))
            .thenReturn(Collections.emptyList());
        when(blockedPeriodRepository.findHolidayOneOffBlocksInRange(slotStart, slotEnd, termStart, termEnd))
            .thenReturn(Collections.emptyList());

        assertThat(checker.blockReason(DayOfWeek.MONDAY, slotStart, slotEnd, term)).isEmpty();
    }

    @Test
    void shouldReturnTheReasonForARecurringBlock() {
        BlockedPeriod block = new BlockedPeriod();
        block.setBlockType(BlockType.RECURRING);
        block.setReason("Staff meeting");
        when(blockedPeriodRepository.findOverlappingRecurringBlocks(DayOfWeek.MONDAY, slotStart, slotEnd, termStart, termEnd))
            .thenReturn(List.of(block));

        assertThat(checker.blockReason(DayOfWeek.MONDAY, slotStart, slotEnd, term)).contains("Staff meeting");
    }

    @Test
    void shouldReturnTheReasonForAHolidayDerivedOneOffBlockOnTheMatchingWeekday() {
        CalendarEvent holiday = new CalendarEvent();
        holiday.setTitle("Independence Day");
        BlockedPeriod block = new BlockedPeriod();
        block.setBlockType(BlockType.ONE_OFF);
        block.setSpecificDate(LocalDate.of(2024, 8, 5)); // a Monday
        block.setReason("Auto-blocked — Independence Day");
        block.setSourceCalendarEvent(holiday);

        when(blockedPeriodRepository.findOverlappingRecurringBlocks(DayOfWeek.MONDAY, slotStart, slotEnd, termStart, termEnd))
            .thenReturn(Collections.emptyList());
        when(blockedPeriodRepository.findHolidayOneOffBlocksInRange(slotStart, slotEnd, termStart, termEnd))
            .thenReturn(List.of(block));

        assertThat(checker.blockReason(DayOfWeek.MONDAY, slotStart, slotEnd, term))
            .contains("Auto-blocked — Independence Day");
    }

    @Test
    void shouldIgnoreAHolidayDerivedOneOffBlockOnADifferentWeekday() {
        CalendarEvent holiday = new CalendarEvent();
        BlockedPeriod block = new BlockedPeriod();
        block.setBlockType(BlockType.ONE_OFF);
        block.setSpecificDate(LocalDate.of(2024, 8, 5)); // a Monday
        block.setReason("Auto-blocked");
        block.setSourceCalendarEvent(holiday);

        when(blockedPeriodRepository.findOverlappingRecurringBlocks(DayOfWeek.TUESDAY, slotStart, slotEnd, termStart, termEnd))
            .thenReturn(Collections.emptyList());
        when(blockedPeriodRepository.findHolidayOneOffBlocksInRange(slotStart, slotEnd, termStart, termEnd))
            .thenReturn(List.of(block));

        assertThat(checker.blockReason(DayOfWeek.TUESDAY, slotStart, slotEnd, term)).isEmpty();
    }

    @Test
    void shouldPreferTheRecurringBlockReasonWhenBothTypesApply() {
        BlockedPeriod recurring = new BlockedPeriod();
        recurring.setBlockType(BlockType.RECURRING);
        recurring.setReason("Recurring lock");
        when(blockedPeriodRepository.findOverlappingRecurringBlocks(DayOfWeek.MONDAY, slotStart, slotEnd, termStart, termEnd))
            .thenReturn(List.of(recurring));

        assertThat(checker.blockReason(DayOfWeek.MONDAY, slotStart, slotEnd, term)).contains("Recurring lock");
    }

    @Test
    void shouldDetectAConflictWhenACandidateWindowOverlapsAWiderBlockedPeriod() {
        // A "combined double-period" session running 9:00-11:00 must still be caught by a
        // BlockedPeriod tied to a plain 10:00-10:50 Period row it merely overlaps — this is the
        // whole point of matching by clock-time overlap rather than period-id equality.
        LocalTime combinedStart = LocalTime.of(9, 0);
        LocalTime combinedEnd = LocalTime.of(11, 0);
        BlockedPeriod block = new BlockedPeriod();
        block.setBlockType(BlockType.RECURRING);
        block.setReason("Assembly");
        when(blockedPeriodRepository.findOverlappingRecurringBlocks(DayOfWeek.MONDAY, combinedStart, combinedEnd, termStart, termEnd))
            .thenReturn(List.of(block));

        assertThat(checker.blockReason(DayOfWeek.MONDAY, combinedStart, combinedEnd, term))
            .contains("Assembly");
    }

    @Test
    void shouldBlockSaturdayOutrightWhenNoWorkingSaturdayPatternIsConfigured() {
        term.setWorkingSaturdayWeeks(java.util.Set.of());

        assertThat(checker.blockReason(DayOfWeek.SATURDAY, slotStart, slotEnd, term)).isPresent();
    }

    @Test
    void shouldNotBlockSaturdayAtThisCoarseCheckOnceAWorkingSaturdayPatternExists() {
        term.setWorkingSaturdayWeeks(java.util.Set.of(WeekOfMonth.FIRST));
        when(blockedPeriodRepository.findOverlappingRecurringBlocks(DayOfWeek.SATURDAY, slotStart, slotEnd, termStart, termEnd))
            .thenReturn(Collections.emptyList());
        when(blockedPeriodRepository.findHolidayOneOffBlocksInRange(slotStart, slotEnd, termStart, termEnd))
            .thenReturn(Collections.emptyList());

        // The exact date-level precision (which Saturdays actually get a real occurrence) is
        // enforced downstream by ClassScheduleOccurrenceService, not this coarse weekly check.
        assertThat(checker.blockReason(DayOfWeek.SATURDAY, slotStart, slotEnd, term)).isEmpty();
    }
}
