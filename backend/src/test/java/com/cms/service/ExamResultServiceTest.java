package com.cms.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
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

import com.cms.dto.ExamResultRequest;
import com.cms.dto.ExamResultResponse;
import com.cms.exception.ResourceNotFoundException;
import com.cms.model.Course;
import com.cms.model.ExamResult;
import com.cms.model.Examination;
import com.cms.model.Semester;
import com.cms.model.Student;
import com.cms.model.enums.ExamResultStatus;
import com.cms.model.enums.ExamType;
import com.cms.model.enums.StudentStatus;
import com.cms.repository.ExamResultRepository;
import com.cms.repository.ExaminationRepository;
import com.cms.repository.StudentRepository;

@ExtendWith(MockitoExtension.class)
class ExamResultServiceTest {

    @Mock
    private ExamResultRepository examResultRepository;

    @Mock
    private ExaminationRepository examinationRepository;

    @Mock
    private StudentRepository studentRepository;

    private ExamResultService examResultService;

    @BeforeEach
    void setUp() {
        examResultService = new ExamResultService(examResultRepository, examinationRepository, studentRepository);
    }

    @Test
    void shouldCreateExamResult() {
        Examination examination = createExamination();
        Student student = createStudent();
        ExamResultRequest request = new ExamResultRequest(
            1L, 1L, new BigDecimal("85.50"), "A", ExamResultStatus.PUBLISHED
        );
        ExamResult saved = createExamResult(examination, student);

        when(examinationRepository.findById(1L)).thenReturn(Optional.of(examination));
        when(studentRepository.findById(1L)).thenReturn(Optional.of(student));
        when(examResultRepository.save(any(ExamResult.class))).thenReturn(saved);

        ExamResultResponse response = examResultService.create(request);

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.grade()).isEqualTo("A");
        assertThat(response.status()).isEqualTo(ExamResultStatus.PUBLISHED);

        ArgumentCaptor<ExamResult> captor = ArgumentCaptor.forClass(ExamResult.class);
        verify(examResultRepository).save(captor.capture());
        assertThat(captor.getValue().getGrade()).isEqualTo("A");
    }

    @Test
    void shouldThrowWhenExaminationNotFound() {
        ExamResultRequest request = new ExamResultRequest(
            999L, 1L, new BigDecimal("85.50"), "A", ExamResultStatus.PUBLISHED
        );

        when(examinationRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> examResultService.create(request))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessage("Examination not found with id: 999");

        verify(examResultRepository, never()).save(any());
    }

    @Test
    void shouldThrowWhenStudentNotFound() {
        Examination examination = createExamination();
        ExamResultRequest request = new ExamResultRequest(
            1L, 999L, new BigDecimal("85.50"), "A", ExamResultStatus.PUBLISHED
        );

        when(examinationRepository.findById(1L)).thenReturn(Optional.of(examination));
        when(studentRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> examResultService.create(request))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessage("Student not found with id: 999");

        verify(examResultRepository, never()).save(any());
    }

    @Test
    void shouldFindByExaminationId() {
        Examination examination = createExamination();
        Student student = createStudent();
        ExamResult examResult = createExamResult(examination, student);

        when(examResultRepository.findByExaminationId(1L)).thenReturn(List.of(examResult));

        List<ExamResultResponse> responses = examResultService.findByExaminationId(1L);

        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).examinationId()).isEqualTo(1L);
        verify(examResultRepository).findByExaminationId(1L);
    }

    @Test
    void shouldFindByStudentId() {
        Examination examination = createExamination();
        Student student = createStudent();
        ExamResult examResult = createExamResult(examination, student);

        when(examResultRepository.findByStudentId(1L)).thenReturn(List.of(examResult));

        List<ExamResultResponse> responses = examResultService.findByStudentId(1L);

        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).studentId()).isEqualTo(1L);
        verify(examResultRepository).findByStudentId(1L);
    }

    @Test
    void shouldFindById() {
        Examination examination = createExamination();
        Student student = createStudent();
        ExamResult examResult = createExamResult(examination, student);

        when(examResultRepository.findById(1L)).thenReturn(Optional.of(examResult));

        ExamResultResponse response = examResultService.findById(1L);

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.grade()).isEqualTo("A");
        verify(examResultRepository).findById(1L);
    }

    @Test
    void shouldThrowWhenNotFoundById() {
        when(examResultRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> examResultService.findById(999L))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessage("Exam result not found with id: 999");
    }

    @Test
    void shouldUpdateExamResult() {
        Examination examination = createExamination();
        Student student = createStudent();
        ExamResult existing = createExamResult(examination, student);
        ExamResultRequest request = new ExamResultRequest(
            1L, 1L, new BigDecimal("90.00"), "A+", ExamResultStatus.PUBLISHED
        );
        ExamResult updated = createExamResult(examination, student);
        updated.setGrade("A+");
        updated.setMarksObtained(new BigDecimal("90.00"));

        when(examResultRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(examinationRepository.findById(1L)).thenReturn(Optional.of(examination));
        when(studentRepository.findById(1L)).thenReturn(Optional.of(student));
        when(examResultRepository.save(any(ExamResult.class))).thenReturn(updated);

        ExamResultResponse response = examResultService.update(1L, request);

        assertThat(response.grade()).isEqualTo("A+");
        verify(examResultRepository).findById(1L);
        verify(examResultRepository).save(any(ExamResult.class));
    }

    @Test
    void shouldDeleteExamResult() {
        when(examResultRepository.existsById(1L)).thenReturn(true);

        examResultService.delete(1L);

        verify(examResultRepository).existsById(1L);
        verify(examResultRepository).deleteById(1L);
    }

    @Test
    void shouldThrowWhenNotFoundOnDelete() {
        when(examResultRepository.existsById(999L)).thenReturn(false);

        assertThatThrownBy(() -> examResultService.delete(999L))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessage("Exam result not found with id: 999");

        verify(examResultRepository, never()).deleteById(any());
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

    private Examination createExamination() {
        Examination exam = new Examination("Midterm", createCourse(), ExamType.THEORY,
            LocalDate.of(2024, 6, 1), 120, 100, createSemester());
        exam.setId(1L);
        exam.setCreatedAt(Instant.now());
        exam.setUpdatedAt(Instant.now());
        return exam;
    }

    private Student createStudent() {
        Student student = new Student("ROLL001", "John", "Doe", "john@example.com",
            null, 1, LocalDate.of(2024, 1, 1), StudentStatus.ACTIVE);
        student.setId(1L);
        return student;
    }

    private ExamResult createExamResult(Examination examination, Student student) {
        ExamResult examResult = new ExamResult(examination, student,
            new BigDecimal("85.50"), "A", ExamResultStatus.PUBLISHED);
        examResult.setId(1L);
        examResult.setCreatedAt(Instant.now());
        examResult.setUpdatedAt(Instant.now());
        return examResult;
    }
}
