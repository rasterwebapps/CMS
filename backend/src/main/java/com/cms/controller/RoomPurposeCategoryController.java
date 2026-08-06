package com.cms.controller;

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
import com.cms.dto.RoomPurposeCategoryRequest;
import com.cms.dto.RoomPurposeCategoryResponse;
import com.cms.service.RoomPurposeCategoryService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/room-purpose-categories")
public class RoomPurposeCategoryController {

    private final RoomPurposeCategoryService roomPurposeCategoryService;

    public RoomPurposeCategoryController(RoomPurposeCategoryService roomPurposeCategoryService) {
        this.roomPurposeCategoryService = roomPurposeCategoryService;
    }

    @PostMapping
    @PreAuthorize("@perm.has('ROOM_PURPOSE_CATEGORY_MANAGE')")
    public ResponseEntity<RoomPurposeCategoryResponse> create(@Valid @RequestBody RoomPurposeCategoryRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(roomPurposeCategoryService.create(request));
    }

    // Unpaginated — used to populate the Purpose Category dropdown on the Room form / Campus
    // Setup side panel, so it's readable by anyone who can view Campus Infrastructure.
    @GetMapping
    @PreAuthorize("@perm.hasAny('ROOM_PURPOSE_CATEGORY_VIEW', 'ROOM_PURPOSE_CATEGORY_MANAGE', 'CAMPUS_INFRASTRUCTURE_VIEW', 'CAMPUS_INFRASTRUCTURE_MANAGE')")
    public ResponseEntity<List<RoomPurposeCategoryResponse>> findAll(
            @RequestParam(required = false, defaultValue = "false") boolean activeOnly) {
        List<RoomPurposeCategoryResponse> result = activeOnly
            ? roomPurposeCategoryService.findActive()
            : roomPurposeCategoryService.findAll();
        return ResponseEntity.ok(result);
    }

    @GetMapping("/{id}")
    @PreAuthorize("@perm.hasAny('ROOM_PURPOSE_CATEGORY_VIEW', 'ROOM_PURPOSE_CATEGORY_MANAGE')")
    public ResponseEntity<RoomPurposeCategoryResponse> findById(@PathVariable Long id) {
        return ResponseEntity.ok(roomPurposeCategoryService.findById(id));
    }

    @PutMapping("/{id}")
    @PreAuthorize("@perm.has('ROOM_PURPOSE_CATEGORY_MANAGE')")
    public ResponseEntity<RoomPurposeCategoryResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody RoomPurposeCategoryRequest request) {
        return ResponseEntity.ok(roomPurposeCategoryService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@perm.has('ROOM_PURPOSE_CATEGORY_MANAGE')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        roomPurposeCategoryService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("@perm.has('ROOM_PURPOSE_CATEGORY_MANAGE')")
    public ResponseEntity<ActiveStatusUpdateResponse> updateStatus(
            @PathVariable Long id,
            @Valid @RequestBody ActiveStatusUpdateRequest request) {
        return ResponseEntity.ok(roomPurposeCategoryService.updateStatus(id, request));
    }

    @GetMapping("/page")
    @PreAuthorize("@perm.hasAny('ROOM_PURPOSE_CATEGORY_VIEW', 'ROOM_PURPOSE_CATEGORY_MANAGE')")
    public ResponseEntity<Page<RoomPurposeCategoryResponse>> findPage(
            @RequestParam(required = false) String search,
            @PageableDefault(size = 25, sort = "name", direction = Sort.Direction.ASC) Pageable pageable) {
        return ResponseEntity.ok(roomPurposeCategoryService.findPage(search, pageable));
    }

    @GetMapping("/name-exists")
    @PreAuthorize("@perm.has('ROOM_PURPOSE_CATEGORY_MANAGE')")
    public ResponseEntity<Boolean> nameExists(
            @RequestParam String value,
            @RequestParam(required = false) Long excludeId) {
        return ResponseEntity.ok(roomPurposeCategoryService.nameExists(value, excludeId));
    }

}
