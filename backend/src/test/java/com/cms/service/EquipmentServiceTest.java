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
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.cms.dto.EquipmentRequest;
import com.cms.dto.EquipmentResponse;
import com.cms.exception.ResourceNotFoundException;
import com.cms.model.Department;
import com.cms.model.Equipment;
import com.cms.model.Lab;
import com.cms.model.enums.EquipmentCategory;
import com.cms.model.enums.EquipmentStatus;
import com.cms.model.enums.LabStatus;
import com.cms.model.enums.LabType;
import com.cms.repository.EquipmentRepository;
import com.cms.repository.LabRepository;

@ExtendWith(MockitoExtension.class)
class EquipmentServiceTest {

    @Mock
    private EquipmentRepository equipmentRepository;
    @Mock
    private LabRepository labRepository;

    private EquipmentService equipmentService;

    private Lab testLab;

    @BeforeEach
    void setUp() {
        equipmentService = new EquipmentService(equipmentRepository, labRepository);

        Department dept = new Department("Computer Science", "CS", "CS Dept", "Dr. Smith");
        dept.setId(1L);

        testLab = new Lab("Lab 1", LabType.COMPUTER, dept, "Main Building", "L001", 30, LabStatus.ACTIVE);
        testLab.setId(1L);
    }

    @Test
    void shouldCreateEquipment() {
        EquipmentRequest request = new EquipmentRequest(
            "Dell Computer", "ASSET001", "SN123", EquipmentCategory.COMPUTER, 1L,
            "Dell", "Optiplex 7090", EquipmentStatus.AVAILABLE,
            LocalDate.now(), new BigDecimal("50000.00"), LocalDate.now().plusYears(3),
            "Row 1", "Intel i7, 16GB RAM"
        );

        Equipment saved = createEquipment(1L, "Dell Computer", "ASSET001", EquipmentCategory.COMPUTER, testLab, EquipmentStatus.AVAILABLE);

        when(labRepository.findById(1L)).thenReturn(Optional.of(testLab));
        when(equipmentRepository.save(any(Equipment.class))).thenReturn(saved);

        EquipmentResponse response = equipmentService.create(request);

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.name()).isEqualTo("Dell Computer");
    }

    @Test
    void shouldThrowExceptionWhenLabNotFound() {
        EquipmentRequest request = new EquipmentRequest(
            "Dell Computer", "ASSET001", "SN123", EquipmentCategory.COMPUTER, 999L,
            null, null, EquipmentStatus.AVAILABLE, null, null, null, null, null
        );

        when(labRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> equipmentService.create(request))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessage("Lab not found with id: 999");
    }

    @Test
    void shouldFindAllEquipment() {
        Equipment eq = createEquipment(1L, "Dell Computer", "ASSET001", EquipmentCategory.COMPUTER, testLab, EquipmentStatus.AVAILABLE);

        when(equipmentRepository.findAll()).thenReturn(List.of(eq));

        List<EquipmentResponse> responses = equipmentService.findAll();

        assertThat(responses).hasSize(1);
    }

    @Test
    void shouldFindById() {
        Equipment eq = createEquipment(1L, "Dell Computer", "ASSET001", EquipmentCategory.COMPUTER, testLab, EquipmentStatus.AVAILABLE);

        when(equipmentRepository.findById(1L)).thenReturn(Optional.of(eq));

        EquipmentResponse response = equipmentService.findById(1L);

        assertThat(response.id()).isEqualTo(1L);
    }

    @Test
    void shouldThrowExceptionWhenNotFoundById() {
        when(equipmentRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> equipmentService.findById(999L))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessage("Equipment not found with id: 999");
    }

    @Test
    void shouldFindByLabId() {
        Equipment eq = createEquipment(1L, "Dell Computer", "ASSET001", EquipmentCategory.COMPUTER, testLab, EquipmentStatus.AVAILABLE);

        when(labRepository.existsById(1L)).thenReturn(true);
        when(equipmentRepository.findByLabId(1L)).thenReturn(List.of(eq));

        List<EquipmentResponse> responses = equipmentService.findByLabId(1L);

        assertThat(responses).hasSize(1);
    }

    @Test
    void shouldThrowExceptionWhenLabNotFoundForFindByLabId() {
        when(labRepository.existsById(999L)).thenReturn(false);

        assertThatThrownBy(() -> equipmentService.findByLabId(999L))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessage("Lab not found with id: 999");
    }

    @Test
    void shouldFindByStatus() {
        Equipment eq = createEquipment(1L, "Dell Computer", "ASSET001", EquipmentCategory.COMPUTER, testLab, EquipmentStatus.AVAILABLE);

        when(equipmentRepository.findByStatus(EquipmentStatus.AVAILABLE)).thenReturn(List.of(eq));

        List<EquipmentResponse> responses = equipmentService.findByStatus(EquipmentStatus.AVAILABLE);

        assertThat(responses).hasSize(1);
    }

    @Test
    void shouldFindByCategory() {
        Equipment eq = createEquipment(1L, "Dell Computer", "ASSET001", EquipmentCategory.COMPUTER, testLab, EquipmentStatus.AVAILABLE);

        when(equipmentRepository.findByCategory(EquipmentCategory.COMPUTER)).thenReturn(List.of(eq));

        List<EquipmentResponse> responses = equipmentService.findByCategory(EquipmentCategory.COMPUTER);

        assertThat(responses).hasSize(1);
    }

    @Test
    void shouldFindByAssetCode() {
        Equipment eq = createEquipment(1L, "Dell Computer", "ASSET001", EquipmentCategory.COMPUTER, testLab, EquipmentStatus.AVAILABLE);

        when(equipmentRepository.findByAssetCode("ASSET001")).thenReturn(Optional.of(eq));

        EquipmentResponse response = equipmentService.findByAssetCode("ASSET001");

        assertThat(response.assetCode()).isEqualTo("ASSET001");
    }

    @Test
    void shouldThrowExceptionWhenAssetCodeNotFound() {
        when(equipmentRepository.findByAssetCode("INVALID")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> equipmentService.findByAssetCode("INVALID"))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessage("Equipment not found with asset code: INVALID");
    }

    @Test
    void shouldUpdateEquipment() {
        Equipment existing = createEquipment(1L, "Dell Computer", "ASSET001", EquipmentCategory.COMPUTER, testLab, EquipmentStatus.AVAILABLE);

        EquipmentRequest updateRequest = new EquipmentRequest(
            "HP Computer", "ASSET001", "SN123", EquipmentCategory.COMPUTER, 1L,
            "HP", "EliteDesk", EquipmentStatus.IN_USE, null, null, null, null, null
        );

        Equipment updated = createEquipment(1L, "HP Computer", "ASSET001", EquipmentCategory.COMPUTER, testLab, EquipmentStatus.IN_USE);

        when(equipmentRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(labRepository.findById(1L)).thenReturn(Optional.of(testLab));
        when(equipmentRepository.existsByAssetCodeAndIdNot("ASSET001", 1L)).thenReturn(false);
        when(equipmentRepository.save(any(Equipment.class))).thenReturn(updated);

        EquipmentResponse response = equipmentService.update(1L, updateRequest);

        assertThat(response.name()).isEqualTo("HP Computer");
        assertThat(response.status()).isEqualTo(EquipmentStatus.IN_USE);
    }

    @Test
    void shouldThrowExceptionWhenUpdatingWithDuplicateAssetCode() {
        Equipment existing = createEquipment(1L, "Dell Computer", "ASSET001", EquipmentCategory.COMPUTER, testLab, EquipmentStatus.AVAILABLE);

        EquipmentRequest updateRequest = new EquipmentRequest(
            "Dell Computer", "ASSET002", "SN123", EquipmentCategory.COMPUTER, 1L,
            null, null, EquipmentStatus.AVAILABLE, null, null, null, null, null
        );

        when(equipmentRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(labRepository.findById(1L)).thenReturn(Optional.of(testLab));
        when(equipmentRepository.existsByAssetCodeAndIdNot("ASSET002", 1L)).thenReturn(true);

        assertThatThrownBy(() -> equipmentService.update(1L, updateRequest))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Equipment with asset code 'ASSET002' already exists");

        verify(equipmentRepository, never()).save(any());
    }

    @Test
    void shouldDeleteEquipment() {
        when(equipmentRepository.existsById(1L)).thenReturn(true);

        equipmentService.delete(1L);

        verify(equipmentRepository).deleteById(1L);
    }

    @Test
    void shouldThrowExceptionWhenDeletingNonExistent() {
        when(equipmentRepository.existsById(999L)).thenReturn(false);

        assertThatThrownBy(() -> equipmentService.delete(999L))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessage("Equipment not found with id: 999");

        verify(equipmentRepository, never()).deleteById(any());
    }

    private Equipment createEquipment(Long id, String name, String assetCode,
                                       EquipmentCategory category, Lab lab, EquipmentStatus status) {
        Equipment eq = new Equipment(name, assetCode, category, lab, status);
        eq.setId(id);
        Instant now = Instant.now();
        eq.setCreatedAt(now);
        eq.setUpdatedAt(now);
        return eq;
    }
}
