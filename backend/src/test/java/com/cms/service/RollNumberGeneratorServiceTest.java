package com.cms.service;

import com.cms.dto.GenerateRollNumbersRequest;
import com.cms.dto.RollNumberAssignment;
import com.cms.exception.ResourceNotFoundException;
import com.cms.model.Course;
import com.cms.model.RollNumberSequence;
import com.cms.model.Student;
import com.cms.repository.CourseRepository;
import com.cms.repository.RollNumberSequenceRepository;
import com.cms.repository.StudentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RollNumberGeneratorServiceTest {

    @Mock
    private StudentRepository studentRepository;

    @Mock
    private CourseRepository courseRepository;

    @Mock
    private RollNumberSequenceRepository rollNumberSequenceRepository;

    @Mock
    private SystemConfigurationService systemConfigurationService;

    @InjectMocks
    private RollNumberGeneratorService rollNumberGeneratorService;

    private Course course;
    private List<Student> students;

    @BeforeEach
    void setUp() {
        course = new Course();
        course.setId(1L);
        course.setRollNumberCode("65");

        Student student1 = new Student();
        student1.setId(1L);
        student1.setFirstName("Alice");
        student1.setLastName("Brown");

        Student student2 = new Student();
        student2.setId(2L);
        student2.setFirstName("Bob");
        student2.setLastName("Anderson");

        students = Arrays.asList(student1, student2);
    }

    @Test
    void shouldGenerateAndAssignRollNumbers() {
        // Arrange
        GenerateRollNumbersRequest request = new GenerateRollNumbersRequest(
                Arrays.asList(1L, 2L),
                1L,
                2026
        );

        RollNumberSequence sequence = new RollNumberSequence();
        sequence.setCourseId(1L);
        sequence.setAcademicYear(2026);
        sequence.setLastSequence(0);

        when(studentRepository.findById(1L)).thenReturn(Optional.of(students.get(0)));
        when(studentRepository.findById(2L)).thenReturn(Optional.of(students.get(1)));
        when(courseRepository.findById(1L)).thenReturn(Optional.of(course));
        when(systemConfigurationService.getValueByKey("ROLL_NUMBER_COLLEGE_CODE")).thenReturn("959");
        when(rollNumberSequenceRepository.findByCourseIdAndAcademicYearForUpdate(1L, 2026))
                .thenReturn(Optional.of(sequence));
        when(studentRepository.save(any(Student.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        List<RollNumberAssignment> assignments = rollNumberGeneratorService.generateAndAssignRollNumbers(request);

        // Assert
        assertThat(assignments).hasSize(2);
        assertThat(assignments.get(0).rollNumber()).startsWith("959652026");
        assertThat(assignments.get(1).rollNumber()).startsWith("959652026");

        verify(studentRepository, times(2)).save(any(Student.class));
        verify(rollNumberSequenceRepository, times(2)).save(any(RollNumberSequence.class));
    }

    @Test
    void shouldThrowExceptionWhenCourseNotFound() {
        // Arrange
        GenerateRollNumbersRequest request = new GenerateRollNumbersRequest(
                Arrays.asList(1L, 2L),
                999L,
                2026
        );

        when(courseRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> rollNumberGeneratorService.generateAndAssignRollNumbers(request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Course not found");
    }

    @Test
    void shouldPreviewRollNumbers() {
        // Arrange
        GenerateRollNumbersRequest request = new GenerateRollNumbersRequest(
                Arrays.asList(1L, 2L),
                1L,
                2026
        );

        RollNumberSequence sequence = new RollNumberSequence();
        sequence.setCourseId(1L);
        sequence.setAcademicYear(2026);
        sequence.setLastSequence(0);

        when(studentRepository.findById(1L)).thenReturn(Optional.of(students.get(0)));
        when(studentRepository.findById(2L)).thenReturn(Optional.of(students.get(1)));
        when(courseRepository.findById(1L)).thenReturn(Optional.of(course));
        when(systemConfigurationService.getValueByKey("ROLL_NUMBER_COLLEGE_CODE")).thenReturn("959");
        when(rollNumberSequenceRepository.findByCourseIdAndAcademicYear(1L, 2026))
                .thenReturn(Optional.of(sequence));

        // Act
        List<RollNumberAssignment> preview = rollNumberGeneratorService.previewRollNumbers(request);

        // Assert
        assertThat(preview).hasSize(2);
        assertThat(preview.get(0).rollNumber()).matches("959652026\\d{3}");
        
        // Verify that no save operations were performed
        verify(studentRepository, never()).save(any());
        verify(rollNumberSequenceRepository, never()).save(any());
    }

    @Test
    void shouldSortStudentsAlphabetically() {
        // Arrange
        Student student1 = new Student();
        student1.setId(1L);
        student1.setFirstName("Zara");
        student1.setLastName("Smith");

        Student student2 = new Student();
        student2.setId(2L);
        student2.setFirstName("Adam");
        student2.setLastName("Brown");

        List<Student> unsortedStudents = Arrays.asList(student1, student2);

        GenerateRollNumbersRequest request = new GenerateRollNumbersRequest(
                Arrays.asList(1L, 2L),
                1L,
                2026
        );

        RollNumberSequence sequence = new RollNumberSequence();
        sequence.setCourseId(1L);
        sequence.setAcademicYear(2026);
        sequence.setLastSequence(0);

        when(studentRepository.findById(1L)).thenReturn(Optional.of(student1));
        when(studentRepository.findById(2L)).thenReturn(Optional.of(student2));
        when(courseRepository.findById(1L)).thenReturn(Optional.of(course));
        when(systemConfigurationService.getValueByKey("ROLL_NUMBER_COLLEGE_CODE")).thenReturn("959");
        when(rollNumberSequenceRepository.findByCourseIdAndAcademicYearForUpdate(1L, 2026))
                .thenReturn(Optional.of(sequence));
        when(studentRepository.save(any(Student.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        List<RollNumberAssignment> assignments = rollNumberGeneratorService.generateAndAssignRollNumbers(request);

        // Assert
        assertThat(assignments).hasSize(2);
        // Adam Brown should come before Zara Smith
        assertThat(assignments.get(0).studentName()).isEqualTo("Adam Brown");
        assertThat(assignments.get(1).studentName()).isEqualTo("Zara Smith");
    }

    @Test
    void shouldCreateNewSequenceIfNotExists() {
        // Arrange
        GenerateRollNumbersRequest request = new GenerateRollNumbersRequest(
                Arrays.asList(1L, 2L),
                1L,
                2027
        );

        when(studentRepository.findById(1L)).thenReturn(Optional.of(students.get(0)));
        when(studentRepository.findById(2L)).thenReturn(Optional.of(students.get(1)));
        when(courseRepository.findById(1L)).thenReturn(Optional.of(course));
        when(systemConfigurationService.getValueByKey("ROLL_NUMBER_COLLEGE_CODE")).thenReturn("959");
        when(rollNumberSequenceRepository.findByCourseIdAndAcademicYearForUpdate(1L, 2027))
                .thenReturn(Optional.empty());
        when(rollNumberSequenceRepository.save(any(RollNumberSequence.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(studentRepository.save(any(Student.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        List<RollNumberAssignment> assignments = rollNumberGeneratorService.generateAndAssignRollNumbers(request);

        // Assert
        assertThat(assignments).hasSize(2);
        // Verify sequence was created and then updated for each student
        verify(rollNumberSequenceRepository, atLeast(1)).save(any(RollNumberSequence.class));
    }

    @Test
    void shouldThrowExceptionWhenCourseHasNoRollNumberCode() {
        // Arrange
        course.setRollNumberCode(null);
        GenerateRollNumbersRequest request = new GenerateRollNumbersRequest(
                Arrays.asList(1L),
                1L,
                2026
        );

        when(courseRepository.findById(1L)).thenReturn(Optional.of(course));

        // Act & Assert
        assertThatThrownBy(() -> rollNumberGeneratorService.generateAndAssignRollNumbers(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("has no roll number code configured");
    }

    @Test
    void shouldThrowExceptionWhenStudentNotFound() {
        // Arrange
        GenerateRollNumbersRequest request = new GenerateRollNumbersRequest(
                Arrays.asList(999L),
                1L,
                2026
        );

        when(courseRepository.findById(1L)).thenReturn(Optional.of(course));
        when(studentRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> rollNumberGeneratorService.generateAndAssignRollNumbers(request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Student not found");
    }

    @Test
    void shouldThrowExceptionWhenStudentAlreadyHasRollNumber() {
        // Arrange
        Student student = new Student();
        student.setId(1L);
        student.setFirstName("Alice");
        student.setLastName("Brown");
        student.setRollNumber("959652026001"); // Already has a roll number

        GenerateRollNumbersRequest request = new GenerateRollNumbersRequest(
                Arrays.asList(1L),
                1L,
                2026
        );

        when(courseRepository.findById(1L)).thenReturn(Optional.of(course));
        when(studentRepository.findById(1L)).thenReturn(Optional.of(student));

        // Act & Assert
        assertThatThrownBy(() -> rollNumberGeneratorService.generateAndAssignRollNumbers(request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("already has a roll number");
    }

    @Test
    void shouldUseDefaultCollegeCodeWhenConfigNotFound() {
        // Arrange
        GenerateRollNumbersRequest request = new GenerateRollNumbersRequest(
                Arrays.asList(1L),
                1L,
                2026
        );

        RollNumberSequence sequence = new RollNumberSequence();
        sequence.setCourseId(1L);
        sequence.setAcademicYear(2026);
        sequence.setLastSequence(0);

        when(studentRepository.findById(1L)).thenReturn(Optional.of(students.get(0)));
        when(courseRepository.findById(1L)).thenReturn(Optional.of(course));
        when(systemConfigurationService.getValueByKey("ROLL_NUMBER_COLLEGE_CODE"))
                .thenThrow(new RuntimeException("Config not found"));
        when(rollNumberSequenceRepository.findByCourseIdAndAcademicYearForUpdate(1L, 2026))
                .thenReturn(Optional.of(sequence));
        when(studentRepository.save(any(Student.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        List<RollNumberAssignment> assignments = rollNumberGeneratorService.generateAndAssignRollNumbers(request);

        // Assert
        assertThat(assignments).hasSize(1);
        assertThat(assignments.get(0).rollNumber()).startsWith("000"); // Default college code
    }

    @Test
    void shouldThrowExceptionWhenSequenceExceedsMaximum() {
        // Arrange
        GenerateRollNumbersRequest request = new GenerateRollNumbersRequest(
                Arrays.asList(1L),
                1L,
                2026
        );

        RollNumberSequence sequence = new RollNumberSequence();
        sequence.setCourseId(1L);
        sequence.setAcademicYear(2026);
        sequence.setLastSequence(999); // At maximum

        when(studentRepository.findById(1L)).thenReturn(Optional.of(students.get(0)));
        when(courseRepository.findById(1L)).thenReturn(Optional.of(course));
        when(systemConfigurationService.getValueByKey("ROLL_NUMBER_COLLEGE_CODE")).thenReturn("959");
        when(rollNumberSequenceRepository.findByCourseIdAndAcademicYearForUpdate(1L, 2026))
                .thenReturn(Optional.of(sequence));

        // Act & Assert
        assertThatThrownBy(() -> rollNumberGeneratorService.generateAndAssignRollNumbers(request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Sequence number exceeded maximum");
    }

    @Test
    void shouldThrowExceptionInPreviewWhenCourseNotFound() {
        // Arrange
        GenerateRollNumbersRequest request = new GenerateRollNumbersRequest(
                Arrays.asList(1L),
                999L,
                2026
        );

        when(courseRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> rollNumberGeneratorService.previewRollNumbers(request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Course not found");
    }

    @Test
    void shouldThrowExceptionInPreviewWhenCourseHasNoRollNumberCode() {
        // Arrange
        course.setRollNumberCode(null);
        GenerateRollNumbersRequest request = new GenerateRollNumbersRequest(
                Arrays.asList(1L),
                1L,
                2026
        );

        when(courseRepository.findById(1L)).thenReturn(Optional.of(course));

        // Act & Assert
        assertThatThrownBy(() -> rollNumberGeneratorService.previewRollNumbers(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("has no roll number code configured");
    }

    @Test
    void shouldThrowExceptionInPreviewWhenStudentNotFound() {
        // Arrange
        GenerateRollNumbersRequest request = new GenerateRollNumbersRequest(
                Arrays.asList(999L),
                1L,
                2026
        );

        when(courseRepository.findById(1L)).thenReturn(Optional.of(course));
        when(studentRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> rollNumberGeneratorService.previewRollNumbers(request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Student not found");
    }

    @Test
    void shouldThrowExceptionInPreviewWhenSequenceExceedsMaximum() {
        // Arrange
        GenerateRollNumbersRequest request = new GenerateRollNumbersRequest(
                Arrays.asList(1L),
                1L,
                2026
        );

        RollNumberSequence sequence = new RollNumberSequence();
        sequence.setCourseId(1L);
        sequence.setAcademicYear(2026);
        sequence.setLastSequence(999); // At maximum

        when(studentRepository.findById(1L)).thenReturn(Optional.of(students.get(0)));
        when(courseRepository.findById(1L)).thenReturn(Optional.of(course));
        when(systemConfigurationService.getValueByKey("ROLL_NUMBER_COLLEGE_CODE")).thenReturn("959");
        when(rollNumberSequenceRepository.findByCourseIdAndAcademicYear(1L, 2026))
                .thenReturn(Optional.of(sequence));

        // Act & Assert
        assertThatThrownBy(() -> rollNumberGeneratorService.previewRollNumbers(request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Sequence number exceeded maximum");
    }

    @Test
    void shouldUseDefaultCollegeCodeWhenConfigIsNull() {
        // Arrange
        GenerateRollNumbersRequest request = new GenerateRollNumbersRequest(
                Arrays.asList(1L),
                1L,
                2026
        );

        RollNumberSequence sequence = new RollNumberSequence();
        sequence.setCourseId(1L);
        sequence.setAcademicYear(2026);
        sequence.setLastSequence(0);

        when(studentRepository.findById(1L)).thenReturn(Optional.of(students.get(0)));
        when(courseRepository.findById(1L)).thenReturn(Optional.of(course));
        when(systemConfigurationService.getValueByKey("ROLL_NUMBER_COLLEGE_CODE")).thenReturn(null);
        when(rollNumberSequenceRepository.findByCourseIdAndAcademicYearForUpdate(1L, 2026))
                .thenReturn(Optional.of(sequence));
        when(studentRepository.save(any(Student.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        List<RollNumberAssignment> assignments = rollNumberGeneratorService.generateAndAssignRollNumbers(request);

        // Assert
        assertThat(assignments).hasSize(1);
        assertThat(assignments.get(0).rollNumber()).startsWith("000"); // Default college code
    }
}
