package com.cms.inventory.catalog.service;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cms.dto.ActiveStatusUpdateRequest;
import com.cms.dto.ActiveStatusUpdateResponse;
import com.cms.exception.ResourceNotFoundException;
import com.cms.inventory.catalog.dto.UomRequest;
import com.cms.inventory.catalog.dto.UomResponse;
import com.cms.inventory.catalog.model.Uom;
import com.cms.inventory.catalog.repository.UomRepository;

@Service
@Transactional(readOnly = true)
public class UomService {

    private final UomRepository uomRepository;

    public UomService(UomRepository uomRepository) {
        this.uomRepository = uomRepository;
    }

    @Transactional
    public UomResponse create(UomRequest request) {
        String code = requireTrimmed(request.code(), "UOM code is required").toUpperCase();
        String name = requireTrimmed(request.name(), "UOM name is required");

        if (uomRepository.existsByCodeIgnoreCase(code)) {
            throw new IllegalArgumentException("A unit of measure with the code '" + code + "' already exists");
        }
        if (uomRepository.existsByNameIgnoreCase(name)) {
            throw new IllegalArgumentException("A unit of measure with the name '" + name + "' already exists");
        }

        Uom uom = new Uom();
        uom.setCode(code);
        uom.setName(name);
        uom.setDescription(trim(request.description()));
        if (request.isActive() != null) uom.setIsActive(request.isActive());
        return toResponse(uomRepository.save(uom));
    }

    public List<UomResponse> findAll(boolean activeOnly) {
        List<Uom> uoms = activeOnly ? uomRepository.findByIsActiveTrueOrderByNameAsc() : uomRepository.findAllByOrderByNameAsc();
        return uoms.stream().map(this::toResponse).toList();
    }

    public Page<UomResponse> findPage(String search, Pageable pageable) {
        Specification<Uom> spec = (root, query, cb) -> {
            if (search == null || search.isBlank()) return cb.conjunction();
            String pattern = "%" + search.trim().toLowerCase() + "%";
            return cb.or(
                cb.like(cb.lower(root.get("name")), pattern),
                cb.like(cb.lower(root.get("code")), pattern)
            );
        };
        return uomRepository.findAll(spec, pageable).map(this::toResponse);
    }

    public UomResponse findById(Long id) {
        return toResponse(findOrThrow(id));
    }

    @Transactional
    public UomResponse update(Long id, UomRequest request) {
        Uom uom = findOrThrow(id);
        String code = requireTrimmed(request.code(), "UOM code is required").toUpperCase();
        String name = requireTrimmed(request.name(), "UOM name is required");

        if (uomRepository.existsByCodeIgnoreCaseAndIdNot(code, id)) {
            throw new IllegalArgumentException("A unit of measure with the code '" + code + "' already exists");
        }
        if (uomRepository.existsByNameIgnoreCaseAndIdNot(name, id)) {
            throw new IllegalArgumentException("A unit of measure with the name '" + name + "' already exists");
        }

        uom.setCode(code);
        uom.setName(name);
        uom.setDescription(trim(request.description()));
        if (request.isActive() != null) uom.setIsActive(request.isActive());
        return toResponse(uomRepository.save(uom));
    }

    @Transactional
    public void delete(Long id) {
        if (!uomRepository.existsById(id)) {
            throw new ResourceNotFoundException("Unit of measure not found with id: " + id);
        }
        uomRepository.deleteById(id);
    }

    @Transactional
    public ActiveStatusUpdateResponse updateStatus(Long id, ActiveStatusUpdateRequest request) {
        Uom uom = findOrThrow(id);
        uom.setIsActive(Boolean.TRUE.equals(request.isActive()));
        Uom saved = uomRepository.save(uom);
        return new ActiveStatusUpdateResponse(saved.getId(), saved.getIsActive(), saved.getUpdatedAt());
    }

    public boolean codeExists(String code, Long excludeId) {
        String trimmed = code == null ? "" : code.trim();
        return excludeId != null
            ? uomRepository.existsByCodeIgnoreCaseAndIdNot(trimmed, excludeId)
            : uomRepository.existsByCodeIgnoreCase(trimmed);
    }

    public boolean nameExists(String name, Long excludeId) {
        String trimmed = name == null ? "" : name.trim();
        return excludeId != null
            ? uomRepository.existsByNameIgnoreCaseAndIdNot(trimmed, excludeId)
            : uomRepository.existsByNameIgnoreCase(trimmed);
    }

    Uom findOrThrow(Long id) {
        return uomRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Unit of measure not found with id: " + id));
    }

    private UomResponse toResponse(Uom u) {
        return new UomResponse(u.getId(), u.getCode(), u.getName(), u.getDescription(), u.getIsActive(), u.getCreatedAt(), u.getUpdatedAt());
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
