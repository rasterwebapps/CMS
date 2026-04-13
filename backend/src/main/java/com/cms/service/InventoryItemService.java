package com.cms.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cms.dto.InventoryItemRequest;
import com.cms.dto.InventoryItemResponse;
import com.cms.exception.ResourceNotFoundException;
import com.cms.model.InventoryItem;
import com.cms.model.Lab;
import com.cms.repository.InventoryItemRepository;
import com.cms.repository.LabRepository;

@Service
@Transactional(readOnly = true)
public class InventoryItemService {

    private final InventoryItemRepository inventoryItemRepository;
    private final LabRepository labRepository;

    public InventoryItemService(InventoryItemRepository inventoryItemRepository, LabRepository labRepository) {
        this.inventoryItemRepository = inventoryItemRepository;
        this.labRepository = labRepository;
    }

    @Transactional
    public InventoryItemResponse create(InventoryItemRequest request) {
        Lab lab = labRepository.findById(request.labId())
            .orElseThrow(() -> new ResourceNotFoundException("Lab not found with id: " + request.labId()));

        InventoryItem item = new InventoryItem(
            request.name(), request.itemCode(), lab, request.quantity()
        );
        item.setMinimumQuantity(request.minimumQuantity());
        item.setUnit(request.unit());
        item.setDescription(request.description());
        item.setLastRestocked(request.lastRestocked());

        InventoryItem saved = inventoryItemRepository.save(item);
        return toResponse(saved);
    }

    public List<InventoryItemResponse> findAll() {
        return inventoryItemRepository.findAll().stream()
            .map(this::toResponse)
            .toList();
    }

    public InventoryItemResponse findById(Long id) {
        InventoryItem item = inventoryItemRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Inventory item not found with id: " + id));
        return toResponse(item);
    }

    public List<InventoryItemResponse> findByLabId(Long labId) {
        if (!labRepository.existsById(labId)) {
            throw new ResourceNotFoundException("Lab not found with id: " + labId);
        }
        return inventoryItemRepository.findByLabId(labId).stream()
            .map(this::toResponse)
            .toList();
    }

    public InventoryItemResponse findByItemCode(String itemCode) {
        InventoryItem item = inventoryItemRepository.findByItemCode(itemCode)
            .orElseThrow(() -> new ResourceNotFoundException("Inventory item not found with code: " + itemCode));
        return toResponse(item);
    }

    public List<InventoryItemResponse> findLowStockItems() {
        return inventoryItemRepository.findLowStockItems().stream()
            .map(this::toResponse)
            .toList();
    }

    public List<InventoryItemResponse> findLowStockItemsByLabId(Long labId) {
        if (!labRepository.existsById(labId)) {
            throw new ResourceNotFoundException("Lab not found with id: " + labId);
        }
        return inventoryItemRepository.findLowStockItemsByLabId(labId).stream()
            .map(this::toResponse)
            .toList();
    }

    @Transactional
    public InventoryItemResponse update(Long id, InventoryItemRequest request) {
        InventoryItem item = inventoryItemRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Inventory item not found with id: " + id));

        Lab lab = labRepository.findById(request.labId())
            .orElseThrow(() -> new ResourceNotFoundException("Lab not found with id: " + request.labId()));

        item.setName(request.name());
        item.setItemCode(request.itemCode());
        item.setLab(lab);
        item.setQuantity(request.quantity());
        item.setMinimumQuantity(request.minimumQuantity());
        item.setUnit(request.unit());
        item.setDescription(request.description());
        item.setLastRestocked(request.lastRestocked());

        InventoryItem updated = inventoryItemRepository.save(item);
        return toResponse(updated);
    }

    @Transactional
    public InventoryItemResponse updateQuantity(Long id, Integer quantityChange) {
        InventoryItem item = inventoryItemRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Inventory item not found with id: " + id));

        item.setQuantity(item.getQuantity() + quantityChange);

        InventoryItem updated = inventoryItemRepository.save(item);
        return toResponse(updated);
    }

    @Transactional
    public void delete(Long id) {
        if (!inventoryItemRepository.existsById(id)) {
            throw new ResourceNotFoundException("Inventory item not found with id: " + id);
        }
        inventoryItemRepository.deleteById(id);
    }

    private InventoryItemResponse toResponse(InventoryItem item) {
        return new InventoryItemResponse(
            item.getId(),
            item.getName(),
            item.getItemCode(),
            item.getLab().getId(),
            item.getLab().getName(),
            item.getQuantity(),
            item.getMinimumQuantity(),
            item.getUnit(),
            item.getDescription(),
            item.getLastRestocked(),
            item.isLowStock(),
            item.getCreatedAt(),
            item.getUpdatedAt()
        );
    }
}
