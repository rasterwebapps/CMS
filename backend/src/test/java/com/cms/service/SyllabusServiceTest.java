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

import com.cms.dto.SyllabusRequest;
import com.cms.dto.SyllabusResponse;
import com.cms.exception.ResourceNotFoundException;
import com.cms.model.Course;
import com.cms.model.Program;
import com.cms.model.Syllabus;
import com.cms.repository.CourseRepository;
import com.cms.repository.SyllabusRepository;

@ExtendWith(MockitoExtension.class)
class SyllabusServiceTest {

    @Mock
    private SyllabusRepository syllabusRepository;

    @Mock
    private CourseRepository courseRepository;

    private SyllabusService syllabusService;

    private Course testCourse;

    @BeforeEach
    void setUp() {
        syllabusService = new SyllabusService(syllabusRepository, courseRepository);
        testCourse = createCourse(1L, "Data Structures", "CS201");
    }

    @Test
    void shouldCreateSyllabus() {
        SyllabusRequest request = new SyllabusRequest(
            1L, 1, 30, 15, 10,
            "Course objectives", "Course content",
            "Text books", "Reference books",
            "Course outcomes", true
        );

        Syllabus savedSyllabus = createSyllabus(1L, testCourse, 1, true);

        when(courseRepository.findById(1L)).thenReturn(Optional.of(testCourse));
        when(syllabusRepository.save(any(Syllabus.class))).thenReturn(savedSyllabus);

        SyllabusResponse response = syllabusService.create(request);

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.courseId()).isEqualTo(1L);
        assertThat(response.version()).isEqualTo(1);
        assertThat(response.isActive()).isTrue();

        ArgumentCaptor<Syllabus> captor = ArgumentCaptor.forClass(Syllabus.class);
        verify(syllabusRepository).save(captor.capture());
        assertThat(captor.getValue().getVersion()).isEqualTo(1);
    }

    @Test
    void shouldThrowExceptionWhenCreatingSyllabusWithNonExistentCourse() {
        SyllabusRequest request = new SyllabusRequest(
            999L, 1, 30, 15, 10,
            "Objectives", "Content", "Text", "Ref", "CO", true
        );

        when(courseRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> syllabusService.create(request))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessage("Course not found with id: 999");

        verify(syllabusRepository, never()).save(any(Syllabus.class));
    }

    @Test
    void shouldFindAllSyllabi() {
        Syllabus syllabus1 = createSyllabus(1L, testCourse, 1, true);
        Syllabus syllabus2 = createSyllabus(2L, testCourse, 2, false);

        when(syllabusRepository.findAll()).thenReturn(List.of(syllabus1, syllabus2));

        List<SyllabusResponse> responses = syllabusService.findAll();

        assertThat(responses).hasSize(2);
        assertThat(responses.get(0).version()).isEqualTo(1);
        assertThat(responses.get(1).version()).isEqualTo(2);
    }

    @Test
    void shouldFindSyllabusById() {
        Syllabus syllabus = createSyllabus(1L, testCourse, 1, true);

        when(syllabusRepository.findById(1L)).thenReturn(Optional.of(syllabus));

        SyllabusResponse response = syllabusService.findById(1L);

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.courseId()).isEqualTo(1L);
    }

    @Test
    void shouldThrowExceptionWhenSyllabusNotFoundById() {
        when(syllabusRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> syllabusService.findById(999L))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessage("Syllabus not found with id: 999");
    }

    @Test
    void shouldFindSyllabusByCourseId() {
        Syllabus syllabus = createSyllabus(1L, testCourse, 1, true);

        when(courseRepository.existsById(1L)).thenReturn(true);
        when(syllabusRepository.findByCourseId(1L)).thenReturn(List.of(syllabus));

        List<SyllabusResponse> responses = syllabusService.findByCourseId(1L);

        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).courseId()).isEqualTo(1L);
    }

    @Test
    void shouldUpdateSyllabus() {
        Syllabus existingSyllabus = createSyllabus(1L, testCourse, 1, true);

        SyllabusRequest updateRequest = new SyllabusRequest(
            1L, 2, 40, 20, 15,
            "Updated objectives", "Updated content",
            "Updated text books", "Updated ref books",
            "Updated outcomes", true
        );

        Syllabus updatedSyllabus = createSyllabus(1L, testCourse, 2, true);

        when(syllabusRepository.findById(1L)).thenReturn(Optional.of(existingSyllabus));
        when(courseRepository.findById(1L)).thenReturn(Optional.of(testCourse));
        when(syllabusRepository.save(any(Syllabus.class))).thenReturn(updatedSyllabus);

        SyllabusResponse response = syllabusService.update(1L, updateRequest);

        assertThat(response.version()).isEqualTo(2);
        verify(syllabusRepository).save(any(Syllabus.class));
    }

    @Test
    void shouldDeleteSyllabus() {
        when(syllabusRepository.existsById(1L)).thenReturn(true);

        syllabusService.delete(1L);

        verify(syllabusRepository).deleteById(1L);
    }

    @Test
    void shouldThrowExceptionWhenDeletingNonExistentSyllabus() {
        when(syllabusRepository.existsById(999L)).thenReturn(false);

        assertThatThrownBy(() -> syllabusService.delete(999L))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessage("Syllabus not found with id: 999");

        verify(syllabusRepository, never()).deleteById(any());
    }

    private Course createCourse(Long id, String name, String code) {
        Program program = new Program();
        program.setId(1L);
        program.setName("Computer Science");

        Course course = new Course(name, code, 3, 2, 1, program, 1);
        course.setId(id);
        return course;
    }

    private Syllabus createSyllabus(Long id, Course course, Integer version, Boolean isActive) {
        Syllabus syllabus = new Syllabus(
            course, version, 30, 15, 10,
            "Objectives", "Content", "Text books",
            "Reference books", "Course outcomes", isActive
        );
        syllabus.setId(id);
        Instant now = Instant.now();
        syllabus.setCreatedAt(now);
        syllabus.setUpdatedAt(now);
        return syllabus;
    }
}
