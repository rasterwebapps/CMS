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
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.cms.dto.ActiveStatusUpdateRequest;
import com.cms.dto.ActiveStatusUpdateResponse;
import com.cms.dto.HostelRoomTypeRequest;
import com.cms.dto.HostelRoomTypeResponse;
import com.cms.service.HostelRoomTypeService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/hostel-room-types")
public class HostelRoomTypeController {

    private final HostelRoomTypeService hostelRoomTypeService;

    public HostelRoomTypeController(HostelRoomTypeService hostelRoomTypeService) {
        this.hostelRoomTypeService = hostelRoomTypeService;
    }

    @PostMapping
    @PreAuthorize("@perm.has('HOSTEL_ROOM_TYPE_MANAGE')")
    public ResponseEntity<HostelRoomTypeResponse> create(@Valid @RequestBody HostelRoomTypeRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(hostelRoomTypeService.create(request));
    }

    @GetMapping
    @PreAuthorize("@perm.has('HOSTEL_ROOM_TYPE_VIEW')")
    public ResponseEntity<List<HostelRoomTypeResponse>> findAll(
            @RequestParam(required = false, defaultValue = "false") boolean activeOnly) {
        List<HostelRoomTypeResponse> result = activeOnly
            ? hostelRoomTypeService.findActive()
            : hostelRoomTypeService.findAll();
        return ResponseEntity.ok(result);
    }

    @GetMapping("/{id}")
    @PreAuthorize("@perm.has('HOSTEL_ROOM_TYPE_VIEW')")
    public ResponseEntity<HostelRoomTypeResponse> findById(@PathVariable Long id) {
        return ResponseEntity.ok(hostelRoomTypeService.findById(id));
    }

    @PutMapping("/{id}")
    @PreAuthorize("@perm.has('HOSTEL_ROOM_TYPE_MANAGE')")
    public ResponseEntity<HostelRoomTypeResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody HostelRoomTypeRequest request) {
        return ResponseEntity.ok(hostelRoomTypeService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@perm.has('HOSTEL_ROOM_TYPE_MANAGE')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        hostelRoomTypeService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("@perm.has('HOSTEL_ROOM_TYPE_MANAGE')")
    public ResponseEntity<ActiveStatusUpdateResponse> updateStatus(
            @PathVariable Long id,
            @Valid @RequestBody ActiveStatusUpdateRequest request) {
        return ResponseEntity.ok(hostelRoomTypeService.updateStatus(id, request));
    }

    @GetMapping("/page")
    @PreAuthorize("@perm.has('HOSTEL_ROOM_TYPE_VIEW')")
    public ResponseEntity<Page<HostelRoomTypeResponse>> findPage(
            @RequestParam(required = false) String search,
            @PageableDefault(size = 25, sort = "name", direction = Sort.Direction.ASC) Pageable pageable) {
        return ResponseEntity.ok(hostelRoomTypeService.findPage(search, pageable));
    }

    @GetMapping("/name-exists")
    @PreAuthorize("@perm.has('HOSTEL_ROOM_TYPE_MANAGE')")
    public ResponseEntity<Boolean> nameExists(
            @RequestParam String value,
            @RequestParam(required = false) Long excludeId) {
        return ResponseEntity.ok(hostelRoomTypeService.nameExists(value, excludeId));
    }

    @GetMapping("/code-exists")
    @PreAuthorize("@perm.has('HOSTEL_ROOM_TYPE_MANAGE')")
    public ResponseEntity<Boolean> codeExists(
            @RequestParam String value,
            @RequestParam(required = false) Long excludeId) {
        return ResponseEntity.ok(hostelRoomTypeService.codeExists(value, excludeId));
    }
}
