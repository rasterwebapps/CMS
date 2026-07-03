package com.cms.service;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cms.dto.ScholarshipTypeRequest;
import com.cms.dto.ScholarshipTypeResponse;
import com.cms.dto.ActiveStatusUpdateRequest;
import com.cms.dto.ActiveStatusUpdateResponse;
import com.cms.exception.ResourceNotFoundException;
import com.cms.model.ScholarshipType;
import com.cms.repository.ScholarshipTypeRepository;

@Service
@Transactional(readOnly = true)
public class ScholarshipTypeService {

    private final ScholarshipTypeRepository scholarshipTypeRepository;

    public ScholarshipTypeService(ScholarshipTypeRepository scholarshipTypeRepository) {
        this.scholarshipTypeRepository = scholarshipTypeRepository;
    }

    public List<ScholarshipTypeResponse> getAllActive() {
        return scholarshipTypeRepository.findByActiveTrueOrderByNameAsc().stream()
            .map(this::toResponse)
            .toList();
    }

    public List<ScholarshipTypeResponse> findAll(String search) {
        if (search == null || search.isBlank()) {
            return scholarshipTypeRepository.findAll().stream().map(this::toResponse).toList();
        }
        String pattern = "%" + search.trim().toLowerCase() + "%";
        Specification<ScholarshipType> spec = (root, query, cb) ->
            cb.or(
                cb.like(cb.lower(root.get("name")), pattern),
                cb.like(cb.lower(root.get("code")), pattern)
            );
        return scholarshipTypeRepository.findAll(spec).stream().map(this::toResponse).toList();
    }

    public Page<ScholarshipTypeResponse> findPage(String search, Pageable pageable) {
        if (search == null || search.isBlank()) {
            return scholarshipTypeRepository.findAll(pageable).map(this::toResponse);
        }
        String pattern = "%" + search.trim().toLowerCase() + "%";
        Specification<ScholarshipType> spec = (root, query, cb) ->
            cb.or(
                cb.like(cb.lower(root.get("name")), pattern),
                cb.like(cb.lower(root.get("code")), pattern)
            );
        return scholarshipTypeRepository.findAll(spec, pageable).map(this::toResponse);
    }

    public List<ScholarshipTypeResponse> getAll() {
        return scholarshipTypeRepository.findAll().stream()
            .map(this::toResponse)
            .toList();
    }

    public ScholarshipTypeResponse getById(Long id) {
        return toResponse(findEntity(id));
    }

    @Transactional
    public ScholarshipTypeResponse create(ScholarshipTypeRequest request, String actor) {
        String code = normalizeCode(request.code());
        if (scholarshipTypeRepository.existsByCode(code)) {
            throw new IllegalArgumentException("Scholarship type code already exists: " + code);
        }
        ScholarshipType type = new ScholarshipType();
        apply(type, request, code);
        return toResponse(scholarshipTypeRepository.save(type));
    }

    @Transactional
    public ScholarshipTypeResponse update(Long id, ScholarshipTypeRequest request, String actor) {
        ScholarshipType type = findEntity(id);
        String code = normalizeCode(request.code());
        if (scholarshipTypeRepository.existsByCodeAndIdNot(code, id)) {
            throw new IllegalArgumentException("Scholarship type code already exists: " + code);
        }
        apply(type, request, code);
        return toResponse(scholarshipTypeRepository.save(type));
    }

    @Transactional
    public void deactivate(Long id, String actor) {
        updateStatus(id, new ActiveStatusUpdateRequest(false, null), actor);
    }

    @Transactional
    public ActiveStatusUpdateResponse updateStatus(Long id, ActiveStatusUpdateRequest request, String actor) {
        ScholarshipType type = findEntity(id);
        type.setActive(Boolean.TRUE.equals(request.isActive()));
        ScholarshipType saved = scholarshipTypeRepository.save(type);
        return new ActiveStatusUpdateResponse(saved.getId(), saved.isActive(), saved.getUpdatedAt());
    }

    ScholarshipType findEntity(Long id) {
        return scholarshipTypeRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Scholarship type not found with id: " + id));
    }

    ScholarshipTypeResponse toResponse(ScholarshipType type) {
        return new ScholarshipTypeResponse(
            type.getId(),
            type.getCode(),
            type.getName(),
            type.getDescription(),
            type.isGovtScheme(),
            type.getSchemeCode(),
            type.getDiscountType(),
            type.getDiscountValue(),
            type.getMaxAmountPerYear(),
            type.isRenewalRequired(),
            type.isActive(),
            type.getApplicationMode(),
            type.getPortalName(),
            type.getPortalUrl(),
            type.getEligibleFromYear(),
            type.getEligibleToYear(),
            type.getCreatedAt(),
            type.getUpdatedAt()
        );
    }

    private void apply(ScholarshipType type, ScholarshipTypeRequest request, String code) {
        type.setCode(code);
        type.setName(request.name().trim());
        type.setDescription(blankToNull(request.description()));
        type.setGovtScheme(Boolean.TRUE.equals(request.govtScheme()));
        type.setSchemeCode(blankToNull(request.schemeCode()));
        type.setDiscountType(request.discountType());
        type.setDiscountValue(request.discountValue());
        type.setMaxAmountPerYear(request.maxAmountPerYear());
        type.setRenewalRequired(Boolean.TRUE.equals(request.renewalRequired()));
        type.setActive(request.active() == null || Boolean.TRUE.equals(request.active()));
        type.setApplicationMode(request.applicationMode());
        type.setPortalName(blankToNull(request.portalName()));
        type.setPortalUrl(blankToNull(request.portalUrl()));
        type.setEligibleFromYear(request.eligibleFromYear());
        type.setEligibleToYear(request.eligibleToYear());
        validateYearRange(type);
    }

    private static void validateYearRange(ScholarshipType type) {
        if (type.getEligibleFromYear() != null && type.getEligibleToYear() != null
                && type.getEligibleFromYear() > type.getEligibleToYear()) {
            throw new IllegalArgumentException(
                "Eligible from year cannot be greater than eligible to year");
        }
    }

    private static String normalizeCode(String code) {
        return code.trim().toUpperCase();
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}

