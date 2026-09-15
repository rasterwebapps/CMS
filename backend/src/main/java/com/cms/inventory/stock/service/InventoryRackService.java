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
import com.cms.inventory.stock.dto.InventoryRackRequest;
import com.cms.inventory.stock.dto.InventoryRackResponse;
import com.cms.inventory.stock.model.InventoryLocation;
import com.cms.inventory.stock.model.InventoryRack;
import com.cms.inventory.stock.repository.InventoryLocationRepository;
import com.cms.inventory.stock.repository.InventoryRackRepository;

@Service
@Transactional(readOnly = true)
public class InventoryRackService {

    private final InventoryRackRepository rackRepository;
    private final InventoryLocationRepository locationRepository;

    public InventoryRackService(InventoryRackRepository rackRepository, InventoryLocationRepository locationRepository) {
        this.rackRepository = rackRepository;
        this.locationRepository = locationRepository;
    }

    @Transactional
    public InventoryRackResponse create(InventoryRackRequest request) {
        InventoryLocation location = findLocation(request.locationId());
        String name = requireTrimmed(request.name(), "Rack name is required");
        String code = requireTrimmed(request.code(), "Rack code is required");

        if (rackRepository.existsByNameIgnoreCaseAndLocationId(name, location.getId())) {
            throw new IllegalArgumentException("A rack with the name '" + name + "' already exists in this location");
        }
        if (rackRepository.existsByCodeIgnoreCaseAndLocationId(code, location.getId())) {
            throw new IllegalArgumentException("A rack with the code '" + code + "' already exists in this location");
        }

        InventoryRack rack = new InventoryRack();
        rack.setLocation(location);
        rack.setName(name);
        rack.setCode(code.toUpperCase());
        rack.setDescription(trim(request.description()));
        if (request.isActive() != null) rack.setIsActive(request.isActive());
        return toResponse(rackRepository.save(rack));
    }

    public List<InventoryRackResponse> findAll(Long locationId, boolean activeOnly) {
        List<InventoryRack> racks;
        if (locationId != null && activeOnly) {
            racks = rackRepository.findByLocationIdAndIsActiveTrueOrderByNameAsc(locationId);
        } else if (locationId != null) {
            racks = rackRepository.findByLocationIdOrderByNameAsc(locationId);
        } else if (activeOnly) {
            racks = rackRepository.findByIsActiveTrueOrderByNameAsc();
        } else {
            racks = rackRepository.findAll();
        }
        return racks.stream().map(this::toResponse).toList();
    }

    public Page<InventoryRackResponse> findPage(String search, Long locationId, Pageable pageable) {
        Specification<InventoryRack> spec = (root, query, cb) -> {
            var predicates = cb.conjunction();
            if (locationId != null) {
                predicates = cb.and(predicates, cb.equal(root.get("location").get("id"), locationId));
            }
            if (search != null && !search.isBlank()) {
                String pattern = "%" + search.trim().toLowerCase() + "%";
                predicates = cb.and(predicates, cb.or(
                    cb.like(cb.lower(root.get("name")), pattern),
                    cb.like(cb.lower(root.get("code")), pattern)
                ));
            }
            return predicates;
        };
        return rackRepository.findAll(spec, pageable).map(this::toResponse);
    }

    public InventoryRackResponse findById(Long id) {
        return toResponse(findOrThrow(id));
    }

    @Transactional
    public InventoryRackResponse update(Long id, InventoryRackRequest request) {
        InventoryRack rack = findOrThrow(id);
        InventoryLocation location = findLocation(request.locationId());
        String name = requireTrimmed(request.name(), "Rack name is required");
        String code = requireTrimmed(request.code(), "Rack code is required");

        if (rackRepository.existsByNameIgnoreCaseAndLocationIdAndIdNot(name, location.getId(), id)) {
            throw new IllegalArgumentException("A rack with the name '" + name + "' already exists in this location");
        }
        if (rackRepository.existsByCodeIgnoreCaseAndLocationIdAndIdNot(code, location.getId(), id)) {
            throw new IllegalArgumentException("A rack with the code '" + code + "' already exists in this location");
        }

        rack.setLocation(location);
        rack.setName(name);
        rack.setCode(code.toUpperCase());
        rack.setDescription(trim(request.description()));
        if (request.isActive() != null) rack.setIsActive(request.isActive());
        return toResponse(rackRepository.save(rack));
    }

    @Transactional
    public void delete(Long id) {
        if (!rackRepository.existsById(id)) {
            throw new ResourceNotFoundException("Rack not found with id: " + id);
        }
        rackRepository.deleteById(id);
    }

    @Transactional
    public ActiveStatusUpdateResponse updateStatus(Long id, ActiveStatusUpdateRequest request) {
        InventoryRack rack = findOrThrow(id);
        rack.setIsActive(Boolean.TRUE.equals(request.isActive()));
        InventoryRack saved = rackRepository.save(rack);
        return new ActiveStatusUpdateResponse(saved.getId(), saved.getIsActive(), saved.getUpdatedAt());
    }

    public boolean nameExists(String name, Long locationId, Long excludeId) {
        String trimmed = name == null ? "" : name.trim();
        if (excludeId != null) return rackRepository.existsByNameIgnoreCaseAndLocationIdAndIdNot(trimmed, locationId, excludeId);
        return rackRepository.existsByNameIgnoreCaseAndLocationId(trimmed, locationId);
    }

    public boolean codeExists(String code, Long locationId, Long excludeId) {
        String trimmed = code == null ? "" : code.trim();
        if (excludeId != null) return rackRepository.existsByCodeIgnoreCaseAndLocationIdAndIdNot(trimmed, locationId, excludeId);
        return rackRepository.existsByCodeIgnoreCaseAndLocationId(trimmed, locationId);
    }

    InventoryRack findOrThrow(Long id) {
        return rackRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Rack not found with id: " + id));
    }

    private InventoryLocation findLocation(Long locationId) {
        return locationRepository.findById(locationId)
            .orElseThrow(() -> new ResourceNotFoundException("Inventory location not found with id: " + locationId));
    }

    private InventoryRackResponse toResponse(InventoryRack r) {
        return new InventoryRackResponse(r.getId(), r.getLocation().getId(), r.getLocation().getVirtualName(),
            r.getName(), r.getCode(), r.getDescription(), r.getIsActive(), r.getCreatedAt(), r.getUpdatedAt());
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
