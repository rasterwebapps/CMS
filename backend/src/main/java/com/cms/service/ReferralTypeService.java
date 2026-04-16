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

        referralType.setName(request.name());
        referralType.setCode(request.code());
        referralType.setCommissionAmount(request.commissionAmount());
        referralType.setDescription(request.description());

        if (request.hasCommission() != null) {
            referralType.setHasCommission(request.hasCommission());
        }

        if (request.isActive() != null) {
            referralType.setIsActive(request.isActive());
        }

        ReferralType updated = referralTypeRepository.save(referralType);
        return toResponse(updated);
    }

    @Transactional
    public void delete(Long id) {
        if (!referralTypeRepository.existsById(id)) {
            throw new ResourceNotFoundException("Referral type not found with id: " + id);
        }
        referralTypeRepository.deleteById(id);
    }

    private ReferralTypeResponse toResponse(ReferralType rt) {
        return new ReferralTypeResponse(
            rt.getId(), rt.getName(), rt.getCode(), rt.getCommissionAmount(),
            rt.getHasCommission(), rt.getDescription(), rt.getIsActive(),
            rt.getCreatedAt(), rt.getUpdatedAt()
        );
    }
}
