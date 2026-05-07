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
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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
        assertThat(responses.get(0).semesterNumber()).isEqualTo(1);
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

    private AcademicYear createAcademicYear(Long id, String name, LocalDate startDate,
                                             LocalDate endDate, Boolean isCurrent) {
        AcademicYear ay = new AcademicYear(name, startDate, endDate, isCurrent);
        ay.setId(id);
        Instant now = Instant.now();
        ay.setCreatedAt(now);
        ay.setUpdatedAt(now);
        return ay;
    }

    private Semester createSemester(Long id, String name, AcademicYear ay,
                                     LocalDate startDate, LocalDate endDate, Integer semesterNumber) {
        Semester semester = new Semester(name, ay, startDate, endDate, semesterNumber);
        semester.setId(id);
        Instant now = Instant.now();
        semester.setCreatedAt(now);
        semester.setUpdatedAt(now);
        return semester;
    }
}
