package com.cms.service;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cms.dto.ActiveStatusUpdateRequest;
import com.cms.dto.ActiveStatusUpdateResponse;
import com.cms.dto.RoomPurposeCategoryRequest;
import com.cms.dto.RoomPurposeCategoryResponse;
import com.cms.exception.ResourceNotFoundException;
import com.cms.model.RoomPurposeCategory;
import com.cms.repository.RoomPurposeCategoryRepository;

@Service
@Transactional(readOnly = true)
public class RoomPurposeCategoryService {

    private final RoomPurposeCategoryRepository roomPurposeCategoryRepository;

    public RoomPurposeCategoryService(RoomPurposeCategoryRepository roomPurposeCategoryRepository) {
        this.roomPurposeCategoryRepository = roomPurposeCategoryRepository;
    }

    @Transactional
    public RoomPurposeCategoryResponse create(RoomPurposeCategoryRequest request) {
        String name = requireTrimmed(request.name(), "Category name is required");
        String code = requireTrimmed(request.code(), "Category code is required");

        if (roomPurposeCategoryRepository.existsByNameIgnoreCase(name)) {
            throw new IllegalArgumentException(
                "A room purpose category with the name '" + name + "' already exists");
        }
        if (roomPurposeCategoryRepository.existsByCodeIgnoreCase(code)) {
            throw new IllegalArgumentException(
                "A room purpose category with the code '" + code + "' already exists");
        }

        RoomPurposeCategory category = new RoomPurposeCategory(name, code.toUpperCase(),
            request.isResidential(), trim(request.description()));
        if (request.isActive() != null) {
            category.setIsActive(request.isActive());
        }
        return toResponse(roomPurposeCategoryRepository.save(category));
    }

    public List<RoomPurposeCategoryResponse> findAll() {
        return roomPurposeCategoryRepository.findAllByOrderByNameAsc().stream()
            .map(this::toResponse)
            .toList();
    }

    public List<RoomPurposeCategoryResponse> findActive() {
        return roomPurposeCategoryRepository.findByIsActiveTrueOrderByNameAsc().stream()
            .map(this::toResponse)
            .toList();
    }

    public Page<RoomPurposeCategoryResponse> findPage(String search, Pageable pageable) {
        if (search == null || search.isBlank()) {
            return roomPurposeCategoryRepository.findAll(pageable).map(this::toResponse);
        }
        String pattern = "%" + search.trim().toLowerCase() + "%";
        Specification<RoomPurposeCategory> spec = (root, query, cb) ->
            cb.or(
                cb.like(cb.lower(root.get("name")), pattern),
                cb.like(cb.lower(root.get("code")), pattern)
            );
        return roomPurposeCategoryRepository.findAll(spec, pageable).map(this::toResponse);
    }

    public RoomPurposeCategoryResponse findById(Long id) {
        return toResponse(findOrThrow(id));
    }

    @Transactional
    public RoomPurposeCategoryResponse update(Long id, RoomPurposeCategoryRequest request) {
        RoomPurposeCategory category = findOrThrow(id);
        String name = requireTrimmed(request.name(), "Category name is required");
        String code = requireTrimmed(request.code(), "Category code is required");

        if (roomPurposeCategoryRepository.existsByNameIgnoreCaseAndIdNot(name, id)) {
            throw new IllegalArgumentException(
                "A room purpose category with the name '" + name + "' already exists");
        }
        if (roomPurposeCategoryRepository.existsByCodeIgnoreCaseAndIdNot(code, id)) {
            throw new IllegalArgumentException(
                "A room purpose category with the code '" + code + "' already exists");
        }

        category.setName(name);
        category.setCode(code.toUpperCase());
        category.setIsResidential(request.isResidential() != null && request.isResidential());
        category.setDescription(trim(request.description()));
        if (request.isActive() != null) {
            category.setIsActive(request.isActive());
        }
        return toResponse(roomPurposeCategoryRepository.save(category));
    }

    @Transactional
    public void delete(Long id) {
        if (!roomPurposeCategoryRepository.existsById(id)) {
            throw new ResourceNotFoundException("Room purpose category not found with id: " + id);
        }
        roomPurposeCategoryRepository.deleteById(id);
    }

    @Transactional
    public ActiveStatusUpdateResponse updateStatus(Long id, ActiveStatusUpdateRequest request) {
        RoomPurposeCategory category = findOrThrow(id);
        category.setIsActive(Boolean.TRUE.equals(request.isActive()));
        RoomPurposeCategory saved = roomPurposeCategoryRepository.save(category);
        return new ActiveStatusUpdateResponse(saved.getId(), saved.getIsActive(), saved.getUpdatedAt());
    }

    public boolean nameExists(String name, Long excludeId) {
        String trimmed = name == null ? "" : name.trim();
        if (excludeId != null) return roomPurposeCategoryRepository.existsByNameIgnoreCaseAndIdNot(trimmed, excludeId);
        return roomPurposeCategoryRepository.existsByNameIgnoreCase(trimmed);
    }

    public boolean codeExists(String code, Long excludeId) {
        String trimmed = code == null ? "" : code.trim();
        if (excludeId != null) return roomPurposeCategoryRepository.existsByCodeIgnoreCaseAndIdNot(trimmed, excludeId);
        return roomPurposeCategoryRepository.existsByCodeIgnoreCase(trimmed);
    }

    RoomPurposeCategory findOrThrow(Long id) {
        return roomPurposeCategoryRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Room purpose category not found with id: " + id));
    }

    private RoomPurposeCategoryResponse toResponse(RoomPurposeCategory c) {
        return new RoomPurposeCategoryResponse(c.getId(), c.getName(), c.getCode(), c.getIsResidential(),
            c.getDescription(), c.getIsActive(), c.getCreatedAt(), c.getUpdatedAt());
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
