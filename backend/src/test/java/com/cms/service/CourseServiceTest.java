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
import com.cms.exception.ResourceNotFoundException;
import com.cms.model.Course;
import com.cms.model.Department;
import com.cms.model.Program;
import com.cms.model.enums.DegreeType;
import com.cms.repository.CourseRepository;
import com.cms.repository.ProgramRepository;

@ExtendWith(MockitoExtension.class)
class CourseServiceTest {

    @Mock
    private CourseRepository courseRepository;

    @Mock
    private ProgramRepository programRepository;

    private CourseService courseService;

    private Department department;
    private Program program;

    @BeforeEach
    void setUp() {
        courseService = new CourseService(courseRepository, programRepository);
        department = createDepartment(1L, "Computer Science", "CS", "CS Department", "Dr. John");
        program = createProgram(1L, "Bachelor of CS", "BCS", DegreeType.BACHELOR, 4, department);
    }

    @Test
    void shouldCreateCourse() {
        CourseRequest request = new CourseRequest(
            "Data Structures",
            "CS201",
            4,
            3,
            1,
            1L,
            3
        );

        Course savedCourse = createCourse(1L, "Data Structures", "CS201", 4, 3, 1, program, 3);

        when(programRepository.findById(1L)).thenReturn(Optional.of(program));
        when(courseRepository.save(any(Course.class))).thenReturn(savedCourse);

        CourseResponse response = courseService.create(request);

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.name()).isEqualTo("Data Structures");
        assertThat(response.code()).isEqualTo("CS201");
        assertThat(response.credits()).isEqualTo(4);
        assertThat(response.theoryCredits()).isEqualTo(3);
        assertThat(response.labCredits()).isEqualTo(1);
        assertThat(response.semester()).isEqualTo(3);
        assertThat(response.program().id()).isEqualTo(1L);

        ArgumentCaptor<Course> captor = ArgumentCaptor.forClass(Course.class);
        verify(courseRepository).save(captor.capture());
        Course captured = captor.getValue();
        assertThat(captured.getName()).isEqualTo("Data Structures");
        assertThat(captured.getCode()).isEqualTo("CS201");
    }

    @Test
    void shouldThrowExceptionWhenCreatingCourseWithNonExistentProgram() {
        CourseRequest request = new CourseRequest(
            "Data Structures",
            "CS201",
            4,
            3,
            1,
            999L,
            3
        );

        when(programRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> courseService.create(request))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessage("Program not found with id: 999");

        verify(courseRepository, never()).save(any(Course.class));
    }

    @Test
    void shouldFindAllCourses() {
        Course course1 = createCourse(1L, "Data Structures", "CS201", 4, 3, 1, program, 3);
        Course course2 = createCourse(2L, "Algorithms", "CS301", 4, 4, 0, program, 5);

        when(courseRepository.findAll()).thenReturn(List.of(course1, course2));

        List<CourseResponse> responses = courseService.findAll();

        assertThat(responses).hasSize(2);
        assertThat(responses.get(0).name()).isEqualTo("Data Structures");
        assertThat(responses.get(1).name()).isEqualTo("Algorithms");
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
        Course course = createCourse(1L, "Data Structures", "CS201", 4, 3, 1, program, 3);

        when(courseRepository.findById(1L)).thenReturn(Optional.of(course));

        CourseResponse response = courseService.findById(1L);

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.name()).isEqualTo("Data Structures");
        assertThat(response.code()).isEqualTo("CS201");
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
        Course course1 = createCourse(1L, "Data Structures", "CS201", 4, 3, 1, program, 3);
        Course course2 = createCourse(2L, "Algorithms", "CS301", 4, 4, 0, program, 5);

        when(programRepository.existsById(1L)).thenReturn(true);
        when(courseRepository.findByProgramId(1L)).thenReturn(List.of(course1, course2));

        List<CourseResponse> responses = courseService.findByProgramId(1L);

        assertThat(responses).hasSize(2);
        assertThat(responses.get(0).name()).isEqualTo("Data Structures");
        assertThat(responses.get(1).name()).isEqualTo("Algorithms");
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
        Course existingCourse = createCourse(1L, "Data Structures", "CS201", 4, 3, 1, program, 3);

        Program newProgram = createProgram(2L, "Master of CS", "MCS", DegreeType.MASTER, 2, department);

        CourseRequest updateRequest = new CourseRequest(
            "Advanced Data Structures",
            "CS401",
            5,
            3,
            2,
            2L,
            4
        );

        Course updatedCourse = createCourse(1L, "Advanced Data Structures", "CS401", 5, 3, 2, newProgram, 4);

        when(courseRepository.findById(1L)).thenReturn(Optional.of(existingCourse));
        when(programRepository.findById(2L)).thenReturn(Optional.of(newProgram));
        when(courseRepository.save(any(Course.class))).thenReturn(updatedCourse);

        CourseResponse response = courseService.update(1L, updateRequest);

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.name()).isEqualTo("Advanced Data Structures");
        assertThat(response.code()).isEqualTo("CS401");
        assertThat(response.credits()).isEqualTo(5);
        assertThat(response.theoryCredits()).isEqualTo(3);
        assertThat(response.labCredits()).isEqualTo(2);
        assertThat(response.semester()).isEqualTo(4);
        assertThat(response.program().id()).isEqualTo(2L);

        verify(courseRepository).findById(1L);
        verify(courseRepository).save(any(Course.class));
    }

    @Test
    void shouldThrowExceptionWhenUpdatingNonExistentCourse() {
        CourseRequest request = new CourseRequest("Name", "CODE", 4, 3, 1, 1L, 1);

        when(courseRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> courseService.update(999L, request))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessage("Course not found with id: 999");

        verify(courseRepository).findById(999L);
        verify(courseRepository, never()).save(any(Course.class));
    }

    @Test
    void shouldThrowExceptionWhenUpdatingWithNonExistentProgram() {
        Course existingCourse = createCourse(1L, "Data Structures", "CS201", 4, 3, 1, program, 3);

        CourseRequest request = new CourseRequest("Name", "CODE", 4, 3, 1, 999L, 1);

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

        courseService.delete(1L);

        verify(courseRepository).existsById(1L);
        verify(courseRepository).deleteById(1L);
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

    private Department createDepartment(Long id, String name, String code,
                                        String description, String hodName) {
        Department dept = new Department(name, code, description, hodName);
        dept.setId(id);
        Instant now = Instant.now();
        dept.setCreatedAt(now);
        dept.setUpdatedAt(now);
        return dept;
    }

    private Program createProgram(Long id, String name, String code,
                                  DegreeType degreeType, Integer durationYears, Department dept) {
        Program prog = new Program(name, code, degreeType, durationYears, dept);
        prog.setId(id);
        Instant now = Instant.now();
        prog.setCreatedAt(now);
        prog.setUpdatedAt(now);
        return prog;
    }

    private Course createCourse(Long id, String name, String code, Integer credits,
                                Integer theoryCredits, Integer labCredits,
                                Program prog, Integer semester) {
        Course course = new Course(name, code, credits, theoryCredits, labCredits, prog, semester);
        course.setId(id);
        Instant now = Instant.now();
        course.setCreatedAt(now);
        course.setUpdatedAt(now);
        return course;
    }
}
