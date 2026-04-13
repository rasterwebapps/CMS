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

import com.cms.dto.DepartmentRequest;
import com.cms.dto.DepartmentResponse;
import com.cms.exception.ResourceNotFoundException;
import com.cms.model.Department;
import com.cms.repository.DepartmentRepository;

@ExtendWith(MockitoExtension.class)
class DepartmentServiceTest {

    @Mock
    private DepartmentRepository departmentRepository;

    private DepartmentService departmentService;

    @BeforeEach
    void setUp() {
        departmentService = new DepartmentService(departmentRepository);
    }

    @Test
    void shouldCreateDepartment() {
        DepartmentRequest request = new DepartmentRequest(
            "Computer Science",
            "CS",
            "Department of Computer Science",
            "Dr. John Doe"
        );

        Department savedDepartment = createDepartment(1L, "Computer Science", "CS",
            "Department of Computer Science", "Dr. John Doe");

        when(departmentRepository.save(any(Department.class))).thenReturn(savedDepartment);

        DepartmentResponse response = departmentService.create(request);

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.name()).isEqualTo("Computer Science");
        assertThat(response.code()).isEqualTo("CS");
        assertThat(response.description()).isEqualTo("Department of Computer Science");
        assertThat(response.hodName()).isEqualTo("Dr. John Doe");

        ArgumentCaptor<Department> captor = ArgumentCaptor.forClass(Department.class);
        verify(departmentRepository).save(captor.capture());
        Department captured = captor.getValue();
        assertThat(captured.getName()).isEqualTo("Computer Science");
        assertThat(captured.getCode()).isEqualTo("CS");
    }

    @Test
    void shouldFindAllDepartments() {
        Department dept1 = createDepartment(1L, "Computer Science", "CS", "CS Dept", "Dr. A");
        Department dept2 = createDepartment(2L, "Mathematics", "MATH", "Math Dept", "Dr. B");

        when(departmentRepository.findAll()).thenReturn(List.of(dept1, dept2));

        List<DepartmentResponse> responses = departmentService.findAll();

        assertThat(responses).hasSize(2);
        assertThat(responses.get(0).name()).isEqualTo("Computer Science");
        assertThat(responses.get(1).name()).isEqualTo("Mathematics");
        verify(departmentRepository).findAll();
    }

    @Test
    void shouldReturnEmptyListWhenNoDepartments() {
        when(departmentRepository.findAll()).thenReturn(List.of());

        List<DepartmentResponse> responses = departmentService.findAll();

        assertThat(responses).isEmpty();
        verify(departmentRepository).findAll();
    }

    @Test
    void shouldFindDepartmentById() {
        Department department = createDepartment(1L, "Computer Science", "CS",
            "Department of Computer Science", "Dr. John Doe");

        when(departmentRepository.findById(1L)).thenReturn(Optional.of(department));

        DepartmentResponse response = departmentService.findById(1L);

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.name()).isEqualTo("Computer Science");
        assertThat(response.code()).isEqualTo("CS");
        verify(departmentRepository).findById(1L);
    }

    @Test
    void shouldThrowExceptionWhenDepartmentNotFoundById() {
        when(departmentRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> departmentService.findById(999L))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessage("Department not found with id: 999");

        verify(departmentRepository).findById(999L);
    }

    @Test
    void shouldUpdateDepartment() {
        Department existingDepartment = createDepartment(1L, "Computer Science", "CS",
            "Old Description", "Dr. Old");

        DepartmentRequest updateRequest = new DepartmentRequest(
            "Computer Science Updated",
            "CSU",
            "New Description",
            "Dr. New"
        );

        Department updatedDepartment = createDepartment(1L, "Computer Science Updated", "CSU",
            "New Description", "Dr. New");

        when(departmentRepository.findById(1L)).thenReturn(Optional.of(existingDepartment));
        when(departmentRepository.save(any(Department.class))).thenReturn(updatedDepartment);

        DepartmentResponse response = departmentService.update(1L, updateRequest);

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.name()).isEqualTo("Computer Science Updated");
        assertThat(response.code()).isEqualTo("CSU");
        assertThat(response.description()).isEqualTo("New Description");
        assertThat(response.hodName()).isEqualTo("Dr. New");

        verify(departmentRepository).findById(1L);
        verify(departmentRepository).save(any(Department.class));
    }

    @Test
    void shouldThrowExceptionWhenUpdatingNonExistentDepartment() {
        DepartmentRequest request = new DepartmentRequest("Name", "CODE", "Desc", "HOD");

        when(departmentRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> departmentService.update(999L, request))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessage("Department not found with id: 999");

        verify(departmentRepository).findById(999L);
        verify(departmentRepository, never()).save(any(Department.class));
    }

    @Test
    void shouldDeleteDepartment() {
        when(departmentRepository.existsById(1L)).thenReturn(true);

        departmentService.delete(1L);

        verify(departmentRepository).existsById(1L);
        verify(departmentRepository).deleteById(1L);
    }

    @Test
    void shouldThrowExceptionWhenDeletingNonExistentDepartment() {
        when(departmentRepository.existsById(999L)).thenReturn(false);

        assertThatThrownBy(() -> departmentService.delete(999L))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessage("Department not found with id: 999");

        verify(departmentRepository).existsById(999L);
        verify(departmentRepository, never()).deleteById(any());
    }

    private Department createDepartment(Long id, String name, String code,
                                        String description, String hodName) {
        Department department = new Department(name, code, description, hodName);
        department.setId(id);
        Instant now = Instant.now();
        department.setCreatedAt(now);
        department.setUpdatedAt(now);
        return department;
    }
}
