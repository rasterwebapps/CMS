package com.cms.inventory.ticket.service;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Locale;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cms.exception.ResourceNotFoundException;
import com.cms.inventory.stock.model.InventoryLocation;
import com.cms.inventory.stock.repository.InventoryLocationRepository;
import com.cms.inventory.ticket.dto.ServiceTicketAssignRequest;
import com.cms.inventory.ticket.dto.ServiceTicketCancelRequest;
import com.cms.inventory.ticket.dto.ServiceTicketCloseRequest;
import com.cms.inventory.ticket.dto.ServiceTicketCreateRequest;
import com.cms.inventory.ticket.dto.ServiceTicketResolveRequest;
import com.cms.inventory.ticket.dto.ServiceTicketResponse;
import com.cms.inventory.ticket.model.ServiceTicket;
import com.cms.inventory.ticket.model.ServiceTicketCategory;
import com.cms.inventory.ticket.model.enums.ServiceTicketPriority;
import com.cms.inventory.ticket.model.enums.ServiceTicketStatus;
import com.cms.inventory.ticket.repository.ServiceTicketRepository;

/**
 * Owns the Service Ticket workflow — Phase 7's ("Gate Pass, Vendor-Owned Stock & Service
 * Requests") third and final slice, closing the phase. General complaint/service-request
 * tracking, generic and location-scoped (not coupled to any Product/Asset), independent of the
 * rest of the module. {@code OPEN → IN_PROGRESS (assign) → RESOLVED (resolve) → CLOSED (close)},
 * with {@code CANCELLED} reachable only from {@code OPEN}/{@code IN_PROGRESS}. See {@link
 * ServiceTicketStatus} and the "Service Ticket slice" decision-log entry.
 */
@Service
@Transactional(readOnly = true)
public class ServiceTicketService {

    private final ServiceTicketRepository ticketRepository;
    private final ServiceTicketCategoryService categoryService;
    private final InventoryLocationRepository locationRepository;

    public ServiceTicketService(ServiceTicketRepository ticketRepository,
                                 ServiceTicketCategoryService categoryService,
                                 InventoryLocationRepository locationRepository) {
        this.ticketRepository = ticketRepository;
        this.categoryService = categoryService;
        this.locationRepository = locationRepository;
    }

    @Transactional
    public ServiceTicketResponse create(ServiceTicketCreateRequest request, String createdBy) {
        InventoryLocation location = locationRepository.findById(request.locationId())
            .orElseThrow(() -> new ResourceNotFoundException("Inventory location not found with id: " + request.locationId()));
        ServiceTicketCategory category = categoryService.findOrThrow(request.categoryId());

        ServiceTicket ticket = new ServiceTicket();
        ticket.setLocation(location);
        ticket.setCategory(category);
        ticket.setRequestedBy(request.requestedBy().trim());
        ticket.setPriority(request.priority() != null ? parsePriority(request.priority()) : ServiceTicketPriority.MEDIUM);
        ticket.setStatus(ServiceTicketStatus.OPEN);
        ticket.setDescription(request.description().trim());
        ticket.setCreatedBy(createdBy);
        return toResponse(ticketRepository.save(ticket));
    }

    public Page<ServiceTicketResponse> findPage(Long locationId, Long categoryId, String status, String priority, Pageable pageable) {
        Specification<ServiceTicket> spec = (root, query, cb) -> {
            var predicate = cb.conjunction();
            if (locationId != null) predicate = cb.and(predicate, cb.equal(root.get("location").get("id"), locationId));
            if (categoryId != null) predicate = cb.and(predicate, cb.equal(root.get("category").get("id"), categoryId));
            if (status != null && !status.isBlank()) predicate = cb.and(predicate, cb.equal(root.get("status"), parseStatus(status)));
            if (priority != null && !priority.isBlank()) predicate = cb.and(predicate, cb.equal(root.get("priority"), parsePriority(priority)));
            return predicate;
        };
        return ticketRepository.findAll(spec, pageable).map(this::toResponse);
    }

    public ServiceTicketResponse findById(Long id) {
        return toResponse(requireTicket(id));
    }

    @Transactional
    public ServiceTicketResponse assign(Long id, ServiceTicketAssignRequest request, String performedBy) {
        ServiceTicket ticket = requireTicket(id);
        if (ticket.getStatus() != ServiceTicketStatus.OPEN) {
            throw new IllegalArgumentException("Only an open ticket can be assigned");
        }
        ticket.setAssignedTo(request.assignedTo().trim());
        ticket.setAssignedAt(Instant.now());
        ticket.setStatus(ServiceTicketStatus.IN_PROGRESS);
        return toResponse(ticketRepository.save(ticket));
    }

    @Transactional
    public ServiceTicketResponse resolve(Long id, ServiceTicketResolveRequest request, String performedBy) {
        ServiceTicket ticket = requireTicket(id);
        if (ticket.getStatus() != ServiceTicketStatus.IN_PROGRESS) {
            throw new IllegalArgumentException("Only a ticket in progress can be resolved");
        }
        ticket.setResolutionNotes(request.resolutionNotes().trim());
        ticket.setResolutionDate(LocalDate.now());
        ticket.setResolvedBy(performedBy);
        ticket.setStatus(ServiceTicketStatus.RESOLVED);
        return toResponse(ticketRepository.save(ticket));
    }

    @Transactional
    public ServiceTicketResponse close(Long id, ServiceTicketCloseRequest request, String performedBy) {
        ServiceTicket ticket = requireTicket(id);
        if (ticket.getStatus() != ServiceTicketStatus.RESOLVED) {
            throw new IllegalArgumentException("Only a resolved ticket can be closed");
        }
        if (request != null) ticket.setFeedbackRating(request.feedbackRating());
        ticket.setClosedBy(performedBy);
        ticket.setClosedAt(Instant.now());
        ticket.setStatus(ServiceTicketStatus.CLOSED);
        return toResponse(ticketRepository.save(ticket));
    }

    @Transactional
    public ServiceTicketResponse cancel(Long id, ServiceTicketCancelRequest request, String performedBy) {
        ServiceTicket ticket = requireTicket(id);
        if (ticket.getStatus() != ServiceTicketStatus.OPEN && ticket.getStatus() != ServiceTicketStatus.IN_PROGRESS) {
            throw new IllegalArgumentException("Only an open or in-progress ticket can be cancelled");
        }
        ticket.setCancelledBy(performedBy);
        ticket.setCancelledAt(Instant.now());
        ticket.setCancellationReason(request.reason().trim());
        ticket.setStatus(ServiceTicketStatus.CANCELLED);
        return toResponse(ticketRepository.save(ticket));
    }

    private ServiceTicket requireTicket(Long id) {
        return ticketRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Service ticket not found with id: " + id));
    }

    private ServiceTicketPriority parsePriority(String value) {
        try {
            return ServiceTicketPriority.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid priority '" + value + "'");
        }
    }

    private ServiceTicketStatus parseStatus(String value) {
        try {
            return ServiceTicketStatus.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid status '" + value + "'");
        }
    }

    private ServiceTicketResponse toResponse(ServiceTicket t) {
        InventoryLocation location = t.getLocation();
        ServiceTicketCategory category = t.getCategory();
        return new ServiceTicketResponse(
            t.getId(), location.getId(), location.getVirtualName(),
            category.getId(), category.getName(),
            t.getRequestedBy(), t.getPriority().name(), t.getStatus().name(), t.getDescription(),
            t.getAssignedTo(), t.getAssignedAt(),
            t.getResolutionNotes(), t.getResolutionDate(), t.getResolvedBy(), t.getFeedbackRating(),
            t.getClosedBy(), t.getClosedAt(),
            t.getCancelledBy(), t.getCancelledAt(), t.getCancellationReason(),
            t.getCreatedBy(), t.getCreatedAt(), t.getUpdatedAt());
    }
}
