package com.cms.inventory.stock.service;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cms.dto.ActiveStatusUpdateRequest;
import com.cms.dto.ActiveStatusUpdateResponse;
import com.cms.exception.ResourceNotFoundException;
import com.cms.inventory.stock.dto.InventoryLocationRequest;
import com.cms.inventory.stock.dto.InventoryLocationResponse;
import com.cms.inventory.stock.model.InventoryLocation;
import com.cms.inventory.stock.model.enums.LocationRole;
import com.cms.inventory.stock.repository.InventoryLocationRepository;
import com.cms.model.Room;
import com.cms.repository.RoomRepository;

@Service
@Transactional(readOnly = true)
public class InventoryLocationService {

    private final InventoryLocationRepository locationRepository;
    private final RoomRepository roomRepository;

    public InventoryLocationService(InventoryLocationRepository locationRepository, RoomRepository roomRepository) {
        this.locationRepository = locationRepository;
        this.roomRepository = roomRepository;
    }

    @Transactional
    public InventoryLocationResponse create(InventoryLocationRequest request) {
        String virtualName = requireTrimmed(request.virtualName(), "Virtual name is required");
        Room room = findRoomOrThrow(request.roomId());
        LocationRole role = parseRole(request.locationRole());

        if (locationRepository.existsByVirtualNameIgnoreCase(virtualName)) {
            throw new IllegalArgumentException("An inventory location named '" + virtualName + "' already exists");
        }

        InventoryLocation location = new InventoryLocation();
        location.setRoom(room);
        location.setVirtualName(virtualName);
        location.setLocationRole(role);
        location.setDescription(trim(request.description()));
        if (request.isActive() != null) location.setIsActive(request.isActive());
        return toResponse(locationRepository.save(location));
    }

    public List<InventoryLocationResponse> findAll(boolean activeOnly) {
        List<InventoryLocation> locations = activeOnly
            ? locationRepository.findByIsActiveTrueOrderByVirtualNameAsc()
            : locationRepository.findAllByOrderByVirtualNameAsc();
        return locations.stream().map(this::toResponse).toList();
    }

    public Page<InventoryLocationResponse> findPage(String search, Pageable pageable) {
        Specification<InventoryLocation> spec = (root, query, cb) -> {
            if (search == null || search.isBlank()) return cb.conjunction();
            String pattern = "%" + search.trim().toLowerCase() + "%";
            return cb.or(
                cb.like(cb.lower(root.get("virtualName")), pattern),
                cb.like(cb.lower(root.get("room").get("roomNumber")), pattern)
            );
        };
        return locationRepository.findAll(spec, pageable).map(this::toResponse);
    }

    public InventoryLocationResponse findById(Long id) {
        return toResponse(findOrThrow(id));
    }

    @Transactional
    public InventoryLocationResponse update(Long id, InventoryLocationRequest request) {
        InventoryLocation location = findOrThrow(id);
        String virtualName = requireTrimmed(request.virtualName(), "Virtual name is required");
        Room room = findRoomOrThrow(request.roomId());
        LocationRole role = parseRole(request.locationRole());

        if (locationRepository.existsByVirtualNameIgnoreCaseAndIdNot(virtualName, id)) {
            throw new IllegalArgumentException("An inventory location named '" + virtualName + "' already exists");
        }

        location.setRoom(room);
        location.setVirtualName(virtualName);
        location.setLocationRole(role);
        location.setDescription(trim(request.description()));
        if (request.isActive() != null) location.setIsActive(request.isActive());
        return toResponse(locationRepository.save(location));
    }

    @Transactional
    public void delete(Long id) {
        if (!locationRepository.existsById(id)) {
            throw new ResourceNotFoundException("Inventory location not found with id: " + id);
        }
        locationRepository.deleteById(id);
    }

    @Transactional
    public ActiveStatusUpdateResponse updateStatus(Long id, ActiveStatusUpdateRequest request) {
        InventoryLocation location = findOrThrow(id);
        location.setIsActive(Boolean.TRUE.equals(request.isActive()));
        InventoryLocation saved = locationRepository.save(location);
        return new ActiveStatusUpdateResponse(saved.getId(), saved.getIsActive(), saved.getUpdatedAt());
    }

    public boolean nameExists(String virtualName, Long excludeId) {
        String trimmed = virtualName == null ? "" : virtualName.trim();
        return excludeId != null
            ? locationRepository.existsByVirtualNameIgnoreCaseAndIdNot(trimmed, excludeId)
            : locationRepository.existsByVirtualNameIgnoreCase(trimmed);
    }

    InventoryLocation findOrThrow(Long id) {
        return locationRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Inventory location not found with id: " + id));
    }

    private Room findRoomOrThrow(Long roomId) {
        return roomRepository.findById(roomId)
            .orElseThrow(() -> new ResourceNotFoundException("Room not found with id: " + roomId));
    }

    private LocationRole parseRole(String value) {
        try {
            return LocationRole.valueOf(value == null ? "" : value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid location role '" + value + "' — must be one of STORE, REQUESTING_POINT, BOTH");
        }
    }

    private InventoryLocationResponse toResponse(InventoryLocation l) {
        Room room = l.getRoom();
        return new InventoryLocationResponse(l.getId(), room.getId(), room.getRoomNumber(),
            room.getZone().getId(), room.getZone().getName(), l.getVirtualName(), l.getLocationRole().name(),
            l.getDescription(), l.getIsActive(), l.getCreatedAt(), l.getUpdatedAt());
    }

    private static String trim(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }

    private static String requireTrimmed(String s, String message) {
        String t = trim(s);
        if (t == null) throw new IllegalArgumentException(message);
        return t;
    }
}
