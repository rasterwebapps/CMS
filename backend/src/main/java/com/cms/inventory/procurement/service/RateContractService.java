package com.cms.inventory.procurement.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cms.dto.ActiveStatusUpdateRequest;
import com.cms.dto.ActiveStatusUpdateResponse;
import com.cms.exception.ResourceNotFoundException;
import com.cms.inventory.procurement.dto.RateContractRequest;
import com.cms.inventory.procurement.dto.RateContractResponse;
import com.cms.inventory.procurement.model.RateContract;
import com.cms.inventory.procurement.model.Supplier;
import com.cms.inventory.procurement.repository.RateContractRepository;
import com.cms.inventory.procurement.repository.SupplierRepository;

@Service
@Transactional(readOnly = true)
public class RateContractService {

    private final RateContractRepository rateContractRepository;
    private final SupplierRepository supplierRepository;

    public RateContractService(RateContractRepository rateContractRepository, SupplierRepository supplierRepository) {
        this.rateContractRepository = rateContractRepository;
        this.supplierRepository = supplierRepository;
    }

    @Transactional
    public RateContractResponse create(RateContractRequest request) {
        Supplier supplier = requireSupplier(request.supplierId());
        validateDates(request);

        RateContract contract = new RateContract();
        contract.setSupplier(supplier);
        applyFields(contract, request);
        return toResponse(rateContractRepository.save(contract));
    }

    public Page<RateContractResponse> findPage(Long supplierId, Boolean activeOnly, Pageable pageable) {
        Specification<RateContract> spec = (root, query, cb) -> {
            var predicate = cb.conjunction();
            if (supplierId != null) predicate = cb.and(predicate, cb.equal(root.get("supplier").get("id"), supplierId));
            if (Boolean.TRUE.equals(activeOnly)) predicate = cb.and(predicate, cb.isTrue(root.get("isActive")));
            return predicate;
        };
        return rateContractRepository.findAll(spec, pageable).map(this::toResponse);
    }

    public RateContractResponse findById(Long id) {
        return toResponse(findOrThrow(id));
    }

    @Transactional
    public RateContractResponse update(Long id, RateContractRequest request) {
        RateContract contract = findOrThrow(id);
        Supplier supplier = requireSupplier(request.supplierId());
        validateDates(request);

        contract.setSupplier(supplier);
        applyFields(contract, request);
        return toResponse(rateContractRepository.save(contract));
    }

    @Transactional
    public void delete(Long id) {
        if (!rateContractRepository.existsById(id)) {
            throw new ResourceNotFoundException("Rate contract not found with id: " + id);
        }
        rateContractRepository.deleteById(id);
    }

    @Transactional
    public ActiveStatusUpdateResponse updateStatus(Long id, ActiveStatusUpdateRequest request) {
        RateContract contract = findOrThrow(id);
        contract.setIsActive(Boolean.TRUE.equals(request.isActive()));
        RateContract saved = rateContractRepository.save(contract);
        return new ActiveStatusUpdateResponse(saved.getId(), saved.getIsActive(), saved.getUpdatedAt());
    }

    private void validateDates(RateContractRequest request) {
        if (request.endDate() != null && request.endDate().isBefore(request.startDate())) {
            throw new IllegalArgumentException("End date cannot be before the start date");
        }
    }

    private Supplier requireSupplier(Long supplierId) {
        return supplierRepository.findById(supplierId)
            .orElseThrow(() -> new ResourceNotFoundException("Supplier not found with id: " + supplierId));
    }

    private RateContract findOrThrow(Long id) {
        return rateContractRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Rate contract not found with id: " + id));
    }

    private void applyFields(RateContract contract, RateContractRequest request) {
        contract.setStartDate(request.startDate());
        contract.setEndDate(request.endDate());
        contract.setContractValueCap(request.contractValueCap());
        contract.setTermsText(trim(request.termsText()));
        contract.setRenewalReminderDate(request.renewalReminderDate());
        if (request.isActive() != null) contract.setIsActive(request.isActive());
    }

    private RateContractResponse toResponse(RateContract c) {
        Supplier supplier = c.getSupplier();
        return new RateContractResponse(
            c.getId(), supplier.getId(), supplier.getSupplierName(),
            c.getStartDate(), c.getEndDate(), c.getContractValueCap(), c.getTermsText(),
            c.getRenewalReminderDate(), c.getIsActive(), c.getCreatedAt(), c.getUpdatedAt());
    }

    private static String trim(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }
}
