package com.cms.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.cms.dto.SyllabusUnitDto;
import com.cms.dto.SyllabusUnitRequest;
import com.cms.exception.ResourceNotFoundException;
import com.cms.model.CurriculumSemesterCourse;
import com.cms.model.SyllabusUnit;
import com.cms.model.enums.AttendanceType;
import com.cms.repository.CurriculumSemesterCourseRepository;
import com.cms.repository.SyllabusUnitRepository;

@ExtendWith(MockitoExtension.class)
class SyllabusUnitServiceTest {

    @Mock
    private SyllabusUnitRepository syllabusUnitRepository;

    @Mock
    private CurriculumSemesterCourseRepository curriculumSemesterCourseRepository;

    private SyllabusUnitService service;

    @BeforeEach
    void setUp() {
        service = new SyllabusUnitService(syllabusUnitRepository, curriculumSemesterCourseRepository);
    }

    @Test
    void shouldCreateUnitWhenNumberIsUniqueAndWithinHourBudget() {
        CurriculumSemesterCourse course = new CurriculumSemesterCourse();
        course.setId(10L);
        course.setTheoryHours(10);
        SyllabusUnitRequest request = new SyllabusUnitRequest(10L, 1, "Fundamentals", AttendanceType.THEORY, 6, "Intro", 1);

        when(curriculumSemesterCourseRepository.findById(10L)).thenReturn(Optional.of(course));
        when(syllabusUnitRepository.existsByCurriculumSemesterCourseIdAndUnitNumber(10L, 1)).thenReturn(false);
        when(syllabusUnitRepository.findByCurriculumSemesterCourseIdAndComponentType(10L, AttendanceType.THEORY))
            .thenReturn(List.of());
        when(syllabusUnitRepository.save(any(SyllabusUnit.class))).thenAnswer(inv -> {
            SyllabusUnit u = inv.getArgument(0);
            u.setId(100L);
            return u;
        });

        SyllabusUnitDto response = service.create(request);

        assertThat(response.id()).isEqualTo(100L);
        assertThat(response.curriculumTermCourseId()).isEqualTo(10L);
        assertThat(response.unitNumber()).isEqualTo(1);
        assertThat(response.title()).isEqualTo("Fundamentals");
        assertThat(response.componentType()).isEqualTo(AttendanceType.THEORY);
    }

    @Test
    void shouldRejectDuplicateUnitNumberOnCreate() {
        CurriculumSemesterCourse course = new CurriculumSemesterCourse();
        course.setId(10L);
        SyllabusUnitRequest request = new SyllabusUnitRequest(10L, 1, "Fundamentals", AttendanceType.THEORY, null, null, null);

        when(curriculumSemesterCourseRepository.findById(10L)).thenReturn(Optional.of(course));
        when(syllabusUnitRepository.existsByCurriculumSemesterCourseIdAndUnitNumber(10L, 1)).thenReturn(true);

        assertThatThrownBy(() -> service.create(request))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("already exists");
    }

    @Test
    void shouldThrowWhenCurriculumTermCourseNotFound() {
        SyllabusUnitRequest request = new SyllabusUnitRequest(99L, 1, "Fundamentals", AttendanceType.THEORY, null, null, null);
        when(curriculumSemesterCourseRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.create(request))
            .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void shouldRejectDuplicateUnitNumberOnUpdateExcludingSelf() {
        CurriculumSemesterCourse course = new CurriculumSemesterCourse();
        course.setId(10L);
        SyllabusUnit existing = new SyllabusUnit(course, 1, "Fundamentals", AttendanceType.THEORY, null, null, 1);
        existing.setId(5L);
        SyllabusUnitRequest request = new SyllabusUnitRequest(10L, 2, "Renamed", AttendanceType.THEORY, null, null, null);

        when(syllabusUnitRepository.findById(5L)).thenReturn(Optional.of(existing));
        when(syllabusUnitRepository.existsByCurriculumSemesterCourseIdAndUnitNumberAndIdNot(10L, 2, 5L))
            .thenReturn(true);

        assertThatThrownBy(() -> service.update(5L, request))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("already exists");
    }

    @Test
    void shouldRejectCreateWhenPlannedHoursExceedDeclaredTheoryTotal() {
        CurriculumSemesterCourse course = new CurriculumSemesterCourse();
        course.setId(10L);
        course.setTheoryHours(10);
        SyllabusUnitRequest request = new SyllabusUnitRequest(10L, 1, "Fundamentals", AttendanceType.THEORY, 6, null, null);

        SyllabusUnit existingUnit = new SyllabusUnit(course, 2, "Other Unit", AttendanceType.THEORY, 5, null, 2);
        existingUnit.setId(1L);

        when(curriculumSemesterCourseRepository.findById(10L)).thenReturn(Optional.of(course));
        when(syllabusUnitRepository.existsByCurriculumSemesterCourseIdAndUnitNumber(10L, 1)).thenReturn(false);
        when(syllabusUnitRepository.findByCurriculumSemesterCourseIdAndComponentType(10L, AttendanceType.THEORY))
            .thenReturn(List.of(existingUnit));

        assertThatThrownBy(() -> service.create(request))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("exceeding the declared THEORY total of 10h");
    }

    @Test
    void shouldExcludeOwnPriorHoursWhenUpdating() {
        CurriculumSemesterCourse course = new CurriculumSemesterCourse();
        course.setId(10L);
        course.setTheoryHours(10);
        SyllabusUnit existing = new SyllabusUnit(course, 1, "Fundamentals", AttendanceType.THEORY, 6, null, 1);
        existing.setId(5L);
        SyllabusUnitRequest request = new SyllabusUnitRequest(10L, 1, "Fundamentals", AttendanceType.THEORY, 9, null, null);

        when(syllabusUnitRepository.findById(5L)).thenReturn(Optional.of(existing));
        when(syllabusUnitRepository.existsByCurriculumSemesterCourseIdAndUnitNumberAndIdNot(10L, 1, 5L))
            .thenReturn(false);
        when(syllabusUnitRepository.findByCurriculumSemesterCourseIdAndComponentType(10L, AttendanceType.THEORY))
            .thenReturn(List.of(existing));
        when(syllabusUnitRepository.save(any(SyllabusUnit.class))).thenAnswer(inv -> inv.getArgument(0));

        SyllabusUnitDto response = service.update(5L, request);

        assertThat(response.plannedHours()).isEqualTo(9);
    }

    @Test
    void shouldDeleteExistingUnit() {
        when(syllabusUnitRepository.existsById(5L)).thenReturn(true);

        service.delete(5L);

        org.mockito.Mockito.verify(syllabusUnitRepository).deleteById(5L);
    }

    @Test
    void shouldThrowWhenDeletingMissingUnit() {
        when(syllabusUnitRepository.existsById(5L)).thenReturn(false);

        assertThatThrownBy(() -> service.delete(5L))
            .isInstanceOf(ResourceNotFoundException.class);
    }
}
