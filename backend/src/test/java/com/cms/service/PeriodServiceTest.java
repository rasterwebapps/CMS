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
}
