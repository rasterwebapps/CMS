package com.cms.inventory.procurement.model;

import java.math.BigDecimal;
import java.time.Instant;

import com.cms.inventory.catalog.model.Product;
import com.cms.inventory.procurement.model.enums.QuotationRequestLineStatus;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * One product's row on a {@link QuotationRequest}, picked up from one {@code APPROVED} {@link
 * PurchaseRequisitionItem} (required, not optional — a Quotation Request only ever prices out
 * requisitioned demand, same "collective conversion" shape {@code PurchaseOrderService.addLine}
 * already uses). Like {@code PurchaseRequisitionItem}, carries no variant — a variant is chosen
 * fresh only once a {@code PurchaseOrderItem} is actually created during award conversion.
 * {@code awardedResponseLine} is null until {@link #status} reaches {@code AWARDED}; its
 * supplier and price are what {@code QuotationRequestService.convertAwardedLines} uses to build
 * the eventual {@code PurchaseOrderItem}.
 */
@Entity
@Table(name = "quotation_request_lines")
public class QuotationRequestLine {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "quotation_request_id", nullable = false)
    private QuotationRequest quotationRequest;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "purchase_requisition_item_id", nullable = false)
    private PurchaseRequisitionItem purchaseRequisitionItem;

    @Column(name = "requested_qty", nullable = false, precision = 14, scale = 3)
    private BigDecimal requestedQty;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private QuotationRequestLineStatus status = QuotationRequestLineStatus.PENDING;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "awarded_response_line_id")
    private QuotationResponseLine awardedResponseLine;

    @Column(name = "awarded_by", length = 255)
    private String awardedBy;

    @Column(name = "awarded_at")
    private Instant awardedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public QuotationRequest getQuotationRequest() { return quotationRequest; }
    public void setQuotationRequest(QuotationRequest quotationRequest) { this.quotationRequest = quotationRequest; }

    public Product getProduct() { return product; }
    public void setProduct(Product product) { this.product = product; }

    public PurchaseRequisitionItem getPurchaseRequisitionItem() { return purchaseRequisitionItem; }
    public void setPurchaseRequisitionItem(PurchaseRequisitionItem purchaseRequisitionItem) { this.purchaseRequisitionItem = purchaseRequisitionItem; }

    public BigDecimal getRequestedQty() { return requestedQty; }
    public void setRequestedQty(BigDecimal requestedQty) { this.requestedQty = requestedQty; }

    public QuotationRequestLineStatus getStatus() { return status; }
    public void setStatus(QuotationRequestLineStatus status) { this.status = status; }

    public QuotationResponseLine getAwardedResponseLine() { return awardedResponseLine; }
    public void setAwardedResponseLine(QuotationResponseLine awardedResponseLine) { this.awardedResponseLine = awardedResponseLine; }

    public String getAwardedBy() { return awardedBy; }
    public void setAwardedBy(String awardedBy) { this.awardedBy = awardedBy; }

    public Instant getAwardedAt() { return awardedAt; }
    public void setAwardedAt(Instant awardedAt) { this.awardedAt = awardedAt; }
}
