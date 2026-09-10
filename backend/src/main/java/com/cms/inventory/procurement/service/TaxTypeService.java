package com.cms.inventory.procurement.service;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cms.dto.ActiveStatusUpdateRequest;
import com.cms.dto.ActiveStatusUpdateResponse;
import com.cms.exception.ResourceNotFoundException;
import com.cms.inventory.procurement.dto.TaxTypeRequest;
import com.cms.inventory.procurement.dto.TaxTypeResponse;
import com.cms.inventory.procurement.model.TaxType;
import com.cms.inventory.procurement.repository.TaxTypeRepository;

@Service
@Transactional(readOnly = true)
public class TaxTypeService {

    private final TaxTypeRepository taxTypeRepository;

    public TaxTypeService(TaxTypeRepository taxTypeRepository) {
        this.taxTypeRepository = taxTypeRepository;
    }

    @Transactional
    public TaxTypeResponse create(TaxTypeRequest request) {
        String name = requireTrimmed(request.name(), "Name is required");
        if (taxTypeRepository.existsByNameIgnoreCase(name)) {
            throw new IllegalArgumentException("A tax type named '" + name + "' already exists");
        }

        TaxType taxType = new TaxType();
        taxType.setName(name);
        taxType.setDescription(trim(request.description()));
        if (request.isActive() != null) taxType.setIsActive(request.isActive());
        return toResponse(taxTypeRepository.save(taxType));
    }

    public List<TaxTypeResponse> findAll(boolean activeOnly) {
        List<TaxType> types = activeOnly ? taxTypeRepository.findByIsActiveTrueOrderByNameAsc() : taxTypeRepository.findAllByOrderByNameAsc();
        return types.stream().map(this::toResponse).toList();
    }

    public Page<TaxTypeResponse> findPage(String search, Pageable pageable) {
        Specification<TaxType> spec = (root, query, cb) -> {
            if (search == null || search.isBlank()) return cb.conjunction();
            return cb.like(cb.lower(root.get("name")), "%" + search.trim().toLowerCase() + "%");
        };
        return taxTypeRepository.findAll(spec, pageable).map(this::toResponse);
    }

    public TaxTypeResponse findById(Long id) {
        return toResponse(findOrThrow(id));
    }

    @Transactional
    public TaxTypeResponse update(Long id, TaxTypeRequest request) {
        TaxType taxType = findOrThrow(id);
        String name = requireTrimmed(request.name(), "Name is required");
        if (taxTypeRepository.existsByNameIgnoreCaseAndIdNot(name, id)) {
            throw new IllegalArgumentException("A tax type named '" + name + "' already exists");
        }

        taxType.setName(name);
        taxType.setDescription(trim(request.description()));
        if (request.isActive() != null) taxType.setIsActive(request.isActive());
        return toResponse(taxTypeRepository.save(taxType));
    }

    @Transactional
    public void delete(Long id) {
        if (!taxTypeRepository.existsById(id)) {
            throw new ResourceNotFoundException("Tax type not found with id: " + id);
        }
        taxTypeRepository.deleteById(id);
    }

    @Transactional
    public ActiveStatusUpdateResponse updateStatus(Long id, ActiveStatusUpdateRequest request) {
        TaxType taxType = findOrThrow(id);
        taxType.setIsActive(Boolean.TRUE.equals(request.isActive()));
        TaxType saved = taxTypeRepository.save(taxType);
        return new ActiveStatusUpdateResponse(saved.getId(), saved.getIsActive(), saved.getUpdatedAt());
    }

    public boolean nameExists(String name, Long excludeId) {
        String trimmed = name == null ? "" : name.trim();
        return excludeId != null
            ? taxTypeRepository.existsByNameIgnoreCaseAndIdNot(trimmed, excludeId)
            : taxTypeRepository.existsByNameIgnoreCase(trimmed);
    }

    TaxType findOrThrow(Long id) {
        return taxTypeRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Tax type not found with id: " + id));
    }

    private TaxTypeResponse toResponse(TaxType t) {
        return new TaxTypeResponse(t.getId(), t.getName(), t.getDescription(), t.getIsActive(), t.getCreatedAt(), t.getUpdatedAt());
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
