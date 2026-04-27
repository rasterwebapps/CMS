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

import com.cms.dto.SemesterRequest;
import com.cms.dto.SemesterResponse;
import com.cms.exception.ResourceNotFoundException;
import com.cms.model.AcademicYear;
import com.cms.model.Semester;
import com.cms.repository.AcademicYearRepository;
import com.cms.repository.SemesterRepository;

@ExtendWith(MockitoExtension.class)
class SemesterServiceTest {

    @Mock
    private SemesterRepository semesterRepository;

    @Mock
    private AcademicYearRepository academicYearRepository;

    private SemesterService semesterService;

    private AcademicYear academicYear;

    @BeforeEach
    void setUp() {
        semesterService = new SemesterService(semesterRepository, academicYearRepository);
        academicYear = createAcademicYear(1L, "2024-2025",
            LocalDate.of(2024, 8, 1), LocalDate.of(2025, 5, 31), true);
    }

    @Test
    void shouldCreateSemester() {
        SemesterRequest request = new SemesterRequest(
            "Fall 2024",
            1L,
            LocalDate.of(2024, 8, 1),
            LocalDate.of(2024, 12, 15),
            1
        );

        Semester savedSemester = createSemester(1L, "Fall 2024", academicYear,
            LocalDate.of(2024, 8, 1), LocalDate.of(2024, 12, 15), 1);

        when(academicYearRepository.findById(1L)).thenReturn(Optional.of(academicYear));
        when(semesterRepository.save(any(Semester.class))).thenReturn(savedSemester);

        SemesterResponse response = semesterService.create(request);

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.name()).isEqualTo("Fall 2024");
        assertThat(response.startDate()).isEqualTo(LocalDate.of(2024, 8, 1));
        assertThat(response.endDate()).isEqualTo(LocalDate.of(2024, 12, 15));
        assertThat(response.semesterNumber()).isEqualTo(1);
        assertThat(response.academicYear().id()).isEqualTo(1L);

        ArgumentCaptor<Semester> captor = ArgumentCaptor.forClass(Semester.class);
        verify(semesterRepository).save(captor.capture());
        Semester captured = captor.getValue();
        assertThat(captured.getName()).isEqualTo("Fall 2024");
        assertThat(captured.getSemesterNumber()).isEqualTo(1);
    }

    @Test
    void shouldThrowExceptionWhenCreatingSemesterWithNonExistentAcademicYear() {
        SemesterRequest request = new SemesterRequest(
            "Fall 2024",
            999L,
            LocalDate.of(2024, 8, 1),
            LocalDate.of(2024, 12, 15),
            1
        );

        when(academicYearRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> semesterService.create(request))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessage("Academic year not found with id: 999");

        verify(semesterRepository, never()).save(any(Semester.class));
    }

    @Test
    void shouldThrowExceptionWhenEndDateIsBeforeStartDate() {
        SemesterRequest request = new SemesterRequest(
            "Fall 2024",
            1L,
            LocalDate.of(2024, 12, 15),
            LocalDate.of(2024, 8, 1),
            1
        );

        when(academicYearRepository.findById(1L)).thenReturn(Optional.of(academicYear));

        assertThatThrownBy(() -> semesterService.create(request))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("End date must be after start date");

        verify(semesterRepository, never()).save(any(Semester.class));
    }

    @Test
    void shouldThrowExceptionWhenEndDateEqualsStartDate() {
        SemesterRequest request = new SemesterRequest(
            "Fall 2024",
            1L,
            LocalDate.of(2024, 8, 1),
            LocalDate.of(2024, 8, 1),
            1
        );

        when(academicYearRepository.findById(1L)).thenReturn(Optional.of(academicYear));

        assertThatThrownBy(() -> semesterService.create(request))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("End date must be after start date");

        verify(semesterRepository, never()).save(any(Semester.class));
    }

    @Test
    void shouldThrowExceptionWhenSemesterStartDateIsBeforeAcademicYearStartDate() {
        SemesterRequest request = new SemesterRequest(
            "Fall 2024",
            1L,
            LocalDate.of(2024, 7, 1),
            LocalDate.of(2024, 12, 15),
            1
        );

        when(academicYearRepository.findById(1L)).thenReturn(Optional.of(academicYear));

        assertThatThrownBy(() -> semesterService.create(request))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Semester start date must not be before academic year start date");

        verify(semesterRepository, never()).save(any(Semester.class));
    }

    @Test
    void shouldThrowExceptionWhenSemesterEndDateIsAfterAcademicYearEndDate() {
        SemesterRequest request = new SemesterRequest(
            "Spring 2025",
            1L,
            LocalDate.of(2025, 1, 15),
            LocalDate.of(2025, 6, 30),
            2
        );

        when(academicYearRepository.findById(1L)).thenReturn(Optional.of(academicYear));

        assertThatThrownBy(() -> semesterService.create(request))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Semester end date must not be after academic year end date");

        verify(semesterRepository, never()).save(any(Semester.class));
    }

    @Test
    void shouldThrowExceptionWhenSemesterOverlapsExistingOne() {
        SemesterRequest request = new SemesterRequest(
            "Overlapping",
            1L,
            LocalDate.of(2024, 9, 1),
            LocalDate.of(2024, 11, 30),
            1
        );

        when(academicYearRepository.findById(1L)).thenReturn(Optional.of(academicYear));
        when(semesterRepository.existsOverlappingInAcademicYear(1L,
            LocalDate.of(2024, 9, 1), LocalDate.of(2024, 11, 30), -1L))
            .thenReturn(true);

        assertThatThrownBy(() -> semesterService.create(request))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("The semester dates overlap with an existing semester in this academic year");

        verify(semesterRepository, never()).save(any(Semester.class));
    }

    @Test
    void shouldFindAllSemesters() {
        Semester sem1 = createSemester(1L, "Fall 2024", academicYear,
            LocalDate.of(2024, 8, 1), LocalDate.of(2024, 12, 15), 1);
        Semester sem2 = createSemester(2L, "Spring 2025", academicYear,
            LocalDate.of(2025, 1, 15), LocalDate.of(2025, 5, 31), 2);

        when(semesterRepository.findAll()).thenReturn(List.of(sem1, sem2));

        List<SemesterResponse> responses = semesterService.findAll();

        assertThat(responses).hasSize(2);
        assertThat(responses.get(0).name()).isEqualTo("Fall 2024");
        assertThat(responses.get(1).name()).isEqualTo("Spring 2025");
        verify(semesterRepository).findAll();
    }

    @Test
    void shouldReturnEmptyListWhenNoSemesters() {
        when(semesterRepository.findAll()).thenReturn(List.of());

        List<SemesterResponse> responses = semesterService.findAll();

        assertThat(responses).isEmpty();
        verify(semesterRepository).findAll();
    }

    @Test
    void shouldFindSemesterById() {
        Semester semester = createSemester(1L, "Fall 2024", academicYear,
            LocalDate.of(2024, 8, 1), LocalDate.of(2024, 12, 15), 1);

        when(semesterRepository.findById(1L)).thenReturn(Optional.of(semester));

        SemesterResponse response = semesterService.findById(1L);

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.name()).isEqualTo("Fall 2024");
        assertThat(response.semesterNumber()).isEqualTo(1);
        verify(semesterRepository).findById(1L);
    }

    @Test
    void shouldThrowExceptionWhenSemesterNotFoundById() {
        when(semesterRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> semesterService.findById(999L))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessage("Semester not found with id: 999");

        verify(semesterRepository).findById(999L);
    }

    @Test
    void shouldFindSemestersByAcademicYearId() {
        Semester sem1 = createSemester(1L, "Fall 2024", academicYear,
            LocalDate.of(2024, 8, 1), LocalDate.of(2024, 12, 15), 1);
        Semester sem2 = createSemester(2L, "Spring 2025", academicYear,
            LocalDate.of(2025, 1, 15), LocalDate.of(2025, 5, 31), 2);

        when(academicYearRepository.existsById(1L)).thenReturn(true);
        when(semesterRepository.findByAcademicYearIdOrderBySemesterNumber(1L))
            .thenReturn(List.of(sem1, sem2));

        List<SemesterResponse> responses = semesterService.findByAcademicYearId(1L);

        assertThat(responses).hasSize(2);
        assertThat(responses.get(0).name()).isEqualTo("Fall 2024");
        assertThat(responses.get(0).semesterNumber()).isEqualTo(1);
        assertThat(responses.get(1).name()).isEqualTo("Spring 2025");
        assertThat(responses.get(1).semesterNumber()).isEqualTo(2);
        verify(semesterRepository).findByAcademicYearIdOrderBySemesterNumber(1L);
    }

    @Test
    void shouldReturnEmptyListWhenNoSemestersForAcademicYear() {
        when(academicYearRepository.existsById(1L)).thenReturn(true);
        when(semesterRepository.findByAcademicYearIdOrderBySemesterNumber(1L)).thenReturn(List.of());

        List<SemesterResponse> responses = semesterService.findByAcademicYearId(1L);

        assertThat(responses).isEmpty();
        verify(semesterRepository).findByAcademicYearIdOrderBySemesterNumber(1L);
    }

    @Test
    void shouldThrowExceptionWhenFindingSemestersByNonExistentAcademicYear() {
        when(academicYearRepository.existsById(999L)).thenReturn(false);

        assertThatThrownBy(() -> semesterService.findByAcademicYearId(999L))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessage("Academic year not found with id: 999");

        verify(semesterRepository, never()).findByAcademicYearIdOrderBySemesterNumber(any());
    }

    @Test
    void shouldUpdateSemester() {
        Semester existingSemester = createSemester(1L, "Fall 2024", academicYear,
            LocalDate.of(2024, 8, 1), LocalDate.of(2024, 12, 15), 1);

        SemesterRequest updateRequest = new SemesterRequest(
            "Fall 2024 Updated",
            1L,
            LocalDate.of(2024, 8, 15),
            LocalDate.of(2024, 12, 20),
            1
        );

        Semester updatedSemester = createSemester(1L, "Fall 2024 Updated", academicYear,
            LocalDate.of(2024, 8, 15), LocalDate.of(2024, 12, 20), 1);

        when(semesterRepository.findById(1L)).thenReturn(Optional.of(existingSemester));
        when(academicYearRepository.findById(1L)).thenReturn(Optional.of(academicYear));
        when(semesterRepository.existsByNameAndAcademicYearIdAndIdNot("Fall 2024 Updated", 1L, 1L))
            .thenReturn(false);
        when(semesterRepository.save(any(Semester.class))).thenReturn(updatedSemester);

        SemesterResponse response = semesterService.update(1L, updateRequest);

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.name()).isEqualTo("Fall 2024 Updated");
        assertThat(response.startDate()).isEqualTo(LocalDate.of(2024, 8, 15));
        assertThat(response.endDate()).isEqualTo(LocalDate.of(2024, 12, 20));
        verify(semesterRepository).findById(1L);
        verify(semesterRepository).save(any(Semester.class));
    }

    @Test
    void shouldUpdateSemesterWithDifferentAcademicYear() {
        Semester existingSemester = createSemester(1L, "Fall 2024", academicYear,
            LocalDate.of(2024, 8, 1), LocalDate.of(2024, 12, 15), 1);

        AcademicYear newAcademicYear = createAcademicYear(2L, "2025-2026",
            LocalDate.of(2025, 8, 1), LocalDate.of(2026, 5, 31), false);

        SemesterRequest updateRequest = new SemesterRequest(
            "Fall 2025",
            2L,
            LocalDate.of(2025, 8, 1),
            LocalDate.of(2025, 12, 15),
            1
        );

        Semester updatedSemester = createSemester(1L, "Fall 2025", newAcademicYear,
            LocalDate.of(2025, 8, 1), LocalDate.of(2025, 12, 15), 1);

        when(semesterRepository.findById(1L)).thenReturn(Optional.of(existingSemester));
        when(academicYearRepository.findById(2L)).thenReturn(Optional.of(newAcademicYear));
        when(semesterRepository.existsByNameAndAcademicYearIdAndIdNot("Fall 2025", 2L, 1L))
            .thenReturn(false);
        when(semesterRepository.save(any(Semester.class))).thenReturn(updatedSemester);

        SemesterResponse response = semesterService.update(1L, updateRequest);

        assertThat(response.academicYear().id()).isEqualTo(2L);
        verify(academicYearRepository).findById(2L);
    }

    @Test
    void shouldThrowWhenUpdatingSemesterWithDuplicateName() {
        Semester existingSemester = createSemester(1L, "Fall 2024", academicYear,
            LocalDate.of(2024, 8, 1), LocalDate.of(2024, 12, 15), 1);

        SemesterRequest request = new SemesterRequest(
            "Spring 2025",
            1L,
            LocalDate.of(2024, 8, 1),
            LocalDate.of(2024, 12, 15),
            1
        );

        when(semesterRepository.findById(1L)).thenReturn(Optional.of(existingSemester));
        when(academicYearRepository.findById(1L)).thenReturn(Optional.of(academicYear));
        when(semesterRepository.existsByNameAndAcademicYearIdAndIdNot("Spring 2025", 1L, 1L))
            .thenReturn(true);

        assertThatThrownBy(() -> semesterService.update(1L, request))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Spring 2025")
            .hasMessageContaining("already exists");

        verify(semesterRepository, never()).save(any(Semester.class));
    }

    @Test
    void shouldThrowExceptionWhenUpdatingNonExistentSemester() {
        SemesterRequest request = new SemesterRequest(
            "Fall 2024",
            1L,
            LocalDate.of(2024, 8, 1),
            LocalDate.of(2024, 12, 15),
            1
        );

        when(semesterRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> semesterService.update(999L, request))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessage("Semester not found with id: 999");

        verify(semesterRepository).findById(999L);
        verify(semesterRepository, never()).save(any(Semester.class));
    }

    @Test
    void shouldThrowExceptionWhenUpdatingWithNonExistentAcademicYear() {
        Semester existingSemester = createSemester(1L, "Fall 2024", academicYear,
            LocalDate.of(2024, 8, 1), LocalDate.of(2024, 12, 15), 1);

        SemesterRequest request = new SemesterRequest(
            "Fall 2024",
            999L,
            LocalDate.of(2024, 8, 1),
            LocalDate.of(2024, 12, 15),
            1
        );

        when(semesterRepository.findById(1L)).thenReturn(Optional.of(existingSemester));
        when(academicYearRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> semesterService.update(1L, request))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessage("Academic year not found with id: 999");

        verify(semesterRepository, never()).save(any(Semester.class));
    }

    @Test
    void shouldThrowExceptionWhenUpdatingWithInvalidDates() {
        Semester existingSemester = createSemester(1L, "Fall 2024", academicYear,
            LocalDate.of(2024, 8, 1), LocalDate.of(2024, 12, 15), 1);

        SemesterRequest request = new SemesterRequest(
            "Fall 2024",
            1L,
            LocalDate.of(2024, 12, 15),
            LocalDate.of(2024, 8, 1),
            1
        );

        when(semesterRepository.findById(1L)).thenReturn(Optional.of(existingSemester));
        when(academicYearRepository.findById(1L)).thenReturn(Optional.of(academicYear));

        assertThatThrownBy(() -> semesterService.update(1L, request))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("End date must be after start date");

        verify(semesterRepository, never()).save(any(Semester.class));
    }

    @Test
    void shouldThrowExceptionWhenUpdatingSemesterStartDateBeforeAcademicYear() {
        Semester existingSemester = createSemester(1L, "Fall 2024", academicYear,
            LocalDate.of(2024, 8, 1), LocalDate.of(2024, 12, 15), 1);

        SemesterRequest request = new SemesterRequest(
            "Fall 2024",
            1L,
            LocalDate.of(2024, 7, 1),
            LocalDate.of(2024, 12, 15),
            1
        );

        when(semesterRepository.findById(1L)).thenReturn(Optional.of(existingSemester));
        when(academicYearRepository.findById(1L)).thenReturn(Optional.of(academicYear));

        assertThatThrownBy(() -> semesterService.update(1L, request))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Semester start date must not be before academic year start date");

        verify(semesterRepository, never()).save(any(Semester.class));
    }

    @Test
    void shouldThrowExceptionWhenUpdatingSemesterEndDateAfterAcademicYear() {
        Semester existingSemester = createSemester(1L, "Spring 2025", academicYear,
            LocalDate.of(2025, 1, 15), LocalDate.of(2025, 5, 31), 2);

        SemesterRequest request = new SemesterRequest(
            "Spring 2025",
            1L,
            LocalDate.of(2025, 1, 15),
            LocalDate.of(2025, 6, 30),
            2
        );

        when(semesterRepository.findById(1L)).thenReturn(Optional.of(existingSemester));
        when(academicYearRepository.findById(1L)).thenReturn(Optional.of(academicYear));

        assertThatThrownBy(() -> semesterService.update(1L, request))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Semester end date must not be after academic year end date");

        verify(semesterRepository, never()).save(any(Semester.class));
    }

    @Test
    void shouldDeleteSemester() {
        when(semesterRepository.existsById(1L)).thenReturn(true);

        semesterService.delete(1L);

        verify(semesterRepository).existsById(1L);
        verify(semesterRepository).deleteById(1L);
    }

    @Test
    void shouldThrowExceptionWhenDeletingNonExistentSemester() {
        when(semesterRepository.existsById(999L)).thenReturn(false);

        assertThatThrownBy(() -> semesterService.delete(999L))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessage("Semester not found with id: 999");

        verify(semesterRepository).existsById(999L);
        verify(semesterRepository, never()).deleteById(any());
    }

    private AcademicYear createAcademicYear(Long id, String name, LocalDate startDate,
                                             LocalDate endDate, Boolean isCurrent) {
        AcademicYear ay = new AcademicYear(name, startDate, endDate, isCurrent);
        ay.setId(id);
        Instant now = Instant.now();
        ay.setCreatedAt(now);
        ay.setUpdatedAt(now);
        return ay;
    }

    private Semester createSemester(Long id, String name, AcademicYear academicYear,
                                     LocalDate startDate, LocalDate endDate, Integer semesterNumber) {
        Semester semester = new Semester(name, academicYear, startDate, endDate, semesterNumber);
        semester.setId(id);
        Instant now = Instant.now();
        semester.setCreatedAt(now);
        semester.setUpdatedAt(now);
        return semester;
    }
}
