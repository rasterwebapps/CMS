package com.cms.inventory.stock.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
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
import com.cms.inventory.catalog.model.ProductUomChainVersion;
import com.cms.inventory.catalog.model.enums.StockTrackingMode;
import com.cms.inventory.catalog.repository.ProductRepository;
import com.cms.inventory.catalog.repository.ProductUomChainVersionRepository;
import com.cms.inventory.stock.dto.StockMovementRequest;
import com.cms.inventory.stock.model.InventoryLocation;
import com.cms.inventory.stock.model.StockBalance;
import com.cms.inventory.stock.model.StockBatch;
import com.cms.inventory.stock.model.StockLedger;
import com.cms.inventory.stock.repository.InventoryLocationRepository;
import com.cms.inventory.stock.repository.StockBalanceRepository;
import com.cms.inventory.stock.repository.StockBatchRepository;
import com.cms.inventory.stock.repository.StockLedgerRepository;

@ExtendWith(MockitoExtension.class)
class StockMovementServiceTest {

    @Mock private ProductRepository productRepository;
    @Mock private InventoryLocationRepository locationRepository;
    @Mock private StockBatchRepository batchRepository;
    @Mock private StockLedgerRepository ledgerRepository;
    @Mock private StockBalanceRepository balanceRepository;
    @Mock private ProductUomChainVersionRepository uomChainVersionRepository;
    private StockMovementService service;

    private final Product product = product(10L);
    private final InventoryLocation location = location(1L);

    @BeforeEach
    void setUp() {
        service = new StockMovementService(productRepository, locationRepository, batchRepository, ledgerRepository,
            balanceRepository, uomChainVersionRepository);
        // lenient: several tests below throw before ever reaching the ledger save.
        lenient().when(ledgerRepository.save(any(StockLedger.class))).thenAnswer(inv -> { StockLedger l = inv.getArgument(0); l.setId(900L); return l; });
    }

    @Test
    void shouldRecordReceiptAsAPositiveQtyDeltaOnANewBalance() {
        when(productRepository.findById(10L)).thenReturn(Optional.of(product));
        when(locationRepository.findById(1L)).thenReturn(Optional.of(location));
        when(balanceRepository.findByProductIdAndLocationIdAndBatchIsNull(10L, 1L)).thenReturn(Optional.empty());

        var req = new StockMovementRequest(10L, 1L, null, null, "RECEIPT", null, new BigDecimal("50"), new BigDecimal("2.00"), null);
        var res = service.recordMovement(req, "clerk");

        assertThat(res.qtyDelta()).isEqualByComparingTo("50");
        assertThat(res.newQtyOnHand()).isEqualByComparingTo("50");
        verify(balanceRepository).upsertBalance(10L, 1L, null, new BigDecimal("50"), new BigDecimal("100.00"));
    }

    @Test
    void shouldStampANewlyCreatedBatchWithTheProductsActiveChainVersion() {
        ProductUomChainVersion activeVersion = new ProductUomChainVersion();
        activeVersion.setId(77L);
        when(productRepository.findById(10L)).thenReturn(Optional.of(product));
        when(locationRepository.findById(1L)).thenReturn(Optional.of(location));
        when(batchRepository.findByProductAndBatchOrSerialNo(10L, "BATCH-1")).thenReturn(Optional.empty());
        when(uomChainVersionRepository.findByProductIdAndIsActiveTrue(10L)).thenReturn(Optional.of(activeVersion));
        when(batchRepository.save(any(StockBatch.class))).thenAnswer(inv -> { StockBatch b = inv.getArgument(0); b.setId(200L); return b; });
        when(balanceRepository.findByProductIdAndLocationIdAndBatchId(10L, 1L, 200L)).thenReturn(Optional.empty());

        var req = new StockMovementRequest(10L, 1L, "BATCH-1", LocalDate.of(2027, 1, 1), "RECEIPT", null, new BigDecimal("10"), BigDecimal.ONE, null);
        service.recordMovement(req, "clerk");

        org.mockito.ArgumentCaptor<StockBatch> captor = org.mockito.ArgumentCaptor.forClass(StockBatch.class);
        verify(batchRepository).save(captor.capture());
        assertThat(captor.getValue().getChainVersion()).isSameAs(activeVersion);
    }

    @Test
    void shouldLeaveChainVersionNullWhenProductHasNoActiveChain() {
        when(productRepository.findById(10L)).thenReturn(Optional.of(product));
        when(locationRepository.findById(1L)).thenReturn(Optional.of(location));
        when(batchRepository.findByProductAndBatchOrSerialNo(10L, "BATCH-2")).thenReturn(Optional.empty());
        when(uomChainVersionRepository.findByProductIdAndIsActiveTrue(10L)).thenReturn(Optional.empty());
        when(batchRepository.save(any(StockBatch.class))).thenAnswer(inv -> { StockBatch b = inv.getArgument(0); b.setId(201L); return b; });
        when(balanceRepository.findByProductIdAndLocationIdAndBatchId(10L, 1L, 201L)).thenReturn(Optional.empty());

        var req = new StockMovementRequest(10L, 1L, "BATCH-2", null, "RECEIPT", null, new BigDecimal("10"), BigDecimal.ONE, null);
        service.recordMovement(req, "clerk");

        org.mockito.ArgumentCaptor<StockBatch> captor = org.mockito.ArgumentCaptor.forClass(StockBatch.class);
        verify(batchRepository).save(captor.capture());
        assertThat(captor.getValue().getChainVersion()).isNull();
    }

    @Test
    void shouldNotResolveChainVersionWhenReusingAnExistingBatch() {
        StockBatch existingBatch = new StockBatch();
        existingBatch.setId(300L);
        when(productRepository.findById(10L)).thenReturn(Optional.of(product));
        when(locationRepository.findById(1L)).thenReturn(Optional.of(location));
        when(batchRepository.findByProductAndBatchOrSerialNo(10L, "BATCH-3")).thenReturn(Optional.of(existingBatch));
        when(balanceRepository.findByProductIdAndLocationIdAndBatchId(10L, 1L, 300L)).thenReturn(Optional.empty());

        var req = new StockMovementRequest(10L, 1L, "BATCH-3", null, "RECEIPT", null, new BigDecimal("10"), BigDecimal.ONE, null);
        service.recordMovement(req, "clerk");

        verify(uomChainVersionRepository, never()).findByProductIdAndIsActiveTrue(any());
        verify(batchRepository, never()).save(any());
    }

    @Test
    void shouldNotResolveABatchWhenNoBatchOrSerialNoGiven() {
        when(productRepository.findById(10L)).thenReturn(Optional.of(product));
        when(locationRepository.findById(1L)).thenReturn(Optional.of(location));
        when(balanceRepository.findByProductIdAndLocationIdAndBatchIsNull(10L, 1L)).thenReturn(Optional.empty());

        var req = new StockMovementRequest(10L, 1L, null, null, "RECEIPT", null, new BigDecimal("10"), BigDecimal.ONE, null);
        service.recordMovement(req, "clerk");

        verify(batchRepository, never()).findByProductAndBatchOrSerialNo(any(), any());
    }

    // ── Product.trackingMode enforcement (2026-09-11 "Serial/batch tracking-mode flag") ───────

    @Test
    void shouldRejectMovementOnBatchTrackedProductWithNoBatchNumber() {
        Product batchTracked = product(20L);
        batchTracked.setTrackingMode(StockTrackingMode.BATCH);
        when(productRepository.findById(20L)).thenReturn(Optional.of(batchTracked));
        when(locationRepository.findById(1L)).thenReturn(Optional.of(location));

        var req = new StockMovementRequest(20L, 1L, null, null, "RECEIPT", null, new BigDecimal("10"), BigDecimal.ONE, null);
        assertThatThrownBy(() -> service.recordMovement(req, "clerk"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("batch number is required");
    }

    @Test
    void shouldRejectMovementOnSerialTrackedProductWithNoSerialNumber() {
        Product serialTracked = product(21L);
        serialTracked.setTrackingMode(StockTrackingMode.SERIAL);
        when(productRepository.findById(21L)).thenReturn(Optional.of(serialTracked));
        when(locationRepository.findById(1L)).thenReturn(Optional.of(location));

        var req = new StockMovementRequest(21L, 1L, null, null, "RECEIPT", null, BigDecimal.ONE, BigDecimal.ONE, null);
        assertThatThrownBy(() -> service.recordMovement(req, "clerk"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("serial number is required");
    }

    @Test
    void shouldRejectSerialTrackedMovementWithQuantityOtherThanOne() {
        Product serialTracked = product(22L);
        serialTracked.setTrackingMode(StockTrackingMode.SERIAL);
        when(productRepository.findById(22L)).thenReturn(Optional.of(serialTracked));
        when(locationRepository.findById(1L)).thenReturn(Optional.of(location));

        var req = new StockMovementRequest(22L, 1L, "SN-001", null, "RECEIPT", null, new BigDecimal("2"), BigDecimal.ONE, null);
        assertThatThrownBy(() -> service.recordMovement(req, "clerk"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("quantity must be exactly 1");
    }

    @Test
    void shouldAllowSerialTrackedMovementWithOneUnitAndSerialNumber() {
        Product serialTracked = product(23L);
        serialTracked.setTrackingMode(StockTrackingMode.SERIAL);
        when(productRepository.findById(23L)).thenReturn(Optional.of(serialTracked));
        when(locationRepository.findById(1L)).thenReturn(Optional.of(location));
        when(batchRepository.findByProductAndBatchOrSerialNo(23L, "SN-001")).thenReturn(Optional.empty());
        when(uomChainVersionRepository.findByProductIdAndIsActiveTrue(23L)).thenReturn(Optional.empty());
        when(batchRepository.save(any(StockBatch.class))).thenAnswer(inv -> { StockBatch b = inv.getArgument(0); b.setId(400L); return b; });
        when(balanceRepository.findByProductIdAndLocationIdAndBatchId(23L, 1L, 400L)).thenReturn(Optional.empty());

        var req = new StockMovementRequest(23L, 1L, "SN-001", null, "RECEIPT", null, BigDecimal.ONE, BigDecimal.ONE, null);
        var res = service.recordMovement(req, "clerk");

        assertThat(res.qtyDelta()).isEqualByComparingTo("1");
    }

    @Test
    void shouldAllowBatchTrackedMovementWithMultipleUnitsGivenABatchNumber() {
        Product batchTracked = product(24L);
        batchTracked.setTrackingMode(StockTrackingMode.BATCH);
        when(productRepository.findById(24L)).thenReturn(Optional.of(batchTracked));
        when(locationRepository.findById(1L)).thenReturn(Optional.of(location));
        when(batchRepository.findByProductAndBatchOrSerialNo(24L, "LOT-1")).thenReturn(Optional.empty());
        when(uomChainVersionRepository.findByProductIdAndIsActiveTrue(24L)).thenReturn(Optional.empty());
        when(batchRepository.save(any(StockBatch.class))).thenAnswer(inv -> { StockBatch b = inv.getArgument(0); b.setId(401L); return b; });
        when(balanceRepository.findByProductIdAndLocationIdAndBatchId(24L, 1L, 401L)).thenReturn(Optional.empty());

        var req = new StockMovementRequest(24L, 1L, "LOT-1", null, "RECEIPT", null, new BigDecimal("50"), BigDecimal.ONE, null);
        var res = service.recordMovement(req, "clerk");

        assertThat(res.qtyDelta()).isEqualByComparingTo("50");
    }

    @Test
    void shouldRejectMovementResultingInNegativeBalance() {
        when(productRepository.findById(10L)).thenReturn(Optional.of(product));
        when(locationRepository.findById(1L)).thenReturn(Optional.of(location));
        when(balanceRepository.findByProductIdAndLocationIdAndBatchIsNull(10L, 1L)).thenReturn(Optional.of(balance(new BigDecimal("5"), new BigDecimal("10"))));

        var req = new StockMovementRequest(10L, 1L, null, null, "ISSUE", null, new BigDecimal("20"), null, null);
        assertThatThrownBy(() -> service.recordMovement(req, "clerk"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("negative on-hand quantity");
    }

    @Test
    void shouldUseWeightedAverageCostOnDecreaseWhenNoUnitCostGiven() {
        when(productRepository.findById(10L)).thenReturn(Optional.of(product));
        when(locationRepository.findById(1L)).thenReturn(Optional.of(location));
        // 100 units on hand worth 250 total -> weighted average 2.50/unit.
        when(balanceRepository.findByProductIdAndLocationIdAndBatchIsNull(10L, 1L)).thenReturn(Optional.of(balance(new BigDecimal("100"), new BigDecimal("250.00"))));

        var req = new StockMovementRequest(10L, 1L, null, null, "ISSUE", null, new BigDecimal("10"), null, null);
        service.recordMovement(req, "clerk");

        // 10 units x 2.50 = 25.00 removed.
        verify(balanceRepository).upsertBalance(10L, 1L, null, new BigDecimal("-10"), new BigDecimal("-25.00"));
    }

    @Test
    void shouldTreatAdjustmentIncreaseDirectionAsPositive() {
        when(productRepository.findById(10L)).thenReturn(Optional.of(product));
        when(locationRepository.findById(1L)).thenReturn(Optional.of(location));
        when(balanceRepository.findByProductIdAndLocationIdAndBatchIsNull(10L, 1L)).thenReturn(Optional.empty());

        var req = new StockMovementRequest(10L, 1L, null, null, "ADJUSTMENT", "INCREASE", new BigDecimal("5"), BigDecimal.ONE, null);
        assertThat(service.recordMovement(req, "clerk").qtyDelta()).isEqualByComparingTo("5");
    }

    @Test
    void shouldTreatAdjustmentDecreaseDirectionAsNegative() {
        when(productRepository.findById(10L)).thenReturn(Optional.of(product));
        when(locationRepository.findById(1L)).thenReturn(Optional.of(location));
        when(balanceRepository.findByProductIdAndLocationIdAndBatchIsNull(10L, 1L)).thenReturn(Optional.of(balance(new BigDecimal("10"), new BigDecimal("10"))));

        var req = new StockMovementRequest(10L, 1L, null, null, "ADJUSTMENT", "DECREASE", new BigDecimal("5"), null, null);
        assertThat(service.recordMovement(req, "clerk").qtyDelta()).isEqualByComparingTo("-5");
    }

    @Test
    void shouldRejectInvalidTxnTypeValue() {
        when(productRepository.findById(10L)).thenReturn(Optional.of(product));
        when(locationRepository.findById(1L)).thenReturn(Optional.of(location));
        var req = new StockMovementRequest(10L, 1L, null, null, "NOT_A_TYPE", null, BigDecimal.ONE, null, null);
        assertThatThrownBy(() -> service.recordMovement(req, "clerk")).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void shouldRejectTxnTypeNotYetReachableThroughThisApi() {
        when(productRepository.findById(10L)).thenReturn(Optional.of(product));
        when(locationRepository.findById(1L)).thenReturn(Optional.of(location));
        var req = new StockMovementRequest(10L, 1L, null, null, "CONSIGNMENT_CONSUMPTION", null, BigDecimal.ONE, null, null);
        assertThatThrownBy(() -> service.recordMovement(req, "clerk"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("not yet available");
    }

    @Test
    void shouldThrowWhenProductNotFound() {
        when(productRepository.findById(99L)).thenReturn(Optional.empty());
        var req = new StockMovementRequest(99L, 1L, null, null, "RECEIPT", null, BigDecimal.ONE, null, null);
        assertThatThrownBy(() -> service.recordMovement(req, "clerk")).isInstanceOf(ResourceNotFoundException.class);
    }

    // ── fixtures ─────────────────────────────────────────────────────────────

    private Product product(Long id) {
        Product p = new Product();
        p.setId(id);
        p.setProductCode("PROD-" + id);
        p.setProductName("Product " + id);
        return p;
    }

    private InventoryLocation location(Long id) {
        InventoryLocation l = new InventoryLocation();
        l.setId(id);
        l.setVirtualName("Store " + id);
        return l;
    }

    private StockBalance balance(BigDecimal qty, BigDecimal value) {
        StockBalance b = new StockBalance();
        b.setQtyOnHand(qty);
        b.setValueOnHand(value);
        return b;
    }
}
