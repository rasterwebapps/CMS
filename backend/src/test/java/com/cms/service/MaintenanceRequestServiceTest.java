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
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.cms.dto.MaintenanceRequestDto;
import com.cms.dto.MaintenanceRequestResponse;
import com.cms.exception.ResourceNotFoundException;
import com.cms.model.Department;
import com.cms.model.Equipment;
import com.cms.model.Lab;
import com.cms.model.MaintenanceRequest;
import com.cms.model.enums.EquipmentCategory;
import com.cms.model.enums.EquipmentStatus;
import com.cms.model.enums.LabStatus;
import com.cms.model.enums.LabType;
import com.cms.model.enums.MaintenancePriority;
import com.cms.model.enums.MaintenanceStatus;
import com.cms.model.enums.MaintenanceType;
import com.cms.repository.EquipmentRepository;
import com.cms.repository.FacultyRepository;
import com.cms.repository.MaintenanceRequestRepository;

@ExtendWith(MockitoExtension.class)
class MaintenanceRequestServiceTest {

    @Mock
    private MaintenanceRequestRepository maintenanceRequestRepository;
    @Mock
    private EquipmentRepository equipmentRepository;
    @Mock
    private FacultyRepository facultyRepository;

    private MaintenanceRequestService maintenanceRequestService;

    private Lab testLab;
    private Equipment testEquipment;

    @BeforeEach
    void setUp() {
        maintenanceRequestService = new MaintenanceRequestService(
            maintenanceRequestRepository, equipmentRepository, facultyRepository
        );

        Department dept = new Department("Computer Science", "CS", "CS Dept", "Dr. Smith");
        dept.setId(1L);

        testLab = new Lab("Lab 1", LabType.COMPUTER, dept, "Main Building", "L001", 30, LabStatus.ACTIVE);
        testLab.setId(1L);

        testEquipment = new Equipment("Dell Computer", "ASSET001", EquipmentCategory.COMPUTER, testLab, EquipmentStatus.AVAILABLE);
        testEquipment.setId(1L);
    }

    @Test
    void shouldCreateMaintenanceRequest() {
        MaintenanceRequestDto request = new MaintenanceRequestDto(
            1L, "Screen not working", "Monitor shows no display", MaintenanceType.CORRECTIVE,
            MaintenancePriority.HIGH, MaintenanceStatus.REQUESTED, null, LocalDate.now(),
            null, null, null, null, null, null
        );

        MaintenanceRequest saved = createMaintenanceRequest(1L, testEquipment, "Screen not working",
            MaintenanceType.CORRECTIVE, MaintenancePriority.HIGH, MaintenanceStatus.REQUESTED);

        when(equipmentRepository.findById(1L)).thenReturn(Optional.of(testEquipment));
        when(maintenanceRequestRepository.save(any(MaintenanceRequest.class))).thenReturn(saved);

        MaintenanceRequestResponse response = maintenanceRequestService.create(request);

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.title()).isEqualTo("Screen not working");
    }

    @Test
    void shouldCreateMaintenanceRequestWithInProgressStatus() {
        MaintenanceRequestDto request = new MaintenanceRequestDto(
            1L, "Screen not working", "Monitor shows no display", MaintenanceType.CORRECTIVE,
            MaintenancePriority.HIGH, MaintenanceStatus.IN_PROGRESS, null, LocalDate.now(),
            null, null, null, null, null, null
        );

        MaintenanceRequest saved = createMaintenanceRequest(1L, testEquipment, "Screen not working",
            MaintenanceType.CORRECTIVE, MaintenancePriority.HIGH, MaintenanceStatus.IN_PROGRESS);

        when(equipmentRepository.findById(1L)).thenReturn(Optional.of(testEquipment));
        when(maintenanceRequestRepository.save(any(MaintenanceRequest.class))).thenReturn(saved);

        MaintenanceRequestResponse response = maintenanceRequestService.create(request);

        assertThat(response.status()).isEqualTo(MaintenanceStatus.IN_PROGRESS);
        verify(equipmentRepository).save(any(Equipment.class));
    }

    @Test
    void shouldThrowExceptionWhenEquipmentNotFound() {
        MaintenanceRequestDto request = new MaintenanceRequestDto(
            999L, "Screen not working", null, MaintenanceType.CORRECTIVE,
            MaintenancePriority.HIGH, MaintenanceStatus.REQUESTED, null, LocalDate.now(),
            null, null, null, null, null, null
        );

        when(equipmentRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> maintenanceRequestService.create(request))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessage("Equipment not found with id: 999");
    }

    @Test
    void shouldFindAllRequests() {
        MaintenanceRequest mr = createMaintenanceRequest(1L, testEquipment, "Screen not working",
            MaintenanceType.CORRECTIVE, MaintenancePriority.HIGH, MaintenanceStatus.REQUESTED);

        when(maintenanceRequestRepository.findAll()).thenReturn(List.of(mr));

        List<MaintenanceRequestResponse> responses = maintenanceRequestService.findAll();

        assertThat(responses).hasSize(1);
    }

    @Test
    void shouldFindById() {
        MaintenanceRequest mr = createMaintenanceRequest(1L, testEquipment, "Screen not working",
            MaintenanceType.CORRECTIVE, MaintenancePriority.HIGH, MaintenanceStatus.REQUESTED);

        when(maintenanceRequestRepository.findById(1L)).thenReturn(Optional.of(mr));

        MaintenanceRequestResponse response = maintenanceRequestService.findById(1L);

        assertThat(response.id()).isEqualTo(1L);
    }

    @Test
    void shouldThrowExceptionWhenNotFoundById() {
        when(maintenanceRequestRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> maintenanceRequestService.findById(999L))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessage("Maintenance request not found with id: 999");
    }

    @Test
    void shouldFindByEquipmentId() {
        MaintenanceRequest mr = createMaintenanceRequest(1L, testEquipment, "Screen not working",
            MaintenanceType.CORRECTIVE, MaintenancePriority.HIGH, MaintenanceStatus.REQUESTED);

        when(equipmentRepository.existsById(1L)).thenReturn(true);
        when(maintenanceRequestRepository.findByEquipmentId(1L)).thenReturn(List.of(mr));

        List<MaintenanceRequestResponse> responses = maintenanceRequestService.findByEquipmentId(1L);

        assertThat(responses).hasSize(1);
    }

    @Test
    void shouldThrowExceptionWhenEquipmentNotFoundForFindByEquipmentId() {
        when(equipmentRepository.existsById(999L)).thenReturn(false);

        assertThatThrownBy(() -> maintenanceRequestService.findByEquipmentId(999L))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessage("Equipment not found with id: 999");
    }

    @Test
    void shouldFindByStatus() {
        MaintenanceRequest mr = createMaintenanceRequest(1L, testEquipment, "Screen not working",
            MaintenanceType.CORRECTIVE, MaintenancePriority.HIGH, MaintenanceStatus.REQUESTED);

        when(maintenanceRequestRepository.findByStatus(MaintenanceStatus.REQUESTED)).thenReturn(List.of(mr));

        List<MaintenanceRequestResponse> responses = maintenanceRequestService.findByStatus(MaintenanceStatus.REQUESTED);

        assertThat(responses).hasSize(1);
    }

    @Test
    void shouldFindPendingRequests() {
        MaintenanceRequest mr = createMaintenanceRequest(1L, testEquipment, "Screen not working",
            MaintenanceType.CORRECTIVE, MaintenancePriority.HIGH, MaintenanceStatus.REQUESTED);

        when(maintenanceRequestRepository.findPendingRequests()).thenReturn(List.of(mr));

        List<MaintenanceRequestResponse> responses = maintenanceRequestService.findPendingRequests();

        assertThat(responses).hasSize(1);
    }

    @Test
    void shouldFindByAssignedToId() {
        MaintenanceRequest mr = createMaintenanceRequest(1L, testEquipment, "Screen not working",
            MaintenanceType.CORRECTIVE, MaintenancePriority.HIGH, MaintenanceStatus.REQUESTED);

        when(facultyRepository.existsById(1L)).thenReturn(true);
        when(maintenanceRequestRepository.findByAssignedToId(1L)).thenReturn(List.of(mr));

        List<MaintenanceRequestResponse> responses = maintenanceRequestService.findByAssignedToId(1L);

        assertThat(responses).hasSize(1);
    }

    @Test
    void shouldThrowExceptionWhenFacultyNotFoundForFindByAssignedToId() {
        when(facultyRepository.existsById(999L)).thenReturn(false);

        assertThatThrownBy(() -> maintenanceRequestService.findByAssignedToId(999L))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessage("Faculty not found with id: 999");
    }

    @Test
    void shouldUpdateMaintenanceRequest() {
        MaintenanceRequest existing = createMaintenanceRequest(1L, testEquipment, "Screen not working",
            MaintenanceType.CORRECTIVE, MaintenancePriority.HIGH, MaintenanceStatus.REQUESTED);

        MaintenanceRequestDto updateRequest = new MaintenanceRequestDto(
            1L, "Screen fixed", "Replaced cable", MaintenanceType.CORRECTIVE,
            MaintenancePriority.HIGH, MaintenanceStatus.COMPLETED, null, LocalDate.now(),
            null, LocalDate.now(), null, null, null, "Cable was faulty"
        );

        MaintenanceRequest updated = createMaintenanceRequest(1L, testEquipment, "Screen fixed",
            MaintenanceType.CORRECTIVE, MaintenancePriority.HIGH, MaintenanceStatus.COMPLETED);

        when(maintenanceRequestRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(equipmentRepository.findById(1L)).thenReturn(Optional.of(testEquipment));
        when(maintenanceRequestRepository.save(any(MaintenanceRequest.class))).thenReturn(updated);

        MaintenanceRequestResponse response = maintenanceRequestService.update(1L, updateRequest);

        assertThat(response.status()).isEqualTo(MaintenanceStatus.COMPLETED);
    }

    @Test
    void shouldDeleteMaintenanceRequest() {
        when(maintenanceRequestRepository.existsById(1L)).thenReturn(true);

        maintenanceRequestService.delete(1L);

        verify(maintenanceRequestRepository).deleteById(1L);
    }

    @Test
    void shouldThrowExceptionWhenDeletingNonExistent() {
        when(maintenanceRequestRepository.existsById(999L)).thenReturn(false);

        assertThatThrownBy(() -> maintenanceRequestService.delete(999L))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessage("Maintenance request not found with id: 999");

        verify(maintenanceRequestRepository, never()).deleteById(any());
    }

    private MaintenanceRequest createMaintenanceRequest(Long id, Equipment equipment, String title,
                                                         MaintenanceType type, MaintenancePriority priority,
                                                         MaintenanceStatus status) {
        MaintenanceRequest mr = new MaintenanceRequest(equipment, title, type, priority, status, LocalDate.now());
        mr.setId(id);
        Instant now = Instant.now();
        mr.setCreatedAt(now);
        mr.setUpdatedAt(now);
        return mr;
    }
}
