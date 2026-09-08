package com.cms.inventory.procurement.service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cms.exception.ResourceNotFoundException;
import com.cms.inventory.catalog.model.Product;
import com.cms.inventory.catalog.repository.ProductRepository;
import com.cms.inventory.procurement.dto.PurchaseRequisitionAddLineRequest;
import com.cms.inventory.procurement.dto.PurchaseRequisitionCreateRequest;
import com.cms.inventory.procurement.dto.PurchaseRequisitionItemResponse;
import com.cms.inventory.procurement.dto.PurchaseRequisitionResponse;
import com.cms.inventory.procurement.dto.WantedListConvertRequest;
import com.cms.inventory.procurement.dto.WantedListItemResponse;
import com.cms.inventory.procurement.dto.WantedListRejectRequest;
import com.cms.inventory.procurement.dto.WantedListResolutionRequest;
import com.cms.inventory.procurement.model.ProductLocationQtyProjection;
import com.cms.inventory.procurement.model.PurchaseRequisitionItem;
import com.cms.inventory.procurement.model.WantedListItem;
import com.cms.inventory.procurement.model.enums.WantedListItemStatus;
import com.cms.inventory.procurement.model.enums.WantedListRejectionReason;
import com.cms.inventory.procurement.repository.PurchaseRequisitionItemRepository;
import com.cms.inventory.procurement.repository.PurchaseRequisitionRepository;
import com.cms.inventory.procurement.repository.WantedListItemRepository;
import com.cms.inventory.stock.model.InventoryLocation;
import com.cms.inventory.stock.model.ReorderShortageProjection;
import com.cms.inventory.stock.repository.InventoryLocationRepository;
import com.cms.inventory.stock.repository.StockBalanceRepository;

/**
 * Owns the Wanted List — the ERP-standard MRP "Planned Order" step ahead of Purchase Requisition
 * in this module's procurement chain (Wanted List → Purchase Requisition → Purchase Order, the
 * last not yet built). A scheduled job (plus an equivalent manual trigger) nets each product's
 * configured reorder level against current stock <em>and</em> whatever's already open on a
 * Purchase Requisition, so a shortfall already in the pipeline is never re-flagged — standard MRP
 * netting, not a naive on-hand-vs-reorder-level check. A line's only forward action that commits
 * to real spend is converting it (alone or together with other lines for the same location) into
 * a Purchase Requisition, reusing {@link PurchaseRequisitionService} rather than duplicating its
 * create/add-line/submit logic. See the "Wanted List slice" decision-log entry.
 */
@Service
@Transactional(readOnly = true)
public class WantedListService {

    private static final List<WantedListItemStatus> UNRESOLVED_STATUSES =
        List.of(WantedListItemStatus.PENDING, WantedListItemStatus.DEFERRED);

    private final WantedListItemRepository wantedListItemRepository;
    private final StockBalanceRepository stockBalanceRepository;
    private final PurchaseRequisitionItemRepository requisitionItemRepository;
    private final PurchaseRequisitionRepository requisitionRepository;
    private final ProductRepository productRepository;
    private final InventoryLocationRepository locationRepository;
    private final PurchaseRequisitionService purchaseRequisitionService;

    public WantedListService(WantedListItemRepository wantedListItemRepository,
                              StockBalanceRepository stockBalanceRepository,
                              PurchaseRequisitionItemRepository requisitionItemRepository,
                              PurchaseRequisitionRepository requisitionRepository,
                              ProductRepository productRepository,
                              InventoryLocationRepository locationRepository,
                              PurchaseRequisitionService purchaseRequisitionService) {
        this.wantedListItemRepository = wantedListItemRepository;
        this.stockBalanceRepository = stockBalanceRepository;
        this.requisitionItemRepository = requisitionItemRepository;
        this.requisitionRepository = requisitionRepository;
        this.productRepository = productRepository;
        this.locationRepository = locationRepository;
        this.purchaseRequisitionService = purchaseRequisitionService;
    }

    /** Nightly shortage-netting run — see class-level docs for the netting logic. */
    @Scheduled(cron = "0 0 5 * * *")
    @Transactional
    public void scheduledGenerate() {
        generate();
    }

    /**
     * Runs the same netting computation on demand (the "Run Now" action). Returns how many new
     * lines were created.
     */
    @Transactional
    public int generate() {
        List<ReorderShortageProjection> candidates = stockBalanceRepository.findReorderShortageCandidates();
        if (candidates.isEmpty()) return 0;

        Map<String, BigDecimal> openQtyByKey = requisitionItemRepository.findOpenQtyByProductAndLocation().stream()
            .collect(Collectors.toMap(p -> key(p.getProductId(), p.getLocationId()), ProductLocationQtyProjection::getQty));
        var unresolvedKeys = wantedListItemRepository.findUnresolvedProductLocationKeys(UNRESOLVED_STATUSES);

        Map<Long, Product> products = productRepository
            .findAllById(candidates.stream().map(ReorderShortageProjection::getProductId).distinct().toList())
            .stream().collect(Collectors.toMap(Product::getId, p -> p));
        Map<Long, InventoryLocation> locations = locationRepository
            .findAllById(candidates.stream().map(ReorderShortageProjection::getLocationId).distinct().toList())
            .stream().collect(Collectors.toMap(InventoryLocation::getId, l -> l));

        Instant now = Instant.now();
        int created = 0;
        for (ReorderShortageProjection candidate : candidates) {
            String key = key(candidate.getProductId(), candidate.getLocationId());
            if (unresolvedKeys.contains(key)) continue;

            BigDecimal qtyOnOrder = openQtyByKey.getOrDefault(key, BigDecimal.ZERO);
            BigDecimal netRequirement = candidate.getReorderLevel().subtract(candidate.getQtyOnHand().add(qtyOnOrder));
            if (netRequirement.compareTo(BigDecimal.ZERO) <= 0) continue;

            BigDecimal lotSize = candidate.getReorderQty() != null ? candidate.getReorderQty() : netRequirement;

            WantedListItem item = new WantedListItem();
            item.setProduct(products.get(candidate.getProductId()));
            item.setLocation(locations.get(candidate.getLocationId()));
            item.setStatus(WantedListItemStatus.PENDING);
            item.setQtyOnHandSnapshot(candidate.getQtyOnHand());
            item.setQtyOnOrderSnapshot(qtyOnOrder);
            item.setReorderLevelSnapshot(candidate.getReorderLevel());
            item.setSuggestedQty(lotSize.max(netRequirement));
            item.setGeneratedAt(now);
            item.setUpdatedAt(now);
            wantedListItemRepository.save(item);
            created++;
        }
        return created;
    }

    public Page<WantedListItemResponse> findPage(Long locationId, String status, Pageable pageable) {
        Specification<WantedListItem> spec = (root, query, cb) -> {
            var predicate = cb.conjunction();
            if (locationId != null) predicate = cb.and(predicate, cb.equal(root.get("location").get("id"), locationId));
            if (status != null && !status.isBlank()) predicate = cb.and(predicate, cb.equal(root.get("status"), parseStatus(status)));
            return predicate;
        };
        return wantedListItemRepository.findAll(spec, pageable).map(this::toResponse);
    }

    @Transactional
    public WantedListItemResponse defer(Long id, WantedListResolutionRequest request, String actor) {
        WantedListItem item = requireItem(id);
        requireStatus(item, WantedListItemStatus.PENDING, "defer");
        item.setStatus(WantedListItemStatus.DEFERRED);
        item.setResolvedBy(actor);
        item.setResolvedAt(Instant.now());
        item.setResolutionNotes(trim(request != null ? request.notes() : null));
        item.setUpdatedAt(Instant.now());
        return toResponse(wantedListItemRepository.save(item));
    }

    @Transactional
    public WantedListItemResponse reopen(Long id, String actor) {
        WantedListItem item = requireItem(id);
        requireStatus(item, WantedListItemStatus.DEFERRED, "reopen");
        item.setStatus(WantedListItemStatus.PENDING);
        item.setResolvedBy(actor);
        item.setResolvedAt(Instant.now());
        item.setResolutionNotes(null);
        item.setUpdatedAt(Instant.now());
        return toResponse(wantedListItemRepository.save(item));
    }

    @Transactional
    public WantedListItemResponse reject(Long id, WantedListRejectRequest request, String actor) {
        WantedListItem item = requireItem(id);
        requireUnresolved(item, "reject");
        item.setStatus(WantedListItemStatus.REJECTED);
        item.setRejectionReason(parseReason(request.reason()));
        item.setResolvedBy(actor);
        item.setResolvedAt(Instant.now());
        item.setResolutionNotes(trim(request.notes()));
        item.setUpdatedAt(Instant.now());
        return toResponse(wantedListItemRepository.save(item));
    }

    /**
     * Collectively converts the given lines (all for the same location) into one new Purchase
     * Requisition — created, filled with one line per product, then submitted in the same
     * transaction, ready for that requisition's own approve/reject workflow.
     */
    @Transactional
    public PurchaseRequisitionResponse convert(WantedListConvertRequest request, String actor) {
        List<WantedListItem> items = request.lines().stream()
            .map(l -> requireItem(l.itemId()))
            .toList();

        Long locationId = items.get(0).getLocation().getId();
        for (WantedListItem item : items) {
            requireUnresolved(item, "convert");
            if (!item.getLocation().getId().equals(locationId)) {
                throw new IllegalArgumentException("All selected lines must be for the same location");
            }
        }

        String notes = trim(request.notes()) != null ? request.notes() : "Auto-created from Wanted List";
        PurchaseRequisitionResponse requisition = purchaseRequisitionService.create(
            new PurchaseRequisitionCreateRequest(locationId, request.requisitionDate(), notes), actor);

        Map<Long, BigDecimal> qtyOverrides = request.lines().stream()
            .filter(l -> l.qty() != null)
            .collect(Collectors.toMap(WantedListConvertRequest.Line::itemId, WantedListConvertRequest.Line::qty));

        Instant now = Instant.now();
        for (WantedListItem item : items) {
            BigDecimal qty = qtyOverrides.getOrDefault(item.getId(), item.getSuggestedQty());
            PurchaseRequisitionItemResponse line = purchaseRequisitionService.addLine(requisition.id(),
                new PurchaseRequisitionAddLineRequest(item.getProduct().getId(), qty,
                    "From Wanted List (reorder level " + item.getReorderLevelSnapshot() + ")"));

            item.setStatus(WantedListItemStatus.CONVERTED);
            item.setConvertedPurchaseRequisition(requisitionRepository.getReferenceById(requisition.id()));
            item.setConvertedPurchaseRequisitionItem(requisitionItemRepository.getReferenceById(line.id()));
            item.setResolvedBy(actor);
            item.setResolvedAt(now);
            item.setUpdatedAt(now);
            wantedListItemRepository.save(item);
        }

        return purchaseRequisitionService.submit(requisition.id(), actor);
    }

    private void requireUnresolved(WantedListItem item, String action) {
        if (item.getStatus() != WantedListItemStatus.PENDING && item.getStatus() != WantedListItemStatus.DEFERRED) {
            throw new IllegalArgumentException(
                "Cannot " + action + " a line that is already " + item.getStatus().name().toLowerCase(Locale.ROOT));
        }
    }

    private void requireStatus(WantedListItem item, WantedListItemStatus required, String action) {
        if (item.getStatus() != required) {
            throw new IllegalArgumentException(
                "Cannot " + action + " a line that is " + item.getStatus().name().toLowerCase(Locale.ROOT)
                    + " — it must be " + required.name().toLowerCase(Locale.ROOT));
        }
    }

    private WantedListItem requireItem(Long id) {
        return wantedListItemRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Wanted list line not found with id: " + id));
    }

    private WantedListItemStatus parseStatus(String value) {
        try {
            return WantedListItemStatus.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid status '" + value + "'");
        }
    }

    private WantedListRejectionReason parseReason(String value) {
        try {
            return WantedListRejectionReason.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid rejection reason '" + value + "'");
        }
    }

    private static String key(Long productId, Long locationId) {
        return productId + "-" + locationId;
    }

    private static String trim(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }

    private WantedListItemResponse toResponse(WantedListItem item) {
        Product product = item.getProduct();
        InventoryLocation location = item.getLocation();
        PurchaseRequisitionItem convertedItem = item.getConvertedPurchaseRequisitionItem();
        return new WantedListItemResponse(
            item.getId(), product.getId(), product.getProductCode(), product.getProductName(),
            product.getBaseUom() != null ? product.getBaseUom().getCode() : null,
            location.getId(), location.getVirtualName(),
            item.getStatus().name(),
            item.getQtyOnHandSnapshot(), item.getQtyOnOrderSnapshot(), item.getReorderLevelSnapshot(), item.getSuggestedQty(),
            item.getGeneratedAt(), item.getResolvedBy(), item.getResolvedAt(), item.getResolutionNotes(),
            item.getRejectionReason() != null ? item.getRejectionReason().name() : null,
            item.getConvertedPurchaseRequisition() != null ? item.getConvertedPurchaseRequisition().getId() : null,
            convertedItem != null ? convertedItem.getId() : null);
    }
}
