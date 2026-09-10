package com.cms.inventory.receiving.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.cms.exception.ResourceNotFoundException;
import com.cms.inventory.catalog.model.Product;
import com.cms.inventory.catalog.model.ProductUomLevel;
import com.cms.inventory.catalog.model.Uom;
import com.cms.inventory.catalog.service.ProductUomChainService;
import com.cms.inventory.procurement.model.PurchaseOrder;
import com.cms.inventory.procurement.model.PurchaseOrderItem;
import com.cms.inventory.procurement.model.Supplier;
import com.cms.inventory.procurement.model.enums.PurchaseOrderStatus;
import com.cms.inventory.procurement.repository.PurchaseOrderItemRepository;
import com.cms.inventory.procurement.service.PurchaseOrderService;
import com.cms.inventory.receiving.dto.GoodsReceiptAddLineRequest;
import com.cms.inventory.receiving.model.GoodsReceipt;
import com.cms.inventory.receiving.model.GoodsReceiptLine;
import com.cms.inventory.receiving.model.enums.GoodsReceiptStatus;
import com.cms.inventory.receiving.repository.GoodsReceiptLineRepository;
import com.cms.inventory.receiving.repository.GoodsReceiptRepository;
import com.cms.inventory.stock.dto.StockMovementResponse;
import com.cms.inventory.stock.model.InventoryLocation;
import com.cms.inventory.stock.service.StockMovementService;

@ExtendWith(MockitoExtension.class)
class GoodsReceiptServiceTest {

    @Mock private GoodsReceiptRepository receiptRepository;
    @Mock private GoodsReceiptLineRepository lineRepository;
    @Mock private PurchaseOrderItemRepository purchaseOrderItemRepository;
    @Mock private PurchaseOrderService purchaseOrderService;
    @Mock private StockMovementService stockMovementService;
    @Mock private ProductUomChainService uomChainService;
    private GoodsReceiptService service;

    private final InventoryLocation location = location(1L, "Main Store");
    private final Supplier supplier = supplier(1L, "Acme Supplies");
    private final Uom tablet = uom(1L, "TABLET");

    @BeforeEach
    void setUp() {
        service = new GoodsReceiptService(receiptRepository, lineRepository, purchaseOrderItemRepository,
            purchaseOrderService, stockMovementService, uomChainService);
    }

    // ── addLine — base flow ──────────────────────────────────────────────────

    @Test
    void shouldAddLineInBaseUnitWhenNoUomLevelChosen() {
        GoodsReceipt receipt = receipt(1L, GoodsReceiptStatus.DRAFT, order(1L, PurchaseOrderStatus.ORDERED));
        PurchaseOrderItem poItem = poItem(50L, product(10L, tablet), receipt.getPurchaseOrder(), new BigDecimal("20"), new BigDecimal("5"));
        when(receiptRepository.findById(1L)).thenReturn(Optional.of(receipt));
        when(purchaseOrderItemRepository.findById(50L)).thenReturn(Optional.of(poItem));
        when(lineRepository.sumReceivedQtyInReceiptForItem(1L, 50L)).thenReturn(BigDecimal.ZERO);
        when(lineRepository.save(any(GoodsReceiptLine.class))).thenAnswer(inv -> { GoodsReceiptLine l = inv.getArgument(0); l.setId(500L); return l; });

        var req = new GoodsReceiptAddLineRequest(50L, new BigDecimal("10"), null, null, null, null, null);
        var res = service.addLine(1L, req);

        assertThat(res.receivedQty()).isEqualByComparingTo("10");
        assertThat(res.uomLevelId()).isNull();
        assertThat(res.enteredQty()).isNull();
    }

    @Test
    void shouldConvertEnteredQtyToBaseUnitsWhenUomLevelChosen() {
        GoodsReceipt receipt = receipt(1L, GoodsReceiptStatus.DRAFT, order(1L, PurchaseOrderStatus.ORDERED));
        Product product = product(10L, tablet);
        PurchaseOrderItem poItem = poItem(50L, product, receipt.getPurchaseOrder(), new BigDecimal("1000"), BigDecimal.ZERO);
        ProductUomLevel box = level(uom(3L, "BOX"), "100");

        when(receiptRepository.findById(1L)).thenReturn(Optional.of(receipt));
        when(purchaseOrderItemRepository.findById(50L)).thenReturn(Optional.of(poItem));
        when(uomChainService.requireActiveLevel(10L, 3L)).thenReturn(box);
        when(lineRepository.sumReceivedQtyInReceiptForItem(1L, 50L)).thenReturn(BigDecimal.ZERO);
        when(lineRepository.save(any(GoodsReceiptLine.class))).thenAnswer(inv -> { GoodsReceiptLine l = inv.getArgument(0); l.setId(500L); return l; });

        // Receiving 3 boxes (100 tablets each) against a line with 1000 base units still open.
        var req = new GoodsReceiptAddLineRequest(50L, new BigDecimal("3"), 3L, null, null, null, null);
        var res = service.addLine(1L, req);

        assertThat(res.receivedQty()).isEqualByComparingTo("300");
        assertThat(res.enteredQty()).isEqualByComparingTo("3");
        assertThat(res.uomLevelId()).isEqualTo(3L);
        assertThat(res.enteredUomCode()).isEqualTo("BOX");
    }

    @Test
    void shouldRejectWhenConvertedQtyExceedsOpenBalance() {
        GoodsReceipt receipt = receipt(1L, GoodsReceiptStatus.DRAFT, order(1L, PurchaseOrderStatus.ORDERED));
        Product product = product(10L, tablet);
        // Only 50 base units left open.
        PurchaseOrderItem poItem = poItem(50L, product, receipt.getPurchaseOrder(), new BigDecimal("100"), new BigDecimal("50"));
        ProductUomLevel box = level(uom(3L, "BOX"), "100");

        when(receiptRepository.findById(1L)).thenReturn(Optional.of(receipt));
        when(purchaseOrderItemRepository.findById(50L)).thenReturn(Optional.of(poItem));
        when(uomChainService.requireActiveLevel(10L, 3L)).thenReturn(box);
        when(lineRepository.sumReceivedQtyInReceiptForItem(1L, 50L)).thenReturn(BigDecimal.ZERO);

        // 1 box = 100 base units, but only 50 are open.
        var req = new GoodsReceiptAddLineRequest(50L, new BigDecimal("1"), 3L, null, null, null, null);
        assertThatThrownBy(() -> service.addLine(1L, req))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("exceeds what's still open");
    }

    @Test
    void shouldRejectLineFromADifferentOrder() {
        GoodsReceipt receipt = receipt(1L, GoodsReceiptStatus.DRAFT, order(1L, PurchaseOrderStatus.ORDERED));
        PurchaseOrder otherOrder = order(2L, PurchaseOrderStatus.ORDERED);
        PurchaseOrderItem poItem = poItem(50L, product(10L, tablet), otherOrder, new BigDecimal("20"), BigDecimal.ZERO);
        when(receiptRepository.findById(1L)).thenReturn(Optional.of(receipt));
        when(purchaseOrderItemRepository.findById(50L)).thenReturn(Optional.of(poItem));

        var req = new GoodsReceiptAddLineRequest(50L, new BigDecimal("10"), null, null, null, null, null);
        assertThatThrownBy(() -> service.addLine(1L, req))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("does not belong to this receipt's order");
    }

    @Test
    void shouldRejectAddingLineToNonDraftReceipt() {
        GoodsReceipt receipt = receipt(1L, GoodsReceiptStatus.CONFIRMED, order(1L, PurchaseOrderStatus.COMPLETED));
        when(receiptRepository.findById(1L)).thenReturn(Optional.of(receipt));
        var req = new GoodsReceiptAddLineRequest(50L, new BigDecimal("10"), null, null, null, null, null);
        assertThatThrownBy(() -> service.addLine(1L, req)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void shouldDefaultUnitCostFromPoLineWhenNotProvided() {
        GoodsReceipt receipt = receipt(1L, GoodsReceiptStatus.DRAFT, order(1L, PurchaseOrderStatus.ORDERED));
        PurchaseOrderItem poItem = poItem(50L, product(10L, tablet), receipt.getPurchaseOrder(), new BigDecimal("20"), BigDecimal.ZERO);
        poItem.setUnitPrice(new BigDecimal("12.50"));
        when(receiptRepository.findById(1L)).thenReturn(Optional.of(receipt));
        when(purchaseOrderItemRepository.findById(50L)).thenReturn(Optional.of(poItem));
        when(lineRepository.sumReceivedQtyInReceiptForItem(1L, 50L)).thenReturn(BigDecimal.ZERO);
        when(lineRepository.save(any(GoodsReceiptLine.class))).thenAnswer(inv -> { GoodsReceiptLine l = inv.getArgument(0); l.setId(500L); return l; });

        var req = new GoodsReceiptAddLineRequest(50L, new BigDecimal("5"), null, null, null, null, null);
        assertThat(service.addLine(1L, req).unitCost()).isEqualByComparingTo("12.50");
    }

    // ── removeLine ───────────────────────────────────────────────────────────

    @Test
    void shouldRemoveLineFromDraftReceipt() {
        GoodsReceipt receipt = receipt(1L, GoodsReceiptStatus.DRAFT, order(1L, PurchaseOrderStatus.ORDERED));
        GoodsReceiptLine line = new GoodsReceiptLine();
        line.setId(500L);
        line.setGoodsReceipt(receipt);
        when(receiptRepository.findById(1L)).thenReturn(Optional.of(receipt));
        when(lineRepository.findById(500L)).thenReturn(Optional.of(line));

        service.removeLine(1L, 500L);
        verify(lineRepository).delete(line);
    }

    @Test
    void shouldRejectRemovingLineFromConfirmedReceipt() {
        GoodsReceipt receipt = receipt(1L, GoodsReceiptStatus.CONFIRMED, order(1L, PurchaseOrderStatus.COMPLETED));
        when(receiptRepository.findById(1L)).thenReturn(Optional.of(receipt));
        assertThatThrownBy(() -> service.removeLine(1L, 500L)).isInstanceOf(IllegalArgumentException.class);
    }

    // ── confirm ──────────────────────────────────────────────────────────────

    @Test
    void shouldConfirmReceiptAndPostStockMovementPerLine() {
        PurchaseOrder order = order(1L, PurchaseOrderStatus.ORDERED);
        GoodsReceipt receipt = receipt(1L, GoodsReceiptStatus.DRAFT, order);
        PurchaseOrderItem poItem = poItem(50L, product(10L, tablet), order, new BigDecimal("20"), BigDecimal.ZERO);
        GoodsReceiptLine line = new GoodsReceiptLine();
        line.setId(500L);
        line.setPurchaseOrderItem(poItem);
        line.setReceivedQty(new BigDecimal("20"));
        line.setUnitCost(BigDecimal.TEN);

        when(receiptRepository.findById(1L)).thenReturn(Optional.of(receipt));
        when(lineRepository.findByGoodsReceiptIdOrderByIdAsc(1L)).thenReturn(List.of(line));
        when(stockMovementService.recordMovement(any(), org.mockito.ArgumentMatchers.eq("receiver")))
            .thenReturn(new StockMovementResponse(1L, 10L, 1L, null, "RECEIPT", new BigDecimal("20"),
                new BigDecimal("20"), new BigDecimal("200"), Instant.now()));

        var res = service.confirm(1L, "receiver");

        assertThat(res.status()).isEqualTo("CONFIRMED");
        assertThat(poItem.getReceivedQty()).isEqualByComparingTo("20");
        verify(purchaseOrderItemRepository).save(poItem);
        verify(purchaseOrderService).recalculateReceiptProgress(1L);
    }

    @Test
    void shouldRejectConfirmingReceiptWithNoLines() {
        GoodsReceipt receipt = receipt(1L, GoodsReceiptStatus.DRAFT, order(1L, PurchaseOrderStatus.ORDERED));
        when(receiptRepository.findById(1L)).thenReturn(Optional.of(receipt));
        when(lineRepository.findByGoodsReceiptIdOrderByIdAsc(1L)).thenReturn(List.of());
        assertThatThrownBy(() -> service.confirm(1L, "receiver"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Add at least one line");
    }

    @Test
    void shouldRejectConfirmWhenAnotherReceiptAlreadyConsumedTheOpenBalance() {
        PurchaseOrder order = order(1L, PurchaseOrderStatus.ORDERED);
        GoodsReceipt receipt = receipt(1L, GoodsReceiptStatus.DRAFT, order);
        // Only 5 left open now (another receipt confirmed first), but this line still says 20.
        PurchaseOrderItem poItem = poItem(50L, product(10L, tablet), order, new BigDecimal("20"), new BigDecimal("15"));
        GoodsReceiptLine line = new GoodsReceiptLine();
        line.setId(500L);
        line.setPurchaseOrderItem(poItem);
        line.setReceivedQty(new BigDecimal("20"));

        when(receiptRepository.findById(1L)).thenReturn(Optional.of(receipt));
        when(lineRepository.findByGoodsReceiptIdOrderByIdAsc(1L)).thenReturn(List.of(line));

        assertThatThrownBy(() -> service.confirm(1L, "receiver"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("another receipt confirmed first");
        verify(stockMovementService, never()).recordMovement(any(), any());
    }

    @Test
    void shouldThrowWhenReceiptNotFound() {
        when(receiptRepository.findById(9L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.findById(9L)).isInstanceOf(ResourceNotFoundException.class);
    }

    // ── fixtures ─────────────────────────────────────────────────────────────

    private GoodsReceipt receipt(Long id, GoodsReceiptStatus status, PurchaseOrder order) {
        GoodsReceipt r = new GoodsReceipt();
        r.setId(id);
        r.setStatus(status);
        r.setPurchaseOrder(order);
        return r;
    }

    private PurchaseOrder order(Long id, PurchaseOrderStatus status) {
        PurchaseOrder o = new PurchaseOrder();
        o.setId(id);
        o.setStatus(status);
        o.setSupplier(supplier);
        o.setLocation(location);
        return o;
    }

    private Supplier supplier(Long id, String name) {
        Supplier s = new Supplier();
        s.setId(id);
        s.setSupplierName(name);
        return s;
    }

    private InventoryLocation location(Long id, String name) {
        InventoryLocation l = new InventoryLocation();
        l.setId(id);
        l.setVirtualName(name);
        return l;
    }

    private Product product(Long id, Uom baseUom) {
        Product p = new Product();
        p.setId(id);
        p.setProductCode("PROD-" + id);
        p.setProductName("Product " + id);
        p.setBaseUom(baseUom);
        return p;
    }

    private Uom uom(Long id, String code) {
        Uom u = new Uom();
        u.setId(id);
        u.setCode(code);
        u.setName(code);
        return u;
    }

    private ProductUomLevel level(Uom uom, String factor) {
        ProductUomLevel l = new ProductUomLevel();
        l.setId(3L);
        l.setUom(uom);
        l.setFactorToBase(new BigDecimal(factor));
        l.setIsDefaultPurchase(false);
        return l;
    }

    private PurchaseOrderItem poItem(Long id, Product product, PurchaseOrder order, BigDecimal orderedQty, BigDecimal receivedQty) {
        PurchaseOrderItem item = new PurchaseOrderItem();
        item.setId(id);
        item.setProduct(product);
        item.setPurchaseOrder(order);
        item.setOrderedQty(orderedQty);
        item.setReceivedQty(receivedQty);
        item.setUnitPrice(BigDecimal.TEN);
        return item;
    }
}
