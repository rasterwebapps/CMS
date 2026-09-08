package com.cms.inventory.ticket.controller;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
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
import com.cms.inventory.ticket.dto.ServiceTicketCategoryRequest;
import com.cms.inventory.ticket.dto.ServiceTicketCategoryResponse;
import com.cms.inventory.ticket.service.ServiceTicketCategoryService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/inventory/ticket/categories")
public class ServiceTicketCategoryController {

    private static final String VIEW_ANY = "@perm.hasAny('INVENTORY_SERVICE_TICKET_CATEGORY_VIEW', 'INVENTORY_SERVICE_TICKET_CATEGORY_MANAGE')";

    private final ServiceTicketCategoryService categoryService;

    public ServiceTicketCategoryController(ServiceTicketCategoryService categoryService) {
        this.categoryService = categoryService;
    }

    @PostMapping
    @PreAuthorize("@perm.has('INVENTORY_SERVICE_TICKET_CATEGORY_MANAGE')")
    public ResponseEntity<ServiceTicketCategoryResponse> create(@Valid @RequestBody ServiceTicketCategoryRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(categoryService.create(request));
    }

    @GetMapping
    @PreAuthorize(VIEW_ANY)
    public ResponseEntity<List<ServiceTicketCategoryResponse>> findAll(
            @RequestParam(required = false, defaultValue = "false") boolean activeOnly) {
        return ResponseEntity.ok(categoryService.findAll(activeOnly));
    }

    @GetMapping("/page")
    @PreAuthorize(VIEW_ANY)
    public ResponseEntity<Page<ServiceTicketCategoryResponse>> findPage(
            @RequestParam(required = false) String search,
            @PageableDefault(size = 25, sort = "name", direction = Sort.Direction.ASC) Pageable pageable) {
        return ResponseEntity.ok(categoryService.findPage(search, pageable));
    }

    @GetMapping("/{id}")
    @PreAuthorize(VIEW_ANY)
    public ResponseEntity<ServiceTicketCategoryResponse> findById(@PathVariable Long id) {
        return ResponseEntity.ok(categoryService.findById(id));
    }

    @PutMapping("/{id}")
    @PreAuthorize("@perm.has('INVENTORY_SERVICE_TICKET_CATEGORY_MANAGE')")
    public ResponseEntity<ServiceTicketCategoryResponse> update(@PathVariable Long id, @Valid @RequestBody ServiceTicketCategoryRequest request) {
        return ResponseEntity.ok(categoryService.update(id, request));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("@perm.has('INVENTORY_SERVICE_TICKET_CATEGORY_MANAGE')")
    public ResponseEntity<ActiveStatusUpdateResponse> updateStatus(@PathVariable Long id, @Valid @RequestBody ActiveStatusUpdateRequest request) {
        return ResponseEntity.ok(categoryService.updateStatus(id, request));
    }

    @GetMapping("/name-exists")
    @PreAuthorize("@perm.has('INVENTORY_SERVICE_TICKET_CATEGORY_MANAGE')")
    public ResponseEntity<Boolean> nameExists(@RequestParam String value, @RequestParam(required = false) Long excludeId) {
        return ResponseEntity.ok(categoryService.nameExists(value, excludeId));
    }
}
