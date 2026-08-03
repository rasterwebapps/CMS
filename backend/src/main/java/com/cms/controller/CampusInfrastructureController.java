package com.cms.controller;

import java.util.List;

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
import com.cms.dto.BlockRequest;
import com.cms.dto.BlockResponse;
import com.cms.dto.BranchRequest;
import com.cms.dto.BranchResponse;
import com.cms.dto.FloorRequest;
import com.cms.dto.FloorResponse;
import com.cms.dto.HostelRoomRequest;
import com.cms.dto.HostelRoomResponse;
import com.cms.dto.OrganizationRequest;
import com.cms.dto.OrganizationResponse;
import com.cms.dto.ReorderRequest;
import com.cms.dto.RoomRequest;
import com.cms.dto.RoomResponse;
import com.cms.dto.ZoneRequest;
import com.cms.dto.ZoneResponse;
import com.cms.service.CampusInfrastructureService;

import jakarta.validation.Valid;

/**
 * REST controller for the Campus Infrastructure hierarchy: Organization &gt; Branch &gt; Block
 * &gt; Floor &gt; Zone &gt; Room, plus the Room &gt; HostelRoom attachment.
 * Base path: /api/v1/campus-infrastructure. Mirrors {@code IndiaLocationController}'s
 * nested-path-per-level shape.
 */
@RestController
@RequestMapping("/campus-infrastructure")
public class CampusInfrastructureController {

    private final CampusInfrastructureService service;

    public CampusInfrastructureController(CampusInfrastructureService service) {
        this.service = service;
    }

    // ─── Organizations ───────────────────────────────────────────────────────

    @GetMapping("/organizations")
    @PreAuthorize("@perm.has('CAMPUS_INFRASTRUCTURE_VIEW')")
    public ResponseEntity<List<OrganizationResponse>> getOrganizations(
            @RequestParam(required = false, defaultValue = "false") boolean activeOnly) {
        return ResponseEntity.ok(activeOnly ? service.findActiveOrganizations() : service.findAllOrganizations());
    }

    @GetMapping("/organizations/{id}")
    @PreAuthorize("@perm.has('CAMPUS_INFRASTRUCTURE_VIEW')")
    public ResponseEntity<OrganizationResponse> getOrganization(@PathVariable Long id) {
        return ResponseEntity.ok(service.findOrganizationById(id));
    }

    @PostMapping("/organizations")
    @PreAuthorize("@perm.has('CAMPUS_INFRASTRUCTURE_MANAGE')")
    public ResponseEntity<OrganizationResponse> createOrganization(@Valid @RequestBody OrganizationRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.createOrganization(request));
    }

    @PutMapping("/organizations/{id}")
    @PreAuthorize("@perm.has('CAMPUS_INFRASTRUCTURE_MANAGE')")
    public ResponseEntity<OrganizationResponse> updateOrganization(@PathVariable Long id, @Valid @RequestBody OrganizationRequest request) {
        return ResponseEntity.ok(service.updateOrganization(id, request));
    }

    @DeleteMapping("/organizations/{id}")
    @PreAuthorize("@perm.has('CAMPUS_INFRASTRUCTURE_MANAGE')")
    public ResponseEntity<Void> deleteOrganization(@PathVariable Long id) {
        service.deleteOrganization(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/organizations/{id}/status")
    @PreAuthorize("@perm.has('CAMPUS_INFRASTRUCTURE_MANAGE')")
    public ResponseEntity<ActiveStatusUpdateResponse> updateOrganizationStatus(
            @PathVariable Long id, @Valid @RequestBody ActiveStatusUpdateRequest request) {
        return ResponseEntity.ok(service.updateOrganizationStatus(id, request));
    }

    @GetMapping("/organizations/name-exists")
    @PreAuthorize("@perm.has('CAMPUS_INFRASTRUCTURE_MANAGE')")
    public ResponseEntity<Boolean> organizationNameExists(@RequestParam String value, @RequestParam(required = false) Long excludeId) {
        return ResponseEntity.ok(service.organizationNameExists(value, excludeId));
    }

    @GetMapping("/organizations/code-exists")
    @PreAuthorize("@perm.has('CAMPUS_INFRASTRUCTURE_MANAGE')")
    public ResponseEntity<Boolean> organizationCodeExists(@RequestParam String value, @RequestParam(required = false) Long excludeId) {
        return ResponseEntity.ok(service.organizationCodeExists(value, excludeId));
    }

    // ─── Branches ────────────────────────────────────────────────────────────

    @GetMapping("/organizations/{organizationId}/branches")
    @PreAuthorize("@perm.has('CAMPUS_INFRASTRUCTURE_VIEW')")
    public ResponseEntity<List<BranchResponse>> getBranchesByOrganization(
            @PathVariable Long organizationId,
            @RequestParam(required = false, defaultValue = "false") boolean activeOnly) {
        return ResponseEntity.ok(activeOnly ? service.findActiveBranchesByOrganization(organizationId) : service.findBranchesByOrganization(organizationId));
    }

    @PostMapping("/organizations/{organizationId}/branches")
    @PreAuthorize("@perm.has('CAMPUS_INFRASTRUCTURE_MANAGE')")
    public ResponseEntity<BranchResponse> createBranch(@PathVariable Long organizationId, @Valid @RequestBody BranchRequest request) {
        BranchRequest withOrganization = new BranchRequest(request.name(), request.code(), request.description(), request.isActive(), organizationId);
        return ResponseEntity.status(HttpStatus.CREATED).body(service.createBranch(withOrganization));
    }

    @GetMapping("/branches/{id}")
    @PreAuthorize("@perm.has('CAMPUS_INFRASTRUCTURE_VIEW')")
    public ResponseEntity<BranchResponse> getBranch(@PathVariable Long id) {
        return ResponseEntity.ok(service.findBranchById(id));
    }

    @PutMapping("/branches/{id}")
    @PreAuthorize("@perm.has('CAMPUS_INFRASTRUCTURE_MANAGE')")
    public ResponseEntity<BranchResponse> updateBranch(@PathVariable Long id, @Valid @RequestBody BranchRequest request) {
        return ResponseEntity.ok(service.updateBranch(id, request));
    }

    @DeleteMapping("/branches/{id}")
    @PreAuthorize("@perm.has('CAMPUS_INFRASTRUCTURE_MANAGE')")
    public ResponseEntity<Void> deleteBranch(@PathVariable Long id) {
        service.deleteBranch(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/branches/{id}/status")
    @PreAuthorize("@perm.has('CAMPUS_INFRASTRUCTURE_MANAGE')")
    public ResponseEntity<ActiveStatusUpdateResponse> updateBranchStatus(
            @PathVariable Long id, @Valid @RequestBody ActiveStatusUpdateRequest request) {
        return ResponseEntity.ok(service.updateBranchStatus(id, request));
    }

    // ─── Blocks ──────────────────────────────────────────────────────────────

    @GetMapping("/branches/{branchId}/blocks")
    @PreAuthorize("@perm.has('CAMPUS_INFRASTRUCTURE_VIEW')")
    public ResponseEntity<List<BlockResponse>> getBlocksByBranch(
            @PathVariable Long branchId,
            @RequestParam(required = false, defaultValue = "false") boolean activeOnly) {
        return ResponseEntity.ok(activeOnly ? service.findActiveBlocksByBranch(branchId) : service.findBlocksByBranch(branchId));
    }

    @PostMapping("/branches/{branchId}/blocks")
    @PreAuthorize("@perm.has('CAMPUS_INFRASTRUCTURE_MANAGE')")
    public ResponseEntity<BlockResponse> createBlock(@PathVariable Long branchId, @Valid @RequestBody BlockRequest request) {
        BlockRequest withBranch = new BlockRequest(request.name(), request.code(), request.description(),
            request.isHostel(), request.genderRestriction(), request.isActive(), branchId);
        return ResponseEntity.status(HttpStatus.CREATED).body(service.createBlock(withBranch));
    }

    @GetMapping("/blocks/{id}")
    @PreAuthorize("@perm.has('CAMPUS_INFRASTRUCTURE_VIEW')")
    public ResponseEntity<BlockResponse> getBlock(@PathVariable Long id) {
        return ResponseEntity.ok(service.findBlockById(id));
    }

    @PutMapping("/blocks/{id}")
    @PreAuthorize("@perm.has('CAMPUS_INFRASTRUCTURE_MANAGE')")
    public ResponseEntity<BlockResponse> updateBlock(@PathVariable Long id, @Valid @RequestBody BlockRequest request) {
        return ResponseEntity.ok(service.updateBlock(id, request));
    }

    @DeleteMapping("/blocks/{id}")
    @PreAuthorize("@perm.has('CAMPUS_INFRASTRUCTURE_MANAGE')")
    public ResponseEntity<Void> deleteBlock(@PathVariable Long id) {
        service.deleteBlock(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/blocks/{id}/status")
    @PreAuthorize("@perm.has('CAMPUS_INFRASTRUCTURE_MANAGE')")
    public ResponseEntity<ActiveStatusUpdateResponse> updateBlockStatus(
            @PathVariable Long id, @Valid @RequestBody ActiveStatusUpdateRequest request) {
        return ResponseEntity.ok(service.updateBlockStatus(id, request));
    }

    @PutMapping("/branches/{branchId}/blocks/reorder")
    @PreAuthorize("@perm.has('CAMPUS_INFRASTRUCTURE_MANAGE')")
    public ResponseEntity<Void> reorderBlocks(@PathVariable Long branchId, @Valid @RequestBody ReorderRequest request) {
        service.reorderBlocks(branchId, request);
        return ResponseEntity.noContent().build();
    }

    // ─── Floors ──────────────────────────────────────────────────────────────

    @GetMapping("/blocks/{blockId}/floors")
    @PreAuthorize("@perm.has('CAMPUS_INFRASTRUCTURE_VIEW')")
    public ResponseEntity<List<FloorResponse>> getFloorsByBlock(
            @PathVariable Long blockId,
            @RequestParam(required = false, defaultValue = "false") boolean activeOnly) {
        return ResponseEntity.ok(activeOnly ? service.findActiveFloorsByBlock(blockId) : service.findFloorsByBlock(blockId));
    }

    @PostMapping("/blocks/{blockId}/floors")
    @PreAuthorize("@perm.has('CAMPUS_INFRASTRUCTURE_MANAGE')")
    public ResponseEntity<FloorResponse> createFloor(@PathVariable Long blockId, @Valid @RequestBody FloorRequest request) {
        FloorRequest withBlock = new FloorRequest(request.name(), request.floorNumber(),
            request.isHostel(), request.genderRestriction(), request.isBasement(), request.isActive(), blockId);
        return ResponseEntity.status(HttpStatus.CREATED).body(service.createFloor(withBlock));
    }

    @GetMapping("/floors/{id}")
    @PreAuthorize("@perm.has('CAMPUS_INFRASTRUCTURE_VIEW')")
    public ResponseEntity<FloorResponse> getFloor(@PathVariable Long id) {
        return ResponseEntity.ok(service.findFloorById(id));
    }

    @PutMapping("/floors/{id}")
    @PreAuthorize("@perm.has('CAMPUS_INFRASTRUCTURE_MANAGE')")
    public ResponseEntity<FloorResponse> updateFloor(@PathVariable Long id, @Valid @RequestBody FloorRequest request) {
        return ResponseEntity.ok(service.updateFloor(id, request));
    }

    @DeleteMapping("/floors/{id}")
    @PreAuthorize("@perm.has('CAMPUS_INFRASTRUCTURE_MANAGE')")
    public ResponseEntity<Void> deleteFloor(@PathVariable Long id) {
        service.deleteFloor(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/floors/{id}/status")
    @PreAuthorize("@perm.has('CAMPUS_INFRASTRUCTURE_MANAGE')")
    public ResponseEntity<ActiveStatusUpdateResponse> updateFloorStatus(
            @PathVariable Long id, @Valid @RequestBody ActiveStatusUpdateRequest request) {
        return ResponseEntity.ok(service.updateFloorStatus(id, request));
    }

    // ─── Zones ───────────────────────────────────────────────────────────────

    @GetMapping("/zones")
    @PreAuthorize("@perm.has('CAMPUS_INFRASTRUCTURE_VIEW')")
    public ResponseEntity<List<ZoneResponse>> getAllActiveZones() {
        return ResponseEntity.ok(service.findAllActiveZones());
    }

    @GetMapping("/floors/{floorId}/zones")
    @PreAuthorize("@perm.has('CAMPUS_INFRASTRUCTURE_VIEW')")
    public ResponseEntity<List<ZoneResponse>> getZonesByFloor(
            @PathVariable Long floorId,
            @RequestParam(required = false, defaultValue = "false") boolean activeOnly) {
        return ResponseEntity.ok(activeOnly ? service.findActiveZonesByFloor(floorId) : service.findZonesByFloor(floorId));
    }

    @PostMapping("/floors/{floorId}/zones")
    @PreAuthorize("@perm.has('CAMPUS_INFRASTRUCTURE_MANAGE')")
    public ResponseEntity<ZoneResponse> createZone(@PathVariable Long floorId, @Valid @RequestBody ZoneRequest request) {
        ZoneRequest withFloor = new ZoneRequest(request.name(), request.isHostel(), request.genderRestriction(),
            request.wardenId(), request.isActive(), floorId);
        return ResponseEntity.status(HttpStatus.CREATED).body(service.createZone(withFloor));
    }

    @GetMapping("/zones/{id}")
    @PreAuthorize("@perm.has('CAMPUS_INFRASTRUCTURE_VIEW')")
    public ResponseEntity<ZoneResponse> getZone(@PathVariable Long id) {
        return ResponseEntity.ok(service.findZoneById(id));
    }

    @PutMapping("/zones/{id}")
    @PreAuthorize("@perm.has('CAMPUS_INFRASTRUCTURE_MANAGE')")
    public ResponseEntity<ZoneResponse> updateZone(@PathVariable Long id, @Valid @RequestBody ZoneRequest request) {
        return ResponseEntity.ok(service.updateZone(id, request));
    }

    @DeleteMapping("/zones/{id}")
    @PreAuthorize("@perm.has('CAMPUS_INFRASTRUCTURE_MANAGE')")
    public ResponseEntity<Void> deleteZone(@PathVariable Long id) {
        service.deleteZone(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/zones/{id}/status")
    @PreAuthorize("@perm.has('CAMPUS_INFRASTRUCTURE_MANAGE')")
    public ResponseEntity<ActiveStatusUpdateResponse> updateZoneStatus(
            @PathVariable Long id, @Valid @RequestBody ActiveStatusUpdateRequest request) {
        return ResponseEntity.ok(service.updateZoneStatus(id, request));
    }

    @PutMapping("/floors/{floorId}/zones/reorder")
    @PreAuthorize("@perm.has('CAMPUS_INFRASTRUCTURE_MANAGE')")
    public ResponseEntity<Void> reorderZones(@PathVariable Long floorId, @Valid @RequestBody ReorderRequest request) {
        service.reorderZones(floorId, request);
        return ResponseEntity.noContent().build();
    }

    // ─── Rooms ───────────────────────────────────────────────────────────────

    /** Flat, campus-wide room search by purpose — unlike the zone-scoped listing below, this
     *  spans every Branch (including a hospital Branch hosting clinical venues). Used by venue
     *  pickers (Cohort Room Allocation, Classroom/Lab/ClinicalVenue master forms). */
    @GetMapping("/rooms")
    @PreAuthorize("@perm.has('CAMPUS_INFRASTRUCTURE_VIEW')")
    public ResponseEntity<List<RoomResponse>> searchRoomsByPurpose(
            @RequestParam Long purposeCategoryId,
            @RequestParam(required = false) Long subTypeId,
            @RequestParam(required = false) Integer minCapacity) {
        return ResponseEntity.ok(service.findRoomsByPurpose(purposeCategoryId, subTypeId, minCapacity));
    }

    @GetMapping("/zones/{zoneId}/rooms")
    @PreAuthorize("@perm.has('CAMPUS_INFRASTRUCTURE_VIEW')")
    public ResponseEntity<List<RoomResponse>> getRoomsByZone(
            @PathVariable Long zoneId,
            @RequestParam(required = false, defaultValue = "false") boolean activeOnly) {
        return ResponseEntity.ok(activeOnly ? service.findActiveRoomsByZone(zoneId) : service.findRoomsByZone(zoneId));
    }

    @PostMapping("/zones/{zoneId}/rooms")
    @PreAuthorize("@perm.has('CAMPUS_INFRASTRUCTURE_MANAGE')")
    public ResponseEntity<RoomResponse> createRoom(@PathVariable Long zoneId, @Valid @RequestBody RoomRequest request) {
        RoomRequest withZone = new RoomRequest(request.roomNumber(), request.capacity(), request.description(), request.isActive(), zoneId,
            request.purposeCategoryId(), request.subTypeId());
        return ResponseEntity.status(HttpStatus.CREATED).body(service.createRoom(withZone));
    }

    @GetMapping("/rooms/{id}")
    @PreAuthorize("@perm.has('CAMPUS_INFRASTRUCTURE_VIEW')")
    public ResponseEntity<RoomResponse> getRoom(@PathVariable Long id) {
        return ResponseEntity.ok(service.findRoomById(id));
    }

    @PutMapping("/rooms/{id}")
    @PreAuthorize("@perm.has('CAMPUS_INFRASTRUCTURE_MANAGE')")
    public ResponseEntity<RoomResponse> updateRoom(@PathVariable Long id, @Valid @RequestBody RoomRequest request) {
        return ResponseEntity.ok(service.updateRoom(id, request));
    }

    @DeleteMapping("/rooms/{id}")
    @PreAuthorize("@perm.has('CAMPUS_INFRASTRUCTURE_MANAGE')")
    public ResponseEntity<Void> deleteRoom(@PathVariable Long id) {
        service.deleteRoom(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/rooms/{id}/status")
    @PreAuthorize("@perm.has('CAMPUS_INFRASTRUCTURE_MANAGE')")
    public ResponseEntity<ActiveStatusUpdateResponse> updateRoomStatus(
            @PathVariable Long id, @Valid @RequestBody ActiveStatusUpdateRequest request) {
        return ResponseEntity.ok(service.updateRoomStatus(id, request));
    }

    @PutMapping("/zones/{zoneId}/rooms/reorder")
    @PreAuthorize("@perm.has('CAMPUS_INFRASTRUCTURE_MANAGE')")
    public ResponseEntity<Void> reorderRooms(@PathVariable Long zoneId, @Valid @RequestBody ReorderRequest request) {
        service.reorderRooms(zoneId, request);
        return ResponseEntity.noContent().build();
    }

    // ─── Hostel Room attachment (distinct permission — hostel-domain action) ─

    @GetMapping("/rooms/{roomId}/hostel-room")
    @PreAuthorize("@perm.has('HOSTEL_ROOM_VIEW')")
    public ResponseEntity<HostelRoomResponse> getHostelRoom(@PathVariable Long roomId) {
        return ResponseEntity.ok(service.findHostelRoomByRoomId(roomId));
    }

    @PutMapping("/rooms/{roomId}/hostel-room")
    @PreAuthorize("@perm.has('HOSTEL_ROOM_MANAGE')")
    public ResponseEntity<HostelRoomResponse> assignHostelRoom(
            @PathVariable Long roomId, @Valid @RequestBody HostelRoomRequest request) {
        return ResponseEntity.ok(service.assignHostelRoom(roomId, request));
    }

    @DeleteMapping("/rooms/{roomId}/hostel-room")
    @PreAuthorize("@perm.has('HOSTEL_ROOM_MANAGE')")
    public ResponseEntity<Void> unassignHostelRoom(@PathVariable Long roomId) {
        service.unassignHostelRoom(roomId);
        return ResponseEntity.noContent().build();
    }
}
