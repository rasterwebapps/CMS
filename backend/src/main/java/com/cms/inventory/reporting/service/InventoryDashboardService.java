package com.cms.inventory.reporting.service;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.EnumSet;

import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cms.inventory.approval.model.enums.ApprovalInstanceStatus;
import com.cms.inventory.approval.repository.ApprovalInstanceRepository;
import com.cms.inventory.asset.model.enums.AssetStatus;
import com.cms.inventory.asset.repository.AssetRepository;
import com.cms.inventory.budget.service.BudgetService;
import com.cms.inventory.consignment.repository.ConsignmentStockLineRepository;
import com.cms.inventory.gatepass.model.enums.GatePassStatus;
import com.cms.inventory.gatepass.repository.GatePassRepository;
import com.cms.inventory.issue.model.enums.LoanableItemIssueStatus;
import com.cms.inventory.issue.repository.LoanableItemIssueRepository;
import com.cms.inventory.procurement.model.enums.PurchaseOrderStatus;
import com.cms.inventory.procurement.model.enums.PurchaseRequisitionStatus;
import com.cms.inventory.procurement.model.enums.WantedListItemStatus;
import com.cms.inventory.procurement.repository.PurchaseOrderRepository;
import com.cms.inventory.procurement.repository.PurchaseRequisitionRepository;
import com.cms.inventory.procurement.repository.WantedListItemRepository;
import com.cms.inventory.reporting.dto.InventoryDashboardResponse;
import com.cms.inventory.ticket.model.enums.ServiceTicketPriority;
import com.cms.inventory.ticket.model.enums.ServiceTicketStatus;
import com.cms.inventory.ticket.repository.ServiceTicketRepository;

/**
 * Phase 8's ("Reporting & Dashboards") first slice — a single at-a-glance overview of the whole
 * Inventory Management module. Every figure is computed live via {@code
 * JpaSpecificationExecutor#count} against entities that already exist (no new table, no stored
 * snapshot) — the same "computed live, never stored" discipline used throughout this module for
 * overdue flags, depreciation, and budget consumption. See the "Inventory Dashboard slice"
 * decision-log entry for why each of these eleven metrics was chosen as the v1 set.
 */
@Service
@Transactional(readOnly = true)
public class InventoryDashboardService {

    private static final EnumSet<PurchaseOrderStatus> OPEN_PO_STATUSES =
        EnumSet.of(PurchaseOrderStatus.PENDING, PurchaseOrderStatus.ORDERED,
            PurchaseOrderStatus.IN_PROGRESS, PurchaseOrderStatus.PARTIALLY_COMPLETED);

    private static final EnumSet<ServiceTicketStatus> OPEN_TICKET_STATUSES =
        EnumSet.of(ServiceTicketStatus.OPEN, ServiceTicketStatus.IN_PROGRESS);

    private final PurchaseOrderRepository purchaseOrderRepository;
    private final PurchaseRequisitionRepository purchaseRequisitionRepository;
    private final WantedListItemRepository wantedListItemRepository;
    private final ApprovalInstanceRepository approvalInstanceRepository;
    private final GatePassRepository gatePassRepository;
    private final LoanableItemIssueRepository loanableItemIssueRepository;
    private final ServiceTicketRepository serviceTicketRepository;
    private final BudgetService budgetService;
    private final ConsignmentStockLineRepository consignmentStockLineRepository;
    private final AssetRepository assetRepository;

    public InventoryDashboardService(PurchaseOrderRepository purchaseOrderRepository,
                                      PurchaseRequisitionRepository purchaseRequisitionRepository,
                                      WantedListItemRepository wantedListItemRepository,
                                      ApprovalInstanceRepository approvalInstanceRepository,
                                      GatePassRepository gatePassRepository,
                                      LoanableItemIssueRepository loanableItemIssueRepository,
                                      ServiceTicketRepository serviceTicketRepository,
                                      BudgetService budgetService,
                                      ConsignmentStockLineRepository consignmentStockLineRepository,
                                      AssetRepository assetRepository) {
        this.purchaseOrderRepository = purchaseOrderRepository;
        this.purchaseRequisitionRepository = purchaseRequisitionRepository;
        this.wantedListItemRepository = wantedListItemRepository;
        this.approvalInstanceRepository = approvalInstanceRepository;
        this.gatePassRepository = gatePassRepository;
        this.loanableItemIssueRepository = loanableItemIssueRepository;
        this.serviceTicketRepository = serviceTicketRepository;
        this.budgetService = budgetService;
        this.consignmentStockLineRepository = consignmentStockLineRepository;
        this.assetRepository = assetRepository;
    }

    public InventoryDashboardResponse get() {
        LocalDate today = LocalDate.now();

        long openPurchaseOrders = purchaseOrderRepository.count(
            (root, query, cb) -> root.get("status").in(OPEN_PO_STATUSES));

        long pendingPurchaseRequisitions = purchaseRequisitionRepository.count(
            (root, query, cb) -> cb.equal(root.get("status"), PurchaseRequisitionStatus.SUBMITTED));

        long pendingWantedListItems = wantedListItemRepository.count(
            (root, query, cb) -> cb.equal(root.get("status"), WantedListItemStatus.PENDING));

        long activeApprovalInstances = approvalInstanceRepository.count(
            (root, query, cb) -> cb.equal(root.get("status"), ApprovalInstanceStatus.IN_PROGRESS));

        long overdueGatePasses = gatePassRepository.count((root, query, cb) -> cb.and(
            cb.isTrue(root.get("returnable")),
            cb.equal(root.get("status"), GatePassStatus.GATE_VERIFIED),
            cb.lessThan(root.get("expectedReturnDate"), today)));

        long overdueLoanableItems = loanableItemIssueRepository.count((root, query, cb) -> cb.and(
            cb.equal(root.get("status"), LoanableItemIssueStatus.ISSUED),
            cb.lessThan(root.get("expectedReturnDate"), today)));

        long openServiceTickets = serviceTicketRepository.count(
            (root, query, cb) -> root.get("status").in(OPEN_TICKET_STATUSES));

        long urgentOpenServiceTickets = serviceTicketRepository.count((root, query, cb) -> cb.and(
            root.get("status").in(OPEN_TICKET_STATUSES),
            cb.equal(root.get("priority"), ServiceTicketPriority.URGENT)));

        long overAllocatedBudgets = budgetService.findPage(null, true, Pageable.ofSize(1000))
            .getContent().stream().filter(b -> b.overAllocated()).count();

        BigDecimal consignmentOutstandingLiability = consignmentStockLineRepository.sumOutstandingLiabilityValue();

        long assetsUnderMaintenance = assetRepository.count(
            (root, query, cb) -> cb.equal(root.get("status"), AssetStatus.UNDER_MAINTENANCE));

        return new InventoryDashboardResponse(
            openPurchaseOrders, pendingPurchaseRequisitions, pendingWantedListItems, activeApprovalInstances,
            overdueGatePasses, overdueLoanableItems, openServiceTickets, urgentOpenServiceTickets,
            overAllocatedBudgets, consignmentOutstandingLiability, assetsUnderMaintenance, Instant.now());
    }
}
