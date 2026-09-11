package com.cms.inventory.issue.service;

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
import com.cms.inventory.issue.dto.StockIssueRequestAddLineRequest;
import com.cms.inventory.issue.dto.StockIssueRequestReturnLineRequest;
import com.cms.inventory.issue.model.StockIssueRequest;
import com.cms.inventory.issue.model.StockIssueRequestItem;
import com.cms.inventory.issue.model.enums.StockIssueRequestItemStatus;
import com.cms.inventory.issue.model.enums.StockIssueRequestStatus;
import com.cms.inventory.issue.repository.StockIssueRequestItemRepository;
import com.cms.inventory.issue.repository.StockIssueRequestRepository;
import com.cms.inventory.stock.dto.StockMovementRequest;
import com.cms.inventory.stock.model.InventoryLocation;
import com.cms.inventory.stock.repository.InventoryLocationRepository;
import com.cms.inventory.stock.service.StockMovementService;

@ExtendWith(MockitoExtension.class)
class StockIssueRequestServiceTest {

    @Mock private StockIssueRequestRepository requestRepository;
    @Mock private StockIssueRequestItemRepository lineRepository;
    @Mock private InventoryLocationRepository locationRepository;
    @Mock private ProductRepository productRepository;
    @Mock private StockMovementService stockMovementService;
    @Mock private ProductVariantRepository variantRepository;
    private StockIssueRequestService service;

    private final InventoryLocation requesting = location(1L, "Ward A");
    private final InventoryLocation issuing = location(2L, "Main Store");
    private final Product product = product(10L);

    @BeforeEach
    void setUp() {
        service = new StockIssueRequestService(requestRepository, lineRepository, locationRepository, productRepository,
            stockMovementService, variantRepository);
    }

    @Test
    void shouldRejectCreateWithSameRequestingAndIssuingLocation() {
        var req = new com.cms.inventory.issue.dto.StockIssueRequestCreateRequest(1L, 1L, LocalDate.now(), null);
        assertThatThrownBy(() -> service.create(req, "clerk"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("must be different");
    }

    @Test
    void shouldAddLineWithNoVariantWhenProductHasNone() {
        StockIssueRequest issueRequest = issueRequest(1L, StockIssueRequestStatus.DRAFT);
        when(requestRepository.findById(1L)).thenReturn(Optional.of(issueRequest));
        when(productRepository.findById(10L)).thenReturn(Optional.of(product));
        when(lineRepository.existsByStockIssueRequestIdAndProductIdAndVariantIsNull(1L, 10L)).thenReturn(false);
        when(lineRepository.save(any(StockIssueRequestItem.class))).thenAnswer(inv -> { StockIssueRequestItem l = inv.getArgument(0); l.setId(50L); return l; });

        var req = new StockIssueRequestAddLineRequest(10L, null, new BigDecimal("5"), null);
        var res = service.addLine(1L, req);

        assertThat(res.variantId()).isNull();
    }

    @Test
    void shouldRejectAddLineWhenProductHasActiveVariantsButNoneGiven() {
        StockIssueRequest issueRequest = issueRequest(1L, StockIssueRequestStatus.DRAFT);
        when(requestRepository.findById(1L)).thenReturn(Optional.of(issueRequest));
        when(productRepository.findById(10L)).thenReturn(Optional.of(product));
        when(variantRepository.existsByProductIdAndIsActiveTrue(10L)).thenReturn(true);

        var req = new StockIssueRequestAddLineRequest(10L, null, new BigDecimal("5"), null);
        assertThatThrownBy(() -> service.addLine(1L, req))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("has active variants");
    }

    @Test
    void shouldRejectDuplicateLineForTheSameVariant() {
        StockIssueRequest issueRequest = issueRequest(1L, StockIssueRequestStatus.DRAFT);
        ProductVariant variant = variant(77L);
        when(requestRepository.findById(1L)).thenReturn(Optional.of(issueRequest));
        when(productRepository.findById(10L)).thenReturn(Optional.of(product));
        when(variantRepository.findByIdAndProductId(77L, 10L)).thenReturn(Optional.of(variant));
        when(lineRepository.existsByStockIssueRequestIdAndProductIdAndVariantId(1L, 10L, 77L)).thenReturn(true);

        var req = new StockIssueRequestAddLineRequest(10L, 77L, new BigDecimal("5"), null);
        assertThatThrownBy(() -> service.addLine(1L, req))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("already on the request");
    }

    @Test
    void shouldPostAnIssueMovementAgainstTheLinesVariantOnApprove() {
        StockIssueRequest issueRequest = issueRequest(1L, StockIssueRequestStatus.SUBMITTED);
        ProductVariant variant = variant(77L);
        StockIssueRequestItem line = new StockIssueRequestItem();
        line.setId(500L);
        line.setStockIssueRequest(issueRequest);
        line.setProduct(product);
        line.setVariant(variant);
        line.setRequestedQty(new BigDecimal("5"));
        line.setStatus(StockIssueRequestItemStatus.PENDING);

        when(requestRepository.findById(1L)).thenReturn(Optional.of(issueRequest));
        when(lineRepository.findById(500L)).thenReturn(Optional.of(line));
        when(lineRepository.existsByStockIssueRequestIdAndStatus(1L, StockIssueRequestItemStatus.PENDING)).thenReturn(false);

        service.approveLine(1L, 500L, null, "clerk");

        verify(stockMovementService).recordMovement(org.mockito.ArgumentMatchers.argThat(
            (StockMovementRequest r) -> r.productId().equals(10L) && r.variantId().equals(77L)
                && r.locationId().equals(2L) && "ISSUE".equals(r.txnType())), eq("clerk"));
        assertThat(line.getStatus()).isEqualTo(StockIssueRequestItemStatus.APPROVED);
    }

    @Test
    void shouldRejectApprovingAnAlreadyResolvedLine() {
        StockIssueRequest issueRequest = issueRequest(1L, StockIssueRequestStatus.SUBMITTED);
        StockIssueRequestItem line = new StockIssueRequestItem();
        line.setId(500L);
        line.setStockIssueRequest(issueRequest);
        line.setStatus(StockIssueRequestItemStatus.APPROVED);
        when(requestRepository.findById(1L)).thenReturn(Optional.of(issueRequest));
        when(lineRepository.findById(500L)).thenReturn(Optional.of(line));

        assertThatThrownBy(() -> service.approveLine(1L, 500L, null, "clerk"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("already been resolved");
        verify(stockMovementService, never()).recordMovement(any(), any());
    }

    @Test
    void shouldReturnStockAgainstTheLinesVariant() {
        StockIssueRequest issueRequest = issueRequest(1L, StockIssueRequestStatus.SUBMITTED);
        ProductVariant variant = variant(77L);
        StockIssueRequestItem line = new StockIssueRequestItem();
        line.setId(500L);
        line.setStockIssueRequest(issueRequest);
        line.setProduct(product);
        line.setVariant(variant);
        line.setRequestedQty(new BigDecimal("5"));
        line.setReturnedQty(BigDecimal.ZERO);
        line.setStatus(StockIssueRequestItemStatus.APPROVED);

        when(requestRepository.findById(1L)).thenReturn(Optional.of(issueRequest));
        when(lineRepository.findById(500L)).thenReturn(Optional.of(line));

        var req = new StockIssueRequestReturnLineRequest(new BigDecimal("2"), null);
        service.returnLine(1L, 500L, req, "clerk");

        verify(stockMovementService).recordMovement(org.mockito.ArgumentMatchers.argThat(
            (StockMovementRequest r) -> r.productId().equals(10L) && r.variantId().equals(77L)
                && "RETURN".equals(r.txnType()) && "INCREASE".equals(r.direction())), eq("clerk"));
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

    private StockIssueRequest issueRequest(Long id, StockIssueRequestStatus status) {
        StockIssueRequest r = new StockIssueRequest();
        r.setId(id);
        r.setStatus(status);
        r.setRequestingLocation(requesting);
        r.setIssuingLocation(issuing);
        r.setRequestDate(LocalDate.now());
        return r;
    }
}
