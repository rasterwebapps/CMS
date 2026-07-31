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
import com.cms.dto.RoomSubTypeRequest;
import com.cms.dto.RoomSubTypeResponse;
import com.cms.service.RoomSubTypeService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/room-sub-types")
public class RoomSubTypeController {

    private final RoomSubTypeService roomSubTypeService;

    public RoomSubTypeController(RoomSubTypeService roomSubTypeService) {
        this.roomSubTypeService = roomSubTypeService;
    }

    @PostMapping
    @PreAuthorize("@perm.has('ROOM_SUB_TYPE_MANAGE')")
    public ResponseEntity<RoomSubTypeResponse> create(@Valid @RequestBody RoomSubTypeRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(roomSubTypeService.create(request));
    }

    // Unpaginated — used to populate the Sub-Type dropdown on the Room form / Campus Setup side
    // panel (filtered by the selected Purpose Category), so readable by anyone who can view
    // Campus Infrastructure.
    @GetMapping
    @PreAuthorize("@perm.hasAny('ROOM_SUB_TYPE_VIEW', 'ROOM_SUB_TYPE_MANAGE', 'CAMPUS_INFRASTRUCTURE_VIEW', 'CAMPUS_INFRASTRUCTURE_MANAGE')")
    public ResponseEntity<List<RoomSubTypeResponse>> findAll(
            @RequestParam(required = false) Long purposeCategoryId,
            @RequestParam(required = false, defaultValue = "false") boolean activeOnly) {
        return ResponseEntity.ok(roomSubTypeService.findAll(purposeCategoryId, activeOnly));
    }

    @GetMapping("/{id}")
    @PreAuthorize("@perm.hasAny('ROOM_SUB_TYPE_VIEW', 'ROOM_SUB_TYPE_MANAGE')")
    public ResponseEntity<RoomSubTypeResponse> findById(@PathVariable Long id) {
        return ResponseEntity.ok(roomSubTypeService.findById(id));
    }

    @PutMapping("/{id}")
    @PreAuthorize("@perm.has('ROOM_SUB_TYPE_MANAGE')")
    public ResponseEntity<RoomSubTypeResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody RoomSubTypeRequest request) {
        return ResponseEntity.ok(roomSubTypeService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@perm.has('ROOM_SUB_TYPE_MANAGE')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        roomSubTypeService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("@perm.has('ROOM_SUB_TYPE_MANAGE')")
    public ResponseEntity<ActiveStatusUpdateResponse> updateStatus(
            @PathVariable Long id,
            @Valid @RequestBody ActiveStatusUpdateRequest request) {
        return ResponseEntity.ok(roomSubTypeService.updateStatus(id, request));
    }

    @GetMapping("/page")
    @PreAuthorize("@perm.hasAny('ROOM_SUB_TYPE_VIEW', 'ROOM_SUB_TYPE_MANAGE')")
    public ResponseEntity<Page<RoomSubTypeResponse>> findPage(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Long purposeCategoryId,
            @PageableDefault(size = 25, sort = "name", direction = Sort.Direction.ASC) Pageable pageable) {
        return ResponseEntity.ok(roomSubTypeService.findPage(search, purposeCategoryId, pageable));
    }

    @GetMapping("/name-exists")
    @PreAuthorize("@perm.has('ROOM_SUB_TYPE_MANAGE')")
    public ResponseEntity<Boolean> nameExists(
            @RequestParam String value,
            @RequestParam Long purposeCategoryId,
            @RequestParam(required = false) Long excludeId) {
        return ResponseEntity.ok(roomSubTypeService.nameExists(value, purposeCategoryId, excludeId));
    }

    @GetMapping("/code-exists")
    @PreAuthorize("@perm.has('ROOM_SUB_TYPE_MANAGE')")
    public ResponseEntity<Boolean> codeExists(
            @RequestParam String value,
            @RequestParam Long purposeCategoryId,
            @RequestParam(required = false) Long excludeId) {
        return ResponseEntity.ok(roomSubTypeService.codeExists(value, purposeCategoryId, excludeId));
    }
}
