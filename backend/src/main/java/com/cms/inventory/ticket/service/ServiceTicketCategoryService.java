package com.cms.inventory.ticket.service;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cms.dto.ActiveStatusUpdateRequest;
import com.cms.dto.ActiveStatusUpdateResponse;
import com.cms.exception.ResourceNotFoundException;
import com.cms.inventory.ticket.dto.ServiceTicketCategoryRequest;
import com.cms.inventory.ticket.dto.ServiceTicketCategoryResponse;
import com.cms.inventory.ticket.model.ServiceTicketCategory;
import com.cms.inventory.ticket.repository.ServiceTicketCategoryRepository;

/**
 * Owns the Service Ticket Category master — the "configurable lookup" the reference architecture
 * calls for, so no hospital/college-specific category is ever hard-coded. Shaped like the
 * already-shipped {@code UomService} minus the code-uniqueness half (a category only ever needs a
 * unique name). See the "Service Ticket slice" decision-log entry.
 */
@Service
@Transactional(readOnly = true)
public class ServiceTicketCategoryService {

    private final ServiceTicketCategoryRepository categoryRepository;

    public ServiceTicketCategoryService(ServiceTicketCategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    @Transactional
    public ServiceTicketCategoryResponse create(ServiceTicketCategoryRequest request) {
        String name = requireTrimmed(request.name(), "Category name is required");
        if (categoryRepository.existsByNameIgnoreCase(name)) {
            throw new IllegalArgumentException("A service ticket category with the name '" + name + "' already exists");
        }
        ServiceTicketCategory category = new ServiceTicketCategory();
        category.setName(name);
        category.setDescription(trim(request.description()));
        if (request.isActive() != null) category.setIsActive(request.isActive());
        return toResponse(categoryRepository.save(category));
    }

    public List<ServiceTicketCategoryResponse> findAll(boolean activeOnly) {
        List<ServiceTicketCategory> categories = activeOnly
            ? categoryRepository.findByIsActiveTrueOrderByNameAsc()
            : categoryRepository.findAllByOrderByNameAsc();
        return categories.stream().map(this::toResponse).toList();
    }

    public Page<ServiceTicketCategoryResponse> findPage(String search, Pageable pageable) {
        Specification<ServiceTicketCategory> spec = (root, query, cb) -> {
            if (search == null || search.isBlank()) return cb.conjunction();
            return cb.like(cb.lower(root.get("name")), "%" + search.trim().toLowerCase() + "%");
        };
        return categoryRepository.findAll(spec, pageable).map(this::toResponse);
    }

    public ServiceTicketCategoryResponse findById(Long id) {
        return toResponse(findOrThrow(id));
    }

    @Transactional
    public ServiceTicketCategoryResponse update(Long id, ServiceTicketCategoryRequest request) {
        ServiceTicketCategory category = findOrThrow(id);
        String name = requireTrimmed(request.name(), "Category name is required");
        if (categoryRepository.existsByNameIgnoreCaseAndIdNot(name, id)) {
            throw new IllegalArgumentException("A service ticket category with the name '" + name + "' already exists");
        }
        category.setName(name);
        category.setDescription(trim(request.description()));
        if (request.isActive() != null) category.setIsActive(request.isActive());
        return toResponse(categoryRepository.save(category));
    }

    @Transactional
    public ActiveStatusUpdateResponse updateStatus(Long id, ActiveStatusUpdateRequest request) {
        ServiceTicketCategory category = findOrThrow(id);
        category.setIsActive(Boolean.TRUE.equals(request.isActive()));
        ServiceTicketCategory saved = categoryRepository.save(category);
        return new ActiveStatusUpdateResponse(saved.getId(), saved.getIsActive(), saved.getUpdatedAt());
    }

    public boolean nameExists(String name, Long excludeId) {
        String trimmed = name == null ? "" : name.trim();
        return excludeId != null
            ? categoryRepository.existsByNameIgnoreCaseAndIdNot(trimmed, excludeId)
            : categoryRepository.existsByNameIgnoreCase(trimmed);
    }

    ServiceTicketCategory findOrThrow(Long id) {
        return categoryRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Service ticket category not found with id: " + id));
    }

    private ServiceTicketCategoryResponse toResponse(ServiceTicketCategory c) {
        return new ServiceTicketCategoryResponse(c.getId(), c.getName(), c.getDescription(), c.getIsActive(), c.getCreatedAt(), c.getUpdatedAt());
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
