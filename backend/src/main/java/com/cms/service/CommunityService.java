package com.cms.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cms.dto.CommunityRequest;
import com.cms.dto.CommunityResponse;
import com.cms.exception.ResourceNotFoundException;
import com.cms.model.Community;
import com.cms.repository.CommunityRepository;

@Service
@Transactional(readOnly = true)
public class CommunityService {

    private final CommunityRepository communityRepository;

    public CommunityService(CommunityRepository communityRepository) {
        this.communityRepository = communityRepository;
    }

    @Transactional
    public CommunityResponse create(CommunityRequest request) {
        if (communityRepository.existsByCode(request.code())) {
            throw new IllegalArgumentException("Community with code '" + request.code() + "' already exists");
        }
        Community community = new Community(request.name(), request.code(), request.description());
        if (request.isActive() != null) {
            community.setIsActive(request.isActive());
        }
        return toResponse(communityRepository.save(community));
    }

    public List<CommunityResponse> findAll() {
        return communityRepository.findAllByOrderByNameAsc().stream()
            .map(this::toResponse)
            .toList();
    }

    public List<CommunityResponse> findActive() {
        return communityRepository.findByIsActiveTrueOrderByNameAsc().stream()
            .map(this::toResponse)
            .toList();
    }

    public CommunityResponse findById(Long id) {
        return toResponse(findEntityById(id));
    }

    @Transactional
    public CommunityResponse update(Long id, CommunityRequest request) {
        Community community = findEntityById(id);
        if (communityRepository.existsByCodeAndIdNot(request.code(), id)) {
            throw new IllegalArgumentException("Community with code '" + request.code() + "' already exists");
        }
        if (communityRepository.existsByNameAndIdNot(request.name(), id)) {
            throw new IllegalArgumentException("Community with name '" + request.name() + "' already exists");
        }
        community.setName(request.name());
        community.setCode(request.code());
        community.setDescription(request.description());
        if (request.isActive() != null) {
            community.setIsActive(request.isActive());
        }
        return toResponse(communityRepository.save(community));
    }

    @Transactional
    public void delete(Long id) {
        if (!communityRepository.existsById(id)) {
            throw new ResourceNotFoundException("Community not found with id: " + id);
        }
        communityRepository.deleteById(id);
    }

    private Community findEntityById(Long id) {
        return communityRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Community not found with id: " + id));
    }

    private CommunityResponse toResponse(Community c) {
        return new CommunityResponse(
            c.getId(), c.getName(), c.getCode(), c.getDescription(),
            c.getIsActive(), c.getCreatedAt(), c.getUpdatedAt()
        );
    }
}

