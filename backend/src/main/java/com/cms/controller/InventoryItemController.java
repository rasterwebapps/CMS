package com.cms.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.cms.dto.InventoryItemRequest;
import com.cms.dto.InventoryItemResponse;
import com.cms.service.InventoryItemService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/inventory")
public class InventoryItemController {

    private final InventoryItemService inventoryItemService;

    public InventoryItemController(InventoryItemService inventoryItemService) {
        this.inventoryItemService = inventoryItemService;
    }

    @PostMapping
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_LAB_INCHARGE')")
    public ResponseEntity<InventoryItemResponse> create(@Valid @RequestBody InventoryItemRequest request) {
        InventoryItemResponse response = inventoryItemService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<List<InventoryItemResponse>> findAll(
            @RequestParam(required = false) Long labId,
            @RequestParam(required = false) Boolean lowStockOnly) {
        List<InventoryItemResponse> items;
        if (Boolean.TRUE.equals(lowStockOnly)) {
            if (labId != null) {
                items = inventoryItemService.findLowStockItemsByLabId(labId);
            } else {
                items = inventoryItemService.findLowStockItems();
            }
        } else if (labId != null) {
            items = inventoryItemService.findByLabId(labId);
        } else {
            items = inventoryItemService.findAll();
        }
        return ResponseEntity.ok(items);
    }

    @GetMapping("/{id}")
    public ResponseEntity<InventoryItemResponse> findById(@PathVariable Long id) {
        InventoryItemResponse response = inventoryItemService.findById(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/code/{itemCode}")
    public ResponseEntity<InventoryItemResponse> findByItemCode(@PathVariable String itemCode) {
        InventoryItemResponse response = inventoryItemService.findByItemCode(itemCode);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_LAB_INCHARGE')")
    public ResponseEntity<InventoryItemResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody InventoryItemRequest request) {
        InventoryItemResponse response = inventoryItemService.update(id, request);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{id}/quantity")
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_LAB_INCHARGE') or hasRole('ROLE_TECHNICIAN')")
    public ResponseEntity<InventoryItemResponse> updateQuantity(
            @PathVariable Long id,
            @RequestParam Integer change) {
        InventoryItemResponse response = inventoryItemService.updateQuantity(id, change);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        inventoryItemService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
