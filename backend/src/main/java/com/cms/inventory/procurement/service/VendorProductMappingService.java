package com.cms.inventory.procurement.service;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cms.dto.ActiveStatusUpdateRequest;
import com.cms.dto.ActiveStatusUpdateResponse;
import com.cms.exception.ResourceNotFoundException;
import com.cms.inventory.catalog.model.Product;
import com.cms.inventory.catalog.model.Uom;
import com.cms.inventory.catalog.repository.ProductRepository;
import com.cms.inventory.catalog.repository.UomRepository;
import com.cms.inventory.procurement.dto.VendorProductMappingRequest;
import com.cms.inventory.procurement.dto.VendorProductMappingResponse;
import com.cms.inventory.procurement.model.RateContract;
import com.cms.inventory.procurement.model.RateContractLine;
import com.cms.inventory.procurement.model.Supplier;
import com.cms.inventory.procurement.model.VendorProductMapping;
import com.cms.inventory.procurement.repository.RateContractRepository;
import com.cms.inventory.procurement.repository.SupplierRepository;
import com.cms.inventory.procurement.repository.VendorProductMappingRepository;

/**
 * See the "VendorProductMapping slice" decision-log entry: at most one active mapping per
 * (supplier, product) pair, and the effective price shown for a mapping falls back to its own
 * {@code unitPrice} unless its linked {@code RateContract} is currently active and within its
 * start/end date window and carries a {@code RateContractLine} for the same product — resolved
 * here at read time rather than stored, so a contract lapsing or a line changing is reflected
 * immediately without touching the mapping row.
 */
@Service
@Transactional(readOnly = true)
public class VendorProductMappingService {

    private final VendorProductMappingRepository mappingRepository;
    private final SupplierRepository supplierRepository;
    private final ProductRepository productRepository;
    private final UomRepository uomRepository;
    private final RateContractRepository rateContractRepository;

    public VendorProductMappingService(VendorProductMappingRepository mappingRepository,
                                        SupplierRepository supplierRepository,
                                        ProductRepository productRepository,
                                        UomRepository uomRepository,
                                        RateContractRepository rateContractRepository) {
        this.mappingRepository = mappingRepository;
        this.supplierRepository = supplierRepository;
        this.productRepository = productRepository;
        this.uomRepository = uomRepository;
        this.rateContractRepository = rateContractRepository;
    }

    @Transactional
    public VendorProductMappingResponse create(VendorProductMappingRequest request) {
        VendorProductMapping mapping = new VendorProductMapping();
        applyRequest(mapping, request, null);
        return toResponse(mappingRepository.save(mapping));
    }

    public Page<VendorProductMappingResponse> findPage(Long supplierId, Long productId, Boolean activeOnly, Pageable pageable) {
        Specification<VendorProductMapping> spec = (root, query, cb) -> {
            var predicate = cb.conjunction();
            if (supplierId != null) predicate = cb.and(predicate, cb.equal(root.get("supplier").get("id"), supplierId));
            if (productId != null) predicate = cb.and(predicate, cb.equal(root.get("product").get("id"), productId));
            if (Boolean.TRUE.equals(activeOnly)) predicate = cb.and(predicate, cb.isTrue(root.get("isActive")));
            return predicate;
        };
        return mappingRepository.findAll(spec, pageable).map(this::toResponse);
    }

    public VendorProductMappingResponse findById(Long id) {
        return toResponse(findOrThrow(id));
    }

    @Transactional
    public VendorProductMappingResponse update(Long id, VendorProductMappingRequest request) {
        VendorProductMapping mapping = findOrThrow(id);
        applyRequest(mapping, request, id);
        return toResponse(mappingRepository.save(mapping));
    }

    @Transactional
    public void delete(Long id) {
        if (!mappingRepository.existsById(id)) {
            throw new ResourceNotFoundException("Vendor product mapping not found with id: " + id);
        }
        mappingRepository.deleteById(id);
    }

    @Transactional
    public ActiveStatusUpdateResponse updateStatus(Long id, ActiveStatusUpdateRequest request) {
        VendorProductMapping mapping = findOrThrow(id);
        boolean nextActive = Boolean.TRUE.equals(request.isActive());
        if (nextActive && pairExists(mapping.getSupplier().getId(), mapping.getProduct().getId(), id)) {
            throw new IllegalArgumentException(
                "An active vendor rate for '" + mapping.getProduct().getProductName() + "' from '"
                    + mapping.getSupplier().getSupplierName() + "' already exists");
        }
        mapping.setIsActive(nextActive);
        VendorProductMapping saved = mappingRepository.save(mapping);
        return new ActiveStatusUpdateResponse(saved.getId(), saved.getIsActive(), saved.getUpdatedAt());
    }

    public boolean pairExists(Long supplierId, Long productId, Long excludeId) {
        if (supplierId == null || productId == null) return false;
        return excludeId != null
            ? mappingRepository.existsBySupplierIdAndProductIdAndIsActiveTrueAndIdNot(supplierId, productId, excludeId)
            : mappingRepository.existsBySupplierIdAndProductIdAndIsActiveTrue(supplierId, productId);
    }

    private void applyRequest(VendorProductMapping mapping, VendorProductMappingRequest request, Long excludeId) {
        Supplier supplier = supplierRepository.findById(request.supplierId())
            .orElseThrow(() -> new ResourceNotFoundException("Supplier not found with id: " + request.supplierId()));
        Product product = productRepository.findById(request.productId())
            .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + request.productId()));

        if (pairExists(supplier.getId(), product.getId(), excludeId)) {
            throw new IllegalArgumentException(
                "An active vendor rate for '" + product.getProductName() + "' from '" + supplier.getSupplierName() + "' already exists");
        }

        RateContract rateContract = null;
        if (request.rateContractId() != null) {
            rateContract = rateContractRepository.findById(request.rateContractId())
                .orElseThrow(() -> new ResourceNotFoundException("Rate contract not found with id: " + request.rateContractId()));
            if (!rateContract.getSupplier().getId().equals(supplier.getId())) {
                throw new IllegalArgumentException("The selected rate contract does not belong to this supplier");
            }
        }

        Uom uom = null;
        if (request.uomId() != null) {
            uom = uomRepository.findById(request.uomId())
                .orElseThrow(() -> new ResourceNotFoundException("UOM not found with id: " + request.uomId()));
        }

        mapping.setSupplier(supplier);
        mapping.setProduct(product);
        mapping.setRateContract(rateContract);
        mapping.setUnitPrice(request.unitPrice());
        mapping.setCurrencyCode(request.currencyCode() != null && !request.currencyCode().isBlank()
            ? request.currencyCode().toUpperCase() : "INR");
        mapping.setUom(uom);
        mapping.setMinOrderQty(request.minOrderQty());
        mapping.setLeadTimeDays(request.leadTimeDays());
        if (request.isPreferred() != null) mapping.setIsPreferred(request.isPreferred());
        if (request.isActive() != null) mapping.setIsActive(request.isActive());
    }

    private VendorProductMapping findOrThrow(Long id) {
        return mappingRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Vendor product mapping not found with id: " + id));
    }

    private VendorProductMappingResponse toResponse(VendorProductMapping m) {
        Supplier supplier = m.getSupplier();
        Product product = m.getProduct();
        Uom uom = m.getUom();
        RateContract contract = m.getRateContract();

        BigDecimal effectivePrice = m.getUnitPrice();
        String priceSource = "STANDARD";
        if (contract != null && isWithinActiveWindow(contract)) {
            for (RateContractLine line : contract.getLines()) {
                if (line.getProduct().getId().equals(product.getId())) {
                    effectivePrice = line.getNegotiatedRate();
                    priceSource = "CONTRACT";
                    break;
                }
            }
        }

        return new VendorProductMappingResponse(
            m.getId(), supplier.getId(), supplier.getSupplierName(),
            product.getId(), product.getProductCode(), product.getProductName(),
            contract != null ? contract.getId() : null,
            m.getUnitPrice(), m.getCurrencyCode(),
            uom != null ? uom.getId() : null, uom != null ? uom.getCode() : null,
            m.getMinOrderQty(), m.getLeadTimeDays(), m.getIsPreferred(), m.getIsActive(),
            effectivePrice, priceSource, m.getCreatedAt(), m.getUpdatedAt());
    }

    private static boolean isWithinActiveWindow(RateContract contract) {
        if (!Boolean.TRUE.equals(contract.getIsActive())) return false;
        LocalDate today = LocalDate.now();
        if (contract.getStartDate() != null && today.isBefore(contract.getStartDate())) return false;
        return contract.getEndDate() == null || !today.isAfter(contract.getEndDate());
    }
}
