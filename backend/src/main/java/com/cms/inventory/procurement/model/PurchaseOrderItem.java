package com.cms.inventory.procurement.model;

import java.math.BigDecimal;

import com.cms.inventory.catalog.model.Product;
import com.cms.inventory.catalog.model.ProductUomLevel;
import com.cms.inventory.procurement.model.enums.JurisdictionMode;

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
 * One product's line on a {@link PurchaseOrder}. {@code purchaseRequisitionItem} is the {@code
 * APPROVED} requisition line this was picked up from (forward reference, same direction as {@code
 * WantedListItem.convertedPurchaseRequisitionItem}) — kept for traceability, not enforced
 * non-null, since a future slice may allow a direct PO line with no requisition behind it.
 * {@code receivedQty} stays 0 until Phase 3's Goods Receipt slice starts posting against it; it
 * exists now so this entity doesn't need an ALTER once that slice lands. {@code taxAmount}/{@code
 * lineTotal} are computed and stored at line-creation time (not recomputed live), same snapshot
 * spirit as {@code CycleCountLine.systemQtySnapshot} — a line stays self-explanatory even if the
 * referenced {@code TaxRule}'s rate changes later. {@code jurisdictionMode} is the mode resolved
 * at line-creation time (supplier state vs. the institution's home state) that decided which
 * {@code TaxSubType} components applied — null whenever no tax was selected for the line. The
 * per-component breakdown itself lives in {@code PurchaseOrderItemTaxComponent}, snapshotted the
 * same way rather than mapped as a collection here, keeping this entity's fetch graph simple; see
 * the "GAP-02 pickup" decision-log entry. See also the "Purchase Order slice" decision-log entry.
 */
@Entity
@Table(name = "purchase_order_items")
public class PurchaseOrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "purchase_order_id", nullable = false)
    private PurchaseOrder purchaseOrder;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "purchase_requisition_item_id")
    private PurchaseRequisitionItem purchaseRequisitionItem;

    /** Always the base-unit quantity — every open-qty/received-progress comparison in {@code
     * PurchaseOrderService}/{@code GoodsReceiptService} relies on that being true regardless of
     * which unit was actually chosen at entry. */
    @Column(name = "ordered_qty", nullable = false, precision = 14, scale = 3)
    private BigDecimal orderedQty;

    /** The unit-of-measure level chosen when this line was entered (e.g. "Box"), from the
     * product's active {@code ProductUomChainVersion} — null when entered directly in the base
     * unit (including every line created before this slice). Display/audit only; never used in
     * quantity math, which always operates on {@code orderedQty}/{@code receivedQty} in base
     * units. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "uom_level_id")
    private ProductUomLevel uomLevel;

    /** The raw quantity as typed in {@code uomLevel} (e.g. "5" when uomLevel = Box) — {@code
     * orderedQty} is this multiplied by the level's {@code factorToBase}. Null when {@code
     * uomLevel} is null. */
    @Column(name = "entered_qty", precision = 14, scale = 3)
    private BigDecimal enteredQty;

    @Column(name = "unit_price", nullable = false, precision = 14, scale = 2)
    private BigDecimal unitPrice;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tax_rule_id")
    private TaxRule taxRule;

    @Column(name = "tax_amount", nullable = false, precision = 14, scale = 2)
    private BigDecimal taxAmount = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(name = "jurisdiction_mode", length = 20)
    private JurisdictionMode jurisdictionMode;

    @Column(name = "line_total", nullable = false, precision = 14, scale = 2)
    private BigDecimal lineTotal;

    @Column(name = "received_qty", nullable = false, precision = 14, scale = 3)
    private BigDecimal receivedQty = BigDecimal.ZERO;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public PurchaseOrder getPurchaseOrder() { return purchaseOrder; }
    public void setPurchaseOrder(PurchaseOrder purchaseOrder) { this.purchaseOrder = purchaseOrder; }

    public Product getProduct() { return product; }
    public void setProduct(Product product) { this.product = product; }

    public PurchaseRequisitionItem getPurchaseRequisitionItem() { return purchaseRequisitionItem; }
    public void setPurchaseRequisitionItem(PurchaseRequisitionItem purchaseRequisitionItem) { this.purchaseRequisitionItem = purchaseRequisitionItem; }

    public BigDecimal getOrderedQty() { return orderedQty; }
    public void setOrderedQty(BigDecimal orderedQty) { this.orderedQty = orderedQty; }

    public ProductUomLevel getUomLevel() { return uomLevel; }
    public void setUomLevel(ProductUomLevel uomLevel) { this.uomLevel = uomLevel; }

    public BigDecimal getEnteredQty() { return enteredQty; }
    public void setEnteredQty(BigDecimal enteredQty) { this.enteredQty = enteredQty; }

    public BigDecimal getUnitPrice() { return unitPrice; }
    public void setUnitPrice(BigDecimal unitPrice) { this.unitPrice = unitPrice; }

    public TaxRule getTaxRule() { return taxRule; }
    public void setTaxRule(TaxRule taxRule) { this.taxRule = taxRule; }

    public BigDecimal getTaxAmount() { return taxAmount; }
    public void setTaxAmount(BigDecimal taxAmount) { this.taxAmount = taxAmount; }

    public JurisdictionMode getJurisdictionMode() { return jurisdictionMode; }
    public void setJurisdictionMode(JurisdictionMode jurisdictionMode) { this.jurisdictionMode = jurisdictionMode; }

    public BigDecimal getLineTotal() { return lineTotal; }
    public void setLineTotal(BigDecimal lineTotal) { this.lineTotal = lineTotal; }

    public BigDecimal getReceivedQty() { return receivedQty; }
    public void setReceivedQty(BigDecimal receivedQty) { this.receivedQty = receivedQty; }
}
