package com.cms.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Collections;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.cms.dto.DayMappingOverrideRequest;
import com.cms.exception.ResourceNotFoundException;
import com.cms.model.AcademicYear;
import com.cms.model.DayMappingOverride;
import com.cms.model.TermInstance;
import com.cms.model.enums.DayOfWeek;
import com.cms.model.enums.TermInstanceStatus;
import com.cms.model.enums.TermType;
import com.cms.repository.DayMappingOverrideRepository;
import com.cms.repository.TermInstanceRepository;

@ExtendWith(MockitoExtension.class)
class DayMappingOverrideServiceTest {

    @Mock private DayMappingOverrideRepository dayMappingOverrideRepository;
    @Mock private TermInstanceRepository termInstanceRepository;

    private DayMappingOverrideService service;
    private TermInstance termInstance;

    // Term: Mon 2024-08-05 .. Mon 2024-08-26. 2024-08-10 is a Saturday within this window.
    private static final LocalDate SATURDAY = LocalDate.of(2024, 8, 10);

    @BeforeEach
    void setUp() {
        service = new DayMappingOverrideService(dayMappingOverrideRepository, termInstanceRepository);

        AcademicYear academicYear = new AcademicYear("2024-2025", LocalDate.of(2024, 6, 1), LocalDate.of(2025, 5, 31), false);
        academicYear.setId(1L);
        termInstance = new TermInstance(academicYear, TermType.ODD,
            LocalDate.of(2024, 8, 5), LocalDate.of(2024, 8, 26), TermInstanceStatus.OPEN);
        termInstance.setId(10L);
        termInstance.setCreatedAt(Instant.now());
        termInstance.setUpdatedAt(Instant.now());

        lenient().when(termInstanceRepository.findById(10L)).thenReturn(Optional.of(termInstance));
    }

    private DayMappingOverrideRequest validRequest() {
        return new DayMappingOverrideRequest(10L, SATURDAY, DayOfWeek.MONDAY, "Compensatory working day");
    }

    @Test
    void shouldCreateAValidMapping() {
        when(dayMappingOverrideRepository.findByMappedDate(SATURDAY)).thenReturn(Optional.empty());
        when(dayMappingOverrideRepository.save(any(DayMappingOverride.class))).thenAnswer(inv -> {
            DayMappingOverride saved = inv.getArgument(0);
            saved.setId(1L);
            saved.setCreatedAt(Instant.now());
            saved.setUpdatedAt(Instant.now());
            return saved;
        });

        var response = service.create(validRequest());

        assertThat(response.mappedDate()).isEqualTo(SATURDAY);
        assertThat(response.borrowedDayOfWeek()).isEqualTo(DayOfWeek.MONDAY);
    }

    @Test
    void shouldRejectAMappedDateOutsideTheTermsBounds() {
        DayMappingOverrideRequest request = new DayMappingOverrideRequest(
            10L, LocalDate.of(2024, 9, 2), DayOfWeek.MONDAY, "Out of term");

        assertThatThrownBy(() -> service.create(request))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("term's bounds");
    }

    @Test
    void shouldRejectBorrowedDayOfWeekEqualToTheMappedDatesOwnWeekday() {
        // SATURDAY (2024-08-10) mapped to borrow SATURDAY itself is a meaningless no-op mapping.
        DayMappingOverrideRequest request = new DayMappingOverrideRequest(10L, SATURDAY, DayOfWeek.SATURDAY, "No-op");

        assertThatThrownBy(() -> service.create(request))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("differ from the mapped date's own weekday");
    }

    @Test
    void shouldRejectASundayMappedDate() {
        DayMappingOverrideRequest request = new DayMappingOverrideRequest(
            10L, LocalDate.of(2024, 8, 11), DayOfWeek.MONDAY, "Sunday attempt");

        assertThatThrownBy(() -> service.create(request))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Sunday");
    }

    @Test
    void shouldRejectADuplicateMappedDate() {
        DayMappingOverride existing = new DayMappingOverride();
        existing.setId(99L);
        when(dayMappingOverrideRepository.findByMappedDate(SATURDAY)).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> service.create(validRequest()))
            .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void shouldThrowNotFoundWhenTermInstanceDoesNotExist() {
        when(termInstanceRepository.findById(999L)).thenReturn(Optional.empty());
        DayMappingOverrideRequest request = new DayMappingOverrideRequest(999L, SATURDAY, DayOfWeek.MONDAY, "reason");

        assertThatThrownBy(() -> service.create(request))
            .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void resolveEffectiveDayOfWeekShouldReturnTheBorrowedDayWhenAMappingExists() {
        DayMappingOverride mapping = new DayMappingOverride();
        mapping.setBorrowedDayOfWeek(DayOfWeek.MONDAY);
        when(dayMappingOverrideRepository.findByMappedDate(SATURDAY)).thenReturn(Optional.of(mapping));

        assertThat(service.resolveEffectiveDayOfWeek(SATURDAY)).contains(DayOfWeek.MONDAY);
    }

    @Test
    void resolveEffectiveDayOfWeekShouldReturnTheActualWeekdayWhenNoMappingExists() {
        when(dayMappingOverrideRepository.findByMappedDate(SATURDAY)).thenReturn(Optional.empty());

        assertThat(service.resolveEffectiveDayOfWeek(SATURDAY)).contains(DayOfWeek.SATURDAY);
    }

    @Test
    void resolveEffectiveDayOfWeekShouldReturnEmptyForASundayWithNoMapping() {
        LocalDate sunday = LocalDate.of(2024, 8, 11);
        when(dayMappingOverrideRepository.findByMappedDate(sunday)).thenReturn(Optional.empty());

        assertThat(service.resolveEffectiveDayOfWeek(sunday)).isEmpty();
    }

    @Test
    void deleteShouldThrowNotFoundForAMissingId() {
        assertThatThrownBy(() -> service.delete(404L)).isInstanceOf(ResourceNotFoundException.class);
    }
}
