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
import com.cms.inventory.catalog.dto.BrandRequest;
import com.cms.inventory.catalog.dto.BrandResponse;
import com.cms.inventory.catalog.model.Brand;
import com.cms.inventory.catalog.repository.BrandRepository;

/** Same shape as {@link UomService} minus the code field — see the Brand entity javadoc. */
@Service
@Transactional(readOnly = true)
public class BrandService {

    private final BrandRepository brandRepository;

    public BrandService(BrandRepository brandRepository) {
        this.brandRepository = brandRepository;
    }

    @Transactional
    public BrandResponse create(BrandRequest request) {
        String name = requireTrimmed(request.name(), "Brand name is required");
        if (brandRepository.existsByNameIgnoreCase(name)) {
            throw new IllegalArgumentException("A brand named '" + name + "' already exists");
        }

        Brand brand = new Brand();
        brand.setName(name);
        brand.setDescription(trim(request.description()));
        if (request.isActive() != null) brand.setIsActive(request.isActive());
        return toResponse(brandRepository.save(brand));
    }

    public List<BrandResponse> findAll(boolean activeOnly) {
        List<Brand> brands = activeOnly ? brandRepository.findByIsActiveTrueOrderByNameAsc() : brandRepository.findAllByOrderByNameAsc();
        return brands.stream().map(this::toResponse).toList();
    }

    public Page<BrandResponse> findPage(String search, Pageable pageable) {
        Specification<Brand> spec = (root, query, cb) -> {
            if (search == null || search.isBlank()) return cb.conjunction();
            String pattern = "%" + search.trim().toLowerCase() + "%";
            return cb.like(cb.lower(root.get("name")), pattern);
        };
        return brandRepository.findAll(spec, pageable).map(this::toResponse);
    }

    public BrandResponse findById(Long id) {
        return toResponse(findOrThrow(id));
    }

    @Transactional
    public BrandResponse update(Long id, BrandRequest request) {
        Brand brand = findOrThrow(id);
        String name = requireTrimmed(request.name(), "Brand name is required");
        if (brandRepository.existsByNameIgnoreCaseAndIdNot(name, id)) {
            throw new IllegalArgumentException("A brand named '" + name + "' already exists");
        }

        brand.setName(name);
        brand.setDescription(trim(request.description()));
        if (request.isActive() != null) brand.setIsActive(request.isActive());
        return toResponse(brandRepository.save(brand));
    }

    @Transactional
    public void delete(Long id) {
        if (!brandRepository.existsById(id)) {
            throw new ResourceNotFoundException("Brand not found with id: " + id);
        }
        brandRepository.deleteById(id);
    }

    @Transactional
    public ActiveStatusUpdateResponse updateStatus(Long id, ActiveStatusUpdateRequest request) {
        Brand brand = findOrThrow(id);
        brand.setIsActive(Boolean.TRUE.equals(request.isActive()));
        Brand saved = brandRepository.save(brand);
        return new ActiveStatusUpdateResponse(saved.getId(), saved.getIsActive(), saved.getUpdatedAt());
    }

    public boolean nameExists(String name, Long excludeId) {
        String trimmed = name == null ? "" : name.trim();
        return excludeId != null
            ? brandRepository.existsByNameIgnoreCaseAndIdNot(trimmed, excludeId)
            : brandRepository.existsByNameIgnoreCase(trimmed);
    }

    Brand findOrThrow(Long id) {
        return brandRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Brand not found with id: " + id));
    }

    private BrandResponse toResponse(Brand b) {
        return new BrandResponse(b.getId(), b.getName(), b.getDescription(), b.getIsActive(), b.getCreatedAt(), b.getUpdatedAt());
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
