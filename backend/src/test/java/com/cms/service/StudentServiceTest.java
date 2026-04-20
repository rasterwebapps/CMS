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

import com.cms.dto.StudentRequest;
import com.cms.dto.StudentResponse;
import com.cms.exception.ResourceNotFoundException;
import com.cms.model.Department;
import com.cms.model.Program;
import com.cms.model.Student;
import com.cms.model.enums.StudentStatus;
import com.cms.repository.ProgramRepository;
import com.cms.repository.StudentRepository;

@ExtendWith(MockitoExtension.class)
class StudentServiceTest {

    @Mock
    private StudentRepository studentRepository;

    @Mock
    private ProgramRepository programRepository;

    @Mock
    private com.cms.repository.CourseRepository courseRepository;

    @Mock
    private com.cms.repository.DepartmentRepository departmentRepository;

    private StudentService studentService;

    private Program testProgram;

    @BeforeEach
    void setUp() {
        studentService = new StudentService(studentRepository, programRepository, courseRepository, departmentRepository);

        testProgram = new Program();
        testProgram.setId(1L);
        testProgram.setName("B.Tech Computer Science");
    }

    @Test
    void shouldCreateStudent() {
        StudentRequest request = new StudentRequest(
            "CS2024001", "John", "Doe", "john@college.edu", "1234567890",
            1L, null, null, 1, LocalDate.of(2024, 6, 1), "Batch-A", StudentStatus.ACTIVE,
            LocalDate.of(2005, 5, 15), null, null,
            "Indian", null, null, null, null,
            "Father Name", "Mother Name", "9876543210",
            null
        );

        Student savedStudent = createStudent(1L, "CS2024001", "John", "Doe", "john@college.edu");

        when(programRepository.findById(1L)).thenReturn(Optional.of(testProgram));
        when(studentRepository.save(any(Student.class))).thenReturn(savedStudent);

        StudentResponse response = studentService.create(request);

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.rollNumber()).isEqualTo("CS2024001");
        assertThat(response.firstName()).isEqualTo("John");
        assertThat(response.fullName()).isEqualTo("John Doe");

        ArgumentCaptor<Student> captor = ArgumentCaptor.forClass(Student.class);
        verify(studentRepository).save(captor.capture());
        assertThat(captor.getValue().getRollNumber()).isEqualTo("CS2024001");
    }

    @Test
    void shouldThrowExceptionWhenCreatingStudentWithNonExistentProgram() {
        StudentRequest request = new StudentRequest(
            "CS2024001", "John", "Doe", "john@college.edu", "1234567890",
            999L, null, null, 1, LocalDate.of(2024, 6, 1), null, null,
            null, null, null, null, null, null, null, null,
            null, null, null, null
        );

        when(programRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> studentService.create(request))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessage("Program not found with id: 999");

        verify(studentRepository, never()).save(any(Student.class));
    }

    @Test
    void shouldFindAllStudents() {
        Student student1 = createStudent(1L, "CS2024001", "John", "Doe", "john@college.edu");
        Student student2 = createStudent(2L, "CS2024002", "Jane", "Smith", "jane@college.edu");

        when(studentRepository.findAll()).thenReturn(List.of(student1, student2));

        List<StudentResponse> responses = studentService.findAll();

        assertThat(responses).hasSize(2);
        assertThat(responses.get(0).rollNumber()).isEqualTo("CS2024001");
        assertThat(responses.get(1).rollNumber()).isEqualTo("CS2024002");
    }

    @Test
    void shouldFindStudentById() {
        Student student = createStudent(1L, "CS2024001", "John", "Doe", "john@college.edu");

        when(studentRepository.findById(1L)).thenReturn(Optional.of(student));

        StudentResponse response = studentService.findById(1L);

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.rollNumber()).isEqualTo("CS2024001");
    }

    @Test
    void shouldThrowExceptionWhenStudentNotFoundById() {
        when(studentRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> studentService.findById(999L))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessage("Student not found with id: 999");
    }

    @Test
    void shouldFindStudentByRollNumber() {
        Student student = createStudent(1L, "CS2024001", "John", "Doe", "john@college.edu");

        when(studentRepository.findByRollNumber("CS2024001")).thenReturn(Optional.of(student));

        StudentResponse response = studentService.findByRollNumber("CS2024001");

        assertThat(response.rollNumber()).isEqualTo("CS2024001");
    }

    @Test
    void shouldFindStudentsByProgramId() {
        Student student = createStudent(1L, "CS2024001", "John", "Doe", "john@college.edu");

        when(programRepository.existsById(1L)).thenReturn(true);
        when(studentRepository.findByProgramId(1L)).thenReturn(List.of(student));

        List<StudentResponse> responses = studentService.findByProgramId(1L);

        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).programId()).isEqualTo(1L);
    }

    @Test
    void shouldFindStudentsByStatus() {
        Student student = createStudent(1L, "CS2024001", "John", "Doe", "john@college.edu");
        student.setStatus(StudentStatus.ACTIVE);

        when(studentRepository.findByStatus(StudentStatus.ACTIVE)).thenReturn(List.of(student));

        List<StudentResponse> responses = studentService.findByStatus(StudentStatus.ACTIVE);

        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).status()).isEqualTo(StudentStatus.ACTIVE);
    }

    @Test
    void shouldUpdateStudent() {
        Student existingStudent = createStudent(1L, "CS2024001", "John", "Doe", "john@college.edu");

        StudentRequest updateRequest = new StudentRequest(
            "CS2024001", "Johnny", "Doe", "johnny@college.edu", "9999999999",
            1L, null, null, 2, LocalDate.of(2024, 6, 1), "Batch-B", StudentStatus.ACTIVE,
            null, null, null, null, null, null, null, null,
            null, null, null, null
        );

        Student updatedStudent = createStudent(1L, "CS2024001", "Johnny", "Doe", "johnny@college.edu");

        when(studentRepository.findById(1L)).thenReturn(Optional.of(existingStudent));
        when(programRepository.findById(1L)).thenReturn(Optional.of(testProgram));
        when(studentRepository.save(any(Student.class))).thenReturn(updatedStudent);

        StudentResponse response = studentService.update(1L, updateRequest);

        assertThat(response.firstName()).isEqualTo("Johnny");
        verify(studentRepository).save(any(Student.class));
    }

    @Test
    void shouldDeleteStudent() {
        when(studentRepository.existsById(1L)).thenReturn(true);

        studentService.delete(1L);

        verify(studentRepository).deleteById(1L);
    }

    @Test
    void shouldThrowExceptionWhenDeletingNonExistentStudent() {
        when(studentRepository.existsById(999L)).thenReturn(false);

        assertThatThrownBy(() -> studentService.delete(999L))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessage("Student not found with id: 999");

        verify(studentRepository, never()).deleteById(any());
    }

    @Test
    void shouldCreateStudentWithoutRollNumber() {
        StudentRequest request = new StudentRequest(
            null, "John", "Doe", "john@college.edu", "1234567890",
            1L, null, null, 1, LocalDate.of(2024, 6, 1), "Batch-A", StudentStatus.ACTIVE,
            null, null, null, null, null, null, null, null,
            null, null, null, null
        );

        Student savedStudent = createStudent(1L, null, "John", "Doe", "john@college.edu");

        when(programRepository.findById(1L)).thenReturn(Optional.of(testProgram));
        when(studentRepository.save(any(Student.class))).thenReturn(savedStudent);

        StudentResponse response = studentService.create(request);

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.rollNumber()).isNull();
    }

    @Test
    void shouldAssignRollNumber() {
        Student student = createStudent(1L, null, "John", "Doe", "john@college.edu");
        Student updated = createStudent(1L, "CS2024001", "John", "Doe", "john@college.edu");

        when(studentRepository.findById(1L)).thenReturn(Optional.of(student));
        when(studentRepository.existsByRollNumber("CS2024001")).thenReturn(false);
        when(studentRepository.save(any(Student.class))).thenReturn(updated);

        StudentResponse response = studentService.assignRollNumber(1L, "CS2024001");

        assertThat(response.rollNumber()).isEqualTo("CS2024001");
        verify(studentRepository).save(any(Student.class));
    }

    @Test
    void shouldRejectRollNumberAssignmentIfAlreadySet() {
        Student student = createStudent(1L, "EXISTING001", "John", "Doe", "john@college.edu");

        when(studentRepository.findById(1L)).thenReturn(Optional.of(student));

        assertThatThrownBy(() -> studentService.assignRollNumber(1L, "CS2024001"))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("Student already has a roll number");
    }

    @Test
    void shouldRejectDuplicateRollNumber() {
        Student student = createStudent(1L, null, "John", "Doe", "john@college.edu");

        when(studentRepository.findById(1L)).thenReturn(Optional.of(student));
        when(studentRepository.existsByRollNumber("CS2024001")).thenReturn(true);

        assertThatThrownBy(() -> studentService.assignRollNumber(1L, "CS2024001"))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("Roll number already in use");
    }

    @Test
    void shouldBulkAssignRollNumbers() {
        Student student1 = createStudent(1L, null, "John", "Doe", "john@college.edu");
        Student student2 = createStudent(2L, null, "Jane", "Smith", "jane@college.edu");
        Student updated1 = createStudent(1L, "CS2024001", "John", "Doe", "john@college.edu");
        Student updated2 = createStudent(2L, "CS2024002", "Jane", "Smith", "jane@college.edu");

        when(studentRepository.findById(1L)).thenReturn(Optional.of(student1));
        when(studentRepository.findById(2L)).thenReturn(Optional.of(student2));
        when(studentRepository.existsByRollNumber("CS2024001")).thenReturn(false);
        when(studentRepository.existsByRollNumber("CS2024002")).thenReturn(false);
        when(studentRepository.save(student1)).thenReturn(updated1);
        when(studentRepository.save(student2)).thenReturn(updated2);

        List<com.cms.dto.StudentResponse> responses = studentService.bulkAssignRollNumbers(List.of(
            new com.cms.dto.BulkRollNumberItem(1L, "CS2024001"),
            new com.cms.dto.BulkRollNumberItem(2L, "CS2024002")
        ));

        assertThat(responses).hasSize(2);
        assertThat(responses.get(0).rollNumber()).isEqualTo("CS2024001");
        assertThat(responses.get(1).rollNumber()).isEqualTo("CS2024002");
    }

    @Test
    void shouldFindStudentsWithoutRollNumber() {
        Student student = createStudent(1L, null, "John", "Doe", "john@college.edu");

        when(studentRepository.findByRollNumberIsNull()).thenReturn(List.of(student));

        List<StudentResponse> responses = studentService.findStudentsWithoutRollNumber(null, null);

        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).rollNumber()).isNull();
    }

    @Test
    void shouldFindStudentsWithoutRollNumberByCourse() {
        Student student = createStudent(1L, null, "John", "Doe", "john@college.edu");

        when(studentRepository.findByCourseIdAndRollNumberIsNull(5L)).thenReturn(List.of(student));

        List<StudentResponse> responses = studentService.findStudentsWithoutRollNumber(5L, null);

        assertThat(responses).hasSize(1);
        verify(studentRepository).findByCourseIdAndRollNumberIsNull(5L);
    }

    @Test
    void shouldFindStudentsWithoutRollNumberByProgram() {
        Student student = createStudent(1L, null, "John", "Doe", "john@college.edu");

        when(studentRepository.findByProgramIdAndRollNumberIsNull(1L)).thenReturn(List.of(student));

        List<StudentResponse> responses = studentService.findStudentsWithoutRollNumber(null, 1L);

        assertThat(responses).hasSize(1);
        verify(studentRepository).findByProgramIdAndRollNumberIsNull(1L);
    }

    private Student createStudent(Long id, String rollNumber, String firstName, String lastName, String email) {
        Student student = new Student(
            rollNumber, firstName, lastName, email,
            testProgram, 1, LocalDate.of(2024, 6, 1), StudentStatus.ACTIVE
        );
        student.setId(id);
        Instant now = Instant.now();
        student.setCreatedAt(now);
        student.setUpdatedAt(now);
        return student;
    }
}
