package com.cms.inventory.procurement.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.lenient;
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

import com.cms.inventory.catalog.model.Product;
import com.cms.inventory.procurement.dto.PurchaseOrderResponse;
import com.cms.inventory.procurement.dto.QuotationRequestAddLineRequest;
import com.cms.inventory.procurement.dto.QuotationRequestAddSupplierRequest;
import com.cms.inventory.procurement.dto.QuotationRequestAwardRequest;
import com.cms.inventory.procurement.dto.QuotationRequestCreateRequest;
import com.cms.inventory.procurement.dto.QuotationResponseLineRequest;
import com.cms.inventory.procurement.model.PurchaseRequisition;
import com.cms.inventory.procurement.model.PurchaseRequisitionItem;
import com.cms.inventory.procurement.model.QuotationRequest;
import com.cms.inventory.procurement.model.QuotationRequestLine;
import com.cms.inventory.procurement.model.QuotationRequestSupplier;
import com.cms.inventory.procurement.model.QuotationResponseLine;
import com.cms.inventory.procurement.model.Supplier;
import com.cms.inventory.procurement.model.enums.PurchaseRequisitionItemStatus;
import com.cms.inventory.procurement.model.enums.QuotationRequestLineStatus;
import com.cms.inventory.procurement.model.enums.QuotationRequestStatus;
import com.cms.inventory.procurement.repository.PurchaseRequisitionItemRepository;
import com.cms.inventory.procurement.repository.QuotationRequestLineRepository;
import com.cms.inventory.procurement.repository.QuotationRequestRepository;
import com.cms.inventory.procurement.repository.QuotationRequestSupplierRepository;
import com.cms.inventory.procurement.repository.QuotationResponseLineRepository;
import com.cms.inventory.procurement.repository.SupplierRepository;
import com.cms.inventory.stock.model.InventoryLocation;
import com.cms.inventory.stock.repository.InventoryLocationRepository;

@ExtendWith(MockitoExtension.class)
class QuotationRequestServiceTest {

    @Mock private QuotationRequestRepository requestRepository;
    @Mock private QuotationRequestLineRepository lineRepository;
    @Mock private QuotationRequestSupplierRepository supplierLinkRepository;
    @Mock private QuotationResponseLineRepository responseRepository;
    @Mock private PurchaseRequisitionItemRepository requisitionItemRepository;
    @Mock private SupplierRepository supplierRepository;
    @Mock private InventoryLocationRepository locationRepository;
    @Mock private PurchaseOrderService purchaseOrderService;
    private QuotationRequestService service;

    private final InventoryLocation location = location(1L, "Main Store");
    private final Supplier supplierA = supplier(10L, "Acme Supplies");
    private final Supplier supplierB = supplier(20L, "Bharat Traders");

    @BeforeEach
    void setUp() {
        service = new QuotationRequestService(requestRepository, lineRepository, supplierLinkRepository,
            responseRepository, requisitionItemRepository, supplierRepository, locationRepository, purchaseOrderService);
        lenient().when(requestRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        lenient().when(lineRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        lenient().when(supplierLinkRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        lenient().when(responseRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        lenient().when(lineRepository.findByQuotationRequestIdOrderByIdAsc(any())).thenReturn(List.of());
        lenient().when(supplierLinkRepository.findByQuotationRequestIdOrderByIdAsc(any())).thenReturn(List.of());
        lenient().when(responseRepository.findByQuotationRequestLineIdOrderByIdAsc(any())).thenReturn(List.of());
    }

    // ── create ───────────────────────────────────────────────────────────────

    @Test
    void shouldCreateInDraftStatus() {
        when(locationRepository.findById(1L)).thenReturn(Optional.of(location));
        var res = service.create(new QuotationRequestCreateRequest(1L, LocalDate.now(), "notes"), "buyer");
        assertThat(res.status()).isEqualTo("DRAFT");
        assertThat(res.locationId()).isEqualTo(1L);
    }

    // ── addLine ──────────────────────────────────────────────────────────────

    @Test
    void shouldAddLineFromApprovedRequisitionItem() {
        QuotationRequest request = quotationRequest(1L, QuotationRequestStatus.DRAFT, location);
        PurchaseRequisitionItem reqItem = requisitionItem(50L, product(5L), new BigDecimal("10"), location);
        when(requestRepository.findById(1L)).thenReturn(Optional.of(request));
        when(requisitionItemRepository.findById(50L)).thenReturn(Optional.of(reqItem));
        when(lineRepository.existsByPurchaseRequisitionItemIdAndStatusNot(50L, QuotationRequestLineStatus.REJECTED)).thenReturn(false);

        var res = service.addLine(1L, new QuotationRequestAddLineRequest(50L, null));
        assertThat(res.status()).isEqualTo("PENDING");
        assertThat(res.requestedQty()).isEqualTo(new BigDecimal("10"));
    }

    @Test
    void shouldRejectAddingLineFromNonApprovedRequisitionItem() {
        QuotationRequest request = quotationRequest(1L, QuotationRequestStatus.DRAFT, location);
        PurchaseRequisitionItem reqItem = requisitionItem(50L, product(5L), new BigDecimal("10"), location);
        reqItem.setStatus(PurchaseRequisitionItemStatus.PENDING);
        when(requestRepository.findById(1L)).thenReturn(Optional.of(request));
        when(requisitionItemRepository.findById(50L)).thenReturn(Optional.of(reqItem));

        assertThatThrownBy(() -> service.addLine(1L, new QuotationRequestAddLineRequest(50L, null)))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("not approved");
    }

    @Test
    void shouldRejectAddingLineAlreadyLiveOnAnotherQuotationRequest() {
        QuotationRequest request = quotationRequest(1L, QuotationRequestStatus.DRAFT, location);
        PurchaseRequisitionItem reqItem = requisitionItem(50L, product(5L), new BigDecimal("10"), location);
        when(requestRepository.findById(1L)).thenReturn(Optional.of(request));
        when(requisitionItemRepository.findById(50L)).thenReturn(Optional.of(reqItem));
        when(lineRepository.existsByPurchaseRequisitionItemIdAndStatusNot(50L, QuotationRequestLineStatus.REJECTED)).thenReturn(true);

        assertThatThrownBy(() -> service.addLine(1L, new QuotationRequestAddLineRequest(50L, null)))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("already on another live Quotation Request");
    }

    @Test
    void shouldRejectAddingLineFromDifferentLocation() {
        QuotationRequest request = quotationRequest(1L, QuotationRequestStatus.DRAFT, location);
        InventoryLocation otherLocation = location(2L, "Ward B");
        PurchaseRequisitionItem reqItem = requisitionItem(50L, product(5L), new BigDecimal("10"), otherLocation);
        when(requestRepository.findById(1L)).thenReturn(Optional.of(request));
        when(requisitionItemRepository.findById(50L)).thenReturn(Optional.of(reqItem));

        assertThatThrownBy(() -> service.addLine(1L, new QuotationRequestAddLineRequest(50L, null)))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("different location");
    }

    // ── addSupplier / submit ─────────────────────────────────────────────────

    @Test
    void shouldRejectInvitingSameSupplierTwice() {
        QuotationRequest request = quotationRequest(1L, QuotationRequestStatus.DRAFT, location);
        when(requestRepository.findById(1L)).thenReturn(Optional.of(request));
        when(supplierLinkRepository.existsByQuotationRequestIdAndSupplierId(1L, 10L)).thenReturn(true);

        assertThatThrownBy(() -> service.addSupplier(1L, new QuotationRequestAddSupplierRequest(10L)))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("already been invited");
    }

    @Test
    void shouldRejectSubmittingWithNoLines() {
        QuotationRequest request = quotationRequest(1L, QuotationRequestStatus.DRAFT, location);
        when(requestRepository.findById(1L)).thenReturn(Optional.of(request));
        when(lineRepository.findByQuotationRequestIdOrderByIdAsc(1L)).thenReturn(List.of());

        assertThatThrownBy(() -> service.submit(1L, "buyer"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Add at least one line");
    }

    @Test
    void shouldRejectSubmittingWithNoSuppliers() {
        QuotationRequest request = quotationRequest(1L, QuotationRequestStatus.DRAFT, location);
        QuotationRequestLine line = quotationLine(100L, request, product(5L));
        when(requestRepository.findById(1L)).thenReturn(Optional.of(request));
        when(lineRepository.findByQuotationRequestIdOrderByIdAsc(1L)).thenReturn(List.of(line));
        when(supplierLinkRepository.findByQuotationRequestIdOrderByIdAsc(1L)).thenReturn(List.of());

        assertThatThrownBy(() -> service.submit(1L, "buyer"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Invite at least one supplier");
    }

    @Test
    void shouldSubmitWhenLinesAndSuppliersExist() {
        QuotationRequest request = quotationRequest(1L, QuotationRequestStatus.DRAFT, location);
        QuotationRequestLine line = quotationLine(100L, request, product(5L));
        when(requestRepository.findById(1L)).thenReturn(Optional.of(request));
        when(lineRepository.findByQuotationRequestIdOrderByIdAsc(1L)).thenReturn(List.of(line));
        when(supplierLinkRepository.findByQuotationRequestIdOrderByIdAsc(1L))
            .thenReturn(List.of(supplierLink(1L, request, supplierA)));

        var res = service.submit(1L, "buyer");
        assertThat(res.status()).isEqualTo("SUBMITTED");
    }

    // ── recordResponse / award / reject ─────────────────────────────────────

    @Test
    void shouldRejectRecordingResponseFromUninvitedSupplier() {
        QuotationRequest request = quotationRequest(1L, QuotationRequestStatus.SUBMITTED, location);
        QuotationRequestLine line = quotationLine(100L, request, product(5L));
        when(requestRepository.findById(1L)).thenReturn(Optional.of(request));
        when(lineRepository.findById(100L)).thenReturn(Optional.of(line));
        when(supplierLinkRepository.existsByQuotationRequestIdAndSupplierId(1L, 99L)).thenReturn(false);

        assertThatThrownBy(() -> service.recordResponse(1L, 100L, 99L,
                new QuotationResponseLineRequest(new BigDecimal("50"), 5, null), "buyer"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("not invited");
    }

    @Test
    void shouldRecordResponseAndAllowAwardingIt() {
        QuotationRequest request = quotationRequest(1L, QuotationRequestStatus.SUBMITTED, location);
        QuotationRequestLine line = quotationLine(100L, request, product(5L));
        when(requestRepository.findById(1L)).thenReturn(Optional.of(request));
        when(lineRepository.findById(100L)).thenReturn(Optional.of(line));
        when(supplierLinkRepository.existsByQuotationRequestIdAndSupplierId(1L, 10L)).thenReturn(true);
        when(supplierRepository.findById(10L)).thenReturn(Optional.of(supplierA));
        when(responseRepository.findByQuotationRequestLineIdAndSupplierId(100L, 10L)).thenReturn(Optional.empty());

        var responseRes = service.recordResponse(1L, 100L, 10L,
            new QuotationResponseLineRequest(new BigDecimal("45.50"), 7, "quick turnaround"), "buyer");
        assertThat(responseRes.quotedUnitPrice()).isEqualTo(new BigDecimal("45.50"));

        QuotationResponseLine response = responseLine(200L, line, supplierA, new BigDecimal("45.50"));
        when(responseRepository.findById(200L)).thenReturn(Optional.of(response));

        var awardRes = service.award(1L, 100L, new QuotationRequestAwardRequest(200L), "buyer");
        assertThat(awardRes.status()).isEqualTo("AWARDED");
        assertThat(awardRes.awardedSupplierId()).isEqualTo(10L);
    }

    @Test
    void shouldRejectAwardingWithResponseFromADifferentLine() {
        QuotationRequest request = quotationRequest(1L, QuotationRequestStatus.SUBMITTED, location);
        QuotationRequestLine line = quotationLine(100L, request, product(5L));
        QuotationRequestLine otherLine = quotationLine(101L, request, product(6L));
        QuotationResponseLine response = responseLine(200L, otherLine, supplierA, new BigDecimal("10"));
        when(requestRepository.findById(1L)).thenReturn(Optional.of(request));
        when(lineRepository.findById(100L)).thenReturn(Optional.of(line));
        when(responseRepository.findById(200L)).thenReturn(Optional.of(response));

        assertThatThrownBy(() -> service.award(1L, 100L, new QuotationRequestAwardRequest(200L), "buyer"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("not recorded against this line");
    }

    @Test
    void shouldRejectLineAndCompleteHeaderWhenNoLinesRemainUnresolved() {
        QuotationRequest request = quotationRequest(1L, QuotationRequestStatus.SUBMITTED, location);
        QuotationRequestLine line = quotationLine(100L, request, product(5L));
        when(requestRepository.findById(1L)).thenReturn(Optional.of(request));
        when(lineRepository.findById(100L)).thenReturn(Optional.of(line));
        when(lineRepository.existsByQuotationRequestIdAndStatus(1L, QuotationRequestLineStatus.PENDING)).thenReturn(false);
        when(lineRepository.existsByQuotationRequestIdAndStatus(1L, QuotationRequestLineStatus.AWARDED)).thenReturn(false);

        var res = service.rejectLine(1L, 100L);
        assertThat(res.status()).isEqualTo("REJECTED");
        assertThat(request.getStatus()).isEqualTo(QuotationRequestStatus.COMPLETED);
    }

    // ── convertAwardedLines ──────────────────────────────────────────────────

    @Test
    void shouldConvertAwardedLinesIntoOnePurchaseOrderPerWinningSupplier() {
        QuotationRequest request = quotationRequest(1L, QuotationRequestStatus.SUBMITTED, location);
        PurchaseRequisitionItem reqItemA = requisitionItem(50L, product(5L), new BigDecimal("10"), location);
        PurchaseRequisitionItem reqItemB = requisitionItem(51L, product(6L), new BigDecimal("4"), location);

        QuotationRequestLine lineA = quotationLine(100L, request, product(5L));
        lineA.setPurchaseRequisitionItem(reqItemA);
        lineA.setRequestedQty(new BigDecimal("10"));
        lineA.setStatus(QuotationRequestLineStatus.AWARDED);
        lineA.setAwardedResponseLine(responseLine(200L, lineA, supplierA, new BigDecimal("45.50")));

        QuotationRequestLine lineB = quotationLine(101L, request, product(6L));
        lineB.setPurchaseRequisitionItem(reqItemB);
        lineB.setRequestedQty(new BigDecimal("4"));
        lineB.setStatus(QuotationRequestLineStatus.AWARDED);
        lineB.setAwardedResponseLine(responseLine(201L, lineB, supplierB, new BigDecimal("12.00")));

        when(requestRepository.findById(1L)).thenReturn(Optional.of(request));
        when(lineRepository.findAwardedNotYetOrdered(1L)).thenReturn(List.of(lineA, lineB));
        when(purchaseOrderService.create(any(), any())).thenAnswer(inv ->
            new PurchaseOrderResponse(900L, null, null, null, null, "PENDING", null, null, null, null, null, null, null, null, null, null, null, null, 0, null, null));
        when(purchaseOrderService.findById(900L)).thenReturn(
            new PurchaseOrderResponse(900L, null, null, null, null, "PENDING", null, null, null, null, null, null, null, null, null, null, null, null, 1, null, null));
        when(lineRepository.existsByQuotationRequestIdAndStatus(anyLong(), any())).thenReturn(false);

        var created = service.convertAwardedLines(1L, "buyer");
        assertThat(created).hasSize(2);
        assertThat(request.getStatus()).isEqualTo(QuotationRequestStatus.COMPLETED);
    }

    @Test
    void shouldRejectConvertingWhenNoLinesAreAwarded() {
        QuotationRequest request = quotationRequest(1L, QuotationRequestStatus.SUBMITTED, location);
        when(requestRepository.findById(1L)).thenReturn(Optional.of(request));
        when(lineRepository.findAwardedNotYetOrdered(1L)).thenReturn(List.of());

        assertThatThrownBy(() -> service.convertAwardedLines(1L, "buyer"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("No awarded lines");
    }

    // ── fixtures ─────────────────────────────────────────────────────────────

    private QuotationRequest quotationRequest(Long id, QuotationRequestStatus status, InventoryLocation location) {
        QuotationRequest r = new QuotationRequest();
        r.setId(id);
        r.setStatus(status);
        r.setLocation(location);
        return r;
    }

    private QuotationRequestLine quotationLine(Long id, QuotationRequest request, Product product) {
        QuotationRequestLine l = new QuotationRequestLine();
        l.setId(id);
        l.setQuotationRequest(request);
        l.setProduct(product);
        l.setPurchaseRequisitionItem(requisitionItem(id + 900, product, BigDecimal.TEN, request.getLocation()));
        l.setRequestedQty(BigDecimal.TEN);
        l.setStatus(QuotationRequestLineStatus.PENDING);
        return l;
    }

    private QuotationRequestSupplier supplierLink(Long id, QuotationRequest request, Supplier supplier) {
        QuotationRequestSupplier s = new QuotationRequestSupplier();
        s.setId(id);
        s.setQuotationRequest(request);
        s.setSupplier(supplier);
        return s;
    }

    private QuotationResponseLine responseLine(Long id, QuotationRequestLine line, Supplier supplier, BigDecimal price) {
        QuotationResponseLine r = new QuotationResponseLine();
        r.setId(id);
        r.setQuotationRequestLine(line);
        r.setSupplier(supplier);
        r.setQuotedUnitPrice(price);
        return r;
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

    private Product product(Long id) {
        Product p = new Product();
        p.setId(id);
        p.setProductCode("PROD-" + id);
        p.setProductName("Product " + id);
        return p;
    }
}
