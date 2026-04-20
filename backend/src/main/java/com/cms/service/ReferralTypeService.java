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
        if (referralTypeRepository.existsByCode(request.code())) {
            throw new IllegalArgumentException("Referral type with code '" + request.code() + "' already exists");
        }

        Boolean isActive = request.isActive() != null ? request.isActive() : true;
        Boolean hasCommission = request.hasCommission() != null ? request.hasCommission() : false;

        ReferralType referralType = new ReferralType(
            request.name(), request.code(), request.commissionAmount(),
            hasCommission, request.description(), isActive
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

        if (referralTypeRepository.existsByNameAndIdNot(request.name(), id)) {
            throw new IllegalArgumentException(
                "A referral type with the name '" + request.name() + "' already exists");
        }

        boolean isSystemDefined = Boolean.TRUE.equals(referralType.getIsSystemDefined());

        if (!isSystemDefined && referralTypeRepository.existsByCodeAndIdNot(request.code(), id)) {
            throw new IllegalArgumentException(
                "A referral type with the code '" + request.code() + "' already exists");
        }

        referralType.setName(request.name());
        referralType.setCommissionAmount(request.commissionAmount());
        referralType.setDescription(request.description());

        if (!isSystemDefined) {
            referralType.setCode(request.code());
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

    private ReferralTypeResponse toResponse(ReferralType rt) {
        return new ReferralTypeResponse(
            rt.getId(), rt.getName(), rt.getCode(), rt.getCommissionAmount(),
            rt.getHasCommission(), rt.getDescription(), rt.getIsActive(),
            Boolean.TRUE.equals(rt.getIsSystemDefined()),
            rt.getCreatedAt(), rt.getUpdatedAt()
        );
    }
}
