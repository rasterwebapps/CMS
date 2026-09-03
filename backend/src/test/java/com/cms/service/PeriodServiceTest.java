package com.cms.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.time.LocalTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.cms.dto.PeriodRequest;
import com.cms.dto.PeriodResponse;
import com.cms.model.Period;
import com.cms.repository.PeriodRepository;

@ExtendWith(MockitoExtension.class)
class PeriodServiceTest {

    @Mock
    private PeriodRepository periodRepository;

    private PeriodService periodService;

    @BeforeEach
    void setUp() {
        periodService = new PeriodService(periodRepository);
    }

    @Test
    void shouldDeriveEndTimeFromStartTimeAndDuration() {
        PeriodRequest request = new PeriodRequest("1st Period", LocalTime.of(9, 0), 50, 1, true);
        when(periodRepository.existsByNameIgnoreCase("1st Period")).thenReturn(false);
        when(periodRepository.save(any(Period.class))).thenAnswer(inv -> inv.getArgument(0));

        PeriodResponse response = periodService.create(request);

        assertThat(response.startTime()).isEqualTo(LocalTime.of(9, 0));
        assertThat(response.endTime()).isEqualTo(LocalTime.of(9, 50));
        assertThat(response.durationMinutes()).isEqualTo(50);
    }

    @Test
    void shouldRejectDurationThatCrossesMidnight() {
        PeriodRequest request = new PeriodRequest("Late Period", LocalTime.of(23, 30), 90, 1, true);
        when(periodRepository.existsByNameIgnoreCase("Late Period")).thenReturn(false);

        assertThatThrownBy(() -> periodService.create(request))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("cross midnight");
    }

    @Test
    void shouldRejectDuplicateNameOnCreate() {
        PeriodRequest request = new PeriodRequest("1st Period", LocalTime.of(9, 0), 50, 1, true);
        when(periodRepository.existsByNameIgnoreCase("1st Period")).thenReturn(true);

        assertThatThrownBy(() -> periodService.create(request))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("already exists");
    }

    @Test
    void shouldUpdateDurationAndRecomputeEndTime() {
        Period existing = new Period("1st Period", LocalTime.of(9, 0), LocalTime.of(9, 50), 1);
        existing.setId(1L);
        existing.setDurationMinutes(50);
        PeriodRequest request = new PeriodRequest("1st Period", LocalTime.of(9, 0), 45, 1, true);

        when(periodRepository.findById(1L)).thenReturn(java.util.Optional.of(existing));
        when(periodRepository.existsByNameIgnoreCaseAndIdNot("1st Period", 1L)).thenReturn(false);
        when(periodRepository.save(any(Period.class))).thenAnswer(inv -> inv.getArgument(0));

        PeriodResponse response = periodService.update(1L, request);

        assertThat(response.endTime()).isEqualTo(LocalTime.of(9, 45));
        assertThat(response.durationMinutes()).isEqualTo(45);
    }

    /** The real incident behind {@code PeriodService#requireNoActiveOverlap} and V415: Period 1 was
     *  widened 09:00-09:50 -> 09:00-10:00 while Period 2 kept starting at 09:50, and nothing
     *  rejected it. Downstream room/faculty checks compare real time ranges, so the two grid
     *  columns then clashed with each other. */
    @Test
    void shouldRejectAnUpdateThatOverlapsAnotherActivePeriod() {
        Period first = new Period("Period 1", LocalTime.of(9, 0), LocalTime.of(9, 50), 1);
        first.setId(1L);
        first.setDurationMinutes(50);
        Period second = new Period("Period 2", LocalTime.of(9, 50), LocalTime.of(10, 40), 2);
        second.setId(2L);
        second.setDurationMinutes(50);

        PeriodRequest widenFirstToAnHour = new PeriodRequest("Period 1", LocalTime.of(9, 0), 60, 1, true);
        when(periodRepository.findById(1L)).thenReturn(java.util.Optional.of(first));
        when(periodRepository.existsByNameIgnoreCaseAndIdNot("Period 1", 1L)).thenReturn(false);
        when(periodRepository.findByIsActiveTrueOrderByPeriodOrderAsc()).thenReturn(java.util.List.of(first, second));

        assertThatThrownBy(() -> periodService.update(1L, widenFirstToAnHour))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("overlaps 'Period 2'");
    }

    /** Back-to-back is the normal grid shape, not an overlap — one period ending exactly when the
     *  next starts must stay saveable, and a period never counts as overlapping itself. */
    @Test
    void shouldAllowAnUpdateThatEndsExactlyWhenTheNextPeriodStarts() {
        Period first = new Period("Period 1", LocalTime.of(9, 0), LocalTime.of(10, 0), 1);
        first.setId(1L);
        first.setDurationMinutes(60);
        Period second = new Period("Period 2", LocalTime.of(9, 50), LocalTime.of(10, 40), 2);
        second.setId(2L);
        second.setDurationMinutes(50);

        PeriodRequest pullFirstBack = new PeriodRequest("Period 1", LocalTime.of(9, 0), 50, 1, true);
        when(periodRepository.findById(1L)).thenReturn(java.util.Optional.of(first));
        when(periodRepository.existsByNameIgnoreCaseAndIdNot("Period 1", 1L)).thenReturn(false);
        when(periodRepository.findByIsActiveTrueOrderByPeriodOrderAsc()).thenReturn(java.util.List.of(first, second));
        when(periodRepository.save(any(Period.class))).thenAnswer(inv -> inv.getArgument(0));

        PeriodResponse response = periodService.update(1L, pullFirstBack);

        assertThat(response.endTime()).isEqualTo(LocalTime.of(9, 50));
    }

    /** Retired rows legitimately overlap the live grid — the old standalone Lab Slot master rows
     *  (inactive since V331) still span Periods 1-3 — so an INACTIVE period is exempt on both
     *  sides, or no real period could ever be saved again. */
    @Test
    void shouldIgnoreInactivePeriodsWhenCheckingOverlap() {
        Period activeFirst = new Period("Period 1", LocalTime.of(9, 0), LocalTime.of(9, 50), 1);
        activeFirst.setId(1L);
        activeFirst.setDurationMinutes(50);

        PeriodRequest retiredLabSlot = new PeriodRequest("Lab Slot 1", LocalTime.of(9, 0), 120, 2, false);
        when(periodRepository.existsByNameIgnoreCase("Lab Slot 1")).thenReturn(false);
        when(periodRepository.save(any(Period.class))).thenAnswer(inv -> inv.getArgument(0));

        PeriodResponse response = periodService.create(retiredLabSlot);

        assertThat(response.endTime()).isEqualTo(LocalTime.of(11, 0));
        assertThat(response.isActive()).isFalse();
    }
}
