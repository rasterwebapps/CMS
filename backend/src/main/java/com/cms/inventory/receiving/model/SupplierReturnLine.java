package com.cms.inventory.receiving.model;

import java.math.BigDecimal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/** One line of a {@link SupplierReturn} — always against a specific {@link GoodsReceiptLine}. */
@Entity
@Table(name = "supplier_return_lines")
public class SupplierReturnLine {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "supplier_return_id", nullable = false)
    private SupplierReturn supplierReturn;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "goods_receipt_line_id", nullable = false)
    private GoodsReceiptLine goodsReceiptLine;

    @Column(name = "returned_qty", nullable = false, precision = 14, scale = 3)
    private BigDecimal returnedQty;

    @Column(length = 500)
    private String notes;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public SupplierReturn getSupplierReturn() { return supplierReturn; }
    public void setSupplierReturn(SupplierReturn supplierReturn) { this.supplierReturn = supplierReturn; }

    public GoodsReceiptLine getGoodsReceiptLine() { return goodsReceiptLine; }
    public void setGoodsReceiptLine(GoodsReceiptLine goodsReceiptLine) { this.goodsReceiptLine = goodsReceiptLine; }

    public BigDecimal getReturnedQty() { return returnedQty; }
    public void setReturnedQty(BigDecimal returnedQty) { this.returnedQty = returnedQty; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
}
