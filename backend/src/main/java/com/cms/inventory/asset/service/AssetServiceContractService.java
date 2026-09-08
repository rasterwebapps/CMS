package com.cms.inventory.asset.service;

import java.time.LocalDate;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cms.exception.ResourceNotFoundException;
import com.cms.inventory.asset.dto.AssetServiceContractRequest;
import com.cms.inventory.asset.dto.AssetServiceContractResponse;
import com.cms.inventory.asset.model.Asset;
import com.cms.inventory.asset.model.AssetServiceContract;
import com.cms.inventory.asset.repository.AssetRepository;
import com.cms.inventory.asset.repository.AssetServiceContractRepository;
import com.cms.inventory.procurement.model.Supplier;
import com.cms.inventory.procurement.repository.SupplierRepository;

/**
 * Owns Asset Service Contracts — Phase 5's second slice, second half. See the {@link
 * AssetServiceContract} class docs for why it links the existing {@code Supplier} master rather
 * than a free-text vendor name, and the "Maintenance & Service Contracts slice" decision-log
 * entry.
 */
@Service
@Transactional(readOnly = true)
public class AssetServiceContractService {

    private final AssetServiceContractRepository contractRepository;
    private final AssetRepository assetRepository;
    private final SupplierRepository supplierRepository;

    public AssetServiceContractService(AssetServiceContractRepository contractRepository,
                                        AssetRepository assetRepository,
                                        SupplierRepository supplierRepository) {
        this.contractRepository = contractRepository;
        this.assetRepository = assetRepository;
        this.supplierRepository = supplierRepository;
    }

    @Transactional
    public AssetServiceContractResponse create(AssetServiceContractRequest request) {
        AssetServiceContract contract = new AssetServiceContract();
        applyRequest(contract, request);
        return toResponse(contractRepository.save(contract));
    }

    public Page<AssetServiceContractResponse> findPage(Long assetId, Boolean activeOnly, Pageable pageable) {
        Specification<AssetServiceContract> spec = (root, query, cb) -> {
            var predicate = cb.conjunction();
            if (assetId != null) predicate = cb.and(predicate, cb.equal(root.get("asset").get("id"), assetId));
            if (Boolean.TRUE.equals(activeOnly)) predicate = cb.and(predicate, cb.isTrue(root.get("isActive")));
            return predicate;
        };
        return contractRepository.findAll(spec, pageable).map(this::toResponse);
    }

    public AssetServiceContractResponse findById(Long id) {
        return toResponse(findOrThrow(id));
    }

    @Transactional
    public AssetServiceContractResponse update(Long id, AssetServiceContractRequest request) {
        AssetServiceContract contract = findOrThrow(id);
        applyRequest(contract, request);
        return toResponse(contractRepository.save(contract));
    }

    private void applyRequest(AssetServiceContract contract, AssetServiceContractRequest request) {
        Asset asset = assetRepository.findById(request.assetId())
            .orElseThrow(() -> new ResourceNotFoundException("Asset not found with id: " + request.assetId()));
        Supplier supplier = supplierRepository.findById(request.supplierId())
            .orElseThrow(() -> new ResourceNotFoundException("Supplier not found with id: " + request.supplierId()));
        if (request.endDate() != null && request.endDate().isBefore(request.startDate())) {
            throw new IllegalArgumentException("End date cannot be before the start date");
        }

        contract.setAsset(asset);
        contract.setSupplier(supplier);
        contract.setContractNumber(trim(request.contractNumber()));
        contract.setStartDate(request.startDate());
        contract.setEndDate(request.endDate());
        contract.setRenewalReminderDate(request.renewalReminderDate());
        contract.setCoverageDetails(trim(request.coverageDetails()));
        if (request.isActive() != null) contract.setIsActive(request.isActive());
    }

    private AssetServiceContract findOrThrow(Long id) {
        return contractRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Asset service contract not found with id: " + id));
    }

    private static String trim(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }

    private AssetServiceContractResponse toResponse(AssetServiceContract contract) {
        Asset asset = contract.getAsset();
        Supplier supplier = contract.getSupplier();
        boolean expired = contract.getEndDate() != null && contract.getEndDate().isBefore(LocalDate.now());
        return new AssetServiceContractResponse(
            contract.getId(), asset.getId(), asset.getAssetTag(),
            supplier.getId(), supplier.getSupplierName(),
            contract.getContractNumber(), contract.getStartDate(), contract.getEndDate(), contract.getRenewalReminderDate(),
            expired, contract.getCoverageDetails(), contract.getIsActive(),
            contract.getCreatedAt(), contract.getUpdatedAt());
    }
}
