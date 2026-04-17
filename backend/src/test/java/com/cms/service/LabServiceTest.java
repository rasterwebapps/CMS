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

import com.cms.dto.LabInChargeAssignmentRequest;
import com.cms.dto.LabInChargeAssignmentResponse;
import com.cms.dto.LabRequest;
import com.cms.dto.LabResponse;
import com.cms.exception.ResourceNotFoundException;
import com.cms.model.Department;
import com.cms.model.Lab;
import com.cms.model.LabInChargeAssignment;
import com.cms.model.enums.LabInChargeRole;
import com.cms.model.enums.LabStatus;
import com.cms.model.enums.LabType;
import com.cms.repository.DepartmentRepository;
import com.cms.repository.LabInChargeAssignmentRepository;
import com.cms.repository.LabRepository;

@ExtendWith(MockitoExtension.class)
class LabServiceTest {

    @Mock
    private LabRepository labRepository;

    @Mock
    private DepartmentRepository departmentRepository;

    @Mock
    private LabInChargeAssignmentRepository assignmentRepository;

    private LabService labService;

    private Department department;

    @BeforeEach
    void setUp() {
        labService = new LabService(labRepository, departmentRepository, assignmentRepository);
        department = createDepartment(1L, "Computer Science", "CS", "CS Department", "Dr. John");
    }

    @Test
    void shouldCreateLab() {
        LabRequest request = new LabRequest(
            "Computer Lab 1",
            LabType.COMPUTER,
            1L,
            "Main Building",
            "101",
            30,
            LabStatus.ACTIVE
        );

        Lab savedLab = createLab(1L, "Computer Lab 1", LabType.COMPUTER, department,
            "Main Building", "101", 30, LabStatus.ACTIVE);

        when(departmentRepository.findById(1L)).thenReturn(Optional.of(department));
        when(labRepository.save(any(Lab.class))).thenReturn(savedLab);

        LabResponse response = labService.create(request);

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.name()).isEqualTo("Computer Lab 1");
        assertThat(response.labType()).isEqualTo(LabType.COMPUTER);
        assertThat(response.building()).isEqualTo("Main Building");
        assertThat(response.roomNumber()).isEqualTo("101");
        assertThat(response.capacity()).isEqualTo(30);
        assertThat(response.status()).isEqualTo(LabStatus.ACTIVE);
        assertThat(response.department().id()).isEqualTo(1L);

        ArgumentCaptor<Lab> captor = ArgumentCaptor.forClass(Lab.class);
        verify(labRepository).save(captor.capture());
        Lab captured = captor.getValue();
        assertThat(captured.getName()).isEqualTo("Computer Lab 1");
        assertThat(captured.getLabType()).isEqualTo(LabType.COMPUTER);
    }

    @Test
    void shouldThrowExceptionWhenCreatingLabWithNonExistentDepartment() {
        LabRequest request = new LabRequest(
            "Computer Lab 1",
            LabType.COMPUTER,
            999L,
            "Main Building",
            "101",
            30,
            LabStatus.ACTIVE
        );

        when(departmentRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> labService.create(request))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessage("Department not found with id: 999");

        verify(labRepository, never()).save(any(Lab.class));
    }

    @Test
    void shouldFindAllLabs() {
        Lab lab1 = createLab(1L, "Computer Lab 1", LabType.COMPUTER, department,
            "Main Building", "101", 30, LabStatus.ACTIVE);
        Lab lab2 = createLab(2L, "Physics Lab", LabType.PHYSICS, department,
            "Science Building", "201", 25, LabStatus.ACTIVE);

        when(labRepository.findAll()).thenReturn(List.of(lab1, lab2));

        List<LabResponse> responses = labService.findAll();

        assertThat(responses).hasSize(2);
        assertThat(responses.get(0).name()).isEqualTo("Computer Lab 1");
        assertThat(responses.get(1).name()).isEqualTo("Physics Lab");
        verify(labRepository).findAll();
    }

    @Test
    void shouldReturnEmptyListWhenNoLabs() {
        when(labRepository.findAll()).thenReturn(List.of());

        List<LabResponse> responses = labService.findAll();

        assertThat(responses).isEmpty();
        verify(labRepository).findAll();
    }

    @Test
    void shouldFindLabById() {
        Lab lab = createLab(1L, "Computer Lab 1", LabType.COMPUTER, department,
            "Main Building", "101", 30, LabStatus.ACTIVE);

        when(labRepository.findById(1L)).thenReturn(Optional.of(lab));

        LabResponse response = labService.findById(1L);

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.name()).isEqualTo("Computer Lab 1");
        assertThat(response.labType()).isEqualTo(LabType.COMPUTER);
        verify(labRepository).findById(1L);
    }

    @Test
    void shouldThrowExceptionWhenLabNotFoundById() {
        when(labRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> labService.findById(999L))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessage("Lab not found with id: 999");

        verify(labRepository).findById(999L);
    }

    @Test
    void shouldFindLabsByDepartmentId() {
        Lab lab1 = createLab(1L, "Computer Lab 1", LabType.COMPUTER, department,
            "Main Building", "101", 30, LabStatus.ACTIVE);
        Lab lab2 = createLab(2L, "Computer Lab 2", LabType.COMPUTER, department,
            "Main Building", "102", 25, LabStatus.ACTIVE);

        when(departmentRepository.existsById(1L)).thenReturn(true);
        when(labRepository.findByDepartmentId(1L)).thenReturn(List.of(lab1, lab2));

        List<LabResponse> responses = labService.findByDepartmentId(1L);

        assertThat(responses).hasSize(2);
        assertThat(responses.get(0).name()).isEqualTo("Computer Lab 1");
        assertThat(responses.get(1).name()).isEqualTo("Computer Lab 2");
        verify(labRepository).findByDepartmentId(1L);
    }

    @Test
    void shouldReturnEmptyListWhenNoLabsForDepartment() {
        when(departmentRepository.existsById(1L)).thenReturn(true);
        when(labRepository.findByDepartmentId(1L)).thenReturn(List.of());

        List<LabResponse> responses = labService.findByDepartmentId(1L);

        assertThat(responses).isEmpty();
        verify(labRepository).findByDepartmentId(1L);
    }

    @Test
    void shouldThrowExceptionWhenFindingLabsByNonExistentDepartment() {
        when(departmentRepository.existsById(999L)).thenReturn(false);

        assertThatThrownBy(() -> labService.findByDepartmentId(999L))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessage("Department not found with id: 999");

        verify(labRepository, never()).findByDepartmentId(any());
    }

    @Test
    void shouldUpdateLab() {
        Lab existingLab = createLab(1L, "Computer Lab 1", LabType.COMPUTER, department,
            "Main Building", "101", 30, LabStatus.ACTIVE);

        Department newDepartment = createDepartment(2L, "Electronics", "EC", "EC Dept", "Dr. Jane");

        LabRequest updateRequest = new LabRequest(
            "Electronics Lab Updated",
            LabType.ELECTRONICS,
            2L,
            "Science Building",
            "301",
            40,
            LabStatus.UNDER_MAINTENANCE
        );

        Lab updatedLab = createLab(1L, "Electronics Lab Updated", LabType.ELECTRONICS, newDepartment,
            "Science Building", "301", 40, LabStatus.UNDER_MAINTENANCE);

        when(labRepository.findById(1L)).thenReturn(Optional.of(existingLab));
        when(departmentRepository.findById(2L)).thenReturn(Optional.of(newDepartment));
        when(labRepository.existsByNameAndDepartmentIdAndIdNot("Electronics Lab Updated", 2L, 1L))
            .thenReturn(false);
        when(labRepository.save(any(Lab.class))).thenReturn(updatedLab);

        LabResponse response = labService.update(1L, updateRequest);

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.name()).isEqualTo("Electronics Lab Updated");
        assertThat(response.labType()).isEqualTo(LabType.ELECTRONICS);
        assertThat(response.building()).isEqualTo("Science Building");
        assertThat(response.roomNumber()).isEqualTo("301");
        assertThat(response.capacity()).isEqualTo(40);
        assertThat(response.status()).isEqualTo(LabStatus.UNDER_MAINTENANCE);
        assertThat(response.department().id()).isEqualTo(2L);

        verify(labRepository).findById(1L);
        verify(labRepository).save(any(Lab.class));
    }

    @Test
    void shouldThrowWhenUpdatingLabWithDuplicateNameInSameDepartment() {
        Lab existingLab = createLab(1L, "Computer Lab 1", LabType.COMPUTER, department,
            "Main Building", "101", 30, LabStatus.ACTIVE);

        LabRequest request = new LabRequest(
            "Computer Lab 2", LabType.COMPUTER, 1L, "Main Building", "102", 25, LabStatus.ACTIVE
        );

        when(labRepository.findById(1L)).thenReturn(Optional.of(existingLab));
        when(departmentRepository.findById(1L)).thenReturn(Optional.of(department));
        when(labRepository.existsByNameAndDepartmentIdAndIdNot("Computer Lab 2", 1L, 1L))
            .thenReturn(true);

        assertThatThrownBy(() -> labService.update(1L, request))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Computer Lab 2")
            .hasMessageContaining("already exists");

        verify(labRepository, never()).save(any(Lab.class));
    }

    @Test
    void shouldThrowExceptionWhenUpdatingNonExistentLab() {
        LabRequest request = new LabRequest("Name", LabType.COMPUTER, 1L, "Building", "101", 30, LabStatus.ACTIVE);

        when(labRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> labService.update(999L, request))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessage("Lab not found with id: 999");

        verify(labRepository).findById(999L);
        verify(labRepository, never()).save(any(Lab.class));
    }

    @Test
    void shouldThrowExceptionWhenUpdatingWithNonExistentDepartment() {
        Lab existingLab = createLab(1L, "Computer Lab 1", LabType.COMPUTER, department,
            "Main Building", "101", 30, LabStatus.ACTIVE);

        LabRequest request = new LabRequest("Name", LabType.COMPUTER, 999L, "Building", "101", 30, LabStatus.ACTIVE);

        when(labRepository.findById(1L)).thenReturn(Optional.of(existingLab));
        when(departmentRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> labService.update(1L, request))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessage("Department not found with id: 999");

        verify(labRepository, never()).save(any(Lab.class));
    }

    @Test
    void shouldDeleteLab() {
        when(labRepository.existsById(1L)).thenReturn(true);

        labService.delete(1L);

        verify(labRepository).existsById(1L);
        verify(assignmentRepository).deleteByLabId(1L);
        verify(labRepository).deleteById(1L);
    }

    @Test
    void shouldThrowExceptionWhenDeletingNonExistentLab() {
        when(labRepository.existsById(999L)).thenReturn(false);

        assertThatThrownBy(() -> labService.delete(999L))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessage("Lab not found with id: 999");

        verify(labRepository).existsById(999L);
        verify(labRepository, never()).deleteById(any());
    }

    @Test
    void shouldAssignInCharge() {
        Lab lab = createLab(1L, "Computer Lab 1", LabType.COMPUTER, department,
            "Main Building", "101", 30, LabStatus.ACTIVE);

        LabInChargeAssignmentRequest request = new LabInChargeAssignmentRequest(
            100L,
            "Dr. Smith",
            LabInChargeRole.LAB_INCHARGE,
            LocalDate.of(2024, 1, 15)
        );

        LabInChargeAssignment savedAssignment = createAssignment(1L, lab, 100L, "Dr. Smith",
            LabInChargeRole.LAB_INCHARGE, LocalDate.of(2024, 1, 15));

        when(labRepository.findById(1L)).thenReturn(Optional.of(lab));
        when(assignmentRepository.save(any(LabInChargeAssignment.class))).thenReturn(savedAssignment);

        LabInChargeAssignmentResponse response = labService.assignInCharge(1L, request);

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.labId()).isEqualTo(1L);
        assertThat(response.labName()).isEqualTo("Computer Lab 1");
        assertThat(response.assigneeId()).isEqualTo(100L);
        assertThat(response.assigneeName()).isEqualTo("Dr. Smith");
        assertThat(response.role()).isEqualTo(LabInChargeRole.LAB_INCHARGE);
        assertThat(response.assignedDate()).isEqualTo(LocalDate.of(2024, 1, 15));

        verify(assignmentRepository).save(any(LabInChargeAssignment.class));
    }

    @Test
    void shouldThrowExceptionWhenAssigningToNonExistentLab() {
        LabInChargeAssignmentRequest request = new LabInChargeAssignmentRequest(
            100L,
            "Dr. Smith",
            LabInChargeRole.LAB_INCHARGE,
            LocalDate.of(2024, 1, 15)
        );

        when(labRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> labService.assignInCharge(999L, request))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessage("Lab not found with id: 999");

        verify(assignmentRepository, never()).save(any(LabInChargeAssignment.class));
    }

    @Test
    void shouldRemoveAssignment() {
        Lab lab = createLab(1L, "Computer Lab 1", LabType.COMPUTER, department,
            "Main Building", "101", 30, LabStatus.ACTIVE);
        LabInChargeAssignment assignment = createAssignment(10L, lab, 100L, "Dr. Smith",
            LabInChargeRole.LAB_INCHARGE, LocalDate.of(2024, 1, 15));

        when(labRepository.existsById(1L)).thenReturn(true);
        when(assignmentRepository.findById(10L)).thenReturn(Optional.of(assignment));

        labService.removeAssignment(1L, 10L);

        verify(assignmentRepository).deleteById(10L);
    }

    @Test
    void shouldThrowExceptionWhenRemovingAssignmentFromNonExistentLab() {
        when(labRepository.existsById(999L)).thenReturn(false);

        assertThatThrownBy(() -> labService.removeAssignment(999L, 10L))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessage("Lab not found with id: 999");

        verify(assignmentRepository, never()).deleteById(any());
    }

    @Test
    void shouldThrowExceptionWhenRemovingNonExistentAssignment() {
        when(labRepository.existsById(1L)).thenReturn(true);
        when(assignmentRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> labService.removeAssignment(1L, 999L))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessage("Assignment not found with id: 999");

        verify(assignmentRepository, never()).deleteById(any());
    }

    @Test
    void shouldThrowExceptionWhenAssignmentDoesNotBelongToLab() {
        Lab lab1 = createLab(1L, "Computer Lab 1", LabType.COMPUTER, department,
            "Main Building", "101", 30, LabStatus.ACTIVE);
        Lab lab2 = createLab(2L, "Computer Lab 2", LabType.COMPUTER, department,
            "Main Building", "102", 25, LabStatus.ACTIVE);
        LabInChargeAssignment assignment = createAssignment(10L, lab2, 100L, "Dr. Smith",
            LabInChargeRole.LAB_INCHARGE, LocalDate.of(2024, 1, 15));

        when(labRepository.existsById(1L)).thenReturn(true);
        when(assignmentRepository.findById(10L)).thenReturn(Optional.of(assignment));

        assertThatThrownBy(() -> labService.removeAssignment(1L, 10L))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessage("Assignment 10 does not belong to lab 1");

        verify(assignmentRepository, never()).deleteById(any());
    }

    @Test
    void shouldFindAssignmentsByLabId() {
        Lab lab = createLab(1L, "Computer Lab 1", LabType.COMPUTER, department,
            "Main Building", "101", 30, LabStatus.ACTIVE);
        LabInChargeAssignment assignment1 = createAssignment(1L, lab, 100L, "Dr. Smith",
            LabInChargeRole.LAB_INCHARGE, LocalDate.of(2024, 1, 15));
        LabInChargeAssignment assignment2 = createAssignment(2L, lab, 101L, "Mr. Brown",
            LabInChargeRole.TECHNICIAN, LocalDate.of(2024, 2, 1));

        when(labRepository.existsById(1L)).thenReturn(true);
        when(assignmentRepository.findByLabId(1L)).thenReturn(List.of(assignment1, assignment2));

        List<LabInChargeAssignmentResponse> responses = labService.findAssignmentsByLabId(1L);

        assertThat(responses).hasSize(2);
        assertThat(responses.get(0).assigneeName()).isEqualTo("Dr. Smith");
        assertThat(responses.get(0).role()).isEqualTo(LabInChargeRole.LAB_INCHARGE);
        assertThat(responses.get(1).assigneeName()).isEqualTo("Mr. Brown");
        assertThat(responses.get(1).role()).isEqualTo(LabInChargeRole.TECHNICIAN);
        verify(assignmentRepository).findByLabId(1L);
    }

    @Test
    void shouldReturnEmptyListWhenNoAssignmentsForLab() {
        when(labRepository.existsById(1L)).thenReturn(true);
        when(assignmentRepository.findByLabId(1L)).thenReturn(List.of());

        List<LabInChargeAssignmentResponse> responses = labService.findAssignmentsByLabId(1L);

        assertThat(responses).isEmpty();
        verify(assignmentRepository).findByLabId(1L);
    }

    @Test
    void shouldThrowExceptionWhenFindingAssignmentsForNonExistentLab() {
        when(labRepository.existsById(999L)).thenReturn(false);

        assertThatThrownBy(() -> labService.findAssignmentsByLabId(999L))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessage("Lab not found with id: 999");

        verify(assignmentRepository, never()).findByLabId(any());
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

    private Lab createLab(Long id, String name, LabType labType, Department dept,
                          String building, String roomNumber, Integer capacity, LabStatus status) {
        Lab lab = new Lab(name, labType, dept, building, roomNumber, capacity, status);
        lab.setId(id);
        Instant now = Instant.now();
        lab.setCreatedAt(now);
        lab.setUpdatedAt(now);
        return lab;
    }

    private LabInChargeAssignment createAssignment(Long id, Lab lab, Long assigneeId,
                                                    String assigneeName, LabInChargeRole role, LocalDate assignedDate) {
        LabInChargeAssignment assignment = new LabInChargeAssignment(lab, assigneeId, assigneeName, role, assignedDate);
        assignment.setId(id);
        Instant now = Instant.now();
        assignment.setCreatedAt(now);
        assignment.setUpdatedAt(now);
        return assignment;
    }
}
