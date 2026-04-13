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

import com.cms.dto.InventoryItemRequest;
import com.cms.dto.InventoryItemResponse;
import com.cms.exception.ResourceNotFoundException;
import com.cms.model.Department;
import com.cms.model.InventoryItem;
import com.cms.model.Lab;
import com.cms.model.enums.LabStatus;
import com.cms.model.enums.LabType;
import com.cms.repository.InventoryItemRepository;
import com.cms.repository.LabRepository;

@ExtendWith(MockitoExtension.class)
class InventoryItemServiceTest {

    @Mock
    private InventoryItemRepository inventoryItemRepository;
    @Mock
    private LabRepository labRepository;

    private InventoryItemService inventoryItemService;

    private Lab testLab;

    @BeforeEach
    void setUp() {
        inventoryItemService = new InventoryItemService(inventoryItemRepository, labRepository);

        Department dept = new Department("Computer Science", "CS", "CS Dept", "Dr. Smith");
        dept.setId(1L);

        testLab = new Lab("Lab 1", LabType.COMPUTER, dept, "Main Building", "L001", 30, LabStatus.ACTIVE);
        testLab.setId(1L);
    }

    @Test
    void shouldCreateInventoryItem() {
        InventoryItemRequest request = new InventoryItemRequest(
            "Network Cable", "INV001", 1L, 100, 20, "pcs", "Cat6 cable", LocalDate.now()
        );

        InventoryItem saved = createInventoryItem(1L, "Network Cable", "INV001", testLab, 100, 20);

        when(labRepository.findById(1L)).thenReturn(Optional.of(testLab));
        when(inventoryItemRepository.save(any(InventoryItem.class))).thenReturn(saved);

        InventoryItemResponse response = inventoryItemService.create(request);

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.name()).isEqualTo("Network Cable");
    }

    @Test
    void shouldThrowExceptionWhenLabNotFound() {
        InventoryItemRequest request = new InventoryItemRequest(
            "Network Cable", "INV001", 999L, 100, 20, "pcs", null, null
        );

        when(labRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> inventoryItemService.create(request))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessage("Lab not found with id: 999");
    }

    @Test
    void shouldFindAllItems() {
        InventoryItem item = createInventoryItem(1L, "Network Cable", "INV001", testLab, 100, 20);

        when(inventoryItemRepository.findAll()).thenReturn(List.of(item));

        List<InventoryItemResponse> responses = inventoryItemService.findAll();

        assertThat(responses).hasSize(1);
    }

    @Test
    void shouldFindById() {
        InventoryItem item = createInventoryItem(1L, "Network Cable", "INV001", testLab, 100, 20);

        when(inventoryItemRepository.findById(1L)).thenReturn(Optional.of(item));

        InventoryItemResponse response = inventoryItemService.findById(1L);

        assertThat(response.id()).isEqualTo(1L);
    }

    @Test
    void shouldThrowExceptionWhenNotFoundById() {
        when(inventoryItemRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> inventoryItemService.findById(999L))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessage("Inventory item not found with id: 999");
    }

    @Test
    void shouldFindByLabId() {
        InventoryItem item = createInventoryItem(1L, "Network Cable", "INV001", testLab, 100, 20);

        when(labRepository.existsById(1L)).thenReturn(true);
        when(inventoryItemRepository.findByLabId(1L)).thenReturn(List.of(item));

        List<InventoryItemResponse> responses = inventoryItemService.findByLabId(1L);

        assertThat(responses).hasSize(1);
    }

    @Test
    void shouldThrowExceptionWhenLabNotFoundForFindByLabId() {
        when(labRepository.existsById(999L)).thenReturn(false);

        assertThatThrownBy(() -> inventoryItemService.findByLabId(999L))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessage("Lab not found with id: 999");
    }

    @Test
    void shouldFindByItemCode() {
        InventoryItem item = createInventoryItem(1L, "Network Cable", "INV001", testLab, 100, 20);

        when(inventoryItemRepository.findByItemCode("INV001")).thenReturn(Optional.of(item));

        InventoryItemResponse response = inventoryItemService.findByItemCode("INV001");

        assertThat(response.itemCode()).isEqualTo("INV001");
    }

    @Test
    void shouldThrowExceptionWhenItemCodeNotFound() {
        when(inventoryItemRepository.findByItemCode("INVALID")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> inventoryItemService.findByItemCode("INVALID"))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessage("Inventory item not found with code: INVALID");
    }

    @Test
    void shouldFindLowStockItems() {
        InventoryItem item = createInventoryItem(1L, "Network Cable", "INV001", testLab, 10, 20);

        when(inventoryItemRepository.findLowStockItems()).thenReturn(List.of(item));

        List<InventoryItemResponse> responses = inventoryItemService.findLowStockItems();

        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).lowStock()).isTrue();
    }

    @Test
    void shouldFindLowStockItemsByLabId() {
        InventoryItem item = createInventoryItem(1L, "Network Cable", "INV001", testLab, 10, 20);

        when(labRepository.existsById(1L)).thenReturn(true);
        when(inventoryItemRepository.findLowStockItemsByLabId(1L)).thenReturn(List.of(item));

        List<InventoryItemResponse> responses = inventoryItemService.findLowStockItemsByLabId(1L);

        assertThat(responses).hasSize(1);
    }

    @Test
    void shouldThrowExceptionWhenLabNotFoundForLowStock() {
        when(labRepository.existsById(999L)).thenReturn(false);

        assertThatThrownBy(() -> inventoryItemService.findLowStockItemsByLabId(999L))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessage("Lab not found with id: 999");
    }

    @Test
    void shouldUpdateInventoryItem() {
        InventoryItem existing = createInventoryItem(1L, "Network Cable", "INV001", testLab, 100, 20);

        InventoryItemRequest updateRequest = new InventoryItemRequest(
            "Cat6 Cable", "INV001", 1L, 150, 30, "pcs", "Updated description", null
        );

        InventoryItem updated = createInventoryItem(1L, "Cat6 Cable", "INV001", testLab, 150, 30);

        when(inventoryItemRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(labRepository.findById(1L)).thenReturn(Optional.of(testLab));
        when(inventoryItemRepository.save(any(InventoryItem.class))).thenReturn(updated);

        InventoryItemResponse response = inventoryItemService.update(1L, updateRequest);

        assertThat(response.name()).isEqualTo("Cat6 Cable");
        assertThat(response.quantity()).isEqualTo(150);
    }

    @Test
    void shouldUpdateQuantity() {
        InventoryItem existing = createInventoryItem(1L, "Network Cable", "INV001", testLab, 100, 20);

        InventoryItem updated = createInventoryItem(1L, "Network Cable", "INV001", testLab, 110, 20);

        when(inventoryItemRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(inventoryItemRepository.save(any(InventoryItem.class))).thenReturn(updated);

        InventoryItemResponse response = inventoryItemService.updateQuantity(1L, 10);

        assertThat(response.quantity()).isEqualTo(110);
    }

    @Test
    void shouldDeleteInventoryItem() {
        when(inventoryItemRepository.existsById(1L)).thenReturn(true);

        inventoryItemService.delete(1L);

        verify(inventoryItemRepository).deleteById(1L);
    }

    @Test
    void shouldThrowExceptionWhenDeletingNonExistent() {
        when(inventoryItemRepository.existsById(999L)).thenReturn(false);

        assertThatThrownBy(() -> inventoryItemService.delete(999L))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessage("Inventory item not found with id: 999");

        verify(inventoryItemRepository, never()).deleteById(any());
    }

    private InventoryItem createInventoryItem(Long id, String name, String itemCode,
                                               Lab lab, Integer quantity, Integer minimumQuantity) {
        InventoryItem item = new InventoryItem(name, itemCode, lab, quantity);
        item.setId(id);
        item.setMinimumQuantity(minimumQuantity);
        Instant now = Instant.now();
        item.setCreatedAt(now);
        item.setUpdatedAt(now);
        return item;
    }
}
