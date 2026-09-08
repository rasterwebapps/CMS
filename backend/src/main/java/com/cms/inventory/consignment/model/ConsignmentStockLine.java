package com.cms.inventory.consignment.model;

import java.math.BigDecimal;
import java.time.Instant;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import com.cms.inventory.catalog.model.Product;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * One running balance per ({@link ConsignmentAgreement}, {@link Product}) — the vendor-ownership
 * side-ledger, per the reference architecture (called {@code ConsignmentStockLedger} in
 * {@code ER_DIAGRAM_AND_MODULE_BOUNDARIES.md} §6). Deliberately stores only the two raw facts,
 * {@code receivedQty} and {@code consumedQty} — the "on-hand, still vendor-owned" balance is
 * derived as {@code receivedQty - consumedQty} at read time in the service, matching this
 * module's own "computed live, never stored" discipline already used for overdue flags,
 * depreciation, and budget consumption, rather than the ER doc's original flat {@code QtyOnHand}
 * field. Receiving stock (increasing {@code receivedQty}) also posts a real {@code RECEIPT} to
 * the main {@code StockLedger} via {@code StockMovementService} — physically the stock is on the
 * shelf and usable from day one, it just isn't owned yet. Recording consumption (increasing
 * {@code consumedQty}, i.e. "ownership transferred, this portion should now be billed by the
 * supplier") is a separate, manual, purely financial reconciliation action — it does **not**
 * post a second stock movement, since the physical decrease already happened independently
 * through whatever normal flow actually used the stock (Stock Issue Request, etc.); correlating
 * exactly which physical units came from consignment vs. owned stock would require real lot-level
 * ownership costing, which is explicitly out of scope (same deferral as FIFO/FEFO valuation). See
 * the "Consignment stock slice" decision-log entry.
 */
@Entity
@Table(name = "consignment_stock_lines")
@EntityListeners(AuditingEntityListener.class)
public class ConsignmentStockLine {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "agreement_id", nullable = false)
    private ConsignmentAgreement agreement;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(name = "consignment_price", nullable = false, precision = 14, scale = 2)
    private BigDecimal consignmentPrice;

    @Column(name = "received_qty", nullable = false, precision = 14, scale = 3)
    private BigDecimal receivedQty = BigDecimal.ZERO;

    @Column(name = "consumed_qty", nullable = false, precision = 14, scale = 3)
    private BigDecimal consumedQty = BigDecimal.ZERO;

    @Column(name = "last_received_by", length = 255)
    private String lastReceivedBy;

    @Column(name = "last_received_at")
    private Instant lastReceivedAt;

    @Column(name = "last_consumed_by", length = 255)
    private String lastConsumedBy;

    @Column(name = "last_consumed_at")
    private Instant lastConsumedAt;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public ConsignmentAgreement getAgreement() { return agreement; }
    public void setAgreement(ConsignmentAgreement agreement) { this.agreement = agreement; }

    public Product getProduct() { return product; }
    public void setProduct(Product product) { this.product = product; }

    public BigDecimal getConsignmentPrice() { return consignmentPrice; }
    public void setConsignmentPrice(BigDecimal consignmentPrice) { this.consignmentPrice = consignmentPrice; }

    public BigDecimal getReceivedQty() { return receivedQty; }
    public void setReceivedQty(BigDecimal receivedQty) { this.receivedQty = receivedQty; }

    public BigDecimal getConsumedQty() { return consumedQty; }
    public void setConsumedQty(BigDecimal consumedQty) { this.consumedQty = consumedQty; }

    public String getLastReceivedBy() { return lastReceivedBy; }
    public void setLastReceivedBy(String lastReceivedBy) { this.lastReceivedBy = lastReceivedBy; }

    public Instant getLastReceivedAt() { return lastReceivedAt; }
    public void setLastReceivedAt(Instant lastReceivedAt) { this.lastReceivedAt = lastReceivedAt; }

    public String getLastConsumedBy() { return lastConsumedBy; }
    public void setLastConsumedBy(String lastConsumedBy) { this.lastConsumedBy = lastConsumedBy; }

    public Instant getLastConsumedAt() { return lastConsumedAt; }
    public void setLastConsumedAt(Instant lastConsumedAt) { this.lastConsumedAt = lastConsumedAt; }

    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
