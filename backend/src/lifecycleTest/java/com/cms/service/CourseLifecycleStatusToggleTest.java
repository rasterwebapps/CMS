package com.cms.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.cms.dto.CourseStatusUpdateRequest;
import com.cms.exception.LifecycleConflictException;
import com.cms.model.Course;
import com.cms.model.Program;
import com.cms.model.enums.ProgramStatus;
import com.cms.repository.CourseRepository;
import com.cms.repository.FeeStructureGroupRepository;
import com.cms.repository.ProgramRepository;

@ExtendWith(MockitoExtension.class)
class CourseLifecycleStatusToggleTest {

    @Mock
    private CourseRepository courseRepository;
    @Mock
    private ProgramRepository programRepository;
    @Mock
    private ProgramService programService;
    @Mock
    private FeeStructureGroupRepository feeStructureGroupRepository;

    private CourseService courseService;

    @BeforeEach
    void setUp() {
        courseService = new CourseService(courseRepository, programRepository, programService, feeStructureGroupRepository);
    }

    @Test
    void shouldDeactivateCourseWhenNoActiveDependenciesExist() {
        Course course = createCourse(1L, ProgramStatus.ACTIVE, true);
        when(courseRepository.findById(1L)).thenReturn(Optional.of(course));
        when(feeStructureGroupRepository.existsByCourseIdAndIsActiveTrue(1L)).thenReturn(false);
        when(courseRepository.save(any(Course.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = courseService.updateStatus(1L, new CourseStatusUpdateRequest(false, "retired"));

        assertThat(response.isActive()).isFalse();
    }

    @Test
    void shouldBlockDeactivationWhenActiveFeeStructuresExist() {
        Course course = createCourse(1L, ProgramStatus.ACTIVE, true);
        when(courseRepository.findById(1L)).thenReturn(Optional.of(course));
        when(feeStructureGroupRepository.existsByCourseIdAndIsActiveTrue(1L)).thenReturn(true);

        assertThatThrownBy(() -> courseService.updateStatus(1L, new CourseStatusUpdateRequest(false, "retired")))
            .isInstanceOf(LifecycleConflictException.class)
            .satisfies(ex -> {
                LifecycleConflictException conflict = (LifecycleConflictException) ex;
                assertThat(conflict.getCode()).isEqualTo("ACTIVE_REFERENCE_EXISTS");
            });
    }

    @Test
    void shouldBlockActivationWhenParentProgramIsInactive() {
        Course course = createCourse(1L, ProgramStatus.INACTIVE, false);
        when(courseRepository.findById(1L)).thenReturn(Optional.of(course));

        assertThatThrownBy(() -> courseService.updateStatus(1L, new CourseStatusUpdateRequest(true, "reopen")))
            .isInstanceOf(LifecycleConflictException.class)
            .satisfies(ex -> {
                LifecycleConflictException conflict = (LifecycleConflictException) ex;
                assertThat(conflict.getCode()).isEqualTo("ANCESTOR_INACTIVE");
            });
    }

    private Course createCourse(Long id, ProgramStatus programStatus, boolean isActive) {
        Program program = new Program("Bachelor", "BACHELOR", 4);
        program.setStatus(programStatus);

        Course course = new Course("B.Sc Nursing", "BSN", null, program);
        course.setId(id);
        course.setRollNumberCode("RN");
        course.setIsActive(isActive);
        Instant now = Instant.now();
        course.setCreatedAt(now);
        course.setUpdatedAt(now);
        return course;
    }
}

