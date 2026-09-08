package com.cms.inventory.consignment.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cms.exception.ResourceNotFoundException;
import com.cms.inventory.consignment.dto.ConsignmentAgreementRequest;
import com.cms.inventory.consignment.dto.ConsignmentAgreementResponse;
import com.cms.inventory.consignment.model.ConsignmentAgreement;
import com.cms.inventory.consignment.repository.ConsignmentAgreementRepository;
import com.cms.inventory.procurement.model.Supplier;
import com.cms.inventory.procurement.repository.SupplierRepository;
import com.cms.inventory.stock.model.InventoryLocation;
import com.cms.inventory.stock.repository.InventoryLocationRepository;

/**
 * Owns Consignment Agreements — Phase 7's ("Gate Pass, Vendor-Owned Stock & Service Requests")
 * second slice's header entity. A simple master (create/list/get/update, no delete), same shape
 * as {@code BudgetService}. Individual product balances live in {@link
 * com.cms.inventory.consignment.model.ConsignmentStockLine}, owned by {@link
 * ConsignmentStockLineService}. See the "Consignment stock slice" decision-log entry.
 */
@Service
@Transactional(readOnly = true)
public class ConsignmentAgreementService {

    private final ConsignmentAgreementRepository agreementRepository;
    private final SupplierRepository supplierRepository;
    private final InventoryLocationRepository locationRepository;

    public ConsignmentAgreementService(ConsignmentAgreementRepository agreementRepository,
                                        SupplierRepository supplierRepository,
                                        InventoryLocationRepository locationRepository) {
        this.agreementRepository = agreementRepository;
        this.supplierRepository = supplierRepository;
        this.locationRepository = locationRepository;
    }

    @Transactional
    public ConsignmentAgreementResponse create(ConsignmentAgreementRequest request) {
        ConsignmentAgreement agreement = new ConsignmentAgreement();
        applyRequest(agreement, request);
        return toResponse(agreementRepository.save(agreement));
    }

    public Page<ConsignmentAgreementResponse> findPage(Long supplierId, Long locationId, Boolean activeOnly, Pageable pageable) {
        Specification<ConsignmentAgreement> spec = (root, query, cb) -> {
            var predicate = cb.conjunction();
            if (supplierId != null) predicate = cb.and(predicate, cb.equal(root.get("supplier").get("id"), supplierId));
            if (locationId != null) predicate = cb.and(predicate, cb.equal(root.get("location").get("id"), locationId));
            if (Boolean.TRUE.equals(activeOnly)) predicate = cb.and(predicate, cb.isTrue(root.get("isActive")));
            return predicate;
        };
        return agreementRepository.findAll(spec, pageable).map(this::toResponse);
    }

    public ConsignmentAgreementResponse findById(Long id) {
        return toResponse(findOrThrow(id));
    }

    @Transactional
    public ConsignmentAgreementResponse update(Long id, ConsignmentAgreementRequest request) {
        ConsignmentAgreement agreement = findOrThrow(id);
        applyRequest(agreement, request);
        return toResponse(agreementRepository.save(agreement));
    }

    private void applyRequest(ConsignmentAgreement agreement, ConsignmentAgreementRequest request) {
        if (request.endDate() != null && request.endDate().isBefore(request.startDate())) {
            throw new IllegalArgumentException("End date cannot be before the start date");
        }
        Supplier supplier = supplierRepository.findById(request.supplierId())
            .orElseThrow(() -> new ResourceNotFoundException("Supplier not found with id: " + request.supplierId()));
        InventoryLocation location = locationRepository.findById(request.locationId())
            .orElseThrow(() -> new ResourceNotFoundException("Inventory location not found with id: " + request.locationId()));

        agreement.setSupplier(supplier);
        agreement.setLocation(location);
        agreement.setAgreementNumber(request.agreementNumber().trim());
        agreement.setStartDate(request.startDate());
        agreement.setEndDate(request.endDate());
        agreement.setBillingCycleDays(request.billingCycleDays());
        agreement.setNotes(trim(request.notes()));
        if (request.isActive() != null) agreement.setIsActive(request.isActive());
    }

    ConsignmentAgreement findOrThrow(Long id) {
        return agreementRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Consignment agreement not found with id: " + id));
    }

    private static String trim(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }

    private ConsignmentAgreementResponse toResponse(ConsignmentAgreement agreement) {
        Supplier supplier = agreement.getSupplier();
        InventoryLocation location = agreement.getLocation();
        return new ConsignmentAgreementResponse(
            agreement.getId(), supplier.getId(), supplier.getSupplierName(),
            location.getId(), location.getVirtualName(),
            agreement.getAgreementNumber(), agreement.getStartDate(), agreement.getEndDate(),
            agreement.getBillingCycleDays(), agreement.getNotes(), agreement.getIsActive(),
            agreement.getCreatedAt(), agreement.getUpdatedAt());
    }
}
