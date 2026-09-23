package com.cms.inventory.indent.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.cms.exception.ResourceNotFoundException;
import com.cms.inventory.catalog.model.Product;
import com.cms.inventory.catalog.model.ProductVariant;
import com.cms.inventory.catalog.repository.ProductRepository;
import com.cms.inventory.catalog.repository.ProductVariantRepository;
import com.cms.inventory.indent.dto.StockIndentAddLineRequest;
import com.cms.inventory.indent.dto.StockIndentFulfillViaTransferRequest;
import com.cms.inventory.indent.dto.StockIndentRaisePoRequest;
import com.cms.inventory.indent.dto.StockIndentReturnLineRequest;
import com.cms.inventory.indent.model.StockIndent;
import com.cms.inventory.indent.model.StockIndentItem;
import com.cms.inventory.indent.model.enums.StockIndentItemStatus;
import com.cms.inventory.indent.model.enums.StockIndentStatus;
import com.cms.inventory.indent.repository.StockIndentItemRepository;
import com.cms.inventory.indent.repository.StockIndentRepository;
import com.cms.inventory.procurement.dto.PurchaseRequisitionItemResponse;
import com.cms.inventory.procurement.dto.PurchaseRequisitionResponse;
import com.cms.inventory.procurement.repository.PurchaseRequisitionItemRepository;
import com.cms.inventory.procurement.repository.PurchaseRequisitionRepository;
import com.cms.inventory.procurement.service.PurchaseRequisitionService;
import com.cms.inventory.stock.dto.StockMovementRequest;
import com.cms.inventory.stock.dto.StockTransferResponse;
import com.cms.inventory.stock.model.InventoryLocation;
import com.cms.inventory.stock.model.StockBalance;
import com.cms.inventory.stock.repository.InventoryLocationRepository;
import com.cms.inventory.stock.repository.StockBalanceRepository;
import com.cms.inventory.stock.repository.StockTransferRepository;
import com.cms.inventory.stock.service.StockMovementService;
import com.cms.inventory.stock.service.StockTransferService;

@ExtendWith(MockitoExtension.class)
class StockIndentServiceTest {

    @Mock private StockIndentRepository requestRepository;
    @Mock private StockIndentItemRepository lineRepository;
    @Mock private InventoryLocationRepository locationRepository;
    @Mock private ProductRepository productRepository;
    @Mock private StockMovementService stockMovementService;
    @Mock private ProductVariantRepository variantRepository;
    @Mock private StockBalanceRepository balanceRepository;
    @Mock private StockTransferService stockTransferService;
    @Mock private StockTransferRepository stockTransferRepository;
    @Mock private PurchaseRequisitionService purchaseRequisitionService;
    @Mock private PurchaseRequisitionRepository purchaseRequisitionRepository;
    @Mock private PurchaseRequisitionItemRepository purchaseRequisitionItemRepository;
    private StockIndentService service;

    private final InventoryLocation requesting = location(1L, "Ward A");
    private final InventoryLocation issuing = location(2L, "Main Store");
    private final Product product = product(10L);

    @BeforeEach
    void setUp() {
        service = new StockIndentService(requestRepository, lineRepository, locationRepository, productRepository,
            stockMovementService, variantRepository, balanceRepository, stockTransferService, stockTransferRepository,
            purchaseRequisitionService, purchaseRequisitionRepository, purchaseRequisitionItemRepository);
        lenient().when(lineRepository.existsByStockIndentIdAndStatusIn(any(), any())).thenReturn(true);
    }

    @Test
    void shouldRejectCreateWithSameRequestingAndIssuingLocation() {
        var req = new com.cms.inventory.indent.dto.StockIndentCreateRequest(1L, 1L, LocalDate.now(), null);
        assertThatThrownBy(() -> service.create(req, "clerk"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("must be different");
    }

    @Test
    void shouldRejectCreateWhenRequestingLocationIsAStore() {
        InventoryLocation store = location(3L, "Pharmacy Store");
        store.setLocationRole(com.cms.inventory.stock.model.enums.LocationRole.STORE);
        when(locationRepository.findById(3L)).thenReturn(Optional.of(store));
        when(locationRepository.findById(2L)).thenReturn(Optional.of(issuing));

        var req = new com.cms.inventory.indent.dto.StockIndentCreateRequest(3L, 2L, LocalDate.now(), null);
        assertThatThrownBy(() -> service.create(req, "clerk"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("cannot request stock");
    }

    @Test
    void shouldRejectCreateWhenIssuingLocationIsARequestingPoint() {
        InventoryLocation requestingPoint = location(1L, "Ward A");
        requestingPoint.setLocationRole(com.cms.inventory.stock.model.enums.LocationRole.REQUESTING_POINT);
        InventoryLocation anotherRequestingPoint = location(4L, "Ward B");
        anotherRequestingPoint.setLocationRole(com.cms.inventory.stock.model.enums.LocationRole.REQUESTING_POINT);
        when(locationRepository.findById(1L)).thenReturn(Optional.of(requestingPoint));
        when(locationRepository.findById(4L)).thenReturn(Optional.of(anotherRequestingPoint));

        var req = new com.cms.inventory.indent.dto.StockIndentCreateRequest(1L, 4L, LocalDate.now(), null);
        assertThatThrownBy(() -> service.create(req, "clerk"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("cannot issue stock");
    }

    @Test
    void shouldAddLineWithNoVariantWhenProductHasNone() {
        StockIndent indent = indent(1L, StockIndentStatus.DRAFT);
        when(requestRepository.findById(1L)).thenReturn(Optional.of(indent));
        when(productRepository.findById(10L)).thenReturn(Optional.of(product));
        when(lineRepository.existsByStockIndentIdAndProductIdAndVariantIsNull(1L, 10L)).thenReturn(false);
        when(lineRepository.save(any(StockIndentItem.class))).thenAnswer(inv -> { StockIndentItem l = inv.getArgument(0); l.setId(50L); return l; });

        var req = new StockIndentAddLineRequest(10L, null, new BigDecimal("5"), null);
        var res = service.addLine(1L, req);

        assertThat(res.variantId()).isNull();
    }

    @Test
    void shouldRejectAddLineWhenProductHasActiveVariantsButNoneGiven() {
        StockIndent indent = indent(1L, StockIndentStatus.DRAFT);
        when(requestRepository.findById(1L)).thenReturn(Optional.of(indent));
        when(productRepository.findById(10L)).thenReturn(Optional.of(product));
        when(variantRepository.existsByProductIdAndIsActiveTrue(10L)).thenReturn(true);

        var req = new StockIndentAddLineRequest(10L, null, new BigDecimal("5"), null);
        assertThatThrownBy(() -> service.addLine(1L, req))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("has active variants");
    }

    @Test
    void shouldRejectDuplicateLineForTheSameVariant() {
        StockIndent indent = indent(1L, StockIndentStatus.DRAFT);
        ProductVariant variant = variant(77L);
        when(requestRepository.findById(1L)).thenReturn(Optional.of(indent));
        when(productRepository.findById(10L)).thenReturn(Optional.of(product));
        when(variantRepository.findByIdAndProductId(77L, 10L)).thenReturn(Optional.of(variant));
        when(lineRepository.existsByStockIndentIdAndProductIdAndVariantId(1L, 10L, 77L)).thenReturn(true);

        var req = new StockIndentAddLineRequest(10L, 77L, new BigDecimal("5"), null);
        assertThatThrownBy(() -> service.addLine(1L, req))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("already on the request");
    }

    @Test
    void shouldApproveLineWithoutPostingAnyMovement() {
        StockIndent indent = indent(1L, StockIndentStatus.SUBMITTED);
        StockIndentItem line = pendingLine(500L, indent);
        when(requestRepository.findById(1L)).thenReturn(Optional.of(indent));
        when(lineRepository.findById(500L)).thenReturn(Optional.of(line));

        service.approveLine(1L, 500L, null, "dept-head");

        assertThat(line.getStatus()).isEqualTo(StockIndentItemStatus.APPROVED);
        assertThat(line.getResolvedBy()).isEqualTo("dept-head");
        verify(stockMovementService, never()).recordMovement(any(), any());
    }

    @Test
    void shouldRejectApprovingAnAlreadyResolvedLine() {
        StockIndent indent = indent(1L, StockIndentStatus.SUBMITTED);
        StockIndentItem line = new StockIndentItem();
        line.setId(500L);
        line.setStockIndent(indent);
        line.setStatus(StockIndentItemStatus.APPROVED);
        when(requestRepository.findById(1L)).thenReturn(Optional.of(indent));
        when(lineRepository.findById(500L)).thenReturn(Optional.of(line));

        assertThatThrownBy(() -> service.approveLine(1L, 500L, null, "clerk"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("already been resolved");
        verify(stockMovementService, never()).recordMovement(any(), any());
    }

    @Test
    void shouldRejectStoreActionsOnAPendingLine() {
        StockIndent indent = indent(1L, StockIndentStatus.SUBMITTED);
        StockIndentItem line = pendingLine(500L, indent);
        when(requestRepository.findById(1L)).thenReturn(Optional.of(indent));
        when(lineRepository.findById(500L)).thenReturn(Optional.of(line));

        assertThatThrownBy(() -> service.fulfillLine(1L, 500L, null, "store-keeper"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("must be approved by the department head first");
    }

    @Test
    void shouldPostAnIssueMovementOnFulfillLine() {
        StockIndent indent = indent(1L, StockIndentStatus.SUBMITTED);
        ProductVariant variant = variant(77L);
        StockIndentItem line = approvedLine(500L, indent, variant);

        when(requestRepository.findById(1L)).thenReturn(Optional.of(indent));
        when(lineRepository.findById(500L)).thenReturn(Optional.of(line));

        service.fulfillLine(1L, 500L, null, "store-keeper");

        verify(stockMovementService).recordMovement(org.mockito.ArgumentMatchers.argThat(
            (StockMovementRequest r) -> r.productId().equals(10L) && r.variantId().equals(77L)
                && r.locationId().equals(2L) && "ISSUE".equals(r.txnType())), eq("store-keeper"));
        assertThat(line.getStatus()).isEqualTo(StockIndentItemStatus.FULFILLED);
        assertThat(line.getStoreDecidedBy()).isEqualTo("store-keeper");
    }

    @Test
    void shouldFulfillViaTransferAndPostBothTheTransferAndTheIssue() {
        StockIndent indent = indent(1L, StockIndentStatus.SUBMITTED);
        StockIndentItem line = approvedLine(500L, indent, null);
        when(requestRepository.findById(1L)).thenReturn(Optional.of(indent));
        when(lineRepository.findById(500L)).thenReturn(Optional.of(line));
        when(stockTransferService.create(any(), eq("store-keeper"))).thenReturn(transferResponse(900L));

        var req = new StockIndentFulfillViaTransferRequest(3L, new BigDecimal("5"), "pulled from Lab Store");
        service.fulfillViaTransferLine(1L, 500L, req, "store-keeper");

        verify(stockTransferService).addLine(eq(900L), any());
        verify(stockTransferService).complete(900L, "store-keeper");
        verify(stockMovementService).recordMovement(org.mockito.ArgumentMatchers.argThat(
            (StockMovementRequest r) -> "ISSUE".equals(r.txnType())), eq("store-keeper"));
        assertThat(line.getStatus()).isEqualTo(StockIndentItemStatus.FULFILLED);
    }

    @Test
    void shouldRejectTransferFulfillmentWhenSourceIsTheIssuingLocationItself() {
        StockIndent indent = indent(1L, StockIndentStatus.SUBMITTED);
        StockIndentItem line = approvedLine(500L, indent, null);
        when(requestRepository.findById(1L)).thenReturn(Optional.of(indent));
        when(lineRepository.findById(500L)).thenReturn(Optional.of(line));

        var req = new StockIndentFulfillViaTransferRequest(2L, new BigDecimal("5"), null);
        assertThatThrownBy(() -> service.fulfillViaTransferLine(1L, 500L, req, "store-keeper"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("must be different");
    }

    @Test
    void shouldRaisePurchaseRequisitionAndMarkPoRaised() {
        StockIndent indent = indent(1L, StockIndentStatus.SUBMITTED);
        StockIndentItem line = approvedLine(500L, indent, null);
        when(requestRepository.findById(1L)).thenReturn(Optional.of(indent));
        when(lineRepository.findById(500L)).thenReturn(Optional.of(line));
        when(purchaseRequisitionService.create(any(), eq("store-keeper"))).thenReturn(requisitionResponse(700L));
        when(purchaseRequisitionService.addLine(eq(700L), any())).thenReturn(requisitionItemResponse(701L));

        var req = new StockIndentRaisePoRequest(new BigDecimal("5"), "no stock anywhere");
        service.raisePoLine(1L, 500L, req, "store-keeper");

        verify(purchaseRequisitionService).submit(700L, "store-keeper");
        assertThat(line.getStatus()).isEqualTo(StockIndentItemStatus.PO_RAISED);
        assertThat(line.getStoreDecisionNotes()).isEqualTo("no stock anywhere");
    }

    @Test
    void shouldDenyLineWithoutPostingAnyMovement() {
        StockIndent indent = indent(1L, StockIndentStatus.SUBMITTED);
        StockIndentItem line = approvedLine(500L, indent, null);
        when(requestRepository.findById(1L)).thenReturn(Optional.of(indent));
        when(lineRepository.findById(500L)).thenReturn(Optional.of(line));

        service.denyLine(1L, 500L, null, "store-keeper");

        assertThat(line.getStatus()).isEqualTo(StockIndentItemStatus.DENIED);
        verify(stockMovementService, never()).recordMovement(any(), any());
    }

    @Test
    void shouldReturnStockAgainstTheLinesVariant() {
        StockIndent indent = indent(1L, StockIndentStatus.SUBMITTED);
        ProductVariant variant = variant(77L);
        StockIndentItem line = new StockIndentItem();
        line.setId(500L);
        line.setStockIndent(indent);
        line.setProduct(product);
        line.setVariant(variant);
        line.setRequestedQty(new BigDecimal("5"));
        line.setReturnedQty(BigDecimal.ZERO);
        line.setStatus(StockIndentItemStatus.FULFILLED);

        when(requestRepository.findById(1L)).thenReturn(Optional.of(indent));
        when(lineRepository.findById(500L)).thenReturn(Optional.of(line));

        StockBalance balance = new StockBalance();
        balance.setQtyOnHand(new BigDecimal("20"));
        balance.setValueOnHand(new BigDecimal("40.00"));
        when(balanceRepository.findByProductIdAndVariantIdAndLocationIdAndBatchIsNull(10L, 77L, 2L))
            .thenReturn(Optional.of(balance));

        var req = new StockIndentReturnLineRequest(new BigDecimal("2"), null);
        service.returnLine(1L, 500L, req, "clerk");

        verify(stockMovementService).recordMovement(org.mockito.ArgumentMatchers.argThat(
            (StockMovementRequest r) -> r.productId().equals(10L) && r.variantId().equals(77L)
                && "RETURN".equals(r.txnType()) && "INCREASE".equals(r.direction())
                && r.unitCost().compareTo(new BigDecimal("2.00")) == 0), eq("clerk"));
        assertThat(line.getReturnedQty()).isEqualByComparingTo("2");
    }

    @Test
    void shouldRejectReturningALineThatIsOnlyApprovedNotYetFulfilled() {
        StockIndent indent = indent(1L, StockIndentStatus.SUBMITTED);
        StockIndentItem line = approvedLine(500L, indent, null);
        when(requestRepository.findById(1L)).thenReturn(Optional.of(indent));
        when(lineRepository.findById(500L)).thenReturn(Optional.of(line));

        var req = new StockIndentReturnLineRequest(new BigDecimal("2"), null);
        assertThatThrownBy(() -> service.returnLine(1L, 500L, req, "clerk"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Only a fulfilled");
    }

    @Test
    void shouldThrowWhenRequestNotFound() {
        when(requestRepository.findById(99L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.findById(99L)).isInstanceOf(ResourceNotFoundException.class);
    }

    // ── fixtures ─────────────────────────────────────────────────────────────

    private Product product(Long id) {
        Product p = new Product();
        p.setId(id);
        p.setProductCode("PROD-" + id);
        p.setProductName("Product " + id);
        return p;
    }

    private ProductVariant variant(Long id) {
        ProductVariant v = new ProductVariant();
        v.setId(id);
        v.setVariantCode("VAR-" + id);
        v.setVariantName("Variant " + id);
        return v;
    }

    private InventoryLocation location(Long id, String name) {
        InventoryLocation l = new InventoryLocation();
        l.setId(id);
        l.setVirtualName(name);
        return l;
    }

    private StockIndent indent(Long id, StockIndentStatus status) {
        StockIndent r = new StockIndent();
        r.setId(id);
        r.setStatus(status);
        r.setRequestingLocation(requesting);
        r.setIssuingLocation(issuing);
        r.setRequestDate(LocalDate.now());
        return r;
    }

    private StockIndentItem pendingLine(Long id, StockIndent indent) {
        StockIndentItem line = new StockIndentItem();
        line.setId(id);
        line.setStockIndent(indent);
        line.setProduct(product);
        line.setRequestedQty(new BigDecimal("5"));
        line.setStatus(StockIndentItemStatus.PENDING);
        return line;
    }

    private StockIndentItem approvedLine(Long id, StockIndent indent, ProductVariant variant) {
        StockIndentItem line = pendingLine(id, indent);
        line.setVariant(variant);
        line.setStatus(StockIndentItemStatus.APPROVED);
        return line;
    }

    private StockTransferResponse transferResponse(Long id) {
        return new StockTransferResponse(id, 3L, "Lab Store", 2L, "Main Store", "DRAFT", LocalDate.now(),
            null, "store-keeper", null, null, null, 1, List.of());
    }

    private PurchaseRequisitionResponse requisitionResponse(Long id) {
        return new PurchaseRequisitionResponse(id, 2L, "Main Store", "DRAFT", LocalDate.now(), null,
            "store-keeper", null, null, null, null, 1, 1, List.of());
    }

    private PurchaseRequisitionItemResponse requisitionItemResponse(Long id) {
        return new PurchaseRequisitionItemResponse(id, 10L, "PROD-10", "Product 10", null,
            new BigDecimal("5"), "PENDING", null, null, null, null);
    }
}
