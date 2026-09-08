package com.cms.inventory.approval.service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cms.config.PermSecurityBean;
import com.cms.exception.ResourceNotFoundException;
import com.cms.inventory.approval.dto.ApprovalActionBypassRequest;
import com.cms.inventory.approval.dto.ApprovalActionResolutionRequest;
import com.cms.inventory.approval.dto.ApprovalActionResponse;
import com.cms.inventory.approval.dto.ApprovalInstanceResponse;
import com.cms.inventory.approval.dto.ApprovalInstanceStartRequest;
import com.cms.inventory.approval.model.ApprovalAction;
import com.cms.inventory.approval.model.ApprovalInstance;
import com.cms.inventory.approval.model.ApprovalWorkflow;
import com.cms.inventory.approval.model.ApprovalWorkflowStep;
import com.cms.inventory.approval.model.enums.ApprovalActionStatus;
import com.cms.inventory.approval.model.enums.ApprovalDocumentType;
import com.cms.inventory.approval.model.enums.ApprovalExceptionReason;
import com.cms.inventory.approval.model.enums.ApprovalInstanceStatus;
import com.cms.inventory.approval.repository.ApprovalActionRepository;
import com.cms.inventory.approval.repository.ApprovalInstanceRepository;
import com.cms.inventory.approval.repository.ApprovalWorkflowRepository;
import com.cms.inventory.procurement.model.PurchaseOrder;
import com.cms.inventory.procurement.model.PurchaseOrderItem;
import com.cms.inventory.procurement.model.PurchaseRequisition;
import com.cms.inventory.procurement.repository.PurchaseOrderItemRepository;
import com.cms.inventory.procurement.repository.PurchaseOrderRepository;
import com.cms.inventory.procurement.repository.PurchaseRequisitionRepository;

/**
 * Runs Approval Instances — the actual sequential/parallel routing engine. An instance is
 * started explicitly against one already-existing {@code PurchaseRequisition}/{@code
 * PurchaseOrder} (never auto-triggered by their own submit/send actions — see the {@code
 * ApprovalWorkflow} class docs for the exact scope boundary this slice deliberately stays
 * within). {@link #approveAction}/{@link #rejectAction} implement the routing rule: steps
 * sharing a {@code stepOrder} are parallel (every one must {@code APPROVED} before that stage
 * completes); a single {@code REJECTED} at any stage immediately fails the whole instance;
 * completing the last stage's parallel group marks the instance {@code APPROVED}. See the
 * "Multi-level approval routing slice" decision-log entry.
 */
@Service
@Transactional(readOnly = true)
public class ApprovalInstanceService {

    private final ApprovalInstanceRepository instanceRepository;
    private final ApprovalActionRepository actionRepository;
    private final ApprovalWorkflowRepository workflowRepository;
    private final PurchaseRequisitionRepository purchaseRequisitionRepository;
    private final PurchaseOrderRepository purchaseOrderRepository;
    private final PurchaseOrderItemRepository purchaseOrderItemRepository;
    private final PermSecurityBean perm;

    public ApprovalInstanceService(ApprovalInstanceRepository instanceRepository,
                                    ApprovalActionRepository actionRepository,
                                    ApprovalWorkflowRepository workflowRepository,
                                    PurchaseRequisitionRepository purchaseRequisitionRepository,
                                    PurchaseOrderRepository purchaseOrderRepository,
                                    PurchaseOrderItemRepository purchaseOrderItemRepository,
                                    PermSecurityBean perm) {
        this.instanceRepository = instanceRepository;
        this.actionRepository = actionRepository;
        this.workflowRepository = workflowRepository;
        this.purchaseRequisitionRepository = purchaseRequisitionRepository;
        this.purchaseOrderRepository = purchaseOrderRepository;
        this.purchaseOrderItemRepository = purchaseOrderItemRepository;
        this.perm = perm;
    }

    @Transactional
    public ApprovalInstanceResponse start(ApprovalInstanceStartRequest request, String actor) {
        ApprovalWorkflow workflow = workflowRepository.findById(request.workflowId())
            .orElseThrow(() -> new ResourceNotFoundException("Approval workflow not found with id: " + request.workflowId()));
        if (!Boolean.TRUE.equals(workflow.getIsActive())) {
            throw new IllegalArgumentException("This approval workflow is not active");
        }
        if (workflow.getSteps().isEmpty()) {
            throw new IllegalArgumentException("This approval workflow has no steps defined");
        }

        ApprovalInstance instance = new ApprovalInstance();
        instance.setWorkflow(workflow);

        if (workflow.getDocumentType() == ApprovalDocumentType.PURCHASE_REQUISITION) {
            if (request.purchaseRequisitionId() == null) {
                throw new IllegalArgumentException("A purchase requisition id is required for this workflow's document type");
            }
            PurchaseRequisition requisition = purchaseRequisitionRepository.findById(request.purchaseRequisitionId())
                .orElseThrow(() -> new ResourceNotFoundException("Purchase requisition not found with id: " + request.purchaseRequisitionId()));
            if (workflow.getLocation() != null && !workflow.getLocation().getId().equals(requisition.getLocation().getId())) {
                throw new IllegalArgumentException("This workflow does not apply to this requisition's location");
            }
            if (instanceRepository.findFirstByPurchaseRequisitionIdAndStatus(requisition.getId(), ApprovalInstanceStatus.IN_PROGRESS).isPresent()) {
                throw new IllegalArgumentException("This requisition already has an approval in progress");
            }
            instance.setPurchaseRequisition(requisition);
        } else {
            if (request.purchaseOrderId() == null) {
                throw new IllegalArgumentException("A purchase order id is required for this workflow's document type");
            }
            PurchaseOrder order = purchaseOrderRepository.findById(request.purchaseOrderId())
                .orElseThrow(() -> new ResourceNotFoundException("Purchase order not found with id: " + request.purchaseOrderId()));
            if (workflow.getLocation() != null && !workflow.getLocation().getId().equals(order.getLocation().getId())) {
                throw new IllegalArgumentException("This workflow does not apply to this order's location");
            }
            BigDecimal orderTotal = purchaseOrderItemRepository.findByPurchaseOrderIdOrderByIdAsc(order.getId()).stream()
                .map(PurchaseOrderItem::getLineTotal).reduce(BigDecimal.ZERO, BigDecimal::add);
            if (workflow.getMinAmount() != null && orderTotal.compareTo(workflow.getMinAmount()) < 0) {
                throw new IllegalArgumentException("This order's total (" + orderTotal + ") is below this workflow's minimum amount (" + workflow.getMinAmount() + ")");
            }
            if (instanceRepository.findFirstByPurchaseOrderIdAndStatus(order.getId(), ApprovalInstanceStatus.IN_PROGRESS).isPresent()) {
                throw new IllegalArgumentException("This order already has an approval in progress");
            }
            instance.setPurchaseOrder(order);
        }

        int firstStepOrder = workflow.getSteps().stream().map(ApprovalWorkflowStep::getStepOrder).min(Integer::compareTo).orElseThrow();
        instance.setStatus(ApprovalInstanceStatus.IN_PROGRESS);
        instance.setCurrentStepOrder(firstStepOrder);
        instance.setInitiatedBy(actor);
        instance.setInitiatedAt(Instant.now());
        instance.setUpdatedAt(Instant.now());
        instance = instanceRepository.save(instance);

        for (ApprovalWorkflowStep step : workflow.getSteps()) {
            ApprovalAction action = new ApprovalAction();
            action.setInstance(instance);
            action.setWorkflowStep(step);
            action.setStatus(ApprovalActionStatus.PENDING);
            actionRepository.save(action);
        }

        return toResponse(instance);
    }

    public Page<ApprovalInstanceResponse> findPage(String documentType, String status, Pageable pageable) {
        Specification<ApprovalInstance> spec = (root, query, cb) -> {
            var predicate = cb.conjunction();
            if (documentType != null && !documentType.isBlank()) {
                predicate = cb.and(predicate, cb.equal(root.get("workflow").get("documentType"), parseDocumentType(documentType)));
            }
            if (status != null && !status.isBlank()) predicate = cb.and(predicate, cb.equal(root.get("status"), parseInstanceStatus(status)));
            return predicate;
        };
        return instanceRepository.findAll(spec, pageable).map(this::toResponse);
    }

    public ApprovalInstanceResponse findById(Long id) {
        return toResponse(requireInstance(id));
    }

    @Transactional
    public ApprovalInstanceResponse approveAction(Long instanceId, Long actionId, ApprovalActionResolutionRequest request, String actor) {
        ApprovalInstance instance = requireInstance(instanceId);
        ApprovalAction action = requireAction(instance, actionId);
        requireActionable(instance, action);
        return markApprovedAndAdvance(instance, action, trim(request != null ? request.notes() : null), null, actor);
    }

    /**
     * Bypasses a step with a structured exception reason (Phase 6's "Exception handling" slice)
     * instead of an ordinary approval — gated by {@code INVENTORY_APPROVAL_BYPASS}, not the
     * step's own referenced permission, since a bypass is deliberately exercised by someone with
     * exception-granting authority overriding the normal approver requirement, not a substitute
     * way for an ordinary step-permission holder to approve. Counts as approving the step for
     * routing purposes (the stage/instance advances exactly as an ordinary approval would) but is
     * recorded distinctly via {@code exceptionReason} so the audit trail always shows it was an
     * exception, not a normal sign-off. See the "Exception handling slice" decision-log entry.
     */
    @Transactional
    public ApprovalInstanceResponse bypassAction(Long instanceId, Long actionId, ApprovalActionBypassRequest request, String actor) {
        ApprovalInstance instance = requireInstance(instanceId);
        ApprovalAction action = requireAction(instance, actionId);
        requireBypassable(instance, action);
        return markApprovedAndAdvance(instance, action, trim(request.notes()), parseExceptionReason(request.reason()), actor);
    }

    private ApprovalInstanceResponse markApprovedAndAdvance(ApprovalInstance instance, ApprovalAction action, String notes, ApprovalExceptionReason exceptionReason, String actor) {
        action.setStatus(ApprovalActionStatus.APPROVED);
        action.setActedBy(actor);
        action.setActedAt(Instant.now());
        action.setNotes(notes);
        action.setExceptionReason(exceptionReason);
        actionRepository.save(action);

        boolean stageComplete = !actionRepository.existsByInstanceIdAndWorkflowStep_StepOrderAndStatus(
            instance.getId(), instance.getCurrentStepOrder(), ApprovalActionStatus.PENDING);
        if (stageComplete) {
            advanceOrComplete(instance);
        }
        instance.setUpdatedAt(Instant.now());
        instanceRepository.save(instance);
        return toResponse(instance);
    }

    @Transactional
    public ApprovalInstanceResponse rejectAction(Long instanceId, Long actionId, ApprovalActionResolutionRequest request, String actor) {
        ApprovalInstance instance = requireInstance(instanceId);
        ApprovalAction action = requireAction(instance, actionId);
        requireActionable(instance, action);

        action.setStatus(ApprovalActionStatus.REJECTED);
        action.setActedBy(actor);
        action.setActedAt(Instant.now());
        action.setNotes(trim(request != null ? request.notes() : null));
        actionRepository.save(action);

        instance.setStatus(ApprovalInstanceStatus.REJECTED);
        instance.setCompletedAt(Instant.now());
        instance.setUpdatedAt(Instant.now());
        instanceRepository.save(instance);
        return toResponse(instance);
    }

    /** Advances to the next stage's stepOrder, or marks the instance APPROVED if that was the last stage. */
    private void advanceOrComplete(ApprovalInstance instance) {
        List<ApprovalAction> allActions = actionRepository.findByInstanceIdOrderByIdAsc(instance.getId());
        int nextStepOrder = allActions.stream()
            .map(a -> a.getWorkflowStep().getStepOrder())
            .filter(order -> order > instance.getCurrentStepOrder())
            .min(Comparator.naturalOrder())
            .orElse(Integer.MIN_VALUE);
        if (nextStepOrder == Integer.MIN_VALUE) {
            instance.setStatus(ApprovalInstanceStatus.APPROVED);
            instance.setCompletedAt(Instant.now());
        } else {
            instance.setCurrentStepOrder(nextStepOrder);
        }
    }

    private void requireActionable(ApprovalInstance instance, ApprovalAction action) {
        if (instance.getStatus() != ApprovalInstanceStatus.IN_PROGRESS) {
            throw new IllegalArgumentException("This approval is already " + instance.getStatus().name().toLowerCase(Locale.ROOT));
        }
        if (!action.getWorkflowStep().getStepOrder().equals(instance.getCurrentStepOrder())) {
            throw new IllegalArgumentException("This step is not yet at its stage — the approval is currently at step order " + instance.getCurrentStepOrder());
        }
        if (action.getStatus() != ApprovalActionStatus.PENDING) {
            throw new IllegalArgumentException("This step has already been resolved");
        }
        String requiredCode = action.getWorkflowStep().getPermission().getCode();
        if (!perm.has(requiredCode)) {
            throw new IllegalArgumentException("You do not hold the required permission for this step (" + requiredCode + ")");
        }
    }

    /** Same stage/status checks as {@link #requireActionable}, but gated by INVENTORY_APPROVAL_BYPASS instead of the step's own permission. */
    private void requireBypassable(ApprovalInstance instance, ApprovalAction action) {
        if (instance.getStatus() != ApprovalInstanceStatus.IN_PROGRESS) {
            throw new IllegalArgumentException("This approval is already " + instance.getStatus().name().toLowerCase(Locale.ROOT));
        }
        if (!action.getWorkflowStep().getStepOrder().equals(instance.getCurrentStepOrder())) {
            throw new IllegalArgumentException("This step is not yet at its stage — the approval is currently at step order " + instance.getCurrentStepOrder());
        }
        if (action.getStatus() != ApprovalActionStatus.PENDING) {
            throw new IllegalArgumentException("This step has already been resolved");
        }
        if (!perm.has("INVENTORY_APPROVAL_BYPASS")) {
            throw new IllegalArgumentException("You do not hold permission to bypass an approval step");
        }
    }

    private ApprovalInstance requireInstance(Long id) {
        return instanceRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Approval instance not found with id: " + id));
    }

    private ApprovalAction requireAction(ApprovalInstance instance, Long actionId) {
        ApprovalAction action = actionRepository.findById(actionId)
            .orElseThrow(() -> new ResourceNotFoundException("Approval action not found with id: " + actionId));
        if (!action.getInstance().getId().equals(instance.getId())) {
            throw new ResourceNotFoundException("Approval action not found with id: " + actionId);
        }
        return action;
    }

    private ApprovalDocumentType parseDocumentType(String value) {
        try {
            return ApprovalDocumentType.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid document type '" + value + "'");
        }
    }

    private ApprovalExceptionReason parseExceptionReason(String value) {
        try {
            return ApprovalExceptionReason.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid exception reason '" + value + "'");
        }
    }

    private ApprovalInstanceStatus parseInstanceStatus(String value) {
        try {
            return ApprovalInstanceStatus.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid status '" + value + "'");
        }
    }

    private static String trim(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }

    private ApprovalInstanceResponse toResponse(ApprovalInstance instance) {
        ApprovalWorkflow workflow = instance.getWorkflow();
        List<ApprovalActionResponse> actions = actionRepository.findByInstanceIdOrderByIdAsc(instance.getId()).stream()
            .map(a -> toActionResponse(a, instance))
            .toList();
        return new ApprovalInstanceResponse(
            instance.getId(), workflow.getId(), workflow.getName(), workflow.getDocumentType().name(),
            instance.getPurchaseRequisition() != null ? instance.getPurchaseRequisition().getId() : null,
            instance.getPurchaseOrder() != null ? instance.getPurchaseOrder().getId() : null,
            instance.getStatus().name(), instance.getCurrentStepOrder(),
            instance.getInitiatedBy(), instance.getInitiatedAt(), instance.getCompletedAt(), actions);
    }

    private ApprovalActionResponse toActionResponse(ApprovalAction action, ApprovalInstance instance) {
        ApprovalWorkflowStep step = action.getWorkflowStep();
        boolean isCurrentPendingStage = instance.getStatus() == ApprovalInstanceStatus.IN_PROGRESS
            && action.getStatus() == ApprovalActionStatus.PENDING
            && step.getStepOrder().equals(instance.getCurrentStepOrder());
        boolean actionable = isCurrentPendingStage && perm.has(step.getPermission().getCode());
        boolean bypassable = isCurrentPendingStage && perm.has("INVENTORY_APPROVAL_BYPASS");
        return new ApprovalActionResponse(
            action.getId(), step.getStepOrder(), step.getStepName(),
            step.getPermission().getCode(), step.getPermission().getDisplayName(),
            action.getStatus().name(), action.getActedBy(), action.getActedAt(), action.getNotes(),
            action.getExceptionReason() != null ? action.getExceptionReason().name() : null,
            actionable, bypassable);
    }
}
