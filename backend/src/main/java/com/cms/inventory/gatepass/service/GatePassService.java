package com.cms.inventory.gatepass.service;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Locale;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cms.exception.ResourceNotFoundException;
import com.cms.inventory.asset.model.Asset;
import com.cms.inventory.asset.repository.AssetRepository;
import com.cms.inventory.catalog.model.Product;
import com.cms.inventory.catalog.repository.ProductRepository;
import com.cms.inventory.gatepass.dto.GatePassCreateRequest;
import com.cms.inventory.gatepass.dto.GatePassRejectRequest;
import com.cms.inventory.gatepass.dto.GatePassResponse;
import com.cms.inventory.gatepass.dto.GatePassReturnRequest;
import com.cms.inventory.gatepass.model.GatePass;
import com.cms.inventory.gatepass.model.enums.GatePassDirection;
import com.cms.inventory.gatepass.model.enums.GatePassStatus;
import com.cms.inventory.gatepass.repository.GatePassRepository;
import com.cms.inventory.procurement.model.PurchaseOrder;
import com.cms.inventory.procurement.repository.PurchaseOrderRepository;
import com.cms.inventory.stock.model.InventoryLocation;
import com.cms.inventory.stock.repository.InventoryLocationRepository;

/**
 * Owns the Gate Pass workflow — Phase 7's ("Gate Pass, Vendor-Owned Stock & Service Requests")
 * first slice. A gate pass is created (against exactly one of a {@link Product} or an
 * {@link Asset}), approved, then physically verified at the gate — two always-distinct steps
 * even when the same person holds both permissions, per the reference architecture ({@code
 * ER_DIAGRAM_AND_MODULE_BOUNDARIES.md} §6). A non-returnable pass closes as soon as it's gate-
 * verified; a returnable one stays open (and can go overdue) until marked returned. "Overdue" is
 * computed at read time, never stored, matching {@code LoanableItemIssue}. See the "Gate Pass
 * slice" decision-log entry.
 */
@Service
@Transactional(readOnly = true)
public class GatePassService {

    private final GatePassRepository gatePassRepository;
    private final ProductRepository productRepository;
    private final AssetRepository assetRepository;
    private final InventoryLocationRepository locationRepository;
    private final PurchaseOrderRepository purchaseOrderRepository;

    public GatePassService(GatePassRepository gatePassRepository,
                            ProductRepository productRepository,
                            AssetRepository assetRepository,
                            InventoryLocationRepository locationRepository,
                            PurchaseOrderRepository purchaseOrderRepository) {
        this.gatePassRepository = gatePassRepository;
        this.productRepository = productRepository;
        this.assetRepository = assetRepository;
        this.locationRepository = locationRepository;
        this.purchaseOrderRepository = purchaseOrderRepository;
    }

    @Transactional
    public GatePassResponse create(GatePassCreateRequest request, String createdBy) {
        if ((request.productId() == null) == (request.assetId() == null)) {
            throw new IllegalArgumentException("A gate pass must reference exactly one of a product or an asset");
        }
        GatePassDirection direction = parseDirection(request.direction());
        boolean returnable = Boolean.TRUE.equals(request.returnable());
        LocalDate passDate = request.passDate() != null ? request.passDate() : LocalDate.now();
        if (returnable) {
            if (request.expectedReturnDate() == null) {
                throw new IllegalArgumentException("Expected return date is required for a returnable gate pass");
            }
            if (request.expectedReturnDate().isBefore(passDate)) {
                throw new IllegalArgumentException("Expected return date cannot be before the pass date");
            }
        }

        GatePass pass = new GatePass();
        pass.setDirection(direction);
        pass.setReturnable(returnable);
        if (request.productId() != null) {
            pass.setProduct(requireProduct(request.productId()));
        } else {
            pass.setAsset(requireAsset(request.assetId()));
        }
        pass.setLocation(requireLocation(request.locationId()));
        pass.setQuantity(request.quantity());
        pass.setReason(request.reason().trim());
        pass.setPartyName(request.partyName().trim());
        pass.setPartyContact(trim(request.partyContact()));
        if (request.linkedPurchaseOrderId() != null) {
            pass.setLinkedPurchaseOrder(purchaseOrderRepository.findById(request.linkedPurchaseOrderId())
                .orElseThrow(() -> new ResourceNotFoundException("Purchase order not found with id: " + request.linkedPurchaseOrderId())));
        }
        pass.setPassDate(passDate);
        pass.setExpectedReturnDate(returnable ? request.expectedReturnDate() : null);
        pass.setStatus(GatePassStatus.PENDING_APPROVAL);
        pass.setNotes(trim(request.notes()));
        pass.setCreatedBy(createdBy);
        pass.setCreatedAt(Instant.now());
        pass.setUpdatedAt(Instant.now());
        return toResponse(gatePassRepository.save(pass));
    }

    public Page<GatePassResponse> findPage(Long locationId, String direction, String status, Boolean overdueOnly, Pageable pageable) {
        Specification<GatePass> spec = (root, query, cb) -> {
            var predicate = cb.conjunction();
            if (locationId != null) predicate = cb.and(predicate, cb.equal(root.get("location").get("id"), locationId));
            if (direction != null && !direction.isBlank()) predicate = cb.and(predicate, cb.equal(root.get("direction"), parseDirection(direction)));
            if (status != null && !status.isBlank()) predicate = cb.and(predicate, cb.equal(root.get("status"), parseStatus(status)));
            if (Boolean.TRUE.equals(overdueOnly)) {
                predicate = cb.and(predicate,
                    cb.isTrue(root.get("returnable")),
                    cb.equal(root.get("status"), GatePassStatus.GATE_VERIFIED),
                    cb.lessThan(root.get("expectedReturnDate"), LocalDate.now()));
            }
            return predicate;
        };
        return gatePassRepository.findAll(spec, pageable).map(this::toResponse);
    }

    public GatePassResponse findById(Long id) {
        return toResponse(requireGatePass(id));
    }

    @Transactional
    public GatePassResponse approve(Long id, String approvedBy) {
        GatePass pass = requireGatePass(id);
        if (pass.getStatus() != GatePassStatus.PENDING_APPROVAL) {
            throw new IllegalArgumentException("Only a gate pass pending approval can be approved");
        }
        pass.setStatus(GatePassStatus.APPROVED);
        pass.setApprovedBy(approvedBy);
        pass.setApprovedAt(Instant.now());
        pass.setUpdatedAt(Instant.now());
        return toResponse(gatePassRepository.save(pass));
    }

    @Transactional
    public GatePassResponse reject(Long id, GatePassRejectRequest request, String rejectedBy) {
        GatePass pass = requireGatePass(id);
        if (pass.getStatus() != GatePassStatus.PENDING_APPROVAL) {
            throw new IllegalArgumentException("Only a gate pass pending approval can be rejected");
        }
        pass.setStatus(GatePassStatus.REJECTED);
        pass.setRejectedBy(rejectedBy);
        pass.setRejectedAt(Instant.now());
        pass.setRejectionReason(request.reason().trim());
        pass.setUpdatedAt(Instant.now());
        return toResponse(gatePassRepository.save(pass));
    }

    @Transactional
    public GatePassResponse verifyGate(Long id, String verifiedBy) {
        GatePass pass = requireGatePass(id);
        if (pass.getStatus() != GatePassStatus.APPROVED) {
            throw new IllegalArgumentException("Only an approved gate pass can be verified at the gate");
        }
        pass.setGateVerifiedBy(verifiedBy);
        pass.setGateVerifiedAt(Instant.now());
        pass.setStatus(pass.isReturnable() ? GatePassStatus.GATE_VERIFIED : GatePassStatus.CLOSED);
        pass.setUpdatedAt(Instant.now());
        return toResponse(gatePassRepository.save(pass));
    }

    @Transactional
    public GatePassResponse markReturned(Long id, GatePassReturnRequest request, String returnedBy) {
        GatePass pass = requireGatePass(id);
        if (!pass.isReturnable()) {
            throw new IllegalArgumentException("This gate pass is non-returnable");
        }
        if (pass.getStatus() != GatePassStatus.GATE_VERIFIED) {
            throw new IllegalArgumentException("Only a gate-verified pass awaiting return can be marked returned");
        }
        pass.setStatus(GatePassStatus.RETURNED);
        pass.setActualReturnDate(LocalDate.now());
        pass.setReturnedBy(returnedBy);
        pass.setReturnedAt(Instant.now());
        if (request != null && request.notes() != null && !request.notes().isBlank()) {
            pass.setNotes(pass.getNotes() != null ? pass.getNotes() + " | " + request.notes().trim() : request.notes().trim());
        }
        pass.setUpdatedAt(Instant.now());
        return toResponse(gatePassRepository.save(pass));
    }

    private GatePass requireGatePass(Long id) {
        return gatePassRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Gate pass not found with id: " + id));
    }

    private Product requireProduct(Long id) {
        return productRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + id));
    }

    private Asset requireAsset(Long id) {
        return assetRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Asset not found with id: " + id));
    }

    private InventoryLocation requireLocation(Long id) {
        return locationRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Inventory location not found with id: " + id));
    }

    private GatePassDirection parseDirection(String value) {
        try {
            return GatePassDirection.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid direction '" + value + "'");
        }
    }

    private GatePassStatus parseStatus(String value) {
        try {
            return GatePassStatus.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid status '" + value + "'");
        }
    }

    private static String trim(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }

    private GatePassResponse toResponse(GatePass pass) {
        Product product = pass.getProduct();
        Asset asset = pass.getAsset();
        InventoryLocation location = pass.getLocation();
        PurchaseOrder linkedPo = pass.getLinkedPurchaseOrder();
        boolean overdue = pass.isReturnable()
            && pass.getStatus() == GatePassStatus.GATE_VERIFIED
            && pass.getExpectedReturnDate() != null
            && pass.getExpectedReturnDate().isBefore(LocalDate.now());
        return new GatePassResponse(
            pass.getId(), pass.getDirection().name(), pass.isReturnable(),
            product != null ? product.getId() : null, product != null ? product.getProductCode() : null, product != null ? product.getProductName() : null,
            asset != null ? asset.getId() : null, asset != null ? asset.getAssetTag() : null,
            location.getId(), location.getVirtualName(),
            pass.getQuantity(), pass.getReason(), pass.getPartyName(), pass.getPartyContact(),
            linkedPo != null ? linkedPo.getId() : null,
            pass.getPassDate(), pass.getExpectedReturnDate(), pass.getActualReturnDate(), overdue,
            pass.getStatus().name(), pass.getNotes(),
            pass.getCreatedBy(), pass.getCreatedAt(),
            pass.getApprovedBy(), pass.getApprovedAt(),
            pass.getRejectedBy(), pass.getRejectedAt(), pass.getRejectionReason(),
            pass.getGateVerifiedBy(), pass.getGateVerifiedAt(),
            pass.getReturnedBy(), pass.getReturnedAt());
    }
}
