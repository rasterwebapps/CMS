package com.cms.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cms.dto.DesignationRequest;
import com.cms.dto.DesignationResponse;
import com.cms.dto.ActiveStatusUpdateRequest;
import com.cms.dto.ActiveStatusUpdateResponse;
import com.cms.exception.ResourceNotFoundException;
import com.cms.model.DesignationMaster;
import com.cms.repository.DesignationRepository;

@Service
@Transactional(readOnly = true)
public class DesignationService {

    private final DesignationRepository designationRepository;

    public DesignationService(DesignationRepository designationRepository) {
        this.designationRepository = designationRepository;
    }

    @Transactional
    public DesignationResponse create(DesignationRequest request) {
        String name = requireTrimmed(request.name(), "Designation name is required");
        String code = requireTrimmed(request.code(), "Designation code is required");

        if (designationRepository.existsByNameIgnoreCase(name)) {
            throw new IllegalArgumentException(
                "A designation with the name '" + name + "' already exists");
        }
        if (designationRepository.existsByCodeIgnoreCase(code)) {
            throw new IllegalArgumentException(
                "A designation with the code '" + code + "' already exists");
        }

        DesignationMaster designation = new DesignationMaster(name, code.toUpperCase(), trim(request.description()));
        if (request.isActive() != null) {
            designation.setIsActive(request.isActive());
        }
        return toResponse(designationRepository.save(designation));
    }

    public List<DesignationResponse> findAll() {
        return designationRepository.findAllByOrderByNameAsc().stream()
            .map(this::toResponse)
            .toList();
    }

    public List<DesignationResponse> findActive() {
        return designationRepository.findByIsActiveTrueOrderByNameAsc().stream()
            .map(this::toResponse)
            .toList();
    }

    public DesignationResponse findById(Long id) {
        return toResponse(findOrThrow(id));
    }

    @Transactional
    public DesignationResponse update(Long id, DesignationRequest request) {
        DesignationMaster designation = findOrThrow(id);
        String name = requireTrimmed(request.name(), "Designation name is required");
        String code = requireTrimmed(request.code(), "Designation code is required");

        if (designationRepository.existsByNameIgnoreCaseAndIdNot(name, id)) {
            throw new IllegalArgumentException(
                "A designation with the name '" + name + "' already exists");
        }
        if (designationRepository.existsByCodeIgnoreCaseAndIdNot(code, id)) {
            throw new IllegalArgumentException(
                "A designation with the code '" + code + "' already exists");
        }

        designation.setName(name);
        designation.setCode(code.toUpperCase());
        designation.setDescription(trim(request.description()));
        if (request.isActive() != null) {
            designation.setIsActive(request.isActive());
        }
        return toResponse(designationRepository.save(designation));
    }

    @Transactional
    public void delete(Long id) {
        if (!designationRepository.existsById(id)) {
            throw new ResourceNotFoundException("Designation not found with id: " + id);
        }
        designationRepository.deleteById(id);
    }

    @Transactional
    public ActiveStatusUpdateResponse updateStatus(Long id, ActiveStatusUpdateRequest request) {
        DesignationMaster designation = findOrThrow(id);
        designation.setIsActive(Boolean.TRUE.equals(request.isActive()));
        DesignationMaster saved = designationRepository.save(designation);
        return new ActiveStatusUpdateResponse(saved.getId(), saved.getIsActive(), saved.getUpdatedAt());
    }

    public boolean nameExists(String name, Long excludeId) {
        String trimmed = name == null ? "" : name.trim();
        if (excludeId != null) return designationRepository.existsByNameIgnoreCaseAndIdNot(trimmed, excludeId);
        return designationRepository.existsByNameIgnoreCase(trimmed);
    }

    public boolean codeExists(String code, Long excludeId) {
        String trimmed = code == null ? "" : code.trim();
        if (excludeId != null) return designationRepository.existsByCodeIgnoreCaseAndIdNot(trimmed, excludeId);
        return designationRepository.existsByCodeIgnoreCase(trimmed);
    }

    private DesignationMaster findOrThrow(Long id) {
        return designationRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Designation not found with id: " + id));
    }

    private DesignationResponse toResponse(DesignationMaster d) {
        return new DesignationResponse(d.getId(), d.getName(), d.getCode(), d.getDescription(),
            d.getIsActive(), d.getCreatedAt(), d.getUpdatedAt());
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
