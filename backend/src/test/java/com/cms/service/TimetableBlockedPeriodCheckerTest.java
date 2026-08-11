package com.cms.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
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
import com.cms.model.enums.BlockType;
import com.cms.model.enums.DayOfWeek;
import com.cms.repository.BlockedPeriodRepository;

@ExtendWith(MockitoExtension.class)
class TimetableBlockedPeriodCheckerTest {

    @Mock private BlockedPeriodRepository blockedPeriodRepository;

    private TimetableBlockedPeriodChecker checker;

    private final LocalDate termStart = LocalDate.of(2024, 6, 1);
    private final LocalDate termEnd = LocalDate.of(2024, 11, 30);

    @BeforeEach
    void setUp() {
        checker = new TimetableBlockedPeriodChecker(blockedPeriodRepository);
    }

    @Test
    void shouldReturnEmptyWhenNothingBlocksThisSlot() {
        when(blockedPeriodRepository.findOverlappingRecurringBlocks(DayOfWeek.MONDAY, 1L, termStart, termEnd))
            .thenReturn(Collections.emptyList());
        when(blockedPeriodRepository.findHolidayOneOffBlocksInRange(1L, termStart, termEnd))
            .thenReturn(Collections.emptyList());

        assertThat(checker.blockReason(DayOfWeek.MONDAY, 1L, termStart, termEnd)).isEmpty();
    }

    @Test
    void shouldReturnTheReasonForARecurringBlock() {
        BlockedPeriod block = new BlockedPeriod();
        block.setBlockType(BlockType.RECURRING);
        block.setReason("Staff meeting");
        when(blockedPeriodRepository.findOverlappingRecurringBlocks(DayOfWeek.MONDAY, 1L, termStart, termEnd))
            .thenReturn(List.of(block));

        assertThat(checker.blockReason(DayOfWeek.MONDAY, 1L, termStart, termEnd)).contains("Staff meeting");
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

        when(blockedPeriodRepository.findOverlappingRecurringBlocks(DayOfWeek.MONDAY, 1L, termStart, termEnd))
            .thenReturn(Collections.emptyList());
        when(blockedPeriodRepository.findHolidayOneOffBlocksInRange(1L, termStart, termEnd))
            .thenReturn(List.of(block));

        assertThat(checker.blockReason(DayOfWeek.MONDAY, 1L, termStart, termEnd))
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

        when(blockedPeriodRepository.findOverlappingRecurringBlocks(DayOfWeek.TUESDAY, 1L, termStart, termEnd))
            .thenReturn(Collections.emptyList());
        when(blockedPeriodRepository.findHolidayOneOffBlocksInRange(1L, termStart, termEnd))
            .thenReturn(List.of(block));

        assertThat(checker.blockReason(DayOfWeek.TUESDAY, 1L, termStart, termEnd)).isEmpty();
    }

    @Test
    void shouldPreferTheRecurringBlockReasonWhenBothTypesApply() {
        BlockedPeriod recurring = new BlockedPeriod();
        recurring.setBlockType(BlockType.RECURRING);
        recurring.setReason("Recurring lock");
        when(blockedPeriodRepository.findOverlappingRecurringBlocks(DayOfWeek.MONDAY, 1L, termStart, termEnd))
            .thenReturn(List.of(recurring));

        assertThat(checker.blockReason(DayOfWeek.MONDAY, 1L, termStart, termEnd)).contains("Recurring lock");
    }
}
