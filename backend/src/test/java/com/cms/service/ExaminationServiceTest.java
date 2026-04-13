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

import com.cms.dto.ExaminationRequest;
import com.cms.dto.ExaminationResponse;
import com.cms.exception.ResourceNotFoundException;
import com.cms.model.Course;
import com.cms.model.Examination;
import com.cms.model.Semester;
import com.cms.model.enums.ExamType;
import com.cms.repository.CourseRepository;
import com.cms.repository.ExaminationRepository;
import com.cms.repository.SemesterRepository;

@ExtendWith(MockitoExtension.class)
class ExaminationServiceTest {

    @Mock
    private ExaminationRepository examinationRepository;

    @Mock
    private CourseRepository courseRepository;

    @Mock
    private SemesterRepository semesterRepository;

    private ExaminationService examinationService;

    @BeforeEach
    void setUp() {
        examinationService = new ExaminationService(examinationRepository, courseRepository, semesterRepository);
    }

    @Test
    void shouldCreateExamination() {
        Course course = createCourse();
        Semester semester = createSemester();
        ExaminationRequest request = new ExaminationRequest(
            "Midterm", 1L, ExamType.THEORY, LocalDate.of(2024, 6, 1), 120, 100, 1L
        );
        Examination saved = createExamination(course, semester);

        when(courseRepository.findById(1L)).thenReturn(Optional.of(course));
        when(semesterRepository.findById(1L)).thenReturn(Optional.of(semester));
        when(examinationRepository.save(any(Examination.class))).thenReturn(saved);

        ExaminationResponse response = examinationService.create(request);

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.name()).isEqualTo("Midterm");
        assertThat(response.examType()).isEqualTo(ExamType.THEORY);
        assertThat(response.courseId()).isEqualTo(1L);
        assertThat(response.semesterId()).isEqualTo(1L);

        ArgumentCaptor<Examination> captor = ArgumentCaptor.forClass(Examination.class);
        verify(examinationRepository).save(captor.capture());
        assertThat(captor.getValue().getName()).isEqualTo("Midterm");
    }

    @Test
    void shouldCreateExaminationWithNullSemester() {
        Course course = createCourse();
        ExaminationRequest request = new ExaminationRequest(
            "Midterm", 1L, ExamType.THEORY, LocalDate.of(2024, 6, 1), 120, 100, null
        );
        Examination saved = createExaminationNoSemester(course);

        when(courseRepository.findById(1L)).thenReturn(Optional.of(course));
        when(examinationRepository.save(any(Examination.class))).thenReturn(saved);

        ExaminationResponse response = examinationService.create(request);

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.semesterId()).isNull();

        verify(semesterRepository, never()).findById(any());
    }

    @Test
    void shouldThrowWhenCourseNotFoundOnCreate() {
        ExaminationRequest request = new ExaminationRequest(
            "Midterm", 999L, ExamType.THEORY, LocalDate.of(2024, 6, 1), 120, 100, null
        );

        when(courseRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> examinationService.create(request))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessage("Course not found with id: 999");

        verify(examinationRepository, never()).save(any());
    }

    @Test
    void shouldFindAllExaminations() {
        Course course = createCourse();
        Semester semester = createSemester();
        Examination exam1 = createExamination(course, semester);
        Examination exam2 = createExamination(course, semester);
        exam2.setId(2L);
        exam2.setName("Final");

        when(examinationRepository.findAll()).thenReturn(List.of(exam1, exam2));

        List<ExaminationResponse> responses = examinationService.findAll();

        assertThat(responses).hasSize(2);
        assertThat(responses.get(0).name()).isEqualTo("Midterm");
        assertThat(responses.get(1).name()).isEqualTo("Final");
        verify(examinationRepository).findAll();
    }

    @Test
    void shouldReturnEmptyList() {
        when(examinationRepository.findAll()).thenReturn(List.of());

        List<ExaminationResponse> responses = examinationService.findAll();

        assertThat(responses).isEmpty();
        verify(examinationRepository).findAll();
    }

    @Test
    void shouldFindById() {
        Course course = createCourse();
        Semester semester = createSemester();
        Examination exam = createExamination(course, semester);

        when(examinationRepository.findById(1L)).thenReturn(Optional.of(exam));

        ExaminationResponse response = examinationService.findById(1L);

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.name()).isEqualTo("Midterm");
        verify(examinationRepository).findById(1L);
    }

    @Test
    void shouldThrowWhenNotFoundById() {
        when(examinationRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> examinationService.findById(999L))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessage("Examination not found with id: 999");
    }

    @Test
    void shouldFindByCourseId() {
        Course course = createCourse();
        Semester semester = createSemester();
        Examination exam = createExamination(course, semester);

        when(examinationRepository.findByCourseId(1L)).thenReturn(List.of(exam));

        List<ExaminationResponse> responses = examinationService.findByCourseId(1L);

        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).courseId()).isEqualTo(1L);
        verify(examinationRepository).findByCourseId(1L);
    }

    @Test
    void shouldFindBySemesterId() {
        Course course = createCourse();
        Semester semester = createSemester();
        Examination exam = createExamination(course, semester);

        when(examinationRepository.findBySemesterId(1L)).thenReturn(List.of(exam));

        List<ExaminationResponse> responses = examinationService.findBySemesterId(1L);

        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).semesterId()).isEqualTo(1L);
        verify(examinationRepository).findBySemesterId(1L);
    }

    @Test
    void shouldUpdateExamination() {
        Course course = createCourse();
        Semester semester = createSemester();
        Examination existing = createExamination(course, semester);
        ExaminationRequest request = new ExaminationRequest(
            "Final Exam", 1L, ExamType.PRACTICAL, LocalDate.of(2024, 7, 1), 90, 50, 1L
        );
        Examination updated = createExamination(course, semester);
        updated.setName("Final Exam");
        updated.setExamType(ExamType.PRACTICAL);

        when(examinationRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(courseRepository.findById(1L)).thenReturn(Optional.of(course));
        when(semesterRepository.findById(1L)).thenReturn(Optional.of(semester));
        when(examinationRepository.save(any(Examination.class))).thenReturn(updated);

        ExaminationResponse response = examinationService.update(1L, request);

        assertThat(response.name()).isEqualTo("Final Exam");
        assertThat(response.examType()).isEqualTo(ExamType.PRACTICAL);
        verify(examinationRepository).findById(1L);
        verify(examinationRepository).save(any(Examination.class));
    }

    @Test
    void shouldThrowWhenNotFoundOnUpdate() {
        ExaminationRequest request = new ExaminationRequest(
            "Midterm", 1L, ExamType.THEORY, LocalDate.of(2024, 6, 1), 120, 100, 1L
        );

        when(examinationRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> examinationService.update(999L, request))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessage("Examination not found with id: 999");

        verify(examinationRepository, never()).save(any());
    }

    @Test
    void shouldDeleteExamination() {
        when(examinationRepository.existsById(1L)).thenReturn(true);

        examinationService.delete(1L);

        verify(examinationRepository).existsById(1L);
        verify(examinationRepository).deleteById(1L);
    }

    @Test
    void shouldThrowWhenNotFoundOnDelete() {
        when(examinationRepository.existsById(999L)).thenReturn(false);

        assertThatThrownBy(() -> examinationService.delete(999L))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessage("Examination not found with id: 999");

        verify(examinationRepository, never()).deleteById(any());
    }

    private Course createCourse() {
        Course course = new Course("Physics", "PHY101", 4, 3, 1, null, 1);
        course.setId(1L);
        return course;
    }

    private Semester createSemester() {
        Semester semester = new Semester("Semester 1", null, LocalDate.of(2024, 1, 1),
            LocalDate.of(2024, 6, 30), 1);
        semester.setId(1L);
        return semester;
    }

    private Examination createExamination(Course course, Semester semester) {
        Examination exam = new Examination("Midterm", course, ExamType.THEORY,
            LocalDate.of(2024, 6, 1), 120, 100, semester);
        exam.setId(1L);
        exam.setCreatedAt(Instant.now());
        exam.setUpdatedAt(Instant.now());
        return exam;
    }

    private Examination createExaminationNoSemester(Course course) {
        Examination exam = new Examination("Midterm", course, ExamType.THEORY,
            LocalDate.of(2024, 6, 1), 120, 100, null);
        exam.setId(1L);
        exam.setCreatedAt(Instant.now());
        exam.setUpdatedAt(Instant.now());
        return exam;
    }
}
