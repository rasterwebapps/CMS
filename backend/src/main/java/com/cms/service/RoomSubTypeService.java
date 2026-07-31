package com.cms.service;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cms.dto.ActiveStatusUpdateRequest;
import com.cms.dto.ActiveStatusUpdateResponse;
import com.cms.dto.RoomSubTypeRequest;
import com.cms.dto.RoomSubTypeResponse;
import com.cms.exception.ResourceNotFoundException;
import com.cms.model.RoomPurposeCategory;
import com.cms.model.RoomSubType;
import com.cms.repository.RoomSubTypeRepository;

@Service
@Transactional(readOnly = true)
public class RoomSubTypeService {

    private final RoomSubTypeRepository roomSubTypeRepository;
    private final RoomPurposeCategoryService roomPurposeCategoryService;

    public RoomSubTypeService(RoomSubTypeRepository roomSubTypeRepository,
                               RoomPurposeCategoryService roomPurposeCategoryService) {
        this.roomSubTypeRepository = roomSubTypeRepository;
        this.roomPurposeCategoryService = roomPurposeCategoryService;
    }

    @Transactional
    public RoomSubTypeResponse create(RoomSubTypeRequest request) {
        RoomPurposeCategory category = roomPurposeCategoryService.findOrThrow(request.purposeCategoryId());
        String name = requireTrimmed(request.name(), "Sub-type name is required");
        String code = requireTrimmed(request.code(), "Sub-type code is required");

        if (roomSubTypeRepository.existsByNameIgnoreCaseAndPurposeCategoryId(name, category.getId())) {
            throw new IllegalArgumentException(
                "A sub-type with the name '" + name + "' already exists in this category");
        }
        if (roomSubTypeRepository.existsByCodeIgnoreCaseAndPurposeCategoryId(code, category.getId())) {
            throw new IllegalArgumentException(
                "A sub-type with the code '" + code + "' already exists in this category");
        }

        RoomSubType subType = new RoomSubType();
        subType.setPurposeCategory(category);
        subType.setName(name);
        subType.setCode(code.toUpperCase());
        subType.setDescription(trim(request.description()));
        if (request.isActive() != null) subType.setIsActive(request.isActive());
        return toResponse(roomSubTypeRepository.save(subType));
    }

    public List<RoomSubTypeResponse> findAll(Long purposeCategoryId, boolean activeOnly) {
        List<RoomSubType> subTypes;
        if (purposeCategoryId != null && activeOnly) {
            subTypes = roomSubTypeRepository.findByPurposeCategoryIdAndIsActiveTrueOrderByNameAsc(purposeCategoryId);
        } else if (purposeCategoryId != null) {
            subTypes = roomSubTypeRepository.findByPurposeCategoryIdOrderByNameAsc(purposeCategoryId);
        } else if (activeOnly) {
            subTypes = roomSubTypeRepository.findByIsActiveTrueOrderByNameAsc();
        } else {
            subTypes = roomSubTypeRepository.findAll();
        }
        return subTypes.stream().map(this::toResponse).toList();
    }

    public Page<RoomSubTypeResponse> findPage(String search, Long purposeCategoryId, Pageable pageable) {
        Specification<RoomSubType> spec = (root, query, cb) -> {
            var predicates = cb.conjunction();
            if (purposeCategoryId != null) {
                predicates = cb.and(predicates, cb.equal(root.get("purposeCategory").get("id"), purposeCategoryId));
            }
            if (search != null && !search.isBlank()) {
                String pattern = "%" + search.trim().toLowerCase() + "%";
                predicates = cb.and(predicates, cb.or(
                    cb.like(cb.lower(root.get("name")), pattern),
                    cb.like(cb.lower(root.get("code")), pattern)
                ));
            }
            return predicates;
        };
        return roomSubTypeRepository.findAll(spec, pageable).map(this::toResponse);
    }

    public RoomSubTypeResponse findById(Long id) {
        return toResponse(findOrThrow(id));
    }

    @Transactional
    public RoomSubTypeResponse update(Long id, RoomSubTypeRequest request) {
        RoomSubType subType = findOrThrow(id);
        RoomPurposeCategory category = roomPurposeCategoryService.findOrThrow(request.purposeCategoryId());
        String name = requireTrimmed(request.name(), "Sub-type name is required");
        String code = requireTrimmed(request.code(), "Sub-type code is required");

        if (roomSubTypeRepository.existsByNameIgnoreCaseAndPurposeCategoryIdAndIdNot(name, category.getId(), id)) {
            throw new IllegalArgumentException(
                "A sub-type with the name '" + name + "' already exists in this category");
        }
        if (roomSubTypeRepository.existsByCodeIgnoreCaseAndPurposeCategoryIdAndIdNot(code, category.getId(), id)) {
            throw new IllegalArgumentException(
                "A sub-type with the code '" + code + "' already exists in this category");
        }

        subType.setPurposeCategory(category);
        subType.setName(name);
        subType.setCode(code.toUpperCase());
        subType.setDescription(trim(request.description()));
        if (request.isActive() != null) subType.setIsActive(request.isActive());
        return toResponse(roomSubTypeRepository.save(subType));
    }

    @Transactional
    public void delete(Long id) {
        if (!roomSubTypeRepository.existsById(id)) {
            throw new ResourceNotFoundException("Room sub-type not found with id: " + id);
        }
        roomSubTypeRepository.deleteById(id);
    }

    @Transactional
    public ActiveStatusUpdateResponse updateStatus(Long id, ActiveStatusUpdateRequest request) {
        RoomSubType subType = findOrThrow(id);
        subType.setIsActive(Boolean.TRUE.equals(request.isActive()));
        RoomSubType saved = roomSubTypeRepository.save(subType);
        return new ActiveStatusUpdateResponse(saved.getId(), saved.getIsActive(), saved.getUpdatedAt());
    }

    public boolean nameExists(String name, Long purposeCategoryId, Long excludeId) {
        String trimmed = name == null ? "" : name.trim();
        if (excludeId != null) return roomSubTypeRepository.existsByNameIgnoreCaseAndPurposeCategoryIdAndIdNot(trimmed, purposeCategoryId, excludeId);
        return roomSubTypeRepository.existsByNameIgnoreCaseAndPurposeCategoryId(trimmed, purposeCategoryId);
    }

    public boolean codeExists(String code, Long purposeCategoryId, Long excludeId) {
        String trimmed = code == null ? "" : code.trim();
        if (excludeId != null) return roomSubTypeRepository.existsByCodeIgnoreCaseAndPurposeCategoryIdAndIdNot(trimmed, purposeCategoryId, excludeId);
        return roomSubTypeRepository.existsByCodeIgnoreCaseAndPurposeCategoryId(trimmed, purposeCategoryId);
    }

    RoomSubType findOrThrow(Long id) {
        return roomSubTypeRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Room sub-type not found with id: " + id));
    }

    private RoomSubTypeResponse toResponse(RoomSubType s) {
        RoomPurposeCategory c = s.getPurposeCategory();
        return new RoomSubTypeResponse(s.getId(), c.getId(), c.getName(), s.getName(), s.getCode(),
            s.getDescription(), s.getIsActive(), s.getCreatedAt(), s.getUpdatedAt());
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
