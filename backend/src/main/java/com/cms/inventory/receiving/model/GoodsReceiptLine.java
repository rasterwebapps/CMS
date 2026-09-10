package com.cms.inventory.receiving.model;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.cms.inventory.catalog.model.ProductUomLevel;
import com.cms.inventory.procurement.model.PurchaseOrderItem;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * One line of a {@link GoodsReceipt} — always against a specific {@link PurchaseOrderItem}
 * (no direct-receipt-with-no-PO in this slice). {@code batchOrSerialNo}/{@code expiryDate} reuse
 * the same optional fields the Phase 1 "Record Stock Movement" form already exposes — no second
 * batch-entry UI invented, per the Stock Tracking slice's established "batch fields live on the
 * movement, not a dedicated screen" precedent. {@code unitCost} defaults to the PO line's own
 * {@code unitPrice} if not overridden (a delivery sometimes prices slightly differently than
 * ordered). See the "Goods Receipt slice" decision-log entry.
 */
@Entity
@Table(name = "goods_receipt_lines")
public class GoodsReceiptLine {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "goods_receipt_id", nullable = false)
    private GoodsReceipt goodsReceipt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "purchase_order_item_id", nullable = false)
    private PurchaseOrderItem purchaseOrderItem;

    /** Always the base-unit quantity posted to the stock ledger — lets one PO line be received
     * across several lines/receipts in different units (e.g. 3 Cartons on one line, 2 loose
     * Boxes on another) while {@code PurchaseOrderItem.receivedQty}'s running total, and every
     * open-qty comparison against it, stays correct without any unit-aware logic of its own. */
    @Column(name = "received_qty", nullable = false, precision = 14, scale = 3)
    private BigDecimal receivedQty;

    /** The unit-of-measure level this line was actually received in (e.g. "Carton"), from the
     * product's active chain at receipt time — null when received directly in the base unit.
     * Display/audit only. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "uom_level_id")
    private ProductUomLevel uomLevel;

    /** The raw quantity as typed in {@code uomLevel} — {@code receivedQty} is this multiplied by
     * the level's {@code factorToBase}. Null when {@code uomLevel} is null. */
    @Column(name = "entered_qty", precision = 14, scale = 3)
    private BigDecimal enteredQty;

    @Column(name = "unit_cost", precision = 14, scale = 2)
    private BigDecimal unitCost;

    @Column(name = "batch_or_serial_no", length = 100)
    private String batchOrSerialNo;

    @Column(name = "expiry_date")
    private LocalDate expiryDate;

    @Column(length = 500)
    private String notes;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public GoodsReceipt getGoodsReceipt() { return goodsReceipt; }
    public void setGoodsReceipt(GoodsReceipt goodsReceipt) { this.goodsReceipt = goodsReceipt; }

    public PurchaseOrderItem getPurchaseOrderItem() { return purchaseOrderItem; }
    public void setPurchaseOrderItem(PurchaseOrderItem purchaseOrderItem) { this.purchaseOrderItem = purchaseOrderItem; }

    public BigDecimal getReceivedQty() { return receivedQty; }
    public void setReceivedQty(BigDecimal receivedQty) { this.receivedQty = receivedQty; }

    public ProductUomLevel getUomLevel() { return uomLevel; }
    public void setUomLevel(ProductUomLevel uomLevel) { this.uomLevel = uomLevel; }

    public BigDecimal getEnteredQty() { return enteredQty; }
    public void setEnteredQty(BigDecimal enteredQty) { this.enteredQty = enteredQty; }

    public BigDecimal getUnitCost() { return unitCost; }
    public void setUnitCost(BigDecimal unitCost) { this.unitCost = unitCost; }

    public String getBatchOrSerialNo() { return batchOrSerialNo; }
    public void setBatchOrSerialNo(String batchOrSerialNo) { this.batchOrSerialNo = batchOrSerialNo; }

    public LocalDate getExpiryDate() { return expiryDate; }
    public void setExpiryDate(LocalDate expiryDate) { this.expiryDate = expiryDate; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
}
