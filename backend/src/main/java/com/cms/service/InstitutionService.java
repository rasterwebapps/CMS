package com.cms.service;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cms.dto.InstitutionRequest;
import com.cms.dto.InstitutionResponse;
import com.cms.dto.ActiveStatusUpdateRequest;
import com.cms.dto.ActiveStatusUpdateResponse;
import com.cms.exception.ResourceNotFoundException;
import com.cms.model.Institution;
import com.cms.repository.InstitutionRepository;

@Service
@Transactional(readOnly = true)
public class InstitutionService {

    private final InstitutionRepository institutionRepository;

    public InstitutionService(InstitutionRepository institutionRepository) {
        this.institutionRepository = institutionRepository;
    }

    @Transactional
    public InstitutionResponse create(InstitutionRequest request) {
        String name = requireTrimmed(request.name(), "Institution name is required");
        String code = requireTrimmed(request.code(), "Institution code is required");

        if (institutionRepository.existsByNameIgnoreCase(name)) {
            throw new IllegalArgumentException(
                "An institution with the name '" + name + "' already exists");
        }
        if (institutionRepository.existsByCodeIgnoreCase(code)) {
            throw new IllegalArgumentException(
                "An institution with the code '" + code + "' already exists");
        }

        Institution institution = new Institution(name, code.toUpperCase(), trim(request.description()));
        if (request.isActive() != null) {
            institution.setIsActive(request.isActive());
        }
        return toResponse(institutionRepository.save(institution));
    }

    public List<InstitutionResponse> findAll() {
        return institutionRepository.findAllByOrderByNameAsc().stream()
            .map(this::toResponse)
            .toList();
    }

    public Page<InstitutionResponse> findPage(String search, Pageable pageable) {
        if (search == null || search.isBlank()) {
            return institutionRepository.findAll(pageable).map(this::toResponse);
        }
        String pattern = "%" + search.trim().toLowerCase() + "%";
        Specification<Institution> spec = (root, query, cb) ->
            cb.or(
                cb.like(cb.lower(root.get("name")), pattern),
                cb.like(cb.lower(root.get("code")), pattern)
            );
        return institutionRepository.findAll(spec, pageable).map(this::toResponse);
    }

    public List<InstitutionResponse> findActive() {
        return institutionRepository.findByIsActiveTrueOrderByNameAsc().stream()
            .map(this::toResponse)
            .toList();
    }

    public InstitutionResponse findById(Long id) {
        return toResponse(findOrThrow(id));
    }

    @Transactional
    public InstitutionResponse update(Long id, InstitutionRequest request) {
        Institution institution = findOrThrow(id);
        String name = requireTrimmed(request.name(), "Institution name is required");
        String code = requireTrimmed(request.code(), "Institution code is required");

        if (institutionRepository.existsByNameIgnoreCaseAndIdNot(name, id)) {
            throw new IllegalArgumentException(
                "An institution with the name '" + name + "' already exists");
        }
        if (institutionRepository.existsByCodeIgnoreCaseAndIdNot(code, id)) {
            throw new IllegalArgumentException(
                "An institution with the code '" + code + "' already exists");
        }

        institution.setName(name);
        institution.setCode(code.toUpperCase());
        institution.setDescription(trim(request.description()));
        if (request.isActive() != null) {
            institution.setIsActive(request.isActive());
        }
        return toResponse(institutionRepository.save(institution));
    }

    @Transactional
    public void delete(Long id) {
        if (!institutionRepository.existsById(id)) {
            throw new ResourceNotFoundException("Institution not found with id: " + id);
        }
        institutionRepository.deleteById(id);
    }

    @Transactional
    public ActiveStatusUpdateResponse updateStatus(Long id, ActiveStatusUpdateRequest request) {
        Institution institution = findOrThrow(id);
        institution.setIsActive(Boolean.TRUE.equals(request.isActive()));
        Institution saved = institutionRepository.save(institution);
        return new ActiveStatusUpdateResponse(saved.getId(), saved.getIsActive(), saved.getUpdatedAt());
    }

    public boolean nameExists(String name, Long excludeId) {
        String trimmed = name == null ? "" : name.trim();
        if (excludeId != null) return institutionRepository.existsByNameIgnoreCaseAndIdNot(trimmed, excludeId);
        return institutionRepository.existsByNameIgnoreCase(trimmed);
    }

    public boolean codeExists(String code, Long excludeId) {
        String trimmed = code == null ? "" : code.trim();
        if (excludeId != null) return institutionRepository.existsByCodeIgnoreCaseAndIdNot(trimmed, excludeId);
        return institutionRepository.existsByCodeIgnoreCase(trimmed);
    }

    private Institution findOrThrow(Long id) {
        return institutionRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Institution not found with id: " + id));
    }

    private InstitutionResponse toResponse(Institution i) {
        return new InstitutionResponse(i.getId(), i.getName(), i.getCode(), i.getDescription(),
            i.getIsActive(), i.getCreatedAt(), i.getUpdatedAt());
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
