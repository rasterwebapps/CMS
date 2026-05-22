package com.cms.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cms.dto.ReferralTypeRequest;
import com.cms.dto.ReferralTypeResponse;
import com.cms.exception.ResourceNotFoundException;
import com.cms.model.ReferralType;
import com.cms.repository.ReferralTypeRepository;

@Service
@Transactional(readOnly = true)
public class ReferralTypeService {

    private final ReferralTypeRepository referralTypeRepository;

    public ReferralTypeService(ReferralTypeRepository referralTypeRepository) {
        this.referralTypeRepository = referralTypeRepository;
    }

    @Transactional
    public ReferralTypeResponse create(ReferralTypeRequest request) {
        String name = requireTrimmed(request.name(), "Referral type name is required");
        String code = requireTrimmed(request.code(), "Referral type code is required");

        if (referralTypeRepository.existsByNameIgnoreCase(name)) {
            throw new IllegalArgumentException("Referral type with name '" + name + "' already exists");
        }
        if (referralTypeRepository.existsByCodeIgnoreCase(code)) {
            throw new IllegalArgumentException("Referral type with code '" + code + "' already exists");
        }

        Boolean isActive = request.isActive() != null ? request.isActive() : true;
        Boolean hasCommission = request.hasCommission() != null ? request.hasCommission() : false;
        if (Boolean.TRUE.equals(hasCommission)
                && (request.commissionAmount() == null
                    || request.commissionAmount().compareTo(java.math.BigDecimal.ZERO) <= 0)) {
            throw new IllegalArgumentException(
                "Commission amount must be greater than zero when commission is enabled");
        }

        ReferralType referralType = new ReferralType(
            name, code, request.commissionAmount(),
            hasCommission, trim(request.description()), isActive
        );

        ReferralType saved = referralTypeRepository.save(referralType);
        return toResponse(saved);
    }

    public List<ReferralTypeResponse> findAll() {
        return referralTypeRepository.findAll().stream()
            .map(this::toResponse)
            .toList();
    }

    public List<ReferralTypeResponse> findActive() {
        return referralTypeRepository.findByIsActiveTrue().stream()
            .map(this::toResponse)
            .toList();
    }

    public ReferralTypeResponse findById(Long id) {
        ReferralType referralType = referralTypeRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Referral type not found with id: " + id));
        return toResponse(referralType);
    }

    @Transactional
    public ReferralTypeResponse update(Long id, ReferralTypeRequest request) {
        ReferralType referralType = referralTypeRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Referral type not found with id: " + id));
        String name = requireTrimmed(request.name(), "Referral type name is required");
        String code = requireTrimmed(request.code(), "Referral type code is required");

        if (referralTypeRepository.existsByNameIgnoreCaseAndIdNot(name, id)) {
            throw new IllegalArgumentException(
                "A referral type with the name '" + name + "' already exists");
        }

        boolean isSystemDefined = Boolean.TRUE.equals(referralType.getIsSystemDefined());

        if (!isSystemDefined && referralTypeRepository.existsByCodeIgnoreCaseAndIdNot(code, id)) {
            throw new IllegalArgumentException(
                "A referral type with the code '" + code + "' already exists");
        }

        // When commission is enabled (or stays enabled for system-defined), amount must be > 0
        boolean effectiveHasCommission = isSystemDefined
            ? Boolean.TRUE.equals(referralType.getHasCommission())
            : (request.hasCommission() != null ? request.hasCommission() : Boolean.TRUE.equals(referralType.getHasCommission()));
        if (effectiveHasCommission
                && (request.commissionAmount() == null
                    || request.commissionAmount().compareTo(java.math.BigDecimal.ZERO) <= 0)) {
            throw new IllegalArgumentException(
                "Commission amount must be greater than zero when commission is enabled");
        }

        referralType.setName(name);
        referralType.setCommissionAmount(request.commissionAmount());
        referralType.setDescription(trim(request.description()));

        if (!isSystemDefined) {
            referralType.setCode(code);
            if (request.hasCommission() != null) {
                referralType.setHasCommission(request.hasCommission());
            }
        } else if (Boolean.FALSE.equals(request.hasCommission())) {
            throw new IllegalArgumentException(
                "Cannot disable commission for a system-defined referral type");
        }

        if (request.isActive() != null) {
            referralType.setIsActive(request.isActive());
        }

        ReferralType updated = referralTypeRepository.save(referralType);
        return toResponse(updated);
    }

    @Transactional
    public void delete(Long id) {
        ReferralType referralType = referralTypeRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Referral type not found with id: " + id));
        if (Boolean.TRUE.equals(referralType.getIsSystemDefined())) {
            throw new IllegalStateException("System-defined referral types cannot be deleted");
        }
        referralTypeRepository.deleteById(id);
    }

    public boolean nameExists(String name, Long excludeId) {
        String trimmed = name == null ? "" : name.trim();
        if (excludeId != null) {
            return referralTypeRepository.existsByNameIgnoreCaseAndIdNot(trimmed, excludeId);
        }
        return referralTypeRepository.existsByNameIgnoreCase(trimmed);
    }

    public boolean codeExists(String code, Long excludeId) {
        String trimmed = code == null ? "" : code.trim();
        if (excludeId != null) {
            return referralTypeRepository.existsByCodeIgnoreCaseAndIdNot(trimmed, excludeId);
        }
        return referralTypeRepository.existsByCodeIgnoreCase(trimmed);
    }

    private ReferralTypeResponse toResponse(ReferralType rt) {
        return new ReferralTypeResponse(
            rt.getId(), rt.getName(), rt.getCode(), rt.getCommissionAmount(),
            rt.getHasCommission(), rt.getDescription(), rt.getIsActive(),
            Boolean.TRUE.equals(rt.getIsSystemDefined()),
            rt.getCreatedAt(), rt.getUpdatedAt()
        );
    }

    private static String trim(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }

    private static String requireTrimmed(String s, String message) {
        String t = trim(s);
        if (t == null) {
            throw new IllegalArgumentException(message);
        }
        return t;
    }
}
