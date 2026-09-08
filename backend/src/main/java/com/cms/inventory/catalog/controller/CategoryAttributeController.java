package com.cms.inventory.catalog.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.cms.inventory.catalog.dto.CategoryAttributeRequest;
import com.cms.inventory.catalog.dto.CategoryAttributeResponse;
import com.cms.inventory.catalog.service.CategoryAttributeService;

import jakarta.validation.Valid;

/**
 * Nested under Category — an attribute has no lifecycle independent of the category that defines
 * it, so this reuses INVENTORY_CATEGORY_VIEW/MANAGE rather than its own permission codes.
 */
@RestController
@RequestMapping("/inventory/categories/{categoryId}/attributes")
public class CategoryAttributeController {

    private final CategoryAttributeService attributeService;

    public CategoryAttributeController(CategoryAttributeService attributeService) {
        this.attributeService = attributeService;
    }

    @GetMapping
    @PreAuthorize("@perm.hasAny('INVENTORY_CATEGORY_VIEW', 'INVENTORY_CATEGORY_MANAGE')")
    public ResponseEntity<List<CategoryAttributeResponse>> findByCategory(@PathVariable Long categoryId) {
        return ResponseEntity.ok(attributeService.findByCategory(categoryId));
    }

    @PostMapping
    @PreAuthorize("@perm.has('INVENTORY_CATEGORY_MANAGE')")
    public ResponseEntity<CategoryAttributeResponse> create(
            @PathVariable Long categoryId,
            @Valid @RequestBody CategoryAttributeRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(attributeService.create(categoryId, request));
    }

    @PutMapping("/{attributeId}")
    @PreAuthorize("@perm.has('INVENTORY_CATEGORY_MANAGE')")
    public ResponseEntity<CategoryAttributeResponse> update(
            @PathVariable Long categoryId,
            @PathVariable Long attributeId,
            @Valid @RequestBody CategoryAttributeRequest request) {
        return ResponseEntity.ok(attributeService.update(categoryId, attributeId, request));
    }

    @DeleteMapping("/{attributeId}")
    @PreAuthorize("@perm.has('INVENTORY_CATEGORY_MANAGE')")
    public ResponseEntity<Void> delete(@PathVariable Long categoryId, @PathVariable Long attributeId) {
        attributeService.delete(categoryId, attributeId);
        return ResponseEntity.noContent().build();
    }
}
