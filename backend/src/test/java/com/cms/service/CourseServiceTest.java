package com.cms.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.cms.dto.CourseRequest;
import com.cms.dto.CourseResponse;
import com.cms.dto.ProgramResponse;
import com.cms.exception.ResourceNotFoundException;
import com.cms.model.Course;
import com.cms.model.Program;

import com.cms.repository.CourseRepository;
import com.cms.repository.FeeStructureRepository;
import com.cms.repository.ProgramRepository;

@ExtendWith(MockitoExtension.class)
class CourseServiceTest {

    @Mock
    private CourseRepository courseRepository;

    @Mock
    private ProgramRepository programRepository;

    @Mock
    private ProgramService programService;

    @Mock
    private FeeStructureRepository feeStructureRepository;

    private CourseService courseService;

    private Program program;

    @BeforeEach
    void setUp() {
        courseService = new CourseService(courseRepository, programRepository, programService, feeStructureRepository);
        program = createProgram(1L, "Bachelor", "BACHELOR", 4);
        Instant now = Instant.now();
        ProgramResponse progResponse = new ProgramResponse(1L, "Bachelor", "BACHELOR", 4, now, now);
        org.mockito.Mockito.lenient().when(programService.toResponse(any(Program.class))).thenReturn(progResponse);
    }

    @Test
    void shouldCreateCourse() {
        CourseRequest request = new CourseRequest(
            "B.Sc. Nursing",
            "BSN",
            "General",
            1L
        );

        Course savedCourse = createCourse(1L, "B.Sc. Nursing", "BSN", "General", program);

        when(programRepository.findById(1L)).thenReturn(Optional.of(program));
        when(courseRepository.save(any(Course.class))).thenReturn(savedCourse);

        CourseResponse response = courseService.create(request);

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.name()).isEqualTo("B.Sc. Nursing");
        assertThat(response.code()).isEqualTo("BSN");
        assertThat(response.specialization()).isEqualTo("General");
        assertThat(response.program().id()).isEqualTo(1L);
        assertThat(response.program().durationYears()).isEqualTo(4);

        ArgumentCaptor<Course> captor = ArgumentCaptor.forClass(Course.class);
        verify(courseRepository).save(captor.capture());
        Course captured = captor.getValue();
        assertThat(captured.getName()).isEqualTo("B.Sc. Nursing");
        assertThat(captured.getCode()).isEqualTo("BSN");
    }

    @Test
    void shouldThrowExceptionWhenCreatingCourseWithNonExistentProgram() {
        CourseRequest request = new CourseRequest(
            "B.Sc. Nursing",
            "BSN",
            null,
            999L
        );

        when(programRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> courseService.create(request))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessage("Program not found with id: 999");

        verify(courseRepository, never()).save(any(Course.class));
    }

    @Test
    void shouldFindAllCourses() {
        Course course1 = createCourse(1L, "B.Sc. Nursing", "BSN", "General", program);
        Course course2 = createCourse(2L, "M.Sc. Nursing", "MSN", "Obs Gyn", program);

        when(courseRepository.findAll()).thenReturn(List.of(course1, course2));

        List<CourseResponse> responses = courseService.findAll();

        assertThat(responses).hasSize(2);
        assertThat(responses.get(0).name()).isEqualTo("B.Sc. Nursing");
        assertThat(responses.get(1).name()).isEqualTo("M.Sc. Nursing");
        verify(courseRepository).findAll();
    }

    @Test
    void shouldReturnEmptyListWhenNoCourses() {
        when(courseRepository.findAll()).thenReturn(List.of());

        List<CourseResponse> responses = courseService.findAll();

        assertThat(responses).isEmpty();
        verify(courseRepository).findAll();
    }

    @Test
    void shouldFindCourseById() {
        Course course = createCourse(1L, "B.Sc. Nursing", "BSN", "General", program);

        when(courseRepository.findById(1L)).thenReturn(Optional.of(course));

        CourseResponse response = courseService.findById(1L);

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.name()).isEqualTo("B.Sc. Nursing");
        assertThat(response.code()).isEqualTo("BSN");
        verify(courseRepository).findById(1L);
    }

    @Test
    void shouldThrowExceptionWhenCourseNotFoundById() {
        when(courseRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> courseService.findById(999L))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessage("Course not found with id: 999");

        verify(courseRepository).findById(999L);
    }

    @Test
    void shouldFindCoursesByProgramId() {
        Course course1 = createCourse(1L, "B.Sc. Nursing", "BSN", "General", program);
        Course course2 = createCourse(2L, "M.Sc. Nursing", "MSN", "Obs Gyn", program);

        when(programRepository.existsById(1L)).thenReturn(true);
        when(courseRepository.findByProgramId(1L)).thenReturn(List.of(course1, course2));

        List<CourseResponse> responses = courseService.findByProgramId(1L);

        assertThat(responses).hasSize(2);
        assertThat(responses.get(0).name()).isEqualTo("B.Sc. Nursing");
        assertThat(responses.get(1).name()).isEqualTo("M.Sc. Nursing");
        verify(courseRepository).findByProgramId(1L);
    }

    @Test
    void shouldReturnEmptyListWhenNoCoursesForProgram() {
        when(programRepository.existsById(1L)).thenReturn(true);
        when(courseRepository.findByProgramId(1L)).thenReturn(List.of());

        List<CourseResponse> responses = courseService.findByProgramId(1L);

        assertThat(responses).isEmpty();
        verify(courseRepository).findByProgramId(1L);
    }

    @Test
    void shouldThrowExceptionWhenFindingCoursesByNonExistentProgram() {
        when(programRepository.existsById(999L)).thenReturn(false);

        assertThatThrownBy(() -> courseService.findByProgramId(999L))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessage("Program not found with id: 999");

        verify(courseRepository, never()).findByProgramId(any());
    }

    @Test
    void shouldUpdateCourse() {
        Course existingCourse = createCourse(1L, "B.Sc. Nursing", "BSN", "General", program);

        Program newProgram = createProgram(2L, "Master", "MASTER", 2);

        CourseRequest updateRequest = new CourseRequest(
            "M.Sc. Nursing",
            "MSN",
            "Obs Gyn",
            2L
        );

        Course updatedCourse = createCourse(1L, "M.Sc. Nursing", "MSN", "Obs Gyn", newProgram);

        Instant now = Instant.now();
        ProgramResponse newProgResponse = new ProgramResponse(2L, "Master", "MASTER", 2, now, now);
        when(programService.toResponse(newProgram)).thenReturn(newProgResponse);
        when(courseRepository.findById(1L)).thenReturn(Optional.of(existingCourse));
        when(programRepository.findById(2L)).thenReturn(Optional.of(newProgram));
        when(courseRepository.existsByNameAndIdNot("M.Sc. Nursing", 1L)).thenReturn(false);
        when(courseRepository.existsByCodeAndIdNot("MSN", 1L)).thenReturn(false);
        when(courseRepository.save(any(Course.class))).thenReturn(updatedCourse);

        CourseResponse response = courseService.update(1L, updateRequest);

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.name()).isEqualTo("M.Sc. Nursing");
        assertThat(response.code()).isEqualTo("MSN");
        assertThat(response.specialization()).isEqualTo("Obs Gyn");
        assertThat(response.program().id()).isEqualTo(2L);
        assertThat(response.program().durationYears()).isEqualTo(2);

        verify(courseRepository).findById(1L);
        verify(courseRepository).save(any(Course.class));
    }

    @Test
    void shouldThrowWhenUpdatingCourseWithDuplicateName() {
        Course existingCourse = createCourse(1L, "B.Sc. Nursing", "BSN", "General", program);
        CourseRequest request = new CourseRequest("M.Sc. Nursing", "BSN", null, 1L);

        when(courseRepository.findById(1L)).thenReturn(Optional.of(existingCourse));
        when(programRepository.findById(1L)).thenReturn(Optional.of(program));
        when(courseRepository.existsByNameAndIdNot("M.Sc. Nursing", 1L)).thenReturn(true);

        assertThatThrownBy(() -> courseService.update(1L, request))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("M.Sc. Nursing")
            .hasMessageContaining("already exists");

        verify(courseRepository, never()).save(any(Course.class));
    }

    @Test
    void shouldThrowWhenUpdatingCourseWithDuplicateCode() {
        Course existingCourse = createCourse(1L, "B.Sc. Nursing", "BSN", "General", program);
        CourseRequest request = new CourseRequest("B.Sc. Nursing", "MSN", null, 1L);

        when(courseRepository.findById(1L)).thenReturn(Optional.of(existingCourse));
        when(programRepository.findById(1L)).thenReturn(Optional.of(program));
        when(courseRepository.existsByNameAndIdNot("B.Sc. Nursing", 1L)).thenReturn(false);
        when(courseRepository.existsByCodeAndIdNot("MSN", 1L)).thenReturn(true);

        assertThatThrownBy(() -> courseService.update(1L, request))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("MSN")
            .hasMessageContaining("already exists");

        verify(courseRepository, never()).save(any(Course.class));
    }

    @Test
    void shouldThrowExceptionWhenUpdatingNonExistentCourse() {
        CourseRequest request = new CourseRequest("Name", "CODE", null, 1L);

        when(courseRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> courseService.update(999L, request))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessage("Course not found with id: 999");

        verify(courseRepository).findById(999L);
        verify(courseRepository, never()).save(any(Course.class));
    }

    @Test
    void shouldThrowExceptionWhenUpdatingWithNonExistentProgram() {
        Course existingCourse = createCourse(1L, "B.Sc. Nursing", "BSN", "General", program);

        CourseRequest request = new CourseRequest("Name", "CODE", null, 999L);

        when(courseRepository.findById(1L)).thenReturn(Optional.of(existingCourse));
        when(programRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> courseService.update(1L, request))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessage("Program not found with id: 999");

        verify(courseRepository, never()).save(any(Course.class));
    }

    @Test
    void shouldDeleteCourse() {
        when(courseRepository.existsById(1L)).thenReturn(true);
        when(feeStructureRepository.existsByCourseId(1L)).thenReturn(false);

        courseService.delete(1L);

        verify(courseRepository).existsById(1L);
        verify(courseRepository).deleteById(1L);
    }

    @Test
    void shouldThrowWhenDeletingCourseWithFeeStructures() {
        when(courseRepository.existsById(1L)).thenReturn(true);
        when(feeStructureRepository.existsByCourseId(1L)).thenReturn(true);

        assertThatThrownBy(() -> courseService.delete(1L))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("fee structures");

        verify(courseRepository, never()).deleteById(any());
    }

    @Test
    void shouldThrowExceptionWhenDeletingNonExistentCourse() {
        when(courseRepository.existsById(999L)).thenReturn(false);

        assertThatThrownBy(() -> courseService.delete(999L))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessage("Course not found with id: 999");

        verify(courseRepository).existsById(999L);
        verify(courseRepository, never()).deleteById(any());
    }

    private Program createProgram(Long id, String name, String code, Integer durationYears) {
        Program prog = new Program(name, code, durationYears);
        prog.setId(id);
        Instant now = Instant.now();
        prog.setCreatedAt(now);
        prog.setUpdatedAt(now);
        return prog;
    }

    private Course createCourse(Long id, String name, String code, String specialization,
                                Program prog) {
        Course course = new Course(name, code, specialization, prog);
        course.setId(id);
        Instant now = Instant.now();
        course.setCreatedAt(now);
        course.setUpdatedAt(now);
        return course;
    }
}
