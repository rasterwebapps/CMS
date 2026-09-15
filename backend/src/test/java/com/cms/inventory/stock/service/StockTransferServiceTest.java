package com.cms.inventory.stock.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
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
import com.cms.inventory.stock.dto.StockTransferAddLineRequest;
import com.cms.inventory.stock.dto.StockTransferCreateRequest;
import com.cms.inventory.stock.model.InventoryLocation;
import com.cms.inventory.stock.model.StockBalance;
import com.cms.inventory.stock.model.StockTransfer;
import com.cms.inventory.stock.model.StockTransferLine;
import com.cms.inventory.stock.model.enums.StockTransferStatus;
import com.cms.inventory.stock.repository.InventoryBinRepository;
import com.cms.inventory.stock.repository.InventoryLocationRepository;
import com.cms.inventory.stock.repository.StockBalanceRepository;
import com.cms.inventory.stock.repository.StockTransferLineRepository;
import com.cms.inventory.stock.repository.StockTransferRepository;

@ExtendWith(MockitoExtension.class)
class StockTransferServiceTest {

    @Mock private StockTransferRepository transferRepository;
    @Mock private StockTransferLineRepository lineRepository;
    @Mock private InventoryLocationRepository locationRepository;
    @Mock private ProductRepository productRepository;
    @Mock private StockBalanceRepository balanceRepository;
    @Mock private StockMovementService stockMovementService;
    @Mock private ProductVariantRepository variantRepository;
    @Mock private InventoryBinRepository binRepository;
    private StockTransferService service;

    private final InventoryLocation source = location(1L, "Main Store");
    private final InventoryLocation destination = location(2L, "Ward Store");
    private final Product product = product(10L);

    @BeforeEach
    void setUp() {
        service = new StockTransferService(transferRepository, lineRepository, locationRepository, productRepository,
            balanceRepository, stockMovementService, variantRepository, binRepository);
    }

    @Test
    void shouldRejectCreateWithSameSourceAndDestination() {
        var req = new StockTransferCreateRequest(1L, 1L, LocalDate.now(), null);
        assertThatThrownBy(() -> service.create(req, "clerk"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("must be different");
    }

    @Test
    void shouldAddLineWithNoVariantWhenProductHasNone() {
        StockTransfer transfer = transfer(1L, StockTransferStatus.DRAFT, source, destination);
        when(transferRepository.findById(1L)).thenReturn(Optional.of(transfer));
        when(productRepository.findById(10L)).thenReturn(Optional.of(product));
        when(lineRepository.existsByStockTransferIdAndProductIdAndVariantIsNull(1L, 10L)).thenReturn(false);
        when(lineRepository.save(any(StockTransferLine.class))).thenAnswer(inv -> { StockTransferLine l = inv.getArgument(0); l.setId(50L); return l; });

        var req = new StockTransferAddLineRequest(10L, null, new BigDecimal("5"), null, null, null);
        var res = service.addLine(1L, req);

        assertThat(res.variantId()).isNull();
        assertThat(res.quantity()).isEqualByComparingTo("5");
    }

    @Test
    void shouldRejectAddLineWhenProductHasActiveVariantsButNoneGiven() {
        StockTransfer transfer = transfer(1L, StockTransferStatus.DRAFT, source, destination);
        when(transferRepository.findById(1L)).thenReturn(Optional.of(transfer));
        when(productRepository.findById(10L)).thenReturn(Optional.of(product));
        when(variantRepository.existsByProductIdAndIsActiveTrue(10L)).thenReturn(true);

        var req = new StockTransferAddLineRequest(10L, null, new BigDecimal("5"), null, null, null);
        assertThatThrownBy(() -> service.addLine(1L, req))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("has active variants");
    }

    @Test
    void shouldRejectAVariantThatBelongsToADifferentProduct() {
        StockTransfer transfer = transfer(1L, StockTransferStatus.DRAFT, source, destination);
        when(transferRepository.findById(1L)).thenReturn(Optional.of(transfer));
        when(productRepository.findById(10L)).thenReturn(Optional.of(product));
        when(variantRepository.findByIdAndProductId(77L, 10L)).thenReturn(Optional.empty());

        var req = new StockTransferAddLineRequest(10L, 77L, new BigDecimal("5"), null, null, null);
        assertThatThrownBy(() -> service.addLine(1L, req))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("does not belong to");
    }

    @Test
    void shouldAddLineAgainstAGivenVariantAndCheckVariantScopedDuplicate() {
        StockTransfer transfer = transfer(1L, StockTransferStatus.DRAFT, source, destination);
        ProductVariant variant = variant(77L);
        when(transferRepository.findById(1L)).thenReturn(Optional.of(transfer));
        when(productRepository.findById(10L)).thenReturn(Optional.of(product));
        when(variantRepository.findByIdAndProductId(77L, 10L)).thenReturn(Optional.of(variant));
        when(lineRepository.existsByStockTransferIdAndProductIdAndVariantId(1L, 10L, 77L)).thenReturn(false);
        when(lineRepository.save(any(StockTransferLine.class))).thenAnswer(inv -> { StockTransferLine l = inv.getArgument(0); l.setId(51L); return l; });

        var req = new StockTransferAddLineRequest(10L, 77L, new BigDecimal("5"), null, null, null);
        var res = service.addLine(1L, req);

        assertThat(res.variantId()).isEqualTo(77L);
        verify(lineRepository, never()).existsByStockTransferIdAndProductIdAndVariantIsNull(any(), any());
    }

    @Test
    void shouldRejectDuplicateLineForTheSameVariant() {
        StockTransfer transfer = transfer(1L, StockTransferStatus.DRAFT, source, destination);
        ProductVariant variant = variant(77L);
        when(transferRepository.findById(1L)).thenReturn(Optional.of(transfer));
        when(productRepository.findById(10L)).thenReturn(Optional.of(product));
        when(variantRepository.findByIdAndProductId(77L, 10L)).thenReturn(Optional.of(variant));
        when(lineRepository.existsByStockTransferIdAndProductIdAndVariantId(1L, 10L, 77L)).thenReturn(true);

        var req = new StockTransferAddLineRequest(10L, 77L, new BigDecimal("5"), null, null, null);
        assertThatThrownBy(() -> service.addLine(1L, req))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("already on the transfer");
    }

    @Test
    void shouldPostBothLegsWithTheLinesVariantAndItsWeightedAverageCost() {
        StockTransfer transfer = transfer(1L, StockTransferStatus.DRAFT, source, destination);
        ProductVariant variant = variant(77L);
        StockTransferLine line = new StockTransferLine();
        line.setId(50L);
        line.setProduct(product);
        line.setVariant(variant);
        line.setQuantity(new BigDecimal("5"));

        when(transferRepository.findById(1L)).thenReturn(Optional.of(transfer));
        when(lineRepository.findByStockTransferIdOrderByIdAsc(1L)).thenReturn(List.of(line));
        StockBalance balance = new StockBalance();
        balance.setQtyOnHand(new BigDecimal("20"));
        balance.setValueOnHand(new BigDecimal("40.00"));
        when(balanceRepository.findByProductIdAndVariantIdAndLocationIdAndBatchIsNull(10L, 77L, 1L)).thenReturn(Optional.of(balance));

        service.complete(1L, "clerk");

        verify(stockMovementService).recordMovement(org.mockito.ArgumentMatchers.argThat(
            (com.cms.inventory.stock.dto.StockMovementRequest r) ->
                r.productId().equals(10L) && r.variantId().equals(77L) && r.locationId().equals(1L)
                    && "DECREASE".equals(r.direction()) && r.unitCost().compareTo(new BigDecimal("2.00")) == 0), eq("clerk"));
        verify(stockMovementService).recordMovement(org.mockito.ArgumentMatchers.argThat(
            (com.cms.inventory.stock.dto.StockMovementRequest r) ->
                r.productId().equals(10L) && r.variantId().equals(77L) && r.locationId().equals(2L)
                    && "INCREASE".equals(r.direction())), eq("clerk"));
        assertThat(transfer.getStatus()).isEqualTo(StockTransferStatus.COMPLETED);
    }

    @Test
    void shouldRejectCompleteWithNoLines() {
        StockTransfer transfer = transfer(1L, StockTransferStatus.DRAFT, source, destination);
        when(transferRepository.findById(1L)).thenReturn(Optional.of(transfer));
        when(lineRepository.findByStockTransferIdOrderByIdAsc(1L)).thenReturn(List.of());

        assertThatThrownBy(() -> service.complete(1L, "clerk"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Add at least one line");
        verify(stockMovementService, never()).recordMovement(any(), any());
    }

    @Test
    void shouldCancelADraftTransfer() {
        StockTransfer transfer = transfer(1L, StockTransferStatus.DRAFT, source, destination);
        when(transferRepository.findById(1L)).thenReturn(Optional.of(transfer));

        service.cancel(1L);

        assertThat(transfer.getStatus()).isEqualTo(StockTransferStatus.CANCELLED);
        verify(transferRepository, times(1)).save(transfer);
    }

    @Test
    void shouldThrowWhenTransferNotFound() {
        when(transferRepository.findById(99L)).thenReturn(Optional.empty());
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

    private StockTransfer transfer(Long id, StockTransferStatus status, InventoryLocation source, InventoryLocation destination) {
        StockTransfer t = new StockTransfer();
        t.setId(id);
        t.setStatus(status);
        t.setSourceLocation(source);
        t.setDestinationLocation(destination);
        t.setTransferDate(LocalDate.now());
        return t;
    }
}
