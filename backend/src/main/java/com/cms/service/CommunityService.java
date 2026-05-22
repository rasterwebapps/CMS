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
        String name = requireTrimmed(request.name(), "Community name is required");
        String code = requireTrimmed(request.code(), "Community code is required");
        if (communityRepository.existsByNameIgnoreCase(name)) {
            throw new IllegalArgumentException("Community with name '" + name + "' already exists");
        }
        if (communityRepository.existsByCodeIgnoreCase(code)) {
            throw new IllegalArgumentException("Community with code '" + code + "' already exists");
        }
        Community community = new Community(name, code, trim(request.description()));
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
        String name = requireTrimmed(request.name(), "Community name is required");
        String code = requireTrimmed(request.code(), "Community code is required");
        if (communityRepository.existsByCodeIgnoreCaseAndIdNot(code, id)) {
            throw new IllegalArgumentException("Community with code '" + code + "' already exists");
        }
        if (communityRepository.existsByNameIgnoreCaseAndIdNot(name, id)) {
            throw new IllegalArgumentException("Community with name '" + name + "' already exists");
        }
        community.setName(name);
        community.setCode(code);
        community.setDescription(trim(request.description()));
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

    public boolean nameExists(String name, Long excludeId) {
        String trimmed = name == null ? "" : name.trim();
        if (excludeId != null) {
            return communityRepository.existsByNameIgnoreCaseAndIdNot(trimmed, excludeId);
        }
        return communityRepository.existsByNameIgnoreCase(trimmed);
    }

    public boolean codeExists(String code, Long excludeId) {
        String trimmed = code == null ? "" : code.trim();
        if (excludeId != null) {
            return communityRepository.existsByCodeIgnoreCaseAndIdNot(trimmed, excludeId);
        }
        return communityRepository.existsByCodeIgnoreCase(trimmed);
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

