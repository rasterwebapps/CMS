package com.cms.inventory.approval.service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cms.exception.ResourceNotFoundException;
import com.cms.inventory.approval.dto.ApprovalWorkflowRequest;
import com.cms.inventory.approval.dto.ApprovalWorkflowResponse;
import com.cms.inventory.approval.dto.ApprovalWorkflowStepRequest;
import com.cms.inventory.approval.dto.ApprovalWorkflowStepResponse;
import com.cms.inventory.approval.model.ApprovalWorkflow;
import com.cms.inventory.approval.model.ApprovalWorkflowStep;
import com.cms.inventory.approval.model.enums.ApprovalDocumentType;
import com.cms.inventory.approval.repository.ApprovalWorkflowRepository;
import com.cms.inventory.stock.model.InventoryLocation;
import com.cms.inventory.stock.repository.InventoryLocationRepository;
import com.cms.model.Permission;
import com.cms.repository.PermissionRepository;

/**
 * Owns Approval Workflow definitions — see the {@link ApprovalWorkflow}/{@link
 * ApprovalWorkflowStep} class docs for the sequential-vs-parallel {@code stepOrder} shape and why
 * steps reference the platform's existing {@code Permission} rows rather than a new "approver"
 * concept. {@code steps} are replaced wholesale on every save, same pattern as {@code
 * ProductAlias}/{@code RateContractLine} — a step has no lifecycle independent of its parent
 * workflow. See the "Multi-level approval routing slice" decision-log entry.
 */
@Service
@Transactional(readOnly = true)
public class ApprovalWorkflowService {

    private final ApprovalWorkflowRepository workflowRepository;
    private final InventoryLocationRepository locationRepository;
    private final PermissionRepository permissionRepository;

    public ApprovalWorkflowService(ApprovalWorkflowRepository workflowRepository,
                                    InventoryLocationRepository locationRepository,
                                    PermissionRepository permissionRepository) {
        this.workflowRepository = workflowRepository;
        this.locationRepository = locationRepository;
        this.permissionRepository = permissionRepository;
    }

    @Transactional
    public ApprovalWorkflowResponse create(ApprovalWorkflowRequest request) {
        ApprovalWorkflow workflow = new ApprovalWorkflow();
        applyRequest(workflow, request);
        return toResponse(workflowRepository.save(workflow));
    }

    public Page<ApprovalWorkflowResponse> findPage(String documentType, Boolean activeOnly, Pageable pageable) {
        Specification<ApprovalWorkflow> spec = (root, query, cb) -> {
            var predicate = cb.conjunction();
            if (documentType != null && !documentType.isBlank()) predicate = cb.and(predicate, cb.equal(root.get("documentType"), parseDocumentType(documentType)));
            if (Boolean.TRUE.equals(activeOnly)) predicate = cb.and(predicate, cb.isTrue(root.get("isActive")));
            return predicate;
        };
        return workflowRepository.findAll(spec, pageable).map(this::toResponse);
    }

    public ApprovalWorkflowResponse findById(Long id) {
        return toResponse(findOrThrow(id));
    }

    @Transactional
    public ApprovalWorkflowResponse update(Long id, ApprovalWorkflowRequest request) {
        ApprovalWorkflow workflow = findOrThrow(id);
        applyRequest(workflow, request);
        return toResponse(workflowRepository.save(workflow));
    }

    /** Active workflows whose scope (documentType, location, amount for POs) matches the given document. */
    public List<ApprovalWorkflowResponse> findEligible(String documentType, Long locationId, BigDecimal amount) {
        ApprovalDocumentType type = parseDocumentType(documentType);
        Specification<ApprovalWorkflow> spec = (root, query, cb) -> {
            var predicate = cb.and(cb.equal(root.get("documentType"), type), cb.isTrue(root.get("isActive")));
            predicate = cb.and(predicate, cb.or(cb.isNull(root.get("location")), cb.equal(root.get("location").get("id"), locationId)));
            if (amount != null) {
                predicate = cb.and(predicate, cb.or(cb.isNull(root.get("minAmount")), cb.le(root.get("minAmount"), amount)));
            }
            return predicate;
        };
        return workflowRepository.findAll(spec).stream().map(this::toResponse).toList();
    }

    private void applyRequest(ApprovalWorkflow workflow, ApprovalWorkflowRequest request) {
        ApprovalDocumentType documentType = parseDocumentType(request.documentType());
        InventoryLocation location = null;
        if (request.locationId() != null) {
            location = locationRepository.findById(request.locationId())
                .orElseThrow(() -> new ResourceNotFoundException("Inventory location not found with id: " + request.locationId()));
        }

        workflow.setName(request.name().trim());
        workflow.setDocumentType(documentType);
        workflow.setLocation(location);
        workflow.setMinAmount(documentType == ApprovalDocumentType.PURCHASE_ORDER ? request.minAmount() : null);
        if (request.isActive() != null) workflow.setIsActive(request.isActive());

        workflow.getSteps().clear();
        for (ApprovalWorkflowStepRequest stepRequest : request.steps()) {
            Permission permission = permissionRepository.findByCode(stepRequest.permissionCode().trim())
                .orElseThrow(() -> new IllegalArgumentException("Permission code '" + stepRequest.permissionCode() + "' does not exist"));
            ApprovalWorkflowStep step = new ApprovalWorkflowStep();
            step.setWorkflow(workflow);
            step.setStepOrder(stepRequest.stepOrder());
            step.setStepName(stepRequest.stepName().trim());
            step.setPermission(permission);
            workflow.getSteps().add(step);
        }
    }

    private ApprovalWorkflow findOrThrow(Long id) {
        return workflowRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Approval workflow not found with id: " + id));
    }

    private ApprovalDocumentType parseDocumentType(String value) {
        try {
            return ApprovalDocumentType.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid document type '" + value + "'");
        }
    }

    private ApprovalWorkflowResponse toResponse(ApprovalWorkflow workflow) {
        InventoryLocation location = workflow.getLocation();
        List<ApprovalWorkflowStepResponse> steps = workflow.getSteps().stream()
            .map(s -> new ApprovalWorkflowStepResponse(
                s.getId(), s.getStepOrder(), s.getStepName(),
                s.getPermission().getId(), s.getPermission().getCode(), s.getPermission().getDisplayName()))
            .toList();
        return new ApprovalWorkflowResponse(
            workflow.getId(), workflow.getName(), workflow.getDocumentType().name(),
            location != null ? location.getId() : null, location != null ? location.getVirtualName() : null,
            workflow.getMinAmount(), workflow.getIsActive(), steps,
            workflow.getCreatedAt(), workflow.getUpdatedAt());
    }
}
