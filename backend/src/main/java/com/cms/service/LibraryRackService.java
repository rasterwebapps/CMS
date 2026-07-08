package com.cms.service;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cms.dto.ActiveStatusUpdateRequest;
import com.cms.dto.ActiveStatusUpdateResponse;
import com.cms.dto.LibraryRackRequest;
import com.cms.dto.LibraryRackResponse;
import com.cms.exception.ResourceNotFoundException;
import com.cms.model.Library;
import com.cms.model.LibraryRack;
import com.cms.repository.LibraryRackRepository;
import com.cms.repository.LibraryRepository;

@Service
@Transactional(readOnly = true)
public class LibraryRackService {

    private final LibraryRackRepository rackRepository;
    private final LibraryRepository libraryRepository;

    public LibraryRackService(LibraryRackRepository rackRepository, LibraryRepository libraryRepository) {
        this.rackRepository = rackRepository;
        this.libraryRepository = libraryRepository;
    }

    @Transactional
    public LibraryRackResponse create(LibraryRackRequest request) {
        Library library = findLibrary(request.libraryId());
        String name = requireTrimmed(request.name(), "Rack name is required");
        String code = requireTrimmed(request.code(), "Rack code is required");

        if (rackRepository.existsByNameIgnoreCaseAndLibraryId(name, library.getId())) {
            throw new IllegalArgumentException("A rack with the name '" + name + "' already exists in this library");
        }
        if (rackRepository.existsByCodeIgnoreCaseAndLibraryId(code, library.getId())) {
            throw new IllegalArgumentException("A rack with the code '" + code + "' already exists in this library");
        }

        LibraryRack rack = new LibraryRack();
        rack.setLibrary(library);
        rack.setName(name);
        rack.setCode(code.toUpperCase());
        rack.setDescription(trim(request.description()));
        if (request.isActive() != null) rack.setIsActive(request.isActive());
        return toResponse(rackRepository.save(rack));
    }

    public List<LibraryRackResponse> findAll(Long libraryId, boolean activeOnly) {
        List<LibraryRack> racks;
        if (libraryId != null && activeOnly) {
            racks = rackRepository.findByLibraryIdAndIsActiveTrueOrderByNameAsc(libraryId);
        } else if (libraryId != null) {
            racks = rackRepository.findByLibraryIdOrderByNameAsc(libraryId);
        } else if (activeOnly) {
            racks = rackRepository.findByIsActiveTrueOrderByNameAsc();
        } else {
            racks = rackRepository.findAll();
        }
        return racks.stream().map(this::toResponse).toList();
    }

    public Page<LibraryRackResponse> findPage(String search, Long libraryId, Pageable pageable) {
        Specification<LibraryRack> spec = (root, query, cb) -> {
            var predicates = cb.conjunction();
            if (libraryId != null) {
                predicates = cb.and(predicates, cb.equal(root.get("library").get("id"), libraryId));
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
        return rackRepository.findAll(spec, pageable).map(this::toResponse);
    }

    public LibraryRackResponse findById(Long id) {
        return toResponse(findOrThrow(id));
    }

    @Transactional
    public LibraryRackResponse update(Long id, LibraryRackRequest request) {
        LibraryRack rack = findOrThrow(id);
        Library library = findLibrary(request.libraryId());
        String name = requireTrimmed(request.name(), "Rack name is required");
        String code = requireTrimmed(request.code(), "Rack code is required");

        if (rackRepository.existsByNameIgnoreCaseAndLibraryIdAndIdNot(name, library.getId(), id)) {
            throw new IllegalArgumentException("A rack with the name '" + name + "' already exists in this library");
        }
        if (rackRepository.existsByCodeIgnoreCaseAndLibraryIdAndIdNot(code, library.getId(), id)) {
            throw new IllegalArgumentException("A rack with the code '" + code + "' already exists in this library");
        }

        rack.setLibrary(library);
        rack.setName(name);
        rack.setCode(code.toUpperCase());
        rack.setDescription(trim(request.description()));
        if (request.isActive() != null) rack.setIsActive(request.isActive());
        return toResponse(rackRepository.save(rack));
    }

    @Transactional
    public void delete(Long id) {
        if (!rackRepository.existsById(id)) {
            throw new ResourceNotFoundException("Rack not found with id: " + id);
        }
        rackRepository.deleteById(id);
    }

    @Transactional
    public ActiveStatusUpdateResponse updateStatus(Long id, ActiveStatusUpdateRequest request) {
        LibraryRack rack = findOrThrow(id);
        rack.setIsActive(Boolean.TRUE.equals(request.isActive()));
        LibraryRack saved = rackRepository.save(rack);
        return new ActiveStatusUpdateResponse(saved.getId(), saved.getIsActive(), saved.getUpdatedAt());
    }

    public boolean nameExists(String name, Long libraryId, Long excludeId) {
        String trimmed = name == null ? "" : name.trim();
        if (excludeId != null) return rackRepository.existsByNameIgnoreCaseAndLibraryIdAndIdNot(trimmed, libraryId, excludeId);
        return rackRepository.existsByNameIgnoreCaseAndLibraryId(trimmed, libraryId);
    }

    public boolean codeExists(String code, Long libraryId, Long excludeId) {
        String trimmed = code == null ? "" : code.trim();
        if (excludeId != null) return rackRepository.existsByCodeIgnoreCaseAndLibraryIdAndIdNot(trimmed, libraryId, excludeId);
        return rackRepository.existsByCodeIgnoreCaseAndLibraryId(trimmed, libraryId);
    }

    LibraryRack findOrThrow(Long id) {
        return rackRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Rack not found with id: " + id));
    }

    private Library findLibrary(Long libraryId) {
        return libraryRepository.findById(libraryId)
            .orElseThrow(() -> new ResourceNotFoundException("Library not found with id: " + libraryId));
    }

    private LibraryRackResponse toResponse(LibraryRack r) {
        return new LibraryRackResponse(r.getId(), r.getLibrary().getId(), r.getLibrary().getName(),
            r.getName(), r.getCode(), r.getDescription(), r.getIsActive(), r.getCreatedAt(), r.getUpdatedAt());
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
