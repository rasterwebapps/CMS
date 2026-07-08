package com.cms.service;

import java.util.List;
import java.util.regex.Pattern;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cms.dto.ActiveStatusUpdateRequest;
import com.cms.dto.ActiveStatusUpdateResponse;
import com.cms.dto.LibraryShelfRequest;
import com.cms.dto.LibraryShelfResponse;
import com.cms.exception.ResourceNotFoundException;
import com.cms.model.LibraryRack;
import com.cms.model.LibraryShelf;
import com.cms.repository.LibraryRackRepository;
import com.cms.repository.LibraryShelfRepository;

@Service
@Transactional(readOnly = true)
public class LibraryShelfService {

    private static final Pattern NON_ALNUM = Pattern.compile("[^A-Z0-9]+");
    private static final String DEFAULT_TIER_NAME = "General";
    private static final String DEFAULT_TIER_CODE = "GENERAL";

    private final LibraryShelfRepository shelfRepository;
    private final LibraryRackRepository rackRepository;

    public LibraryShelfService(LibraryShelfRepository shelfRepository, LibraryRackRepository rackRepository) {
        this.shelfRepository = shelfRepository;
        this.rackRepository = rackRepository;
    }

    @Transactional
    public LibraryShelfResponse create(LibraryShelfRequest request) {
        LibraryRack rack = findRack(request.rackId());
        String name = requireTrimmed(request.name(), "Shelf name is required");
        String code = requireTrimmed(request.code(), "Shelf code is required");

        if (shelfRepository.existsByNameIgnoreCaseAndRackId(name, rack.getId())) {
            throw new IllegalArgumentException("A shelf with the name '" + name + "' already exists on this rack");
        }
        if (shelfRepository.existsByCodeIgnoreCaseAndRackId(code, rack.getId())) {
            throw new IllegalArgumentException("A shelf with the code '" + code + "' already exists on this rack");
        }

        LibraryShelf shelf = new LibraryShelf();
        shelf.setRack(rack);
        shelf.setName(name);
        shelf.setCode(code.toUpperCase());
        shelf.setDescription(trim(request.description()));
        if (request.isActive() != null) shelf.setIsActive(request.isActive());
        return toResponse(shelfRepository.save(shelf));
    }

    public List<LibraryShelfResponse> findAll(Long rackId, Long libraryId, boolean activeOnly) {
        List<LibraryShelf> shelves;
        if (rackId != null && activeOnly) {
            shelves = shelfRepository.findByRackIdAndIsActiveTrueOrderByNameAsc(rackId);
        } else if (rackId != null) {
            shelves = shelfRepository.findByRackIdOrderByNameAsc(rackId);
        } else if (libraryId != null && activeOnly) {
            shelves = shelfRepository.findByRackLibraryIdAndIsActiveTrueOrderByNameAsc(libraryId);
        } else if (activeOnly) {
            shelves = shelfRepository.findByIsActiveTrueOrderByNameAsc();
        } else {
            shelves = shelfRepository.findAll();
        }
        return shelves.stream().map(this::toResponse).toList();
    }

    public Page<LibraryShelfResponse> findPage(String search, Long rackId, Pageable pageable) {
        Specification<LibraryShelf> spec = (root, query, cb) -> {
            var predicates = cb.conjunction();
            if (rackId != null) {
                predicates = cb.and(predicates, cb.equal(root.get("rack").get("id"), rackId));
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
        return shelfRepository.findAll(spec, pageable).map(this::toResponse);
    }

    public LibraryShelfResponse findById(Long id) {
        return toResponse(findOrThrow(id));
    }

    @Transactional
    public LibraryShelfResponse update(Long id, LibraryShelfRequest request) {
        LibraryShelf shelf = findOrThrow(id);
        LibraryRack rack = findRack(request.rackId());
        String name = requireTrimmed(request.name(), "Shelf name is required");
        String code = requireTrimmed(request.code(), "Shelf code is required");

        if (shelfRepository.existsByNameIgnoreCaseAndRackIdAndIdNot(name, rack.getId(), id)) {
            throw new IllegalArgumentException("A shelf with the name '" + name + "' already exists on this rack");
        }
        if (shelfRepository.existsByCodeIgnoreCaseAndRackIdAndIdNot(code, rack.getId(), id)) {
            throw new IllegalArgumentException("A shelf with the code '" + code + "' already exists on this rack");
        }

        shelf.setRack(rack);
        shelf.setName(name);
        shelf.setCode(code.toUpperCase());
        shelf.setDescription(trim(request.description()));
        if (request.isActive() != null) shelf.setIsActive(request.isActive());
        return toResponse(shelfRepository.save(shelf));
    }

    @Transactional
    public void delete(Long id) {
        if (!shelfRepository.existsById(id)) {
            throw new ResourceNotFoundException("Shelf not found with id: " + id);
        }
        shelfRepository.deleteById(id);
    }

    @Transactional
    public ActiveStatusUpdateResponse updateStatus(Long id, ActiveStatusUpdateRequest request) {
        LibraryShelf shelf = findOrThrow(id);
        shelf.setIsActive(Boolean.TRUE.equals(request.isActive()));
        LibraryShelf saved = shelfRepository.save(shelf);
        return new ActiveStatusUpdateResponse(saved.getId(), saved.getIsActive(), saved.getUpdatedAt());
    }

    public boolean nameExists(String name, Long rackId, Long excludeId) {
        String trimmed = name == null ? "" : name.trim();
        if (excludeId != null) return shelfRepository.existsByNameIgnoreCaseAndRackIdAndIdNot(trimmed, rackId, excludeId);
        return shelfRepository.existsByNameIgnoreCaseAndRackId(trimmed, rackId);
    }

    public boolean codeExists(String code, Long rackId, Long excludeId) {
        String trimmed = code == null ? "" : code.trim();
        if (excludeId != null) return shelfRepository.existsByCodeIgnoreCaseAndRackIdAndIdNot(trimmed, rackId, excludeId);
        return shelfRepository.existsByCodeIgnoreCaseAndRackId(trimmed, rackId);
    }

    LibraryShelf findOrThrow(Long id) {
        return shelfRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Shelf not found with id: " + id));
    }

    /**
     * Resolves free-text legacy shelf-location values (e.g. from CSV import) into a real shelf
     * tier: finds-or-creates a rack named after the text under the given library, with a single
     * default "General" tier — mirrors the V254 backfill migration's auto-create logic.
     */
    @Transactional
    public LibraryShelf resolveOrCreateFromLegacyText(com.cms.model.Library library, String rawText) {
        String name = trim(rawText);
        if (name == null) return null;

        LibraryRack rack = rackRepository.findByLibraryIdOrderByNameAsc(library.getId()).stream()
            .filter(r -> r.getName().equalsIgnoreCase(name))
            .findFirst()
            .orElseGet(() -> {
                LibraryRack created = new LibraryRack();
                created.setLibrary(library);
                created.setName(name);
                created.setCode(uniqueRackCode(library.getId(), name));
                return rackRepository.save(created);
            });

        return shelfRepository.findByNameIgnoreCaseAndRackId(DEFAULT_TIER_NAME, rack.getId())
            .orElseGet(() -> {
                LibraryShelf created = new LibraryShelf();
                created.setRack(rack);
                created.setName(DEFAULT_TIER_NAME);
                created.setCode(DEFAULT_TIER_CODE);
                return shelfRepository.save(created);
            });
    }

    private String uniqueRackCode(Long libraryId, String name) {
        String base = NON_ALNUM.matcher(name.trim().toUpperCase()).replaceAll("_");
        String candidate = base;
        int suffix = 1;
        while (rackRepository.existsByCodeIgnoreCaseAndLibraryId(candidate, libraryId)) {
            suffix++;
            candidate = base + "_" + suffix;
        }
        return candidate;
    }

    private LibraryRack findRack(Long rackId) {
        return rackRepository.findById(rackId)
            .orElseThrow(() -> new ResourceNotFoundException("Rack not found with id: " + rackId));
    }

    private LibraryShelfResponse toResponse(LibraryShelf s) {
        LibraryRack rack = s.getRack();
        return new LibraryShelfResponse(s.getId(), rack.getId(), rack.getName(),
            rack.getLibrary().getId(), rack.getLibrary().getName(),
            s.getName(), s.getCode(), s.getDescription(), s.getIsActive(),
            s.getCreatedAt(), s.getUpdatedAt());
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
