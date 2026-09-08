package com.cms.inventory.catalog.service;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cms.dto.ActiveStatusUpdateRequest;
import com.cms.dto.ActiveStatusUpdateResponse;
import com.cms.exception.ResourceNotFoundException;
import com.cms.inventory.catalog.dto.CategoryRequest;
import com.cms.inventory.catalog.dto.CategoryResponse;
import com.cms.inventory.catalog.model.Category;
import com.cms.inventory.catalog.repository.CategoryRepository;

@Service
@Transactional(readOnly = true)
public class CategoryService {

    private static final int MAX_TREE_DEPTH = 50;

    private final CategoryRepository categoryRepository;

    public CategoryService(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    @Transactional
    public CategoryResponse create(CategoryRequest request) {
        String name = requireTrimmed(request.name(), "Category name is required");
        Category parent = resolveParent(request.parentCategoryId());

        if (nameTaken(name, parent, null)) {
            throw new IllegalArgumentException(
                "A category with the name '" + name + "' already exists" + (parent == null ? " at the top level" : " under '" + parent.getName() + "'"));
        }

        Category category = new Category();
        category.setName(name);
        category.setParentCategory(parent);
        category.setDescription(trim(request.description()));
        if (request.isActive() != null) category.setIsActive(request.isActive());
        return toResponse(categoryRepository.save(category));
    }

    public List<CategoryResponse> findAll(boolean activeOnly) {
        List<Category> categories = activeOnly
            ? categoryRepository.findByIsActiveTrueOrderByNameAsc()
            : categoryRepository.findAllByOrderByNameAsc();
        return categories.stream().map(this::toResponse).toList();
    }

    public Page<CategoryResponse> findPage(String search, Long parentCategoryId, Pageable pageable) {
        Specification<Category> spec = (root, query, cb) -> {
            var predicates = cb.conjunction();
            if (parentCategoryId != null) {
                predicates = cb.and(predicates, cb.equal(root.get("parentCategory").get("id"), parentCategoryId));
            }
            if (search != null && !search.isBlank()) {
                String pattern = "%" + search.trim().toLowerCase() + "%";
                predicates = cb.and(predicates, cb.like(cb.lower(root.get("name")), pattern));
            }
            return predicates;
        };
        return categoryRepository.findAll(spec, pageable).map(this::toResponse);
    }

    public CategoryResponse findById(Long id) {
        return toResponse(findOrThrow(id));
    }

    @Transactional
    public CategoryResponse update(Long id, CategoryRequest request) {
        Category category = findOrThrow(id);
        String name = requireTrimmed(request.name(), "Category name is required");
        Category parent = resolveParent(request.parentCategoryId());

        if (parent != null && parent.getId().equals(id)) {
            throw new IllegalArgumentException("A category cannot be its own parent");
        }
        if (parent != null) {
            assertNotDescendant(parent, id);
        }
        if (nameTaken(name, parent, id)) {
            throw new IllegalArgumentException(
                "A category with the name '" + name + "' already exists" + (parent == null ? " at the top level" : " under '" + parent.getName() + "'"));
        }

        category.setName(name);
        category.setParentCategory(parent);
        category.setDescription(trim(request.description()));
        if (request.isActive() != null) category.setIsActive(request.isActive());
        return toResponse(categoryRepository.save(category));
    }

    @Transactional
    public void delete(Long id) {
        if (!categoryRepository.existsById(id)) {
            throw new ResourceNotFoundException("Category not found with id: " + id);
        }
        if (categoryRepository.existsByParentCategoryId(id)) {
            throw new IllegalArgumentException("Cannot delete a category that has sub-categories — move or delete them first");
        }
        categoryRepository.deleteById(id);
    }

    @Transactional
    public ActiveStatusUpdateResponse updateStatus(Long id, ActiveStatusUpdateRequest request) {
        Category category = findOrThrow(id);
        category.setIsActive(Boolean.TRUE.equals(request.isActive()));
        Category saved = categoryRepository.save(category);
        return new ActiveStatusUpdateResponse(saved.getId(), saved.getIsActive(), saved.getUpdatedAt());
    }

    public boolean nameExists(String name, Long parentCategoryId, Long excludeId) {
        String trimmed = name == null ? "" : name.trim();
        Category parent = parentCategoryId == null ? null : categoryRepository.findById(parentCategoryId).orElse(null);
        return nameTaken(trimmed, parent, excludeId);
    }

    Category findOrThrow(Long id) {
        return categoryRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Category not found with id: " + id));
    }

    private Category resolveParent(Long parentCategoryId) {
        if (parentCategoryId == null) return null;
        return findOrThrow(parentCategoryId);
    }

    private boolean nameTaken(String name, Category parent, Long excludeId) {
        if (parent == null) {
            return excludeId != null
                ? categoryRepository.existsByNameIgnoreCaseAndParentCategoryIdIsNullAndIdNot(name, excludeId)
                : categoryRepository.existsByNameIgnoreCaseAndParentCategoryIdIsNull(name);
        }
        return excludeId != null
            ? categoryRepository.existsByNameIgnoreCaseAndParentCategoryIdAndIdNot(name, parent.getId(), excludeId)
            : categoryRepository.existsByNameIgnoreCaseAndParentCategoryId(name, parent.getId());
    }

    /** Walks the candidate parent's own ancestor chain to reject cycles (a category becoming its own descendant's child). */
    private void assertNotDescendant(Category candidateParent, Long categoryId) {
        Category current = candidateParent;
        int depth = 0;
        while (current != null) {
            if (current.getId().equals(categoryId)) {
                throw new IllegalArgumentException("Cannot move a category under one of its own sub-categories");
            }
            if (++depth > MAX_TREE_DEPTH) {
                throw new IllegalStateException("Category tree exceeds maximum supported depth — check for a data inconsistency");
            }
            current = current.getParentCategory();
        }
    }

    private CategoryResponse toResponse(Category c) {
        Category parent = c.getParentCategory();
        return new CategoryResponse(c.getId(), c.getName(), parent == null ? null : parent.getId(),
            parent == null ? null : parent.getName(), c.getDescription(), c.getIsActive(), c.getCreatedAt(), c.getUpdatedAt());
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
