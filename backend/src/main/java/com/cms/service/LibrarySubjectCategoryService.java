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
import com.cms.dto.LibrarySubjectCategoryRequest;
import com.cms.dto.LibrarySubjectCategoryResponse;
import com.cms.exception.ResourceNotFoundException;
import com.cms.model.LibrarySubjectCategory;
import com.cms.repository.LibraryBookRepository;
import com.cms.repository.LibrarySubjectCategoryRepository;

@Service
@Transactional(readOnly = true)
public class LibrarySubjectCategoryService {

    private static final Pattern NON_ALNUM = Pattern.compile("[^A-Z0-9]+");

    private final LibrarySubjectCategoryRepository categoryRepository;
    private final LibraryBookRepository bookRepository;

    public LibrarySubjectCategoryService(LibrarySubjectCategoryRepository categoryRepository,
                                         LibraryBookRepository bookRepository) {
        this.categoryRepository = categoryRepository;
        this.bookRepository = bookRepository;
    }

    @Transactional
    public LibrarySubjectCategoryResponse create(LibrarySubjectCategoryRequest request) {
        String name = requireTrimmed(request.name(), "Category name is required");
        String code = request.code() != null && !request.code().isBlank()
            ? request.code().trim().toUpperCase()
            : uniqueCategoryCode(name);

        if (categoryRepository.existsByNameIgnoreCase(name)) {
            throw new IllegalArgumentException("A subject category with the name '" + name + "' already exists");
        }
        if (categoryRepository.existsByCodeIgnoreCase(code)) {
            throw new IllegalArgumentException("A subject category with the code '" + code + "' already exists");
        }

        LibrarySubjectCategory category = new LibrarySubjectCategory();
        category.setName(name);
        category.setCode(code);
        category.setDescription(trim(request.description()));
        if (request.isActive() != null) {
            category.setIsActive(request.isActive());
        }
        return toResponse(categoryRepository.save(category));
    }

    public List<LibrarySubjectCategoryResponse> findAll(boolean activeOnly) {
        List<LibrarySubjectCategory> categories = activeOnly
            ? categoryRepository.findByIsActiveTrueOrderByNameAsc()
            : categoryRepository.findAllByOrderByNameAsc();
        return categories.stream().map(this::toResponse).toList();
    }

    public Page<LibrarySubjectCategoryResponse> findPage(String search, Pageable pageable) {
        Specification<LibrarySubjectCategory> spec = (root, query, cb) -> {
            if (search == null || search.isBlank()) {
                return cb.conjunction();
            }
            String pattern = "%" + search.trim().toLowerCase() + "%";
            return cb.or(
                cb.like(cb.lower(root.get("name")), pattern),
                cb.like(cb.lower(root.get("code")), pattern),
                cb.like(cb.lower(root.get("description")), pattern)
            );
        };
        return categoryRepository.findAll(spec, pageable).map(this::toResponse);
    }

    public LibrarySubjectCategoryResponse findById(Long id) {
        return toResponse(findOrThrow(id));
    }

    @Transactional
    public LibrarySubjectCategoryResponse update(Long id, LibrarySubjectCategoryRequest request) {
        LibrarySubjectCategory category = findOrThrow(id);
        String name = requireTrimmed(request.name(), "Category name is required");
        String code = request.code() != null && !request.code().isBlank()
            ? request.code().trim().toUpperCase()
            : category.getCode();

        if (categoryRepository.existsByNameIgnoreCaseAndIdNot(name, id)) {
            throw new IllegalArgumentException("A subject category with the name '" + name + "' already exists");
        }
        if (categoryRepository.existsByCodeIgnoreCaseAndIdNot(code, id)) {
            throw new IllegalArgumentException("A subject category with the code '" + code + "' already exists");
        }

        category.setName(name);
        category.setCode(code);
        category.setDescription(trim(request.description()));
        if (request.isActive() != null) {
            category.setIsActive(request.isActive());
        }
        return toResponse(categoryRepository.save(category));
    }

    @Transactional
    public void delete(Long id) {
        if (!categoryRepository.existsById(id)) {
            throw new ResourceNotFoundException("Subject category not found with id: " + id);
        }
        if (bookRepository.existsBySubjectCategoryMasterId(id)) {
            throw new IllegalStateException("Cannot delete subject category that has associated books. Please deactivate it instead.");
        }
        categoryRepository.deleteById(id);
    }

    @Transactional
    public ActiveStatusUpdateResponse updateStatus(Long id, ActiveStatusUpdateRequest request) {
        LibrarySubjectCategory category = findOrThrow(id);
        category.setIsActive(Boolean.TRUE.equals(request.isActive()));
        LibrarySubjectCategory saved = categoryRepository.save(category);
        return new ActiveStatusUpdateResponse(saved.getId(), saved.getIsActive(), saved.getUpdatedAt());
    }

    public boolean nameExists(String name, Long excludeId) {
        String trimmed = name == null ? "" : name.trim();
        if (excludeId != null) {
            return categoryRepository.existsByNameIgnoreCaseAndIdNot(trimmed, excludeId);
        }
        return categoryRepository.existsByNameIgnoreCase(trimmed);
    }

    public boolean codeExists(String code, Long excludeId) {
        String trimmed = code == null ? "" : code.trim();
        if (excludeId != null) {
            return categoryRepository.existsByCodeIgnoreCaseAndIdNot(trimmed, excludeId);
        }
        return categoryRepository.existsByCodeIgnoreCase(trimmed);
    }

    @Transactional
    public LibrarySubjectCategory resolveOrCreate(String rawText) {
        String name = trim(rawText);
        if (name == null) return null;

        return categoryRepository.findByNameIgnoreCase(name)
            .orElseGet(() -> {
                LibrarySubjectCategory created = new LibrarySubjectCategory();
                created.setName(name);
                created.setCode(uniqueCategoryCode(name));
                created.setIsActive(true);
                return categoryRepository.save(created);
            });
    }

    LibrarySubjectCategory findOrThrow(Long id) {
        return categoryRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Subject category not found with id: " + id));
    }

    private String uniqueCategoryCode(String name) {
        String base = NON_ALNUM.matcher(name.trim().toUpperCase()).replaceAll("_");
        if (base.isEmpty()) base = "CAT";
        if (base.length() > 35) base = base.substring(0, 35);
        String candidate = base;
        int suffix = 1;
        while (categoryRepository.existsByCodeIgnoreCase(candidate)) {
            candidate = base + "_" + suffix++;
        }
        return candidate;
    }

    private static String requireTrimmed(String s, String message) {
        if (s == null || s.trim().isEmpty()) {
            throw new IllegalArgumentException(message);
        }
        return s.trim();
    }

    private static String trim(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }

    LibrarySubjectCategoryResponse toResponse(LibrarySubjectCategory c) {
        return new LibrarySubjectCategoryResponse(
            c.getId(),
            c.getName(),
            c.getCode(),
            c.getDescription(),
            c.getIsActive(),
            c.getCreatedAt(),
            c.getUpdatedAt()
        );
    }
}
