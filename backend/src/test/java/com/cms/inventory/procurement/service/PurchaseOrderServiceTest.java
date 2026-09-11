package com.cms.inventory.procurement.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
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
import com.cms.inventory.procurement.dto.PurchaseOrderAddLineRequest;
import com.cms.inventory.procurement.dto.PurchaseOrderForceCloseRequest;
import com.cms.inventory.procurement.model.PurchaseOrder;
import com.cms.inventory.procurement.model.PurchaseOrderItem;
import com.cms.inventory.procurement.model.PurchaseRequisition;
import com.cms.inventory.procurement.model.PurchaseRequisitionItem;
import com.cms.inventory.procurement.model.Supplier;
import com.cms.inventory.procurement.model.TaxRule;
import com.cms.inventory.procurement.model.TaxSubType;
import com.cms.inventory.procurement.model.enums.JurisdictionMode;
import com.cms.inventory.procurement.model.enums.PurchaseOrderStatus;
import com.cms.inventory.procurement.model.enums.PurchaseRequisitionItemStatus;
import com.cms.inventory.procurement.repository.PurchaseOrderItemRepository;
import com.cms.inventory.procurement.repository.PurchaseOrderItemTaxComponentRepository;
import com.cms.inventory.procurement.repository.PurchaseOrderRepository;
import com.cms.inventory.procurement.repository.PurchaseRequisitionItemRepository;
import com.cms.inventory.procurement.repository.SupplierRepository;
import com.cms.inventory.procurement.repository.TaxRuleRepository;
import com.cms.inventory.stock.model.InventoryLocation;
import com.cms.inventory.stock.repository.InventoryLocationRepository;

@ExtendWith(MockitoExtension.class)
class PurchaseOrderServiceTest {

    @Mock private PurchaseOrderRepository orderRepository;
    @Mock private PurchaseOrderItemRepository itemRepository;
    @Mock private PurchaseRequisitionItemRepository requisitionItemRepository;
    @Mock private SupplierRepository supplierRepository;
    @Mock private InventoryLocationRepository locationRepository;
    @Mock private TaxRuleRepository taxRuleRepository;
    @Mock private VendorProductMappingService vendorProductMappingService;
    @Mock private JurisdictionService jurisdictionService;
    @Mock private TaxSubTypeService taxSubTypeService;
    @Mock private PurchaseOrderItemTaxComponentRepository taxComponentRepository;
    @Mock private ProductUomChainService uomChainService;
    private PurchaseOrderService service;

    private final InventoryLocation location = location(1L, "Main Store");
    private final Supplier supplier = supplier(1L, "Acme Supplies", "Tamil Nadu");
    private final Uom tablet = uom(1L, "TABLET");

    @BeforeEach
    void setUp() {
        service = new PurchaseOrderService(orderRepository, itemRepository, requisitionItemRepository, supplierRepository,
            locationRepository, taxRuleRepository, vendorProductMappingService, jurisdictionService, taxSubTypeService,
            taxComponentRepository, uomChainService);
    }

    // ── addLine — base flow ──────────────────────────────────────────────────

    @Test
    void shouldAddLineInBaseUnitWhenNoUomLevelChosen() {
        PurchaseOrder order = order(1L, PurchaseOrderStatus.PENDING, supplier, location);
        PurchaseRequisitionItem reqItem = requisitionItem(5L, product(10L, tablet), new BigDecimal("20"), location);
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(requisitionItemRepository.findById(5L)).thenReturn(Optional.of(reqItem));
        when(itemRepository.save(any(PurchaseOrderItem.class))).thenAnswer(inv -> { PurchaseOrderItem i = inv.getArgument(0); i.setId(100L); return i; });
        when(taxComponentRepository.findByPurchaseOrderItem_IdOrderByIdAsc(100L)).thenReturn(List.of());

        var req = new PurchaseOrderAddLineRequest(5L, new BigDecimal("20"), null, new BigDecimal("10"), null);
        var res = service.addLine(1L, req);

        assertThat(res.orderedQty()).isEqualByComparingTo("20");
        assertThat(res.uomLevelId()).isNull();
        assertThat(res.enteredQty()).isNull();
        assertThat(res.lineTotal()).isEqualByComparingTo("200.00");
        assertThat(reqItem.getStatus()).isEqualTo(PurchaseRequisitionItemStatus.ORDERED);
    }

    @Test
    void shouldConvertEnteredQtyToBaseUnitsWhenUomLevelChosen() {
        PurchaseOrder order = order(1L, PurchaseOrderStatus.PENDING, supplier, location);
        Product product = product(10L, tablet);
        PurchaseRequisitionItem reqItem = requisitionItem(5L, product, new BigDecimal("1000"), location);
        ProductUomLevel box = level(3L, uom(3L, "BOX"), 2, "100");

        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(requisitionItemRepository.findById(5L)).thenReturn(Optional.of(reqItem));
        when(uomChainService.requireActiveLevel(10L, 3L)).thenReturn(box);
        when(itemRepository.save(any(PurchaseOrderItem.class))).thenAnswer(inv -> { PurchaseOrderItem i = inv.getArgument(0); i.setId(100L); return i; });
        when(taxComponentRepository.findByPurchaseOrderItem_IdOrderByIdAsc(100L)).thenReturn(List.of());

        // Ordering 5 Boxes (100 tablets each) at 500/box.
        var req = new PurchaseOrderAddLineRequest(5L, new BigDecimal("5"), 3L, new BigDecimal("500"), null);
        var res = service.addLine(1L, req);

        assertThat(res.orderedQty()).isEqualByComparingTo("500");   // 5 x 100, base units
        assertThat(res.enteredQty()).isEqualByComparingTo("5");     // as typed
        assertThat(res.uomLevelId()).isEqualTo(3L);
        assertThat(res.enteredUomCode()).isEqualTo("BOX");
        // unitPrice x enteredQty, NOT the base-converted orderedQty (500 x 500 would be wrong).
        assertThat(res.lineTotal()).isEqualByComparingTo("2500.00");
    }

    @Test
    void shouldDefaultQtyFromRequisitionWhenNotProvided() {
        PurchaseOrder order = order(1L, PurchaseOrderStatus.PENDING, supplier, location);
        PurchaseRequisitionItem reqItem = requisitionItem(5L, product(10L, tablet), new BigDecimal("42"), location);
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(requisitionItemRepository.findById(5L)).thenReturn(Optional.of(reqItem));
        when(itemRepository.save(any(PurchaseOrderItem.class))).thenAnswer(inv -> { PurchaseOrderItem i = inv.getArgument(0); i.setId(100L); return i; });
        when(taxComponentRepository.findByPurchaseOrderItem_IdOrderByIdAsc(100L)).thenReturn(List.of());

        var req = new PurchaseOrderAddLineRequest(5L, null, null, new BigDecimal("10"), null);
        assertThat(service.addLine(1L, req).orderedQty()).isEqualByComparingTo("42");
    }

    @Test
    void shouldRejectZeroQuantity() {
        PurchaseOrder order = order(1L, PurchaseOrderStatus.PENDING, supplier, location);
        PurchaseRequisitionItem reqItem = requisitionItem(5L, product(10L, tablet), new BigDecimal("10"), location);
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(requisitionItemRepository.findById(5L)).thenReturn(Optional.of(reqItem));

        var req = new PurchaseOrderAddLineRequest(5L, BigDecimal.ZERO, null, new BigDecimal("10"), null);
        assertThatThrownBy(() -> service.addLine(1L, req))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("greater than zero");
    }

    @Test
    void shouldRejectRequisitionLineNotApproved() {
        PurchaseOrder order = order(1L, PurchaseOrderStatus.PENDING, supplier, location);
        PurchaseRequisitionItem reqItem = requisitionItem(5L, product(10L, tablet), new BigDecimal("10"), location);
        reqItem.setStatus(PurchaseRequisitionItemStatus.ORDERED);
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(requisitionItemRepository.findById(5L)).thenReturn(Optional.of(reqItem));

        var req = new PurchaseOrderAddLineRequest(5L, new BigDecimal("10"), null, new BigDecimal("10"), null);
        assertThatThrownBy(() -> service.addLine(1L, req))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("not approved");
    }

    @Test
    void shouldRejectRequisitionLineFromDifferentLocation() {
        PurchaseOrder order = order(1L, PurchaseOrderStatus.PENDING, supplier, location);
        PurchaseRequisitionItem reqItem = requisitionItem(5L, product(10L, tablet), new BigDecimal("10"), location(2L, "Other Store"));
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(requisitionItemRepository.findById(5L)).thenReturn(Optional.of(reqItem));

        var req = new PurchaseOrderAddLineRequest(5L, new BigDecimal("10"), null, new BigDecimal("10"), null);
        assertThatThrownBy(() -> service.addLine(1L, req))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("different location");
    }

    @Test
    void shouldRejectAddingLineToNonPendingOrder() {
        PurchaseOrder order = order(1L, PurchaseOrderStatus.ORDERED, supplier, location);
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        var req = new PurchaseOrderAddLineRequest(5L, new BigDecimal("10"), null, new BigDecimal("10"), null);
        assertThatThrownBy(() -> service.addLine(1L, req)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void shouldResolveUnitPriceFromVendorRateWhenNotProvided() {
        PurchaseOrder order = order(1L, PurchaseOrderStatus.PENDING, supplier, location);
        PurchaseRequisitionItem reqItem = requisitionItem(5L, product(10L, tablet), new BigDecimal("10"), location);
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(requisitionItemRepository.findById(5L)).thenReturn(Optional.of(reqItem));
        when(vendorProductMappingService.resolveEffectiveRate(1L, 10L))
            .thenReturn(new VendorProductMappingService.EffectiveRate(new BigDecimal("7.50"), "INR", null));
        when(itemRepository.save(any(PurchaseOrderItem.class))).thenAnswer(inv -> { PurchaseOrderItem i = inv.getArgument(0); i.setId(100L); return i; });
        when(taxComponentRepository.findByPurchaseOrderItem_IdOrderByIdAsc(100L)).thenReturn(List.of());

        var req = new PurchaseOrderAddLineRequest(5L, new BigDecimal("10"), null, null, null);
        assertThat(service.addLine(1L, req).unitPrice()).isEqualByComparingTo("7.50");
    }

    @Test
    void shouldThrowWhenNoUnitPriceAndNoVendorRate() {
        PurchaseOrder order = order(1L, PurchaseOrderStatus.PENDING, supplier, location);
        PurchaseRequisitionItem reqItem = requisitionItem(5L, product(10L, tablet), new BigDecimal("10"), location);
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(requisitionItemRepository.findById(5L)).thenReturn(Optional.of(reqItem));
        when(vendorProductMappingService.resolveEffectiveRate(1L, 10L)).thenReturn(null);

        var req = new PurchaseOrderAddLineRequest(5L, new BigDecimal("10"), null, null, null);
        assertThatThrownBy(() -> service.addLine(1L, req))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("No vendor rate is on file");
    }

    // ── addLine — tax jurisdiction ───────────────────────────────────────────

    @Test
    void shouldSkipJurisdictionResolutionWhenNoTaxSelected() {
        PurchaseOrder order = order(1L, PurchaseOrderStatus.PENDING, supplier, location);
        PurchaseRequisitionItem reqItem = requisitionItem(5L, product(10L, tablet), new BigDecimal("10"), location);
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(requisitionItemRepository.findById(5L)).thenReturn(Optional.of(reqItem));
        when(itemRepository.save(any(PurchaseOrderItem.class))).thenAnswer(inv -> { PurchaseOrderItem i = inv.getArgument(0); i.setId(100L); return i; });
        when(taxComponentRepository.findByPurchaseOrderItem_IdOrderByIdAsc(100L)).thenReturn(List.of());

        var req = new PurchaseOrderAddLineRequest(5L, new BigDecimal("10"), null, new BigDecimal("10"), null);
        var res = service.addLine(1L, req);

        assertThat(res.taxAmount()).isEqualByComparingTo("0");
        assertThat(res.jurisdictionMode()).isNull();
        verify(jurisdictionService, never()).resolve(any());
    }

    @Test
    void shouldComputeTaxAndSplitComponentsWhenTaxRuleSelected() {
        PurchaseOrder order = order(1L, PurchaseOrderStatus.PENDING, supplier, location);
        PurchaseRequisitionItem reqItem = requisitionItem(5L, product(10L, tablet), new BigDecimal("10"), location);
        TaxRule gst = taxRule(2L, "GST 18%", new BigDecimal("18"));
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(requisitionItemRepository.findById(5L)).thenReturn(Optional.of(reqItem));
        when(taxRuleRepository.findById(2L)).thenReturn(Optional.of(gst));
        when(jurisdictionService.resolve(supplier)).thenReturn(JurisdictionMode.INTRASTATE);
        when(taxSubTypeService.requireCompleteSplit(2L, JurisdictionMode.INTRASTATE)).thenReturn(List.of(
            subType("CGST", new BigDecimal("50")), subType("SGST", new BigDecimal("50"))));
        when(itemRepository.save(any(PurchaseOrderItem.class))).thenAnswer(inv -> { PurchaseOrderItem i = inv.getArgument(0); i.setId(100L); return i; });
        when(taxComponentRepository.findByPurchaseOrderItem_IdOrderByIdAsc(100L)).thenReturn(List.of());

        // 10 units x 100/unit = 1000 subtotal; 18% GST = 180.
        var req = new PurchaseOrderAddLineRequest(5L, new BigDecimal("10"), null, new BigDecimal("100"), 2L);
        var res = service.addLine(1L, req);

        assertThat(res.taxAmount()).isEqualByComparingTo("180.00");
        assertThat(res.jurisdictionMode()).isEqualTo("INTRASTATE");
        assertThat(res.lineTotal()).isEqualByComparingTo("1180.00");
        // Two components split evenly: 90.00 + 90.00, exactly summing to taxAmount.
        verify(taxComponentRepository, times(2)).save(any());
    }

    @Test
    void shouldThrowWhenTaxRuleIdNotFound() {
        PurchaseOrder order = order(1L, PurchaseOrderStatus.PENDING, supplier, location);
        PurchaseRequisitionItem reqItem = requisitionItem(5L, product(10L, tablet), new BigDecimal("10"), location);
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(requisitionItemRepository.findById(5L)).thenReturn(Optional.of(reqItem));
        when(taxRuleRepository.findById(9L)).thenReturn(Optional.empty());

        var req = new PurchaseOrderAddLineRequest(5L, new BigDecimal("10"), null, new BigDecimal("10"), 9L);
        assertThatThrownBy(() -> service.addLine(1L, req)).isInstanceOf(ResourceNotFoundException.class);
    }

    // ── addLine — product default TaxRule pre-fill (2026-09-11 "HSN/SAC + default TaxRule") ──

    @Test
    void shouldPreFillTaxRuleFromProductDefaultWhenNoneChosen() {
        PurchaseOrder order = order(1L, PurchaseOrderStatus.PENDING, supplier, location);
        Product product = product(10L, tablet);
        product.setDefaultTaxRuleId(2L);
        PurchaseRequisitionItem reqItem = requisitionItem(5L, product, new BigDecimal("10"), location);
        TaxRule gst = taxRule(2L, "GST 18%", new BigDecimal("18"));
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(requisitionItemRepository.findById(5L)).thenReturn(Optional.of(reqItem));
        when(taxRuleRepository.findById(2L)).thenReturn(Optional.of(gst));
        when(jurisdictionService.resolve(supplier)).thenReturn(JurisdictionMode.INTRASTATE);
        when(taxSubTypeService.requireCompleteSplit(2L, JurisdictionMode.INTRASTATE)).thenReturn(List.of(
            subType("CGST", new BigDecimal("50")), subType("SGST", new BigDecimal("50"))));
        when(itemRepository.save(any(PurchaseOrderItem.class))).thenAnswer(inv -> { PurchaseOrderItem i = inv.getArgument(0); i.setId(100L); return i; });
        when(taxComponentRepository.findByPurchaseOrderItem_IdOrderByIdAsc(100L)).thenReturn(List.of());

        // No taxRuleId on the request — pre-filled from the product's own default (id 2).
        var req = new PurchaseOrderAddLineRequest(5L, new BigDecimal("10"), null, new BigDecimal("100"), null);
        var res = service.addLine(1L, req);

        assertThat(res.taxRuleId()).isEqualTo(2L);
        assertThat(res.taxAmount()).isEqualByComparingTo("180.00");
    }

    @Test
    void shouldPreferExplicitTaxRuleIdOverProductDefault() {
        PurchaseOrder order = order(1L, PurchaseOrderStatus.PENDING, supplier, location);
        Product product = product(10L, tablet);
        product.setDefaultTaxRuleId(2L);
        PurchaseRequisitionItem reqItem = requisitionItem(5L, product, new BigDecimal("10"), location);
        TaxRule vat = taxRule(3L, "VAT 5%", new BigDecimal("5"));
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(requisitionItemRepository.findById(5L)).thenReturn(Optional.of(reqItem));
        when(taxRuleRepository.findById(3L)).thenReturn(Optional.of(vat));
        when(jurisdictionService.resolve(supplier)).thenReturn(JurisdictionMode.INTRASTATE);
        when(taxSubTypeService.requireCompleteSplit(3L, JurisdictionMode.INTRASTATE)).thenReturn(List.of(
            subType("CGST", new BigDecimal("50")), subType("SGST", new BigDecimal("50"))));
        when(itemRepository.save(any(PurchaseOrderItem.class))).thenAnswer(inv -> { PurchaseOrderItem i = inv.getArgument(0); i.setId(100L); return i; });
        when(taxComponentRepository.findByPurchaseOrderItem_IdOrderByIdAsc(100L)).thenReturn(List.of());

        // Explicit taxRuleId (3) overrides the product's default (2) — never even looked up.
        var req = new PurchaseOrderAddLineRequest(5L, new BigDecimal("10"), null, new BigDecimal("100"), 3L);
        var res = service.addLine(1L, req);

        assertThat(res.taxRuleId()).isEqualTo(3L);
        verify(taxRuleRepository, never()).findById(2L);
    }

    @Test
    void shouldIgnoreStaleProductDefaultTaxRuleIdWhenItNoLongerResolves() {
        PurchaseOrder order = order(1L, PurchaseOrderStatus.PENDING, supplier, location);
        Product product = product(10L, tablet);
        product.setDefaultTaxRuleId(99L);
        PurchaseRequisitionItem reqItem = requisitionItem(5L, product, new BigDecimal("10"), location);
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(requisitionItemRepository.findById(5L)).thenReturn(Optional.of(reqItem));
        when(taxRuleRepository.findById(99L)).thenReturn(Optional.empty());
        when(itemRepository.save(any(PurchaseOrderItem.class))).thenAnswer(inv -> { PurchaseOrderItem i = inv.getArgument(0); i.setId(100L); return i; });
        when(taxComponentRepository.findByPurchaseOrderItem_IdOrderByIdAsc(100L)).thenReturn(List.of());

        var req = new PurchaseOrderAddLineRequest(5L, new BigDecimal("10"), null, new BigDecimal("100"), null);
        var res = service.addLine(1L, req);

        assertThat(res.taxRuleId()).isNull();
        assertThat(res.taxAmount()).isEqualByComparingTo("0");
        verify(jurisdictionService, never()).resolve(any());
    }

    // ── removeLine ───────────────────────────────────────────────────────────

    @Test
    void shouldRemoveLineAndRevertRequisitionStatusToApproved() {
        PurchaseOrder order = order(1L, PurchaseOrderStatus.PENDING, supplier, location);
        PurchaseRequisitionItem reqItem = requisitionItem(5L, product(10L, tablet), new BigDecimal("10"), location);
        reqItem.setStatus(PurchaseRequisitionItemStatus.ORDERED);
        PurchaseOrderItem item = new PurchaseOrderItem();
        item.setId(100L);
        item.setPurchaseOrder(order);
        item.setPurchaseRequisitionItem(reqItem);

        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(itemRepository.findById(100L)).thenReturn(Optional.of(item));

        service.removeLine(1L, 100L);
        assertThat(reqItem.getStatus()).isEqualTo(PurchaseRequisitionItemStatus.APPROVED);
    }

    @Test
    void shouldRejectRemovingLineFromNonPendingOrder() {
        PurchaseOrder order = order(1L, PurchaseOrderStatus.ORDERED, supplier, location);
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        assertThatThrownBy(() -> service.removeLine(1L, 100L)).isInstanceOf(IllegalArgumentException.class);
    }

    // ── order / forceClose ───────────────────────────────────────────────────

    @Test
    void shouldSendOrderWithAtLeastOneLine() {
        PurchaseOrder order = order(1L, PurchaseOrderStatus.PENDING, supplier, location);
        PurchaseOrderItem line = itemWithQty(new BigDecimal("10"), BigDecimal.ZERO);
        line.setId(100L);
        line.setProduct(product(10L, tablet));
        line.setUnitPrice(BigDecimal.TEN);
        line.setTaxAmount(BigDecimal.ZERO);
        line.setLineTotal(new BigDecimal("100.00"));
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(itemRepository.findByPurchaseOrderIdOrderByIdAsc(1L)).thenReturn(List.of(line));
        when(taxComponentRepository.findByPurchaseOrderItem_IdOrderByIdAsc(any())).thenReturn(List.of());

        var res = service.order(1L, "buyer");
        assertThat(res.status()).isEqualTo("ORDERED");
    }

    @Test
    void shouldRejectSendingOrderWithNoLines() {
        PurchaseOrder order = order(1L, PurchaseOrderStatus.PENDING, supplier, location);
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(itemRepository.findByPurchaseOrderIdOrderByIdAsc(1L)).thenReturn(List.of());
        assertThatThrownBy(() -> service.order(1L, "buyer"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Add at least one line");
    }

    @Test
    void shouldForceCloseOrder() {
        PurchaseOrder order = order(1L, PurchaseOrderStatus.ORDERED, supplier, location);
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(itemRepository.findByPurchaseOrderIdOrderByIdAsc(1L)).thenReturn(List.of());
        var res = service.forceClose(1L, new PurchaseOrderForceCloseRequest("Cancelled by buyer"), "buyer");
        assertThat(res.status()).isEqualTo("FORCE_CLOSED");
    }

    @Test
    void shouldRejectForceClosingAlreadyClosedOrder() {
        PurchaseOrder order = order(1L, PurchaseOrderStatus.COMPLETED, supplier, location);
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        assertThatThrownBy(() -> service.forceClose(1L, new PurchaseOrderForceCloseRequest("x"), "buyer"))
            .isInstanceOf(IllegalArgumentException.class);
    }

    // ── recalculateReceiptProgress ───────────────────────────────────────────

    @Test
    void shouldMarkCompletedWhenAllLinesFullyReceived() {
        PurchaseOrder order = order(1L, PurchaseOrderStatus.PARTIALLY_COMPLETED, supplier, location);
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(itemRepository.findByPurchaseOrderIdOrderByIdAsc(1L))
            .thenReturn(List.of(itemWithQty(new BigDecimal("10"), new BigDecimal("10"))));
        service.recalculateReceiptProgress(1L);
        assertThat(order.getStatus()).isEqualTo(PurchaseOrderStatus.COMPLETED);
    }

    @Test
    void shouldMarkPartiallyCompletedWhenSomeLinesFullyReceivedAndOthersNot() {
        PurchaseOrder order = order(1L, PurchaseOrderStatus.ORDERED, supplier, location);
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(itemRepository.findByPurchaseOrderIdOrderByIdAsc(1L)).thenReturn(List.of(
            itemWithQty(new BigDecimal("10"), new BigDecimal("10")),
            itemWithQty(new BigDecimal("10"), new BigDecimal("5"))));
        service.recalculateReceiptProgress(1L);
        assertThat(order.getStatus()).isEqualTo(PurchaseOrderStatus.PARTIALLY_COMPLETED);
    }

    @Test
    void shouldMarkInProgressWhenSomeReceivedButNoneFull() {
        PurchaseOrder order = order(1L, PurchaseOrderStatus.ORDERED, supplier, location);
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(itemRepository.findByPurchaseOrderIdOrderByIdAsc(1L))
            .thenReturn(List.of(itemWithQty(new BigDecimal("10"), new BigDecimal("3"))));
        service.recalculateReceiptProgress(1L);
        assertThat(order.getStatus()).isEqualTo(PurchaseOrderStatus.IN_PROGRESS);
    }

    @Test
    void shouldRevertToOrderedWhenNothingReceived() {
        PurchaseOrder order = order(1L, PurchaseOrderStatus.PARTIALLY_COMPLETED, supplier, location);
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(itemRepository.findByPurchaseOrderIdOrderByIdAsc(1L))
            .thenReturn(List.of(itemWithQty(new BigDecimal("10"), BigDecimal.ZERO)));
        service.recalculateReceiptProgress(1L);
        assertThat(order.getStatus()).isEqualTo(PurchaseOrderStatus.ORDERED);
    }

    @Test
    void shouldNoOpWhenOrderIsForceClosed() {
        PurchaseOrder order = order(1L, PurchaseOrderStatus.FORCE_CLOSED, supplier, location);
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        service.recalculateReceiptProgress(1L);
        verify(itemRepository, never()).findByPurchaseOrderIdOrderByIdAsc(any());
    }

    // ── fixtures ─────────────────────────────────────────────────────────────

    private PurchaseOrder order(Long id, PurchaseOrderStatus status, Supplier supplier, InventoryLocation location) {
        PurchaseOrder o = new PurchaseOrder();
        o.setId(id);
        o.setSupplier(supplier);
        o.setLocation(location);
        o.setStatus(status);
        return o;
    }

    private Supplier supplier(Long id, String name, String state) {
        Supplier s = new Supplier();
        s.setId(id);
        s.setSupplierName(name);
        s.setState(state);
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

    private ProductUomLevel level(Long id, Uom uom, int rank, String factor) {
        ProductUomLevel l = new ProductUomLevel();
        l.setId(id);
        l.setUom(uom);
        l.setLevelRank(rank);
        l.setFactorToBase(new BigDecimal(factor));
        l.setIsDefaultPurchase(false);
        return l;
    }

    private PurchaseRequisitionItem requisitionItem(Long id, Product product, BigDecimal requestedQty, InventoryLocation location) {
        PurchaseRequisition requisition = new PurchaseRequisition();
        requisition.setLocation(location);
        PurchaseRequisitionItem item = new PurchaseRequisitionItem();
        item.setId(id);
        item.setPurchaseRequisition(requisition);
        item.setProduct(product);
        item.setRequestedQty(requestedQty);
        item.setStatus(PurchaseRequisitionItemStatus.APPROVED);
        return item;
    }

    private TaxRule taxRule(Long id, String name, BigDecimal ratePercent) {
        TaxRule t = new TaxRule();
        t.setId(id);
        t.setName(name);
        t.setRatePercent(ratePercent);
        return t;
    }

    private TaxSubType subType(String name, BigDecimal percent) {
        TaxSubType s = new TaxSubType();
        s.setComponentName(name);
        s.setSplitPercent(percent);
        return s;
    }

    private PurchaseOrderItem itemWithQty(BigDecimal orderedQty, BigDecimal receivedQty) {
        PurchaseOrderItem item = new PurchaseOrderItem();
        item.setOrderedQty(orderedQty);
        item.setReceivedQty(receivedQty);
        return item;
    }
}
