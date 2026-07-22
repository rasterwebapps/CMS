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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.cms.dto.HostelRoomOccupancyResponse;
import com.cms.dto.RoomAllocationRequest;
import com.cms.dto.RoomAllocationResponse;
import com.cms.dto.RoomAllocationStatusUpdateRequest;
import com.cms.model.enums.RoomAllocationStatus;
import com.cms.service.RoomAllocationService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/room-allocations")
public class RoomAllocationController {

    private final RoomAllocationService roomAllocationService;

    public RoomAllocationController(RoomAllocationService roomAllocationService) {
        this.roomAllocationService = roomAllocationService;
    }

    @PostMapping
    @PreAuthorize("@perm.has('HOSTEL_ROOM_ALLOCATION_MANAGE')")
    public ResponseEntity<RoomAllocationResponse> create(@Valid @RequestBody RoomAllocationRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(roomAllocationService.create(request));
    }

    @GetMapping("/{id}")
    @PreAuthorize("@perm.has('HOSTEL_ROOM_ALLOCATION_VIEW')")
    public ResponseEntity<RoomAllocationResponse> findById(@PathVariable Long id) {
        return ResponseEntity.ok(roomAllocationService.findById(id));
    }

    @GetMapping("/student/{studentId}")
    @PreAuthorize("@perm.has('HOSTEL_ROOM_ALLOCATION_VIEW')")
    public ResponseEntity<List<RoomAllocationResponse>> findByStudentId(@PathVariable Long studentId) {
        return ResponseEntity.ok(roomAllocationService.findByStudentId(studentId));
    }

    @GetMapping("/page")
    @PreAuthorize("@perm.has('HOSTEL_ROOM_ALLOCATION_VIEW')")
    public ResponseEntity<Page<RoomAllocationResponse>> findPage(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) RoomAllocationStatus status,
            @PageableDefault(size = 25, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(roomAllocationService.findPage(search, status, pageable));
    }

    @GetMapping("/occupancy")
    @PreAuthorize("@perm.has('HOSTEL_ROOM_ALLOCATION_VIEW')")
    public ResponseEntity<List<HostelRoomOccupancyResponse>> findOccupancy() {
        return ResponseEntity.ok(roomAllocationService.findOccupancy());
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("@perm.has('HOSTEL_ROOM_ALLOCATION_MANAGE')")
    public ResponseEntity<RoomAllocationResponse> updateStatus(
            @PathVariable Long id,
            @Valid @RequestBody RoomAllocationStatusUpdateRequest request) {
        return ResponseEntity.ok(roomAllocationService.updateStatus(id, request.status()));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@perm.has('HOSTEL_ROOM_ALLOCATION_MANAGE')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        roomAllocationService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
