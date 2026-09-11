package com.cms.inventory.catalog.controller;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
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

import com.cms.dto.ActiveStatusUpdateRequest;
import com.cms.dto.ActiveStatusUpdateResponse;
import com.cms.inventory.catalog.dto.UomConversionTemplateRequest;
import com.cms.inventory.catalog.dto.UomConversionTemplateResponse;
import com.cms.inventory.catalog.service.UomConversionTemplateService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/inventory/uom-conversion-templates")
public class UomConversionTemplateController {

    private final UomConversionTemplateService templateService;

    public UomConversionTemplateController(UomConversionTemplateService templateService) {
        this.templateService = templateService;
    }

    @PostMapping
    @PreAuthorize("@perm.has('INVENTORY_UOM_TEMPLATE_MANAGE')")
    public ResponseEntity<UomConversionTemplateResponse> create(@Valid @RequestBody UomConversionTemplateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(templateService.create(request));
    }

    // Unpaginated — used both by the templates screen's card view and by the "apply a template"
    // picker embedded in a product's Unit Hierarchy section (baseUomId filters to templates that
    // actually apply to that product).
    @GetMapping
    @PreAuthorize("@perm.hasAny('INVENTORY_UOM_TEMPLATE_VIEW', 'INVENTORY_UOM_TEMPLATE_MANAGE', 'INVENTORY_PRODUCT_VIEW', 'INVENTORY_PRODUCT_MANAGE')")
    public ResponseEntity<List<UomConversionTemplateResponse>> findAll(
            @RequestParam(required = false, defaultValue = "false") boolean activeOnly,
            @RequestParam(required = false) Long baseUomId) {
        return ResponseEntity.ok(templateService.findAll(activeOnly, baseUomId));
    }

    @GetMapping("/{id}")
    @PreAuthorize("@perm.hasAny('INVENTORY_UOM_TEMPLATE_VIEW', 'INVENTORY_UOM_TEMPLATE_MANAGE')")
    public ResponseEntity<UomConversionTemplateResponse> findById(@PathVariable Long id) {
        return ResponseEntity.ok(templateService.findById(id));
    }

    @PutMapping("/{id}")
    @PreAuthorize("@perm.has('INVENTORY_UOM_TEMPLATE_MANAGE')")
    public ResponseEntity<UomConversionTemplateResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody UomConversionTemplateRequest request) {
        return ResponseEntity.ok(templateService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@perm.has('INVENTORY_UOM_TEMPLATE_MANAGE')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        templateService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("@perm.has('INVENTORY_UOM_TEMPLATE_MANAGE')")
    public ResponseEntity<ActiveStatusUpdateResponse> updateStatus(
            @PathVariable Long id,
            @Valid @RequestBody ActiveStatusUpdateRequest request) {
        return ResponseEntity.ok(templateService.updateStatus(id, request));
    }

    @GetMapping("/page")
    @PreAuthorize("@perm.hasAny('INVENTORY_UOM_TEMPLATE_VIEW', 'INVENTORY_UOM_TEMPLATE_MANAGE')")
    public ResponseEntity<Page<UomConversionTemplateResponse>> findPage(
            @RequestParam(required = false) String search,
            @PageableDefault(size = 25, sort = "name", direction = Sort.Direction.ASC) Pageable pageable) {
        return ResponseEntity.ok(templateService.findPage(search, pageable));
    }

    @GetMapping("/name-exists")
    @PreAuthorize("@perm.has('INVENTORY_UOM_TEMPLATE_MANAGE')")
    public ResponseEntity<Boolean> nameExists(
            @RequestParam String value,
            @RequestParam(required = false) Long excludeId) {
        return ResponseEntity.ok(templateService.nameExists(value, excludeId));
    }
}
