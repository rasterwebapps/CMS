package com.cms.service;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cms.dto.ActiveStatusUpdateRequest;
import com.cms.dto.ActiveStatusUpdateResponse;
import com.cms.dto.HostelRoomTypeRequest;
import com.cms.dto.HostelRoomTypeResponse;
import com.cms.exception.ResourceNotFoundException;
import com.cms.model.HostelRoomType;
import com.cms.repository.HostelRoomTypeRepository;

@Service
@Transactional(readOnly = true)
public class HostelRoomTypeService {

    private final HostelRoomTypeRepository hostelRoomTypeRepository;

    public HostelRoomTypeService(HostelRoomTypeRepository hostelRoomTypeRepository) {
        this.hostelRoomTypeRepository = hostelRoomTypeRepository;
    }

    @Transactional
    public HostelRoomTypeResponse create(HostelRoomTypeRequest request) {
        String name = requireTrimmed(request.name(), "Room type name is required");
        String code = requireTrimmed(request.code(), "Room type code is required");

        if (hostelRoomTypeRepository.existsByNameIgnoreCase(name)) {
            throw new IllegalArgumentException(
                "A hostel room type with the name '" + name + "' already exists");
        }
        if (hostelRoomTypeRepository.existsByCodeIgnoreCase(code)) {
            throw new IllegalArgumentException(
                "A hostel room type with the code '" + code + "' already exists");
        }

        HostelRoomType roomType = new HostelRoomType(name, code.toUpperCase(), request.sharingCapacity(),
            request.isAc() != null && request.isAc(), request.feeAmountPerYear(), trim(request.description()));
        if (request.isActive() != null) {
            roomType.setIsActive(request.isActive());
        }
        return toResponse(hostelRoomTypeRepository.save(roomType));
    }

    public List<HostelRoomTypeResponse> findAll() {
        return hostelRoomTypeRepository.findAllByOrderByNameAsc().stream()
            .map(this::toResponse)
            .toList();
    }

    public List<HostelRoomTypeResponse> findActive() {
        return hostelRoomTypeRepository.findByIsActiveTrueOrderByNameAsc().stream()
            .map(this::toResponse)
            .toList();
    }

    public Page<HostelRoomTypeResponse> findPage(String search, Pageable pageable) {
        if (search == null || search.isBlank()) {
            return hostelRoomTypeRepository.findAll(pageable).map(this::toResponse);
        }
        String pattern = "%" + search.trim().toLowerCase() + "%";
        Specification<HostelRoomType> spec = (root, query, cb) ->
            cb.or(
                cb.like(cb.lower(root.get("name")), pattern),
                cb.like(cb.lower(root.get("code")), pattern)
            );
        return hostelRoomTypeRepository.findAll(spec, pageable).map(this::toResponse);
    }

    public HostelRoomTypeResponse findById(Long id) {
        return toResponse(findOrThrow(id));
    }

    @Transactional
    public HostelRoomTypeResponse update(Long id, HostelRoomTypeRequest request) {
        HostelRoomType roomType = findOrThrow(id);
        String name = requireTrimmed(request.name(), "Room type name is required");
        String code = requireTrimmed(request.code(), "Room type code is required");

        if (hostelRoomTypeRepository.existsByNameIgnoreCaseAndIdNot(name, id)) {
            throw new IllegalArgumentException(
                "A hostel room type with the name '" + name + "' already exists");
        }
        if (hostelRoomTypeRepository.existsByCodeIgnoreCaseAndIdNot(code, id)) {
            throw new IllegalArgumentException(
                "A hostel room type with the code '" + code + "' already exists");
        }

        roomType.setName(name);
        roomType.setCode(code.toUpperCase());
        roomType.setSharingCapacity(request.sharingCapacity());
        roomType.setIsAc(request.isAc() != null && request.isAc());
        roomType.setFeeAmountPerYear(request.feeAmountPerYear());
        roomType.setDescription(trim(request.description()));
        if (request.isActive() != null) {
            roomType.setIsActive(request.isActive());
        }
        return toResponse(hostelRoomTypeRepository.save(roomType));
    }

    @Transactional
    public void delete(Long id) {
        if (!hostelRoomTypeRepository.existsById(id)) {
            throw new ResourceNotFoundException("Hostel room type not found with id: " + id);
        }
        hostelRoomTypeRepository.deleteById(id);
    }

    @Transactional
    public ActiveStatusUpdateResponse updateStatus(Long id, ActiveStatusUpdateRequest request) {
        HostelRoomType roomType = findOrThrow(id);
        roomType.setIsActive(Boolean.TRUE.equals(request.isActive()));
        HostelRoomType saved = hostelRoomTypeRepository.save(roomType);
        return new ActiveStatusUpdateResponse(saved.getId(), saved.getIsActive(), saved.getUpdatedAt());
    }

    public boolean nameExists(String name, Long excludeId) {
        String trimmed = name == null ? "" : name.trim();
        if (excludeId != null) return hostelRoomTypeRepository.existsByNameIgnoreCaseAndIdNot(trimmed, excludeId);
        return hostelRoomTypeRepository.existsByNameIgnoreCase(trimmed);
    }

    public boolean codeExists(String code, Long excludeId) {
        String trimmed = code == null ? "" : code.trim();
        if (excludeId != null) return hostelRoomTypeRepository.existsByCodeIgnoreCaseAndIdNot(trimmed, excludeId);
        return hostelRoomTypeRepository.existsByCodeIgnoreCase(trimmed);
    }

    private HostelRoomType findOrThrow(Long id) {
        return hostelRoomTypeRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Hostel room type not found with id: " + id));
    }

    private HostelRoomTypeResponse toResponse(HostelRoomType r) {
        return new HostelRoomTypeResponse(r.getId(), r.getName(), r.getCode(), r.getSharingCapacity(),
            r.getIsAc(), r.getFeeAmountPerYear(), r.getDescription(), r.getIsActive(),
            r.getCreatedAt(), r.getUpdatedAt());
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
