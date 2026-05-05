package com.cms.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cms.dto.BloodGroupRequest;
import com.cms.dto.BloodGroupResponse;
import com.cms.exception.ResourceNotFoundException;
import com.cms.model.BloodGroupMaster;
import com.cms.repository.BloodGroupRepository;

@Service
@Transactional(readOnly = true)
public class BloodGroupService {

    private final BloodGroupRepository bloodGroupRepository;

    public BloodGroupService(BloodGroupRepository bloodGroupRepository) {
        this.bloodGroupRepository = bloodGroupRepository;
    }

    @Transactional
    public BloodGroupResponse create(BloodGroupRequest request) {
        if (bloodGroupRepository.existsByCode(request.code())) {
            throw new IllegalArgumentException("Blood group with code '" + request.code() + "' already exists");
        }
        BloodGroupMaster bg = new BloodGroupMaster(request.name(), request.code());
        if (request.isActive() != null) {
            bg.setIsActive(request.isActive());
        }
        return toResponse(bloodGroupRepository.save(bg));
    }

    public List<BloodGroupResponse> findAll() {
        return bloodGroupRepository.findAllByOrderByNameAsc().stream()
            .map(this::toResponse)
            .toList();
    }

    public List<BloodGroupResponse> findActive() {
        return bloodGroupRepository.findByIsActiveTrueOrderByNameAsc().stream()
            .map(this::toResponse)
            .toList();
    }

    public BloodGroupResponse findById(Long id) {
        return toResponse(findEntityById(id));
    }

    @Transactional
    public BloodGroupResponse update(Long id, BloodGroupRequest request) {
        BloodGroupMaster bg = findEntityById(id);
        if (bloodGroupRepository.existsByCodeAndIdNot(request.code(), id)) {
            throw new IllegalArgumentException("Blood group with code '" + request.code() + "' already exists");
        }
        if (bloodGroupRepository.existsByNameAndIdNot(request.name(), id)) {
            throw new IllegalArgumentException("Blood group with name '" + request.name() + "' already exists");
        }
        bg.setName(request.name());
        bg.setCode(request.code());
        if (request.isActive() != null) {
            bg.setIsActive(request.isActive());
        }
        return toResponse(bloodGroupRepository.save(bg));
    }

    @Transactional
    public void delete(Long id) {
        if (!bloodGroupRepository.existsById(id)) {
            throw new ResourceNotFoundException("Blood group not found with id: " + id);
        }
        bloodGroupRepository.deleteById(id);
    }

    private BloodGroupMaster findEntityById(Long id) {
        return bloodGroupRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Blood group not found with id: " + id));
    }

    private BloodGroupResponse toResponse(BloodGroupMaster bg) {
        return new BloodGroupResponse(
            bg.getId(), bg.getName(), bg.getCode(),
            bg.getIsActive(), bg.getCreatedAt(), bg.getUpdatedAt()
        );
    }
}

