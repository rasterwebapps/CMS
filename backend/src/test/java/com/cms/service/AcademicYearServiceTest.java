package com.cms.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.cms.dto.AcademicYearRequest;
import com.cms.dto.AcademicYearResponse;
import com.cms.exception.ResourceNotFoundException;
import com.cms.model.AcademicYear;
import com.cms.repository.AcademicYearRepository;

@ExtendWith(MockitoExtension.class)
class AcademicYearServiceTest {

    @Mock
    private AcademicYearRepository academicYearRepository;

    private AcademicYearService academicYearService;

    @BeforeEach
    void setUp() {
        academicYearService = new AcademicYearService(academicYearRepository);
    }

    @Test
    void shouldCreateAcademicYear() {
        AcademicYearRequest request = new AcademicYearRequest(
            "2024-2025",
            LocalDate.of(2024, 8, 1),
            LocalDate.of(2025, 5, 31),
            false
        );

        AcademicYear savedAcademicYear = createAcademicYear(1L, "2024-2025",
            LocalDate.of(2024, 8, 1), LocalDate.of(2025, 5, 31), false);

        when(academicYearRepository.save(any(AcademicYear.class))).thenReturn(savedAcademicYear);

        AcademicYearResponse response = academicYearService.create(request);

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.name()).isEqualTo("2024-2025");
        assertThat(response.startDate()).isEqualTo(LocalDate.of(2024, 8, 1));
        assertThat(response.endDate()).isEqualTo(LocalDate.of(2025, 5, 31));
        assertThat(response.isCurrent()).isFalse();

        ArgumentCaptor<AcademicYear> captor = ArgumentCaptor.forClass(AcademicYear.class);
        verify(academicYearRepository).save(captor.capture());
        AcademicYear captured = captor.getValue();
        assertThat(captured.getName()).isEqualTo("2024-2025");
    }

    @Test
    void shouldCreateAcademicYearWithNullIsCurrent() {
        AcademicYearRequest request = new AcademicYearRequest(
            "2024-2025",
            LocalDate.of(2024, 8, 1),
            LocalDate.of(2025, 5, 31),
            null
        );

        AcademicYear savedAcademicYear = createAcademicYear(1L, "2024-2025",
            LocalDate.of(2024, 8, 1), LocalDate.of(2025, 5, 31), false);

        when(academicYearRepository.save(any(AcademicYear.class))).thenReturn(savedAcademicYear);

        AcademicYearResponse response = academicYearService.create(request);

        assertThat(response.isCurrent()).isFalse();

        ArgumentCaptor<AcademicYear> captor = ArgumentCaptor.forClass(AcademicYear.class);
        verify(academicYearRepository).save(captor.capture());
        assertThat(captor.getValue().getIsCurrent()).isFalse();
    }

    @Test
    void shouldCreateAcademicYearAndSetCurrentClearingOthers() {
        AcademicYearRequest request = new AcademicYearRequest(
            "2024-2025",
            LocalDate.of(2024, 8, 1),
            LocalDate.of(2025, 5, 31),
            true
        );

        AcademicYear savedAcademicYear = createAcademicYear(1L, "2024-2025",
            LocalDate.of(2024, 8, 1), LocalDate.of(2025, 5, 31), true);

        when(academicYearRepository.save(any(AcademicYear.class))).thenReturn(savedAcademicYear);

        AcademicYearResponse response = academicYearService.create(request);

        assertThat(response.isCurrent()).isTrue();
        verify(academicYearRepository).clearCurrentAcademicYear();
    }

    @Test
    void shouldThrowExceptionWhenEndDateIsBeforeStartDate() {
        AcademicYearRequest request = new AcademicYearRequest(
            "2024-2025",
            LocalDate.of(2025, 5, 31),
            LocalDate.of(2024, 8, 1),
            false
        );

        assertThatThrownBy(() -> academicYearService.create(request))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("End date must be after start date");

        verify(academicYearRepository, never()).save(any(AcademicYear.class));
    }

    @Test
    void shouldThrowExceptionWhenEndDateEqualsStartDate() {
        AcademicYearRequest request = new AcademicYearRequest(
            "2024-2025",
            LocalDate.of(2024, 8, 1),
            LocalDate.of(2024, 8, 1),
            false
        );

        assertThatThrownBy(() -> academicYearService.create(request))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("End date must be after start date");

        verify(academicYearRepository, never()).save(any(AcademicYear.class));
    }

    @Test
    void shouldFindAllAcademicYears() {
        AcademicYear ay1 = createAcademicYear(1L, "2023-2024",
            LocalDate.of(2023, 8, 1), LocalDate.of(2024, 5, 31), false);
        AcademicYear ay2 = createAcademicYear(2L, "2024-2025",
            LocalDate.of(2024, 8, 1), LocalDate.of(2025, 5, 31), true);

        when(academicYearRepository.findAll()).thenReturn(List.of(ay1, ay2));

        List<AcademicYearResponse> responses = academicYearService.findAll();

        assertThat(responses).hasSize(2);
        assertThat(responses.get(0).name()).isEqualTo("2023-2024");
        assertThat(responses.get(1).name()).isEqualTo("2024-2025");
        verify(academicYearRepository).findAll();
    }

    @Test
    void shouldReturnEmptyListWhenNoAcademicYears() {
        when(academicYearRepository.findAll()).thenReturn(List.of());

        List<AcademicYearResponse> responses = academicYearService.findAll();

        assertThat(responses).isEmpty();
        verify(academicYearRepository).findAll();
    }

    @Test
    void shouldFindAcademicYearById() {
        AcademicYear academicYear = createAcademicYear(1L, "2024-2025",
            LocalDate.of(2024, 8, 1), LocalDate.of(2025, 5, 31), true);

        when(academicYearRepository.findById(1L)).thenReturn(Optional.of(academicYear));

        AcademicYearResponse response = academicYearService.findById(1L);

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.name()).isEqualTo("2024-2025");
        verify(academicYearRepository).findById(1L);
    }

    @Test
    void shouldThrowExceptionWhenAcademicYearNotFoundById() {
        when(academicYearRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> academicYearService.findById(999L))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessage("Academic year not found with id: 999");

        verify(academicYearRepository).findById(999L);
    }

    @Test
    void shouldFindCurrentAcademicYear() {
        AcademicYear academicYear = createAcademicYear(1L, "2024-2025",
            LocalDate.of(2024, 8, 1), LocalDate.of(2025, 5, 31), true);

        when(academicYearRepository.findByIsCurrentTrue()).thenReturn(Optional.of(academicYear));

        AcademicYearResponse response = academicYearService.findCurrent();

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.isCurrent()).isTrue();
        verify(academicYearRepository).findByIsCurrentTrue();
    }

    @Test
    void shouldThrowExceptionWhenNoCurrentAcademicYear() {
        when(academicYearRepository.findByIsCurrentTrue()).thenReturn(Optional.empty());

        assertThatThrownBy(() -> academicYearService.findCurrent())
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessage("No current academic year found");

        verify(academicYearRepository).findByIsCurrentTrue();
    }

    @Test
    void shouldUpdateAcademicYear() {
        AcademicYear existingAcademicYear = createAcademicYear(1L, "2024-2025",
            LocalDate.of(2024, 8, 1), LocalDate.of(2025, 5, 31), false);

        AcademicYearRequest updateRequest = new AcademicYearRequest(
            "2024-2025 Updated",
            LocalDate.of(2024, 9, 1),
            LocalDate.of(2025, 6, 30),
            false
        );

        AcademicYear updatedAcademicYear = createAcademicYear(1L, "2024-2025 Updated",
            LocalDate.of(2024, 9, 1), LocalDate.of(2025, 6, 30), false);

        when(academicYearRepository.findById(1L)).thenReturn(Optional.of(existingAcademicYear));
        when(academicYearRepository.existsByNameAndIdNot("2024-2025 Updated", 1L)).thenReturn(false);
        when(academicYearRepository.save(any(AcademicYear.class))).thenReturn(updatedAcademicYear);

        AcademicYearResponse response = academicYearService.update(1L, updateRequest);

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.name()).isEqualTo("2024-2025 Updated");
        assertThat(response.startDate()).isEqualTo(LocalDate.of(2024, 9, 1));
        assertThat(response.endDate()).isEqualTo(LocalDate.of(2025, 6, 30));
        verify(academicYearRepository).findById(1L);
        verify(academicYearRepository).save(any(AcademicYear.class));
    }

    @Test
    void shouldThrowWhenUpdatingAcademicYearWithDuplicateName() {
        AcademicYear existing = createAcademicYear(1L, "2024-2025",
            LocalDate.of(2024, 8, 1), LocalDate.of(2025, 5, 31), false);

        AcademicYearRequest request = new AcademicYearRequest(
            "2023-2024",
            LocalDate.of(2024, 8, 1),
            LocalDate.of(2025, 5, 31),
            false
        );

        when(academicYearRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(academicYearRepository.existsByNameAndIdNot("2023-2024", 1L)).thenReturn(true);

        assertThatThrownBy(() -> academicYearService.update(1L, request))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("2023-2024")
            .hasMessageContaining("already exists");

        verify(academicYearRepository, never()).save(any(AcademicYear.class));
    }

    @Test
    void shouldUpdateAcademicYearAndSetCurrentClearingOthers() {
        AcademicYear existingAcademicYear = createAcademicYear(1L, "2024-2025",
            LocalDate.of(2024, 8, 1), LocalDate.of(2025, 5, 31), false);

        AcademicYearRequest updateRequest = new AcademicYearRequest(
            "2024-2025",
            LocalDate.of(2024, 8, 1),
            LocalDate.of(2025, 5, 31),
            true
        );

        AcademicYear updatedAcademicYear = createAcademicYear(1L, "2024-2025",
            LocalDate.of(2024, 8, 1), LocalDate.of(2025, 5, 31), true);

        when(academicYearRepository.findById(1L)).thenReturn(Optional.of(existingAcademicYear));
        when(academicYearRepository.existsByNameAndIdNot("2024-2025", 1L)).thenReturn(false);
        when(academicYearRepository.save(any(AcademicYear.class))).thenReturn(updatedAcademicYear);

        AcademicYearResponse response = academicYearService.update(1L, updateRequest);

        assertThat(response.isCurrent()).isTrue();
        verify(academicYearRepository).clearCurrentAcademicYear();
    }

    @Test
    void shouldNotClearCurrentWhenUpdatingAlreadyCurrentAcademicYear() {
        AcademicYear existingAcademicYear = createAcademicYear(1L, "2024-2025",
            LocalDate.of(2024, 8, 1), LocalDate.of(2025, 5, 31), true);

        AcademicYearRequest updateRequest = new AcademicYearRequest(
            "2024-2025 Updated",
            LocalDate.of(2024, 8, 1),
            LocalDate.of(2025, 5, 31),
            true
        );

        AcademicYear updatedAcademicYear = createAcademicYear(1L, "2024-2025 Updated",
            LocalDate.of(2024, 8, 1), LocalDate.of(2025, 5, 31), true);

        when(academicYearRepository.findById(1L)).thenReturn(Optional.of(existingAcademicYear));
        when(academicYearRepository.existsByNameAndIdNot("2024-2025 Updated", 1L)).thenReturn(false);
        when(academicYearRepository.save(any(AcademicYear.class))).thenReturn(updatedAcademicYear);

        academicYearService.update(1L, updateRequest);

        verify(academicYearRepository, never()).clearCurrentAcademicYear();
    }

    @Test
    void shouldThrowExceptionWhenUpdatingNonExistentAcademicYear() {
        AcademicYearRequest request = new AcademicYearRequest(
            "2024-2025",
            LocalDate.of(2024, 8, 1),
            LocalDate.of(2025, 5, 31),
            false
        );

        when(academicYearRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> academicYearService.update(999L, request))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessage("Academic year not found with id: 999");

        verify(academicYearRepository).findById(999L);
        verify(academicYearRepository, never()).save(any(AcademicYear.class));
    }

    @Test
    void shouldThrowExceptionWhenUpdatingWithInvalidDates() {
        AcademicYear existingAcademicYear = createAcademicYear(1L, "2024-2025",
            LocalDate.of(2024, 8, 1), LocalDate.of(2025, 5, 31), false);

        AcademicYearRequest request = new AcademicYearRequest(
            "2024-2025",
            LocalDate.of(2025, 5, 31),
            LocalDate.of(2024, 8, 1),
            false
        );

        when(academicYearRepository.findById(1L)).thenReturn(Optional.of(existingAcademicYear));

        assertThatThrownBy(() -> academicYearService.update(1L, request))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("End date must be after start date");

        verify(academicYearRepository, never()).save(any(AcademicYear.class));
    }

    @Test
    void shouldDeleteAcademicYear() {
        when(academicYearRepository.existsById(1L)).thenReturn(true);

        academicYearService.delete(1L);

        verify(academicYearRepository).existsById(1L);
        verify(academicYearRepository).deleteById(1L);
    }

    @Test
    void shouldThrowExceptionWhenDeletingNonExistentAcademicYear() {
        when(academicYearRepository.existsById(999L)).thenReturn(false);

        assertThatThrownBy(() -> academicYearService.delete(999L))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessage("Academic year not found with id: 999");

        verify(academicYearRepository).existsById(999L);
        verify(academicYearRepository, never()).deleteById(any());
    }

    private AcademicYear createAcademicYear(Long id, String name, LocalDate startDate,
                                             LocalDate endDate, Boolean isCurrent) {
        AcademicYear academicYear = new AcademicYear(name, startDate, endDate, isCurrent);
        academicYear.setId(id);
        Instant now = Instant.now();
        academicYear.setCreatedAt(now);
        academicYear.setUpdatedAt(now);
        return academicYear;
    }
}
