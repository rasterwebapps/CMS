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

import com.cms.dto.FacultyRequest;
import com.cms.dto.FacultyResponse;
import com.cms.exception.ResourceNotFoundException;
import com.cms.model.Department;
import com.cms.model.Faculty;
import com.cms.model.enums.Designation;
import com.cms.model.enums.FacultyStatus;
import com.cms.repository.DepartmentRepository;
import com.cms.repository.FacultyRepository;

@ExtendWith(MockitoExtension.class)
class FacultyServiceTest {

    @Mock
    private FacultyRepository facultyRepository;

    @Mock
    private DepartmentRepository departmentRepository;

    private FacultyService facultyService;

    private Department testDepartment;

    @BeforeEach
    void setUp() {
        facultyService = new FacultyService(facultyRepository, departmentRepository);
        testDepartment = createDepartment(1L, "Computer Science", "CS");
    }

    @Test
    void shouldCreateFaculty() {
        FacultyRequest request = new FacultyRequest(
            "EMP001",
            "John",
            "Doe",
            "john.doe@college.edu",
            "1234567890",
            1L,
            Designation.PROFESSOR,
            "Artificial Intelligence",
            "Machine Learning Lab",
            LocalDate.of(2020, 1, 15),
            FacultyStatus.ACTIVE
        );

        Faculty savedFaculty = createFaculty(1L, "EMP001", "John", "Doe", "john.doe@college.edu",
            testDepartment, Designation.PROFESSOR, FacultyStatus.ACTIVE);

        when(departmentRepository.findById(1L)).thenReturn(Optional.of(testDepartment));
        when(facultyRepository.save(any(Faculty.class))).thenReturn(savedFaculty);

        FacultyResponse response = facultyService.create(request);

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.employeeCode()).isEqualTo("EMP001");
        assertThat(response.firstName()).isEqualTo("John");
        assertThat(response.lastName()).isEqualTo("Doe");
        assertThat(response.fullName()).isEqualTo("John Doe");
        assertThat(response.email()).isEqualTo("john.doe@college.edu");
        assertThat(response.departmentId()).isEqualTo(1L);
        assertThat(response.departmentName()).isEqualTo("Computer Science");
        assertThat(response.designation()).isEqualTo(Designation.PROFESSOR);
        assertThat(response.status()).isEqualTo(FacultyStatus.ACTIVE);

        ArgumentCaptor<Faculty> captor = ArgumentCaptor.forClass(Faculty.class);
        verify(facultyRepository).save(captor.capture());
        Faculty captured = captor.getValue();
        assertThat(captured.getEmployeeCode()).isEqualTo("EMP001");
        assertThat(captured.getFirstName()).isEqualTo("John");
    }

    @Test
    void shouldCreateFacultyWithDefaultActiveStatus() {
        FacultyRequest request = new FacultyRequest(
            "EMP002",
            "Jane",
            "Smith",
            "jane.smith@college.edu",
            "0987654321",
            1L,
            Designation.ASSISTANT_PROFESSOR,
            "Data Science",
            "Big Data Lab",
            LocalDate.of(2021, 6, 1),
            null  // status is null
        );

        Faculty savedFaculty = createFaculty(2L, "EMP002", "Jane", "Smith", "jane.smith@college.edu",
            testDepartment, Designation.ASSISTANT_PROFESSOR, FacultyStatus.ACTIVE);

        when(departmentRepository.findById(1L)).thenReturn(Optional.of(testDepartment));
        when(facultyRepository.save(any(Faculty.class))).thenReturn(savedFaculty);

        FacultyResponse response = facultyService.create(request);

        assertThat(response.status()).isEqualTo(FacultyStatus.ACTIVE);

        ArgumentCaptor<Faculty> captor = ArgumentCaptor.forClass(Faculty.class);
        verify(facultyRepository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(FacultyStatus.ACTIVE);
    }

    @Test
    void shouldThrowExceptionWhenCreatingFacultyWithNonExistentDepartment() {
        FacultyRequest request = new FacultyRequest(
            "EMP001",
            "John",
            "Doe",
            "john.doe@college.edu",
            "1234567890",
            999L,
            Designation.PROFESSOR,
            "AI",
            "ML Lab",
            LocalDate.of(2020, 1, 15),
            FacultyStatus.ACTIVE
        );

        when(departmentRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> facultyService.create(request))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessage("Department not found with id: 999");

        verify(facultyRepository, never()).save(any(Faculty.class));
    }

    @Test
    void shouldFindAllFaculty() {
        Faculty faculty1 = createFaculty(1L, "EMP001", "John", "Doe", "john@college.edu",
            testDepartment, Designation.PROFESSOR, FacultyStatus.ACTIVE);
        Faculty faculty2 = createFaculty(2L, "EMP002", "Jane", "Smith", "jane@college.edu",
            testDepartment, Designation.ASSISTANT_PROFESSOR, FacultyStatus.ACTIVE);

        when(facultyRepository.findAll()).thenReturn(List.of(faculty1, faculty2));

        List<FacultyResponse> responses = facultyService.findAll();

        assertThat(responses).hasSize(2);
        assertThat(responses.get(0).employeeCode()).isEqualTo("EMP001");
        assertThat(responses.get(1).employeeCode()).isEqualTo("EMP002");
        verify(facultyRepository).findAll();
    }

    @Test
    void shouldReturnEmptyListWhenNoFaculty() {
        when(facultyRepository.findAll()).thenReturn(List.of());

        List<FacultyResponse> responses = facultyService.findAll();

        assertThat(responses).isEmpty();
        verify(facultyRepository).findAll();
    }

    @Test
    void shouldFindFacultyById() {
        Faculty faculty = createFaculty(1L, "EMP001", "John", "Doe", "john@college.edu",
            testDepartment, Designation.PROFESSOR, FacultyStatus.ACTIVE);

        when(facultyRepository.findById(1L)).thenReturn(Optional.of(faculty));

        FacultyResponse response = facultyService.findById(1L);

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.employeeCode()).isEqualTo("EMP001");
        assertThat(response.fullName()).isEqualTo("John Doe");
        verify(facultyRepository).findById(1L);
    }

    @Test
    void shouldThrowExceptionWhenFacultyNotFoundById() {
        when(facultyRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> facultyService.findById(999L))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessage("Faculty not found with id: 999");

        verify(facultyRepository).findById(999L);
    }

    @Test
    void shouldFindFacultyByDepartmentId() {
        Faculty faculty1 = createFaculty(1L, "EMP001", "John", "Doe", "john@college.edu",
            testDepartment, Designation.PROFESSOR, FacultyStatus.ACTIVE);
        Faculty faculty2 = createFaculty(2L, "EMP002", "Jane", "Smith", "jane@college.edu",
            testDepartment, Designation.ASSISTANT_PROFESSOR, FacultyStatus.ACTIVE);

        when(departmentRepository.existsById(1L)).thenReturn(true);
        when(facultyRepository.findByDepartmentId(1L)).thenReturn(List.of(faculty1, faculty2));

        List<FacultyResponse> responses = facultyService.findByDepartmentId(1L);

        assertThat(responses).hasSize(2);
        assertThat(responses.get(0).departmentId()).isEqualTo(1L);
        assertThat(responses.get(1).departmentId()).isEqualTo(1L);
        verify(facultyRepository).findByDepartmentId(1L);
    }

    @Test
    void shouldThrowExceptionWhenFindingByNonExistentDepartmentId() {
        when(departmentRepository.existsById(999L)).thenReturn(false);

        assertThatThrownBy(() -> facultyService.findByDepartmentId(999L))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessage("Department not found with id: 999");

        verify(facultyRepository, never()).findByDepartmentId(any());
    }

    @Test
    void shouldFindFacultyByStatus() {
        Faculty faculty = createFaculty(1L, "EMP001", "John", "Doe", "john@college.edu",
            testDepartment, Designation.PROFESSOR, FacultyStatus.ON_LEAVE);

        when(facultyRepository.findByStatus(FacultyStatus.ON_LEAVE)).thenReturn(List.of(faculty));

        List<FacultyResponse> responses = facultyService.findByStatus(FacultyStatus.ON_LEAVE);

        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).status()).isEqualTo(FacultyStatus.ON_LEAVE);
        verify(facultyRepository).findByStatus(FacultyStatus.ON_LEAVE);
    }

    @Test
    void shouldUpdateFaculty() {
        Faculty existingFaculty = createFaculty(1L, "EMP001", "John", "Doe", "john@college.edu",
            testDepartment, Designation.PROFESSOR, FacultyStatus.ACTIVE);

        Department newDepartment = createDepartment(2L, "Mathematics", "MATH");

        FacultyRequest updateRequest = new FacultyRequest(
            "EMP001-UPD",
            "John Updated",
            "Doe Updated",
            "john.updated@college.edu",
            "9999999999",
            2L,
            Designation.ASSOCIATE_PROFESSOR,
            "Applied Mathematics",
            "Math Lab",
            LocalDate.of(2019, 1, 1),
            FacultyStatus.ON_LEAVE
        );

        Faculty updatedFaculty = createFaculty(1L, "EMP001-UPD", "John Updated", "Doe Updated",
            "john.updated@college.edu", newDepartment, Designation.ASSOCIATE_PROFESSOR, FacultyStatus.ON_LEAVE);

        when(facultyRepository.findById(1L)).thenReturn(Optional.of(existingFaculty));
        when(departmentRepository.findById(2L)).thenReturn(Optional.of(newDepartment));
        when(facultyRepository.save(any(Faculty.class))).thenReturn(updatedFaculty);

        FacultyResponse response = facultyService.update(1L, updateRequest);

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.employeeCode()).isEqualTo("EMP001-UPD");
        assertThat(response.fullName()).isEqualTo("John Updated Doe Updated");
        assertThat(response.departmentId()).isEqualTo(2L);
        assertThat(response.designation()).isEqualTo(Designation.ASSOCIATE_PROFESSOR);
        assertThat(response.status()).isEqualTo(FacultyStatus.ON_LEAVE);

        verify(facultyRepository).findById(1L);
        verify(departmentRepository).findById(2L);
        verify(facultyRepository).save(any(Faculty.class));
    }

    @Test
    void shouldThrowExceptionWhenUpdatingNonExistentFaculty() {
        FacultyRequest request = new FacultyRequest(
            "EMP001", "John", "Doe", "john@college.edu", "123",
            1L, Designation.PROFESSOR, "AI", "ML Lab",
            LocalDate.of(2020, 1, 1), FacultyStatus.ACTIVE
        );

        when(facultyRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> facultyService.update(999L, request))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessage("Faculty not found with id: 999");

        verify(facultyRepository).findById(999L);
        verify(facultyRepository, never()).save(any(Faculty.class));
    }

    @Test
    void shouldThrowExceptionWhenUpdatingWithNonExistentDepartment() {
        Faculty existingFaculty = createFaculty(1L, "EMP001", "John", "Doe", "john@college.edu",
            testDepartment, Designation.PROFESSOR, FacultyStatus.ACTIVE);

        FacultyRequest request = new FacultyRequest(
            "EMP001", "John", "Doe", "john@college.edu", "123",
            999L, Designation.PROFESSOR, "AI", "ML Lab",
            LocalDate.of(2020, 1, 1), FacultyStatus.ACTIVE
        );

        when(facultyRepository.findById(1L)).thenReturn(Optional.of(existingFaculty));
        when(departmentRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> facultyService.update(1L, request))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessage("Department not found with id: 999");

        verify(facultyRepository, never()).save(any(Faculty.class));
    }

    @Test
    void shouldDeleteFaculty() {
        when(facultyRepository.existsById(1L)).thenReturn(true);

        facultyService.delete(1L);

        verify(facultyRepository).existsById(1L);
        verify(facultyRepository).deleteById(1L);
    }

    @Test
    void shouldThrowExceptionWhenDeletingNonExistentFaculty() {
        when(facultyRepository.existsById(999L)).thenReturn(false);

        assertThatThrownBy(() -> facultyService.delete(999L))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessage("Faculty not found with id: 999");

        verify(facultyRepository).existsById(999L);
        verify(facultyRepository, never()).deleteById(any());
    }

    private Department createDepartment(Long id, String name, String code) {
        Department department = new Department(name, code, "Description", "Dr. HOD");
        department.setId(id);
        Instant now = Instant.now();
        department.setCreatedAt(now);
        department.setUpdatedAt(now);
        return department;
    }

    private Faculty createFaculty(Long id, String employeeCode, String firstName, String lastName,
                                   String email, Department department, Designation designation,
                                   FacultyStatus status) {
        Faculty faculty = new Faculty(
            employeeCode,
            firstName,
            lastName,
            email,
            "1234567890",
            department,
            designation,
            "Specialization",
            "Lab Expertise",
            LocalDate.of(2020, 1, 15),
            status
        );
        faculty.setId(id);
        Instant now = Instant.now();
        faculty.setCreatedAt(now);
        faculty.setUpdatedAt(now);
        return faculty;
    }
}
