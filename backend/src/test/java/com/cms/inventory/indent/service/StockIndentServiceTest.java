package com.cms.inventory.indent.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
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
import com.cms.inventory.indent.dto.StockIndentReturnLineRequest;
import com.cms.inventory.indent.model.StockIndent;
import com.cms.inventory.indent.model.StockIndentItem;
import com.cms.inventory.indent.model.enums.StockIndentItemStatus;
import com.cms.inventory.indent.model.enums.StockIndentStatus;
import com.cms.inventory.indent.repository.StockIndentItemRepository;
import com.cms.inventory.indent.repository.StockIndentRepository;
import com.cms.inventory.stock.dto.StockMovementRequest;
import com.cms.inventory.stock.model.InventoryLocation;
import com.cms.inventory.stock.model.StockBalance;
import com.cms.inventory.stock.repository.InventoryLocationRepository;
import com.cms.inventory.stock.repository.StockBalanceRepository;
import com.cms.inventory.stock.service.StockMovementService;

@ExtendWith(MockitoExtension.class)
class StockIndentServiceTest {

    @Mock private StockIndentRepository requestRepository;
    @Mock private StockIndentItemRepository lineRepository;
    @Mock private InventoryLocationRepository locationRepository;
    @Mock private ProductRepository productRepository;
    @Mock private StockMovementService stockMovementService;
    @Mock private ProductVariantRepository variantRepository;
    @Mock private StockBalanceRepository balanceRepository;
    private StockIndentService service;

    private final InventoryLocation requesting = location(1L, "Ward A");
    private final InventoryLocation issuing = location(2L, "Main Store");
    private final Product product = product(10L);

    @BeforeEach
    void setUp() {
        service = new StockIndentService(requestRepository, lineRepository, locationRepository, productRepository,
            stockMovementService, variantRepository, balanceRepository);
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
    void shouldPostAnIssueMovementAgainstTheLinesVariantOnApprove() {
        StockIndent indent = indent(1L, StockIndentStatus.SUBMITTED);
        ProductVariant variant = variant(77L);
        StockIndentItem line = new StockIndentItem();
        line.setId(500L);
        line.setStockIndent(indent);
        line.setProduct(product);
        line.setVariant(variant);
        line.setRequestedQty(new BigDecimal("5"));
        line.setStatus(StockIndentItemStatus.PENDING);

        when(requestRepository.findById(1L)).thenReturn(Optional.of(indent));
        when(lineRepository.findById(500L)).thenReturn(Optional.of(line));
        when(lineRepository.existsByStockIndentIdAndStatus(1L, StockIndentItemStatus.PENDING)).thenReturn(false);

        service.approveLine(1L, 500L, null, "clerk");

        verify(stockMovementService).recordMovement(org.mockito.ArgumentMatchers.argThat(
            (StockMovementRequest r) -> r.productId().equals(10L) && r.variantId().equals(77L)
                && r.locationId().equals(2L) && "ISSUE".equals(r.txnType())), eq("clerk"));
        assertThat(line.getStatus()).isEqualTo(StockIndentItemStatus.APPROVED);
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
        line.setStatus(StockIndentItemStatus.APPROVED);

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
}
