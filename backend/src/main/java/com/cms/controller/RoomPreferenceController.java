package com.cms.controller;

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
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.cms.dto.RoomPreferenceRequest;
import com.cms.dto.RoomPreferenceResponse;
import com.cms.model.enums.RoomPreferenceStatus;
import com.cms.service.RoomPreferenceService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/room-preferences")
public class RoomPreferenceController {

    private final RoomPreferenceService roomPreferenceService;

    public RoomPreferenceController(RoomPreferenceService roomPreferenceService) {
        this.roomPreferenceService = roomPreferenceService;
    }

    @PostMapping
    @PreAuthorize("@perm.has('HOSTEL_ROOM_PREFERENCE_MANAGE')")
    public ResponseEntity<RoomPreferenceResponse> create(@Valid @RequestBody RoomPreferenceRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(roomPreferenceService.create(request));
    }

    @GetMapping("/{id}")
    @PreAuthorize("@perm.has('HOSTEL_ROOM_PREFERENCE_VIEW')")
    public ResponseEntity<RoomPreferenceResponse> findById(@PathVariable Long id) {
        return ResponseEntity.ok(roomPreferenceService.findById(id));
    }

    @GetMapping("/enquiry/{enquiryId}")
    @PreAuthorize("@perm.has('HOSTEL_ROOM_PREFERENCE_VIEW')")
    public ResponseEntity<RoomPreferenceResponse> findByEnquiryId(@PathVariable Long enquiryId) {
        RoomPreferenceResponse response = roomPreferenceService.findByEnquiryId(enquiryId);
        return response != null ? ResponseEntity.ok(response) : ResponseEntity.notFound().build();
    }

    @GetMapping("/student/{studentId}")
    @PreAuthorize("@perm.has('HOSTEL_ROOM_PREFERENCE_VIEW')")
    public ResponseEntity<RoomPreferenceResponse> findByStudentId(@PathVariable Long studentId) {
        RoomPreferenceResponse response = roomPreferenceService.findByStudentId(studentId);
        return response != null ? ResponseEntity.ok(response) : ResponseEntity.notFound().build();
    }

    @GetMapping("/page")
    @PreAuthorize("@perm.has('HOSTEL_ROOM_PREFERENCE_VIEW')")
    public ResponseEntity<Page<RoomPreferenceResponse>> findPage(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) RoomPreferenceStatus status,
            @PageableDefault(size = 25, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(roomPreferenceService.findPage(search, status, pageable));
    }

    @PutMapping("/{id}")
    @PreAuthorize("@perm.has('HOSTEL_ROOM_PREFERENCE_MANAGE')")
    public ResponseEntity<RoomPreferenceResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody RoomPreferenceRequest request) {
        return ResponseEntity.ok(roomPreferenceService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@perm.has('HOSTEL_ROOM_PREFERENCE_MANAGE')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        roomPreferenceService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
