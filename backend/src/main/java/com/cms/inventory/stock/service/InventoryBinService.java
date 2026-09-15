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
import com.cms.inventory.stock.dto.InventoryBinRequest;
import com.cms.inventory.stock.dto.InventoryBinResponse;
import com.cms.inventory.stock.model.InventoryBin;
import com.cms.inventory.stock.model.InventoryRack;
import com.cms.inventory.stock.repository.InventoryBinRepository;
import com.cms.inventory.stock.repository.InventoryRackRepository;

@Service
@Transactional(readOnly = true)
public class InventoryBinService {

    private final InventoryBinRepository binRepository;
    private final InventoryRackRepository rackRepository;

    public InventoryBinService(InventoryBinRepository binRepository, InventoryRackRepository rackRepository) {
        this.binRepository = binRepository;
        this.rackRepository = rackRepository;
    }

    @Transactional
    public InventoryBinResponse create(InventoryBinRequest request) {
        InventoryRack rack = findRack(request.rackId());
        String name = requireTrimmed(request.name(), "Bin name is required");
        String code = requireTrimmed(request.code(), "Bin code is required");

        if (binRepository.existsByNameIgnoreCaseAndRackId(name, rack.getId())) {
            throw new IllegalArgumentException("A bin with the name '" + name + "' already exists on this rack");
        }
        if (binRepository.existsByCodeIgnoreCaseAndRackId(code, rack.getId())) {
            throw new IllegalArgumentException("A bin with the code '" + code + "' already exists on this rack");
        }

        InventoryBin bin = new InventoryBin();
        bin.setRack(rack);
        bin.setName(name);
        bin.setCode(code.toUpperCase());
        bin.setDescription(trim(request.description()));
        if (request.isActive() != null) bin.setIsActive(request.isActive());
        return toResponse(binRepository.save(bin));
    }

    public List<InventoryBinResponse> findAll(Long rackId, Long locationId, boolean activeOnly) {
        List<InventoryBin> bins;
        if (rackId != null && activeOnly) {
            bins = binRepository.findByRackIdAndIsActiveTrueOrderByNameAsc(rackId);
        } else if (rackId != null) {
            bins = binRepository.findByRackIdOrderByNameAsc(rackId);
        } else if (locationId != null && activeOnly) {
            bins = binRepository.findByRackLocationIdAndIsActiveTrueOrderByNameAsc(locationId);
        } else if (activeOnly) {
            bins = binRepository.findByIsActiveTrueOrderByNameAsc();
        } else {
            bins = binRepository.findAll();
        }
        return bins.stream().map(this::toResponse).toList();
    }

    public Page<InventoryBinResponse> findPage(String search, Long rackId, Pageable pageable) {
        Specification<InventoryBin> spec = (root, query, cb) -> {
            var predicates = cb.conjunction();
            if (rackId != null) {
                predicates = cb.and(predicates, cb.equal(root.get("rack").get("id"), rackId));
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
        return binRepository.findAll(spec, pageable).map(this::toResponse);
    }

    public InventoryBinResponse findById(Long id) {
        return toResponse(findOrThrow(id));
    }

    @Transactional
    public InventoryBinResponse update(Long id, InventoryBinRequest request) {
        InventoryBin bin = findOrThrow(id);
        InventoryRack rack = findRack(request.rackId());
        String name = requireTrimmed(request.name(), "Bin name is required");
        String code = requireTrimmed(request.code(), "Bin code is required");

        if (binRepository.existsByNameIgnoreCaseAndRackIdAndIdNot(name, rack.getId(), id)) {
            throw new IllegalArgumentException("A bin with the name '" + name + "' already exists on this rack");
        }
        if (binRepository.existsByCodeIgnoreCaseAndRackIdAndIdNot(code, rack.getId(), id)) {
            throw new IllegalArgumentException("A bin with the code '" + code + "' already exists on this rack");
        }

        bin.setRack(rack);
        bin.setName(name);
        bin.setCode(code.toUpperCase());
        bin.setDescription(trim(request.description()));
        if (request.isActive() != null) bin.setIsActive(request.isActive());
        return toResponse(binRepository.save(bin));
    }

    @Transactional
    public void delete(Long id) {
        if (!binRepository.existsById(id)) {
            throw new ResourceNotFoundException("Bin not found with id: " + id);
        }
        binRepository.deleteById(id);
    }

    @Transactional
    public ActiveStatusUpdateResponse updateStatus(Long id, ActiveStatusUpdateRequest request) {
        InventoryBin bin = findOrThrow(id);
        bin.setIsActive(Boolean.TRUE.equals(request.isActive()));
        InventoryBin saved = binRepository.save(bin);
        return new ActiveStatusUpdateResponse(saved.getId(), saved.getIsActive(), saved.getUpdatedAt());
    }

    public boolean nameExists(String name, Long rackId, Long excludeId) {
        String trimmed = name == null ? "" : name.trim();
        if (excludeId != null) return binRepository.existsByNameIgnoreCaseAndRackIdAndIdNot(trimmed, rackId, excludeId);
        return binRepository.existsByNameIgnoreCaseAndRackId(trimmed, rackId);
    }

    public boolean codeExists(String code, Long rackId, Long excludeId) {
        String trimmed = code == null ? "" : code.trim();
        if (excludeId != null) return binRepository.existsByCodeIgnoreCaseAndRackIdAndIdNot(trimmed, rackId, excludeId);
        return binRepository.existsByCodeIgnoreCaseAndRackId(trimmed, rackId);
    }

    InventoryBin findOrThrow(Long id) {
        return binRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Bin not found with id: " + id));
    }

    private InventoryRack findRack(Long rackId) {
        return rackRepository.findById(rackId)
            .orElseThrow(() -> new ResourceNotFoundException("Rack not found with id: " + rackId));
    }

    private InventoryBinResponse toResponse(InventoryBin b) {
        InventoryRack rack = b.getRack();
        return new InventoryBinResponse(b.getId(), rack.getId(), rack.getName(),
            rack.getLocation().getId(), rack.getLocation().getVirtualName(),
            b.getName(), b.getCode(), b.getDescription(), b.getIsActive(),
            b.getCreatedAt(), b.getUpdatedAt());
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
