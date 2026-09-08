package com.cms.inventory.procurement.model;

import java.math.BigDecimal;
import java.time.Instant;

import com.cms.inventory.catalog.model.Product;
import com.cms.inventory.procurement.model.enums.WantedListItemStatus;
import com.cms.inventory.procurement.model.enums.WantedListRejectionReason;
import com.cms.inventory.stock.model.InventoryLocation;

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
 * One auto-computed reorder-shortage line for a product at a location — the ERP-standard
 * "Planned Order" equivalent that a scheduled MRP-style netting run produces, ahead of it becoming
 * a real Purchase Requisition. {@code qtyOnHandSnapshot}/{@code qtyOnOrderSnapshot}/
 * {@code reorderLevelSnapshot} are captured at generation time (same reasoning as
 * {@code CycleCountLine.systemQtySnapshot}) so a line stays self-explanatory even after the
 * product's configured reorder level or the location's stock later moves on. {@code suggestedQty}
 * is the netted shortfall against the product's fixed reorder lot size — see
 * {@code WantedListService.computeSuggestedQty}. Never written to directly outside
 * {@code WantedListService}. See the "Wanted List slice" decision-log entry.
 */
@Entity
@Table(name = "wanted_list_items")
public class WantedListItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "location_id", nullable = false)
    private InventoryLocation location;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private WantedListItemStatus status = WantedListItemStatus.PENDING;

    @Column(name = "qty_on_hand_snapshot", nullable = false, precision = 14, scale = 3)
    private BigDecimal qtyOnHandSnapshot;

    @Column(name = "qty_on_order_snapshot", nullable = false, precision = 14, scale = 3)
    private BigDecimal qtyOnOrderSnapshot = BigDecimal.ZERO;

    @Column(name = "reorder_level_snapshot", nullable = false, precision = 14, scale = 3)
    private BigDecimal reorderLevelSnapshot;

    @Column(name = "suggested_qty", nullable = false, precision = 14, scale = 3)
    private BigDecimal suggestedQty;

    @Column(name = "generated_at", nullable = false)
    private Instant generatedAt;

    @Column(name = "resolved_by", length = 255)
    private String resolvedBy;

    @Column(name = "resolved_at")
    private Instant resolvedAt;

    @Column(name = "resolution_notes", length = 500)
    private String resolutionNotes;

    @Enumerated(EnumType.STRING)
    @Column(name = "rejection_reason", length = 30)
    private WantedListRejectionReason rejectionReason;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "converted_purchase_requisition_id")
    private PurchaseRequisition convertedPurchaseRequisition;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "converted_purchase_requisition_item_id")
    private PurchaseRequisitionItem convertedPurchaseRequisitionItem;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Product getProduct() { return product; }
    public void setProduct(Product product) { this.product = product; }

    public InventoryLocation getLocation() { return location; }
    public void setLocation(InventoryLocation location) { this.location = location; }

    public WantedListItemStatus getStatus() { return status; }
    public void setStatus(WantedListItemStatus status) { this.status = status; }

    public BigDecimal getQtyOnHandSnapshot() { return qtyOnHandSnapshot; }
    public void setQtyOnHandSnapshot(BigDecimal qtyOnHandSnapshot) { this.qtyOnHandSnapshot = qtyOnHandSnapshot; }

    public BigDecimal getQtyOnOrderSnapshot() { return qtyOnOrderSnapshot; }
    public void setQtyOnOrderSnapshot(BigDecimal qtyOnOrderSnapshot) { this.qtyOnOrderSnapshot = qtyOnOrderSnapshot; }

    public BigDecimal getReorderLevelSnapshot() { return reorderLevelSnapshot; }
    public void setReorderLevelSnapshot(BigDecimal reorderLevelSnapshot) { this.reorderLevelSnapshot = reorderLevelSnapshot; }

    public BigDecimal getSuggestedQty() { return suggestedQty; }
    public void setSuggestedQty(BigDecimal suggestedQty) { this.suggestedQty = suggestedQty; }

    public Instant getGeneratedAt() { return generatedAt; }
    public void setGeneratedAt(Instant generatedAt) { this.generatedAt = generatedAt; }

    public String getResolvedBy() { return resolvedBy; }
    public void setResolvedBy(String resolvedBy) { this.resolvedBy = resolvedBy; }

    public Instant getResolvedAt() { return resolvedAt; }
    public void setResolvedAt(Instant resolvedAt) { this.resolvedAt = resolvedAt; }

    public String getResolutionNotes() { return resolutionNotes; }
    public void setResolutionNotes(String resolutionNotes) { this.resolutionNotes = resolutionNotes; }

    public WantedListRejectionReason getRejectionReason() { return rejectionReason; }
    public void setRejectionReason(WantedListRejectionReason rejectionReason) { this.rejectionReason = rejectionReason; }

    public PurchaseRequisition getConvertedPurchaseRequisition() { return convertedPurchaseRequisition; }
    public void setConvertedPurchaseRequisition(PurchaseRequisition convertedPurchaseRequisition) { this.convertedPurchaseRequisition = convertedPurchaseRequisition; }

    public PurchaseRequisitionItem getConvertedPurchaseRequisitionItem() { return convertedPurchaseRequisitionItem; }
    public void setConvertedPurchaseRequisitionItem(PurchaseRequisitionItem convertedPurchaseRequisitionItem) { this.convertedPurchaseRequisitionItem = convertedPurchaseRequisitionItem; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
