package com.cms.inventory.consignment.service;

import java.math.BigDecimal;
import java.time.Instant;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cms.exception.ResourceNotFoundException;
import com.cms.inventory.catalog.model.Product;
import com.cms.inventory.catalog.repository.ProductRepository;
import com.cms.inventory.consignment.dto.ConsignmentStockConsumeRequest;
import com.cms.inventory.consignment.dto.ConsignmentStockLineResponse;
import com.cms.inventory.consignment.dto.ConsignmentStockReceiveRequest;
import com.cms.inventory.consignment.model.ConsignmentAgreement;
import com.cms.inventory.consignment.model.ConsignmentStockLine;
import com.cms.inventory.consignment.repository.ConsignmentStockLineRepository;
import com.cms.inventory.procurement.model.Supplier;
import com.cms.inventory.stock.dto.StockMovementRequest;
import com.cms.inventory.stock.model.InventoryLocation;
import com.cms.inventory.stock.service.StockMovementService;

/**
 * Owns Consignment Stock Lines — the vendor-ownership side-ledger described on {@link
 * ConsignmentStockLine}. Receiving stock posts a real {@code RECEIPT} to the main stock ledger
 * (via {@link StockMovementService#recordMovement}) so it's immediately usable, while separately
 * tracking that it isn't owned yet; recording consumption is a standalone financial
 * reconciliation action that does not touch the main ledger again. See the "Consignment stock
 * slice" decision-log entry and the class docs on {@link ConsignmentStockLine} for the full
 * reasoning.
 */
@Service
@Transactional(readOnly = true)
public class ConsignmentStockLineService {

    private final ConsignmentStockLineRepository lineRepository;
    private final ConsignmentAgreementService agreementService;
    private final ProductRepository productRepository;
    private final StockMovementService stockMovementService;

    public ConsignmentStockLineService(ConsignmentStockLineRepository lineRepository,
                                        ConsignmentAgreementService agreementService,
                                        ProductRepository productRepository,
                                        StockMovementService stockMovementService) {
        this.lineRepository = lineRepository;
        this.agreementService = agreementService;
        this.productRepository = productRepository;
        this.stockMovementService = stockMovementService;
    }

    @Transactional
    public ConsignmentStockLineResponse receive(ConsignmentStockReceiveRequest request, String performedBy) {
        ConsignmentAgreement agreement = agreementService.findOrThrow(request.agreementId());
        if (!Boolean.TRUE.equals(agreement.getIsActive())) {
            throw new IllegalArgumentException("Stock cannot be received against an inactive consignment agreement");
        }
        Product product = productRepository.findById(request.productId())
            .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + request.productId()));

        ConsignmentStockLine line = lineRepository.findByAgreementIdAndProductId(agreement.getId(), product.getId())
            .orElseGet(() -> {
                ConsignmentStockLine l = new ConsignmentStockLine();
                l.setAgreement(agreement);
                l.setProduct(product);
                return l;
            });
        line.setConsignmentPrice(request.consignmentPrice());
        line.setReceivedQty(line.getReceivedQty().add(request.quantity()));
        line.setLastReceivedBy(performedBy);
        line.setLastReceivedAt(Instant.now());
        line = lineRepository.save(line);

        // Physically the stock is now on the shelf and usable — post it to the main ledger, even
        // though it isn't owned yet. See the class docs for why this doesn't also happen again
        // when consumption is recorded.
        stockMovementService.recordMovement(new StockMovementRequest(
            product.getId(), agreement.getLocation().getId(), null, null,
            "RECEIPT", null, request.quantity(), request.consignmentPrice(),
            "Consignment receipt — agreement " + agreement.getAgreementNumber() + (request.notes() != null ? " — " + request.notes() : "")
        ), performedBy);

        return toResponse(line);
    }

    public Page<ConsignmentStockLineResponse> findPage(Long agreementId, Long productId, Pageable pageable) {
        Specification<ConsignmentStockLine> spec = (root, query, cb) -> {
            var predicate = cb.conjunction();
            if (agreementId != null) predicate = cb.and(predicate, cb.equal(root.get("agreement").get("id"), agreementId));
            if (productId != null) predicate = cb.and(predicate, cb.equal(root.get("product").get("id"), productId));
            return predicate;
        };
        return lineRepository.findAll(spec, pageable).map(this::toResponse);
    }

    public ConsignmentStockLineResponse findById(Long id) {
        return toResponse(findOrThrow(id));
    }

    @Transactional
    public ConsignmentStockLineResponse consume(Long id, ConsignmentStockConsumeRequest request, String performedBy) {
        ConsignmentStockLine line = findOrThrow(id);
        BigDecimal available = line.getReceivedQty().subtract(line.getConsumedQty());
        if (request.quantity().compareTo(available) > 0) {
            throw new IllegalArgumentException(
                "Cannot record consumption of " + request.quantity() + " — only " + available + " is still on hand and unconsumed for this line");
        }
        line.setConsumedQty(line.getConsumedQty().add(request.quantity()));
        line.setLastConsumedBy(performedBy);
        line.setLastConsumedAt(Instant.now());
        return toResponse(lineRepository.save(line));
    }

    private ConsignmentStockLine findOrThrow(Long id) {
        return lineRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Consignment stock line not found with id: " + id));
    }

    private ConsignmentStockLineResponse toResponse(ConsignmentStockLine line) {
        ConsignmentAgreement agreement = line.getAgreement();
        Supplier supplier = agreement.getSupplier();
        InventoryLocation location = agreement.getLocation();
        Product product = line.getProduct();
        BigDecimal qtyOnHand = line.getReceivedQty().subtract(line.getConsumedQty());
        return new ConsignmentStockLineResponse(
            line.getId(), agreement.getId(), agreement.getAgreementNumber(),
            supplier.getId(), supplier.getSupplierName(), location.getId(), location.getVirtualName(),
            product.getId(), product.getProductCode(), product.getProductName(),
            line.getConsignmentPrice(), line.getReceivedQty(), line.getConsumedQty(), qtyOnHand,
            line.getLastReceivedBy(), line.getLastReceivedAt(), line.getLastConsumedBy(), line.getLastConsumedAt(),
            line.getCreatedAt(), line.getUpdatedAt());
    }
}
