package com.cms.inventory.procurement.config;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.transaction.annotation.Transactional;

import com.cms.inventory.asset.dto.AssetDisposalRequest;
import com.cms.inventory.asset.dto.AssetMaintenanceMarkPerformedRequest;
import com.cms.inventory.asset.dto.AssetMaintenanceScheduleRequest;
import com.cms.inventory.asset.dto.AssetRequest;
import com.cms.inventory.asset.dto.AssetResponse;
import com.cms.inventory.asset.dto.AssetServiceContractRequest;
import com.cms.inventory.asset.dto.AssetStatusUpdateRequest;
import com.cms.inventory.asset.model.Asset;
import com.cms.inventory.asset.repository.AssetMaintenanceScheduleRepository;
import com.cms.inventory.asset.repository.AssetRepository;
import com.cms.inventory.asset.repository.AssetServiceContractRepository;
import com.cms.inventory.asset.service.AssetMaintenanceScheduleService;
import com.cms.inventory.asset.service.AssetServiceContractService;
import com.cms.inventory.asset.service.AssetService;
import com.cms.inventory.catalog.model.Product;
import com.cms.inventory.catalog.repository.ProductRepository;
import com.cms.inventory.catalog.repository.ProductVariantRepository;
import com.cms.inventory.procurement.dto.CurrencyExchangeRateRequest;
import com.cms.inventory.procurement.dto.InventoryCurrencySettingsRequest;
import com.cms.inventory.procurement.dto.PurchaseOrderAddLineRequest;
import com.cms.inventory.procurement.dto.PurchaseOrderCreateRequest;
import com.cms.inventory.procurement.dto.PurchaseOrderForceCloseRequest;
import com.cms.inventory.procurement.dto.PurchaseOrderItemResponse;
import com.cms.inventory.procurement.dto.PurchaseOrderResponse;
import com.cms.inventory.procurement.dto.PurchaseRequisitionAddLineRequest;
import com.cms.inventory.procurement.dto.PurchaseRequisitionCreateRequest;
import com.cms.inventory.procurement.dto.PurchaseRequisitionItemResponse;
import com.cms.inventory.procurement.dto.PurchaseRequisitionResolutionRequest;
import com.cms.inventory.procurement.dto.QuotationRequestAddLineRequest;
import com.cms.inventory.procurement.dto.QuotationRequestAddSupplierRequest;
import com.cms.inventory.procurement.dto.QuotationRequestAwardRequest;
import com.cms.inventory.procurement.dto.QuotationRequestCreateRequest;
import com.cms.inventory.procurement.dto.QuotationRequestLineResponse;
import com.cms.inventory.procurement.dto.QuotationResponseLineRequest;
import com.cms.inventory.procurement.dto.QuotationResponseLineResponse;
import com.cms.inventory.procurement.dto.RateContractLineRequest;
import com.cms.inventory.procurement.dto.RateContractRequest;
import com.cms.inventory.procurement.dto.RateContractResponse;
import com.cms.inventory.procurement.dto.SupplierRequest;
import com.cms.inventory.procurement.dto.SupplierResponse;
import com.cms.inventory.procurement.dto.TaxRuleRequest;
import com.cms.inventory.procurement.dto.TaxTypeRequest;
import com.cms.inventory.procurement.dto.VendorProductMappingRequest;
import com.cms.inventory.procurement.model.PurchaseRequisition;
import com.cms.inventory.procurement.model.PurchaseRequisitionItem;
import com.cms.inventory.procurement.model.TaxType;
import com.cms.inventory.procurement.model.enums.PurchaseRequisitionItemStatus;
import com.cms.inventory.procurement.repository.CurrencyExchangeRateRepository;
import com.cms.inventory.procurement.repository.InventoryCurrencySettingRepository;
import com.cms.inventory.procurement.repository.PurchaseOrderRepository;
import com.cms.inventory.procurement.repository.PurchaseRequisitionItemRepository;
import com.cms.inventory.procurement.repository.PurchaseRequisitionRepository;
import com.cms.inventory.procurement.repository.QuotationRequestRepository;
import com.cms.inventory.procurement.repository.RateContractRepository;
import com.cms.inventory.procurement.repository.SupplierRepository;
import com.cms.inventory.procurement.repository.TaxRuleRepository;
import com.cms.inventory.procurement.repository.TaxTypeRepository;
import com.cms.inventory.procurement.repository.VendorProductMappingRepository;
import com.cms.inventory.procurement.service.CurrencyExchangeRateService;
import com.cms.inventory.procurement.service.InventoryCurrencySettingsService;
import com.cms.inventory.procurement.service.PurchaseOrderService;
import com.cms.inventory.procurement.service.PurchaseRequisitionService;
import com.cms.inventory.procurement.service.QuotationRequestService;
import com.cms.inventory.procurement.service.RateContractService;
import com.cms.inventory.procurement.service.SupplierService;
import com.cms.inventory.procurement.service.TaxRuleService;
import com.cms.inventory.procurement.service.TaxTypeService;
import com.cms.inventory.procurement.service.VendorProductMappingService;
import com.cms.inventory.procurement.service.WantedListService;
import com.cms.inventory.receiving.dto.GoodsReceiptAddLineRequest;
import com.cms.inventory.receiving.dto.GoodsReceiptCreateRequest;
import com.cms.inventory.receiving.dto.GoodsReceiptLineResponse;
import com.cms.inventory.receiving.repository.GoodsReceiptLineRepository;
import com.cms.inventory.receiving.repository.GoodsReceiptRepository;
import com.cms.inventory.receiving.service.GoodsReceiptService;
import com.cms.inventory.stock.dto.StockMovementRequest;
import com.cms.inventory.stock.model.InventoryLocation;
import com.cms.inventory.stock.repository.InventoryLocationRepository;
import com.cms.inventory.stock.repository.StockBalanceRepository;
import com.cms.inventory.stock.service.StockMovementService;

/**
 * One-off bulk demo-data generator for the "Purchasing & Suppliers" and "Equipment & Asset
 * Management" nav groups — Suppliers (mixed approval states), Tax Rules, Currency Settings/
 * Exchange Rates, Rate Contracts, Vendor Product Rates (against the 112 products {@code
 * InventoryBulkDemoDataSeeder} already created — reused via {@code productRepo.findAll()}, never
 * recreated), Purchase Requisitions across states, Quotation Requests (RFQ) across every real
 * shipped lifecycle state, Wanted List entries, Purchase Orders spanning the full status lifecycle
 * including Goods Receipts to drive the receipt-progress-computed states, and (Phase 3) Assets
 * across every {@code AssetStatus}, Maintenance Schedules, and Service Contracts. Deliberately its
 * own opt-in flag ({@code cms.seed.bulk-purchasing-asset-demo=true}), separate from the always-on
 * {@code cms.seed.enabled} seeders and from {@code InventoryBulkDemoDataSeeder}'s own flag — never
 * fires on a normal local boot. See docs/inventory-management/PURCHASING_ASSET_OVERNIGHT_PLAN.md
 * (Phases 2-3) and docs/inventory-management/DECISION_LOG.md's 2026-09-22 "Bulk demo data" entries.
 *
 * <p>Idempotency follows the same per-phase-table discipline as {@code
 * InventoryBulkDemoDataSeeder} (each phase method commits independently — the top-level {@code
 * CommandLineRunner}'s own {@code @Transactional} does NOT wrap the whole run), plus every
 * downstream phase re-queries its dependencies from the repository rather than reusing another
 * phase's in-memory return value, since that value is empty on a resumed run once the rows already
 * exist. Direct-PO scenarios (Phase P) run before Quotation Requests (Phase Q, which creates its
 * own PO via {@code convertAwardedLines}) so Phase P's own {@code purchaseOrderRepo.count() == 0}
 * gate is checked before any PO exists yet.
 */
@Configuration
@Profile("local")
@ConditionalOnProperty(prefix = "cms.seed", name = "bulk-purchasing-asset-demo", havingValue = "true")
public class PurchasingAssetBulkDemoDataSeeder {

    private static final Logger log = LoggerFactory.getLogger(PurchasingAssetBulkDemoDataSeeder.class);
    private static final String ACTOR = "bulk-seed";

    @Bean
    @Order(160)
    @Transactional
    CommandLineRunner seedBulkPurchasingAssetDemoData(
            ProductRepository productRepo,
            ProductVariantRepository variantRepo,
            InventoryLocationRepository locationRepo,
            StockBalanceRepository stockBalanceRepo,
            StockMovementService stockMovementService,
            SupplierRepository supplierRepo,
            SupplierService supplierService,
            TaxTypeRepository taxTypeRepo,
            TaxTypeService taxTypeService,
            TaxRuleRepository taxRuleRepo,
            TaxRuleService taxRuleService,
            InventoryCurrencySettingRepository currencySettingRepo,
            InventoryCurrencySettingsService currencySettingsService,
            CurrencyExchangeRateRepository exchangeRateRepo,
            CurrencyExchangeRateService exchangeRateService,
            RateContractRepository rateContractRepo,
            RateContractService rateContractService,
            VendorProductMappingRepository vpmRepo,
            VendorProductMappingService vpmService,
            PurchaseRequisitionRepository requisitionRepo,
            PurchaseRequisitionItemRepository requisitionItemRepo,
            PurchaseRequisitionService requisitionService,
            QuotationRequestRepository qrRepo,
            QuotationRequestService qrService,
            WantedListService wantedListService,
            PurchaseOrderRepository poRepo,
            PurchaseOrderService poService,
            GoodsReceiptRepository grRepo,
            GoodsReceiptLineRepository grLineRepo,
            GoodsReceiptService grService,
            AssetRepository assetRepo,
            AssetService assetService,
            AssetMaintenanceScheduleRepository maintenanceScheduleRepo,
            AssetMaintenanceScheduleService maintenanceScheduleService,
            AssetServiceContractRepository serviceContractRepo,
            AssetServiceContractService serviceContractService) {
        return args -> {
            log.info("🌱 Seeding BULK Purchasing & Suppliers demo data...");

            List<Product> products = productRepo.findAll();
            List<Product> usable = products.stream()
                .filter(p -> !variantRepo.existsByProductIdAndIsActiveTrue(p.getId()))
                .toList();
            if (usable.size() < 50) {
                log.warn("Fewer than 50 usable products found — run InventoryBulkDemoDataSeeder first. Skipping.");
                return;
            }

            seedSuppliers(supplierRepo, supplierService);
            List<Supplier1> suppliers = reloadSuppliers(supplierRepo);

            seedTaxRules(taxTypeRepo, taxTypeService, taxRuleRepo, taxRuleService);
            seedCurrency(currencySettingRepo, currencySettingsService, exchangeRateRepo, exchangeRateService);

            seedRateContracts(rateContractRepo, rateContractService, suppliers, usable);
            List<RateContractResponse> contracts = rateContractRepo.findAll().stream()
                .map(c -> rateContractService.findById(c.getId())).toList();

            seedVendorProductRates(vpmRepo, vpmService, suppliers, usable, contracts);

            seedRequisitions(requisitionRepo, requisitionService, locationRepo, usable);
            List<PurchaseRequisition> requisitions = requisitionRepo.findAll();

            seedDirectPurchaseOrders(poRepo, poService, grRepo, grService, requisitionItemRepo, requisitions, suppliers);
            seedQuotationRequests(qrRepo, qrService, requisitionItemRepo, requisitions, suppliers, poService);

            seedWantedList(stockBalanceRepo, stockMovementService, wantedListService, productRepo, locationRepo);

            log.info("✓ Bulk Purchasing & Suppliers demo data seeding complete.");

            log.info("🌱 Seeding BULK Equipment & Asset Management demo data...");

            List<Long> onboardingGrLineIds = seedAssetOnboardingReceipt(
                requisitionService, poRepo, poService, grRepo, grLineRepo, grService,
                locationRepo, usable, suppliers);

            seedAssets(assetRepo, assetService, productRepo, locationRepo, onboardingGrLineIds);
            List<Asset> assets = assetRepo.findAll();

            seedMaintenanceSchedules(maintenanceScheduleRepo, maintenanceScheduleService, assets);
            seedServiceContracts(serviceContractRepo, serviceContractService, assets, suppliers);

            log.info("✓ Bulk Equipment & Asset Management demo data seeding complete.");
        };
    }

    // ── Small local record so we don't have to carry SupplierResponse's masked fields around ──
    private record Supplier1(Long id, String code, String name, boolean approved, boolean active) {}

    // ── Suppliers ─────────────────────────────────────────────────────────

    private void seedSuppliers(SupplierRepository supplierRepo, SupplierService supplierService) {
        record Seed(String code, String name, String state, String contact, String email, String phone, boolean approve, boolean active) {}
        List<Seed> seeds = List.of(
            new Seed("SUP-001", "MedPlus Pharma Distributors", "Tamil Nadu", "R. Kavitha", "sales@medplusdist.example", "9840011122", true, true),
            new Seed("SUP-002", "Apollo Surgical Supplies", "Tamil Nadu", "S. Ramesh", "orders@apollosurgical.example", "9840022233", true, true),
            new Seed("SUP-003", "3M India Healthcare", "Karnataka", "A. Krishnan", "b2b@3mhealthcare.example", "9880033344", true, true),
            new Seed("SUP-004", "BD Medical India", "Maharashtra", "P. Deshmukh", "sales.in@bdmedical.example", "9820044455", true, true),
            new Seed("SUP-005", "Sri Lakshmi Lab Equipments", "Tamil Nadu", "M. Suresh", "info@srilakshmilab.example", "9840055566", true, true),
            new Seed("SUP-006", "Chennai IT Solutions", "Tamil Nadu", "V. Anand", "sales@chennaiits.example", "9840066677", true, true),
            new Seed("SUP-007", "National Stationery Mart", "Delhi", "N. Gupta", "orders@natstationery.example", "9810077788", true, true),
            new Seed("SUP-008", "HealthCare Linen Supplies", "Tamil Nadu", "K. Priya", "contact@hclinen.example", "9840088899", false, true),
            new Seed("SUP-009", "Metro Housekeeping Traders", "Tamil Nadu", "T. Balaji", "sales@metrohousekeeping.example", "9840099900", false, true),
            new Seed("SUP-010", "Unreliable Vendor Co", "Tamil Nadu", "Unknown", "noreply@unreliablevendor.example", "9840000000", false, false)
        );
        for (Seed s : seeds) {
            if (supplierRepo.existsBySupplierCodeIgnoreCase(s.code())) continue;
            SupplierResponse created = supplierService.create(new SupplierRequest(
                s.code(), s.name(), s.state(), null, null, null, null, null, null,
                s.contact(), s.email(), s.phone(), false, s.active()));
            if (s.approve()) {
                supplierService.approve(created.id());
            }
        }
        log.info("✓ {} supplier(s) in place", supplierRepo.count());
    }

    private List<Supplier1> reloadSuppliers(SupplierRepository supplierRepo) {
        return supplierRepo.findAllByOrderBySupplierNameAsc().stream()
            .map(s -> new Supplier1(s.getId(), s.getSupplierCode(), s.getSupplierName(),
                Boolean.TRUE.equals(s.getIsApproved()), Boolean.TRUE.equals(s.getIsActive())))
            .toList();
    }

    private Supplier1 byCode(List<Supplier1> suppliers, String code) {
        return suppliers.stream().filter(s -> s.code().equals(code)).findFirst()
            .orElseThrow(() -> new IllegalStateException("Supplier " + code + " not found — seedSuppliers must run first"));
    }

    // ── Tax Rules ─────────────────────────────────────────────────────────

    private void seedTaxRules(TaxTypeRepository taxTypeRepo, TaxTypeService taxTypeService,
                               TaxRuleRepository taxRuleRepo, TaxRuleService taxRuleService) {
        TaxType gst = taxTypeRepo.findAllByOrderByNameAsc().stream()
            .filter(t -> "GST".equalsIgnoreCase(t.getName())).findFirst()
            .orElseGet(() -> {
                Long id = taxTypeService.create(new TaxTypeRequest("GST", "Goods and Services Tax", true)).id();
                return taxTypeRepo.findById(id).orElseThrow();
            });

        record Seed(String name, String rate) {}
        List<Seed> seeds = List.of(
            new Seed("GST 5%", "5.00"), new Seed("GST 12%", "12.00"),
            new Seed("GST 18%", "18.00"), new Seed("GST 28%", "28.00"));
        for (Seed s : seeds) {
            if (taxRuleRepo.existsByNameIgnoreCase(s.name())) continue;
            taxRuleService.create(new TaxRuleRequest(gst.getId(), s.name(), new BigDecimal(s.rate()), true));
        }
        log.info("✓ {} tax rule(s) in place under '{}'", taxRuleRepo.count(), gst.getName());
    }

    // ── Currency Settings + Exchange Rates ───────────────────────────────

    private void seedCurrency(InventoryCurrencySettingRepository settingRepo, InventoryCurrencySettingsService settingsService,
                               CurrencyExchangeRateRepository rateRepo, CurrencyExchangeRateService rateService) {
        if (settingRepo.count() == 0) {
            settingsService.save(new InventoryCurrencySettingsRequest("INR"), ACTOR);
        }
        LocalDate today = LocalDate.now();
        if (!rateService.pairExists("USD", today.minusDays(10), null)) {
            rateService.create(new CurrencyExchangeRateRequest("USD", new BigDecimal("83.20"), today.minusDays(10), true));
        }
        if (!rateService.pairExists("USD", today, null)) {
            rateService.create(new CurrencyExchangeRateRequest("USD", new BigDecimal("83.55"), today, true));
        }
        if (!rateService.pairExists("EUR", today.minusDays(5), null)) {
            rateService.create(new CurrencyExchangeRateRequest("EUR", new BigDecimal("90.10"), today.minusDays(5), true));
        }
        log.info("✓ Currency settings (base INR) + {} exchange rate row(s) in place", rateRepo.count());
    }

    // ── Rate Contracts ────────────────────────────────────────────────────

    private void seedRateContracts(RateContractRepository rateContractRepo, RateContractService rateContractService,
                                    List<Supplier1> suppliers, List<Product> products) {
        if (rateContractRepo.count() > 0) {
            log.info("Rate contracts already exist — skipping this run.");
            return;
        }
        LocalDate today = LocalDate.now();
        Product paracetamol = byName(products, "Paracetamol Tablets");
        Product insulin = byName(products, "Insulin Vial");
        Product gloves = byName(products, "Surgical Gloves M");
        Product n95 = byName(products, "N95 Mask");

        rateContractService.create(new RateContractRequest(
            byCode(suppliers, "SUP-001").id(), today.minusDays(60), today.plusDays(305),
            new BigDecimal("500000.00"), "Annual pharmacy consumables rate agreement — FY2026-27.",
            today.plusDays(275), true,
            List.of(new RateContractLineRequest(paracetamol.getId(), new BigDecimal("42.00")),
                    new RateContractLineRequest(insulin.getId(), new BigDecimal("300.00")))));

        rateContractService.create(new RateContractRequest(
            byCode(suppliers, "SUP-002").id(), today.minusDays(400), today.minusDays(30),
            new BigDecimal("200000.00"), "Prior-year surgical consumables rate agreement (lapsed).",
            null, false,
            List.of(new RateContractLineRequest(gloves.getId(), new BigDecimal("8.00")))));

        rateContractService.create(new RateContractRequest(
            byCode(suppliers, "SUP-003").id(), today.plusDays(15), today.plusDays(200),
            new BigDecimal("150000.00"), "Upcoming PPE supply rate agreement — starts next quarter.",
            today.plusDays(185), true,
            List.of(new RateContractLineRequest(n95.getId(), new BigDecimal("14.00")))));

        log.info("✓ Seeded 3 Rate Contracts (active/expired/upcoming)");
    }

    // ── Vendor Product Rates ─────────────────────────────────────────────

    private void seedVendorProductRates(VendorProductMappingRepository vpmRepo, VendorProductMappingService vpmService,
                                         List<Supplier1> suppliers, List<Product> products, List<RateContractResponse> contracts) {
        if (vpmRepo.count() > 0) {
            log.info("Vendor product rates already exist — skipping this run.");
            return;
        }
        Long rc1 = contracts.stream().filter(c -> c.supplierId().equals(byCode(suppliers, "SUP-001").id())).findFirst().map(RateContractResponse::id).orElse(null);
        Long rc2 = contracts.stream().filter(c -> c.supplierId().equals(byCode(suppliers, "SUP-002").id())).findFirst().map(RateContractResponse::id).orElse(null);
        Long rc3 = contracts.stream().filter(c -> c.supplierId().equals(byCode(suppliers, "SUP-003").id())).findFirst().map(RateContractResponse::id).orElse(null);

        vpmService.create(new VendorProductMappingRequest(byCode(suppliers, "SUP-001").id(), byName(products, "Paracetamol Tablets").getId(),
            rc1, "MP-PARA-500", null, new BigDecimal("45.00"), "INR", null, new BigDecimal("100"), 5, true, true));
        vpmService.create(new VendorProductMappingRequest(byCode(suppliers, "SUP-001").id(), byName(products, "Insulin Vial").getId(),
            rc1, "MP-INS-10U", null, new BigDecimal("320.00"), "INR", null, new BigDecimal("20"), 7, false, true));
        vpmService.create(new VendorProductMappingRequest(byCode(suppliers, "SUP-002").id(), byName(products, "Surgical Gloves M").getId(),
            rc2, "AS-GLV-M", null, new BigDecimal("8.50"), "INR", null, new BigDecimal("200"), 3, false, true));
        vpmService.create(new VendorProductMappingRequest(byCode(suppliers, "SUP-002").id(), byName(products, "IV Cannula 20G").getId(),
            null, "AS-CAN-20G", null, new BigDecimal("12.00"), "INR", null, new BigDecimal("50"), 4, false, true));
        vpmService.create(new VendorProductMappingRequest(byCode(suppliers, "SUP-003").id(), byName(products, "N95 Mask").getId(),
            rc3, "3M-N95", null, new BigDecimal("15.00"), "INR", null, new BigDecimal("100"), 10, true, true));
        vpmService.create(new VendorProductMappingRequest(byCode(suppliers, "SUP-003").id(), byName(products, "Face Mask").getId(),
            null, "3M-FM", null, new BigDecimal("3.50"), "INR", null, new BigDecimal("500"), 5, false, true));
        vpmService.create(new VendorProductMappingRequest(byCode(suppliers, "SUP-004").id(), byName(products, "Foley Catheter 16").getId(),
            null, "BD-FC-16", null, new BigDecimal("55.00"), "INR", null, new BigDecimal("20"), 6, false, true));
        vpmService.create(new VendorProductMappingRequest(byCode(suppliers, "SUP-004").id(), byName(products, "Blood Collection Tube").getId(),
            null, "BD-BCT", null, new BigDecimal("6.00"), "INR", null, new BigDecimal("100"), 4, false, true));
        vpmService.create(new VendorProductMappingRequest(byCode(suppliers, "SUP-005").id(), byName(products, "Glass Beaker 250ml").getId(),
            null, "SL-BK-250", null, new BigDecimal("1.10"), "USD", null, new BigDecimal("12"), 15, true, true));
        vpmService.create(new VendorProductMappingRequest(byCode(suppliers, "SUP-006").id(), byName(products, "Laptop").getId(),
            null, "CIT-LT-01", null, new BigDecimal("45000.00"), "INR", null, new BigDecimal("1"), 10, false, true));
        vpmService.create(new VendorProductMappingRequest(byCode(suppliers, "SUP-007").id(), byName(products, "A4 Paper Ream").getId(),
            null, "NSM-A4", null, new BigDecimal("240.00"), "INR", null, new BigDecimal("20"), 3, false, true));

        log.info("✓ Seeded 11 Vendor Product Rate mappings");
    }

    // ── Purchase Requisitions ─────────────────────────────────────────────

    private static final String M_PENDING = "Bulk demo — Ward A pending requisition";
    private static final String M_PARTIAL = "Bulk demo — Ward B partially approved requisition";
    private static final String M_REJECTED = "Bulk demo — OT requisition with a rejected line";
    private static final String M_ORDERED = "Bulk demo — ICU requisition (fully ordered)";
    private static final String M_RFQ_HOUSEKEEPING = "Bulk demo — Housekeeping RFQ sourcing";
    private static final String M_RFQ_LAB = "Bulk demo — Lab RFQ sourcing";
    private static final String M_RFQ_OFFICE = "Bulk demo — Office RFQ sourcing (cancelled)";
    private static final String M_PO_A = "Bulk demo — PO lifecycle sourcing (Ward A)";
    private static final String M_PO_B = "Bulk demo — PO lifecycle sourcing (OT)";

    private void seedRequisitions(PurchaseRequisitionRepository requisitionRepo, PurchaseRequisitionService requisitionService,
                                   InventoryLocationRepository locationRepo, List<Product> products) {
        if (requisitionRepo.count() > 0) {
            log.info("Purchase requisitions already exist — skipping this run.");
            return;
        }
        InventoryLocation wardA = byLocationName(locationRepo, "Ward A Requesting Point");
        InventoryLocation wardB = byLocationName(locationRepo, "Ward B Requesting Point");
        InventoryLocation ot = byLocationName(locationRepo, "OT Requesting Point");
        InventoryLocation icu = byLocationName(locationRepo, "ICU Requesting Point");
        InventoryLocation housekeeping = byLocationName(locationRepo, "Housekeeping Requesting Point");
        InventoryLocation lab = byLocationName(locationRepo, "Lab Requesting Point");
        InventoryLocation office = byLocationName(locationRepo, "Office Store Point");
        LocalDate today = LocalDate.now();

        // 1) pending — never touched past submission
        Long pending = requisitionService.create(new PurchaseRequisitionCreateRequest(wardA.getId(), today, M_PENDING), ACTOR).id();
        addLine(requisitionService, pending, byName(products, "Gauze Roll").getId(), "100");
        addLine(requisitionService, pending, byName(products, "Surgical Gloves M").getId(), "50");
        requisitionService.submit(pending, "ward-a-clerk");

        // 2) partially approved — one line resolved, two still pending
        Long partial = requisitionService.create(new PurchaseRequisitionCreateRequest(wardB.getId(), today.minusDays(2), M_PARTIAL), ACTOR).id();
        var p1 = addLine(requisitionService, partial, byName(products, "IV Cannula 18G").getId(), "60");
        addLine(requisitionService, partial, byName(products, "Face Mask").getId(), "200");
        addLine(requisitionService, partial, byName(products, "N95 Mask").getId(), "100");
        requisitionService.submit(partial, "ward-b-clerk");
        requisitionService.approveLine(partial, p1.id(), new PurchaseRequisitionResolutionRequest("Issued from Main Store"), "store-keeper");

        // 3) rejected line — fully resolved (COMPLETED), one line rejected
        Long rejected = requisitionService.create(new PurchaseRequisitionCreateRequest(ot.getId(), today.minusDays(4), M_REJECTED), ACTOR).id();
        var r1 = addLine(requisitionService, rejected, byName(products, "Foley Catheter 16").getId(), "30");
        var r2 = addLine(requisitionService, rejected, byName(products, "Suction Catheter").getId(), "500");
        requisitionService.submit(rejected, "ot-incharge");
        requisitionService.approveLine(rejected, r1.id(), new PurchaseRequisitionResolutionRequest("Issued"), "store-keeper");
        requisitionService.rejectLine(rejected, r2.id(), new PurchaseRequisitionResolutionRequest("Quantity far exceeds OT's normal usage"), "store-keeper");

        // 4) fully ordered — both lines approved, picked up directly into a Purchase Order later
        Long ordered = requisitionService.create(new PurchaseRequisitionCreateRequest(icu.getId(), today.minusDays(6), M_ORDERED), ACTOR).id();
        var o1 = addLine(requisitionService, ordered, byName(products, "Nasogastric Tube").getId(), "40");
        var o2 = addLine(requisitionService, ordered, byName(products, "BP Cuff").getId(), "10");
        requisitionService.submit(ordered, "icu-incharge");
        requisitionService.approveLine(ordered, o1.id(), new PurchaseRequisitionResolutionRequest("Issued"), "store-keeper");
        requisitionService.approveLine(ordered, o2.id(), new PurchaseRequisitionResolutionRequest("Issued"), "store-keeper");

        // 5) RFQ sourcing — Housekeeping (3 approved lines feed Quotation Requests 1 & 2)
        Long rfqHk = requisitionService.create(new PurchaseRequisitionCreateRequest(housekeeping.getId(), today.minusDays(3), M_RFQ_HOUSEKEEPING), ACTOR).id();
        var h1 = addLine(requisitionService, rfqHk, byName(products, "Floor Cleaner 5L").getId(), "40");
        var h2 = addLine(requisitionService, rfqHk, byName(products, "Toilet Cleaner 1L").getId(), "40");
        var h3 = addLine(requisitionService, rfqHk, byName(products, "Garbage Bags (roll)").getId(), "100");
        requisitionService.submit(rfqHk, "housekeeping-lead");
        requisitionService.approveLine(rfqHk, h1.id(), new PurchaseRequisitionResolutionRequest("Route via RFQ"), "store-keeper");
        requisitionService.approveLine(rfqHk, h2.id(), new PurchaseRequisitionResolutionRequest("Route via RFQ"), "store-keeper");
        requisitionService.approveLine(rfqHk, h3.id(), new PurchaseRequisitionResolutionRequest("Route via RFQ"), "store-keeper");

        // 6) RFQ sourcing — Lab (1 approved line, awarded + converted into a Purchase Order)
        Long rfqLab = requisitionService.create(new PurchaseRequisitionCreateRequest(lab.getId(), today.minusDays(3), M_RFQ_LAB), ACTOR).id();
        var l1 = addLine(requisitionService, rfqLab, byName(products, "Glass Beaker 250ml").getId(), "24");
        requisitionService.submit(rfqLab, "lab-incharge");
        requisitionService.approveLine(rfqLab, l1.id(), new PurchaseRequisitionResolutionRequest("Route via RFQ"), "store-keeper");

        // 7) RFQ sourcing — Office (1 approved line, its Quotation Request is cancelled before submit)
        Long rfqOffice = requisitionService.create(new PurchaseRequisitionCreateRequest(office.getId(), today.minusDays(1), M_RFQ_OFFICE), ACTOR).id();
        var f1 = addLine(requisitionService, rfqOffice, byName(products, "Printer Ink Cartridge").getId(), "15");
        requisitionService.submit(rfqOffice, "office-clerk");
        requisitionService.approveLine(rfqOffice, f1.id(), new PurchaseRequisitionResolutionRequest("Route via RFQ"), "store-keeper");

        // 8) PO lifecycle sourcing — Ward A (feeds PO1 PENDING + PO3 IN_PROGRESS)
        Long poA = requisitionService.create(new PurchaseRequisitionCreateRequest(wardA.getId(), today.minusDays(10), M_PO_A), ACTOR).id();
        var a1 = addLine(requisitionService, poA, byName(products, "Crepe Bandage").getId(), "80");
        var a2 = addLine(requisitionService, poA, byName(products, "Adhesive Tape").getId(), "60");
        var a3 = addLine(requisitionService, poA, byName(products, "Hand Sanitizer 500ml").getId(), "50");
        requisitionService.submit(poA, "ward-a-clerk");
        requisitionService.approveLine(poA, a1.id(), new PurchaseRequisitionResolutionRequest("Route to PO"), "store-keeper");
        requisitionService.approveLine(poA, a2.id(), new PurchaseRequisitionResolutionRequest("Route to PO"), "store-keeper");
        requisitionService.approveLine(poA, a3.id(), new PurchaseRequisitionResolutionRequest("Route to PO"), "store-keeper");

        // 9) PO lifecycle sourcing — OT (feeds PO4 PARTIALLY_COMPLETED + PO5 COMPLETED + PO6 FORCE_CLOSED)
        Long poB = requisitionService.create(new PurchaseRequisitionCreateRequest(ot.getId(), today.minusDays(20), M_PO_B), ACTOR).id();
        // "Foley Catheter 14" (not "Wound Dressing Kit") deliberately — InventoryBulkDemoDataSeeder
        // marks Wound Dressing Kit BATCH-tracked, and this line is later fully received via a
        // Goods Receipt, which requires a batch/serial number this seeder doesn't supply.
        var b1 = addLine(requisitionService, poB, byName(products, "Foley Catheter 14").getId(), "30");
        var b2 = addLine(requisitionService, poB, byName(products, "Urine Bag").getId(), "60");
        var b3 = addLine(requisitionService, poB, byName(products, "Digital Thermometer").getId(), "20");
        var b4 = addLine(requisitionService, poB, byName(products, "Tongue Depressor").getId(), "200");
        requisitionService.submit(poB, "ot-incharge");
        requisitionService.approveLine(poB, b1.id(), new PurchaseRequisitionResolutionRequest("Route to PO"), "store-keeper");
        requisitionService.approveLine(poB, b2.id(), new PurchaseRequisitionResolutionRequest("Route to PO"), "store-keeper");
        requisitionService.approveLine(poB, b3.id(), new PurchaseRequisitionResolutionRequest("Route to PO"), "store-keeper");
        requisitionService.approveLine(poB, b4.id(), new PurchaseRequisitionResolutionRequest("Route to PO"), "store-keeper");

        log.info("✓ Seeded 9 Purchase Requisitions (pending/partially-approved/rejected-line/fully-ordered/RFQ-sourcing×3/PO-lifecycle-sourcing×2)");
    }

    private PurchaseRequisitionItemResponse addLine(PurchaseRequisitionService service, Long requisitionId, Long productId, String qty) {
        return service.addLine(requisitionId, new PurchaseRequisitionAddLineRequest(productId, new BigDecimal(qty), null));
    }

    // ── Direct-to-PO scenarios (PO1, PO3-PO6) — must run before Quotation Requests ──────────

    private void seedDirectPurchaseOrders(PurchaseOrderRepository poRepo, PurchaseOrderService poService,
                                           GoodsReceiptRepository grRepo, GoodsReceiptService grService,
                                           PurchaseRequisitionItemRepository requisitionItemRepo, List<PurchaseRequisition> requisitions,
                                           List<Supplier1> suppliers) {
        if (poRepo.count() > 0) {
            log.info("Purchase orders already exist — skipping the direct-PO seeding phase.");
            return;
        }
        PurchaseRequisition ordered = requireByNotes(requisitions, M_ORDERED);
        PurchaseRequisition poA = requireByNotes(requisitions, M_PO_A);
        PurchaseRequisition poB = requireByNotes(requisitions, M_PO_B);
        List<PurchaseRequisitionItem> orderedLines = approvedLines(requisitionItemRepo, ordered);
        List<PurchaseRequisitionItem> poALines = approvedLines(requisitionItemRepo, poA);
        List<PurchaseRequisitionItem> poBLines = approvedLines(requisitionItemRepo, poB);
        LocalDate today = LocalDate.now();

        // PO1 — PENDING (never sent)
        Long po1 = poService.create(new PurchaseOrderCreateRequest(byCode(suppliers, "SUP-003").id(), poA.getLocation().getId(),
            today.minusDays(2), today.plusDays(12), null, null, "Bulk demo — PENDING lifecycle sample"), ACTOR).id();
        poService.addLine(po1, new PurchaseOrderAddLineRequest(poALines.get(0).getId(), null, null, null, new BigDecimal("6.50"), null, null));

        // PO2 — ORDERED (from the "fully ordered" requisition)
        Long po2 = poService.create(new PurchaseOrderCreateRequest(byCode(suppliers, "SUP-002").id(), ordered.getLocation().getId(),
            today.minusDays(5), today.plusDays(10), null, null, "Bulk demo — ORDERED lifecycle sample"), ACTOR).id();
        poService.addLine(po2, new PurchaseOrderAddLineRequest(orderedLines.get(0).getId(), null, null, null, new BigDecimal("18.00"), null, null));
        poService.addLine(po2, new PurchaseOrderAddLineRequest(orderedLines.get(1).getId(), null, null, null, new BigDecimal("450.00"), null, null));
        poService.order(po2, ACTOR);

        // PO3 — IN_PROGRESS (one line partially received, none fully received)
        Long po3 = poService.create(new PurchaseOrderCreateRequest(byCode(suppliers, "SUP-003").id(), poA.getLocation().getId(),
            today.minusDays(70), today.minusDays(1), null, null, "Bulk demo — IN_PROGRESS lifecycle sample"), ACTOR).id();
        PurchaseOrderItemResponse po3Line1 = poService.addLine(po3, new PurchaseOrderAddLineRequest(poALines.get(1).getId(), null, null, null, new BigDecimal("9.00"), null, null));
        poService.addLine(po3, new PurchaseOrderAddLineRequest(poALines.get(2).getId(), null, null, null, new BigDecimal("85.00"), null, null));
        poService.order(po3, ACTOR);
        Long gr3 = grService.create(new GoodsReceiptCreateRequest(po3, today.minusDays(3), "Partial delivery — balance to follow"), ACTOR).id();
        grService.addLine(gr3, new GoodsReceiptAddLineRequest(po3Line1.id(), new BigDecimal("30"), null, null, null, null, null, null));
        grService.confirm(gr3, ACTOR);

        // PO4 — PARTIALLY_COMPLETED (one line fully received, one line not yet received)
        Long po4 = poService.create(new PurchaseOrderCreateRequest(byCode(suppliers, "SUP-004").id(), poB.getLocation().getId(),
            today.minusDays(45), today.minusDays(10), null, null, "Bulk demo — PARTIALLY_COMPLETED lifecycle sample"), ACTOR).id();
        PurchaseOrderItemResponse po4Line1 = poService.addLine(po4, new PurchaseOrderAddLineRequest(poBLines.get(0).getId(), null, null, null, new BigDecimal("120.00"), null, null));
        poService.addLine(po4, new PurchaseOrderAddLineRequest(poBLines.get(1).getId(), null, null, null, new BigDecimal("15.00"), null, null));
        poService.order(po4, ACTOR);
        Long gr4 = grService.create(new GoodsReceiptCreateRequest(po4, today.minusDays(18), "First line delivered in full"), ACTOR).id();
        grService.addLine(gr4, new GoodsReceiptAddLineRequest(po4Line1.id(), new BigDecimal("30"), null, null, null, null, null, null));
        grService.confirm(gr4, ACTOR);

        // PO5 — COMPLETED (every line fully received)
        Long po5 = poService.create(new PurchaseOrderCreateRequest(byCode(suppliers, "SUP-005").id(), poB.getLocation().getId(),
            today.minusDays(55), today.minusDays(20), null, null, "Bulk demo — COMPLETED lifecycle sample"), ACTOR).id();
        PurchaseOrderItemResponse po5Line1 = poService.addLine(po5, new PurchaseOrderAddLineRequest(poBLines.get(2).getId(), null, null, null, new BigDecimal("250.00"), null, null));
        poService.order(po5, ACTOR);
        Long gr5 = grService.create(new GoodsReceiptCreateRequest(po5, today.minusDays(22), "Full delivery received and inspected"), ACTOR).id();
        grService.addLine(gr5, new GoodsReceiptAddLineRequest(po5Line1.id(), new BigDecimal("20"), null, null, null, null, null, null));
        grService.confirm(gr5, ACTOR);

        // PO6 — FORCE_CLOSED (sent, then closed out before full receipt)
        Long po6 = poService.create(new PurchaseOrderCreateRequest(byCode(suppliers, "SUP-007").id(), poB.getLocation().getId(),
            today.minusDays(40), today.minusDays(25), null, null, "Bulk demo — FORCE_CLOSED lifecycle sample"), ACTOR).id();
        poService.addLine(po6, new PurchaseOrderAddLineRequest(poBLines.get(3).getId(), null, null, null, new BigDecimal("0.50"), null, null));
        poService.order(po6, ACTOR);
        poService.forceClose(po6, new PurchaseOrderForceCloseRequest("Supplier discontinued this line; closing out the unfulfilled balance."), ACTOR);

        log.info("✓ Seeded 6 direct Purchase Orders (PENDING/ORDERED/IN_PROGRESS/PARTIALLY_COMPLETED/COMPLETED/FORCE_CLOSED) with Goods Receipts driving the receipt-progress states");
    }

    /** APPROVED, not-yet-ordered lines for exactly this requisition, oldest-first — scoped by
     *  requisition id rather than {@code PurchaseOrderService.findAvailableRequisitionLines}'s
     *  location-wide pool, since more than one of this seeder's requisitions can share a location
     *  (e.g. both the "rejected line" and "PO lifecycle sourcing" requisitions target OT) and the
     *  location-wide pool would otherwise interleave lines from unrelated requisitions. */
    private List<PurchaseRequisitionItem> approvedLines(PurchaseRequisitionItemRepository requisitionItemRepo, PurchaseRequisition requisition) {
        return requisitionItemRepo.findByPurchaseRequisitionIdOrderByIdAsc(requisition.getId()).stream()
            .filter(item -> item.getStatus() == PurchaseRequisitionItemStatus.APPROVED)
            .toList();
    }

    private PurchaseRequisition requireByNotes(List<PurchaseRequisition> requisitions, String marker) {
        return requisitions.stream().filter(r -> marker.equals(r.getNotes())).findFirst()
            .orElseThrow(() -> new IllegalStateException("Requisition '" + marker + "' not found — seedRequisitions must run first"));
    }

    // ── Quotation Requests (RFQ) — runs after direct POs so its own PO doesn't trip Phase P's gate ──

    private void seedQuotationRequests(QuotationRequestRepository qrRepo, QuotationRequestService qrService,
                                        PurchaseRequisitionItemRepository requisitionItemRepo, List<PurchaseRequisition> requisitions,
                                        List<Supplier1> suppliers, PurchaseOrderService poService) {
        if (qrRepo.count() > 0) {
            log.info("Quotation requests already exist — skipping this run.");
            return;
        }
        PurchaseRequisition rfqHk = requireByNotes(requisitions, M_RFQ_HOUSEKEEPING);
        PurchaseRequisition rfqLab = requireByNotes(requisitions, M_RFQ_LAB);
        PurchaseRequisition rfqOffice = requireByNotes(requisitions, M_RFQ_OFFICE);
        List<PurchaseRequisitionItem> hkLines = approvedLines(requisitionItemRepo, rfqHk);
        List<PurchaseRequisitionItem> labLines = approvedLines(requisitionItemRepo, rfqLab);
        List<PurchaseRequisitionItem> officeLines = approvedLines(requisitionItemRepo, rfqOffice);
        LocalDate today = LocalDate.now();

        // QR1 — DRAFT (never submitted)
        Long qr1 = qrService.create(new QuotationRequestCreateRequest(rfqHk.getLocation().getId(), today, "Bulk demo — DRAFT RFQ sample"), ACTOR).id();
        qrService.addLine(qr1, new QuotationRequestAddLineRequest(hkLines.get(0).getId(), null));
        qrService.addSupplier(qr1, new QuotationRequestAddSupplierRequest(byCode(suppliers, "SUP-001").id()));
        qrService.addSupplier(qr1, new QuotationRequestAddSupplierRequest(byCode(suppliers, "SUP-007").id()));

        // QR2 — SUBMITTED, one line AWARDED, one line still PENDING
        Long qr2 = qrService.create(new QuotationRequestCreateRequest(rfqHk.getLocation().getId(), today.minusDays(2), "Bulk demo — SUBMITTED RFQ sample (partially resolved)"), ACTOR).id();
        QuotationRequestLineResponse qr2Line1 = qrService.addLine(qr2, new QuotationRequestAddLineRequest(hkLines.get(1).getId(), null));
        QuotationRequestLineResponse qr2Line2 = qrService.addLine(qr2, new QuotationRequestAddLineRequest(hkLines.get(2).getId(), null));
        qrService.addSupplier(qr2, new QuotationRequestAddSupplierRequest(byCode(suppliers, "SUP-001").id()));
        qrService.addSupplier(qr2, new QuotationRequestAddSupplierRequest(byCode(suppliers, "SUP-007").id()));
        qrService.submit(qr2, "housekeeping-lead");
        qrService.recordResponse(qr2, qr2Line1.id(), byCode(suppliers, "SUP-001").id(), new QuotationResponseLineRequest(new BigDecimal("18.00"), 5, null), "purchasing-clerk");
        QuotationResponseLineResponse qr2Resp2 = qrService.recordResponse(qr2, qr2Line1.id(), byCode(suppliers, "SUP-007").id(), new QuotationResponseLineRequest(new BigDecimal("16.00"), 3, "Cheaper and faster"), "purchasing-clerk");
        qrService.recordResponse(qr2, qr2Line2.id(), byCode(suppliers, "SUP-001").id(), new QuotationResponseLineRequest(new BigDecimal("25.00"), 7, null), "purchasing-clerk");
        qrService.award(qr2, qr2Line1.id(), new QuotationRequestAwardRequest(qr2Resp2.id()), "purchasing-manager");

        // QR3 — SUBMITTED then COMPLETED via convertAwardedLines (also creates PO7)
        Long qr3 = qrService.create(new QuotationRequestCreateRequest(rfqLab.getLocation().getId(), today.minusDays(3), "Bulk demo — COMPLETED RFQ sample (converted to PO)"), ACTOR).id();
        QuotationRequestLineResponse qr3Line = qrService.addLine(qr3, new QuotationRequestAddLineRequest(labLines.get(0).getId(), null));
        qrService.addSupplier(qr3, new QuotationRequestAddSupplierRequest(byCode(suppliers, "SUP-003").id()));
        qrService.addSupplier(qr3, new QuotationRequestAddSupplierRequest(byCode(suppliers, "SUP-005").id()));
        qrService.submit(qr3, "lab-incharge");
        QuotationResponseLineResponse qr3Resp3m = qrService.recordResponse(qr3, qr3Line.id(), byCode(suppliers, "SUP-003").id(), new QuotationResponseLineRequest(new BigDecimal("85.00"), 4, null), "purchasing-clerk");
        qrService.recordResponse(qr3, qr3Line.id(), byCode(suppliers, "SUP-005").id(), new QuotationResponseLineRequest(new BigDecimal("92.00"), 6, null), "purchasing-clerk");
        qrService.award(qr3, qr3Line.id(), new QuotationRequestAwardRequest(qr3Resp3m.id()), "purchasing-manager");
        List<PurchaseOrderResponse> convertedOrders = qrService.convertAwardedLines(qr3, ACTOR);
        for (PurchaseOrderResponse order : convertedOrders) {
            poService.order(order.id(), ACTOR);
        }

        // QR4 — CANCELLED (abandoned before submission)
        Long qr4 = qrService.create(new QuotationRequestCreateRequest(rfqOffice.getLocation().getId(), today, "Bulk demo — CANCELLED RFQ sample"), ACTOR).id();
        qrService.addLine(qr4, new QuotationRequestAddLineRequest(officeLines.get(0).getId(), null));
        qrService.addSupplier(qr4, new QuotationRequestAddSupplierRequest(byCode(suppliers, "SUP-006").id()));
        qrService.cancel(qr4);

        log.info("✓ Seeded 4 Quotation Requests (DRAFT/SUBMITTED-partial/COMPLETED-converted-to-PO/CANCELLED)");
    }

    // ── Wanted List ───────────────────────────────────────────────────────

    private void seedWantedList(StockBalanceRepository stockBalanceRepo, StockMovementService stockMovementService,
                                 WantedListService wantedListService, ProductRepository productRepo,
                                 InventoryLocationRepository locationRepo) {
        Product syringe10ml = byName(productRepo.findAll(), "Syringe 10ml");
        InventoryLocation mainStore = byLocationName(locationRepo, "Main Store");
        BigDecimal onHand = stockBalanceRepo.sumQtyForProductAndLocation(syringe10ml.getId(), mainStore.getId());
        BigDecimal reorderLevel = syringe10ml.getReorderLevel();
        if (onHand != null && reorderLevel != null && onHand.compareTo(reorderLevel) >= 0) {
            BigDecimal target = reorderLevel.subtract(new BigDecimal("3")).max(BigDecimal.ONE);
            BigDecimal decreaseBy = onHand.subtract(target);
            stockMovementService.recordMovement(new StockMovementRequest(
                syringe10ml.getId(), null, mainStore.getId(), null, null,
                "ADJUSTMENT", "DECREASE", decreaseBy, null,
                "Bulk demo — engineered reorder-level breach for the Wanted List shortage job", null
            ), ACTOR);
        }
        // Same method Wanted List's "Run Now" action calls — a genuine MRP-style netting pass
        // against real stock balances/reorder levels, not fabricated rows.
        int created = wantedListService.generate();
        log.info("✓ Wanted List shortage-netting run created {} new line(s) (existing unresolved lines were left as-is)", created);
    }

    // ── Equipment & Asset Management (Phase 3) ───────────────────────────

    private static final String M_ASSET_ONBOARDING_REQ = "Bulk demo — Asset onboarding sourcing (IT hardware)";
    private static final String M_ASSET_ONBOARDING_PO = "Bulk demo — Asset onboarding PO (IT hardware)";

    /**
     * Builds one real Requisition → PO → Goods Receipt chain for two IT-asset products (Laptop,
     * External HDD 1TB — both {@code NONE}-tracked, avoiding the batch-tracking gap Phase 2 hit),
     * so {@code seedAssets} has genuine {@code GoodsReceiptLine} ids to link a couple of assets
     * back to, exercising the entity's "onboarded through procure→receive" path for real rather
     * than fabricating the FK. Returns the two line ids in creation order (Laptop, External HDD).
     * Resumable: if the marker PO already exists, re-derives the line ids from its own Goods
     * Receipt instead of re-creating anything.
     */
    private List<Long> seedAssetOnboardingReceipt(
            PurchaseRequisitionService requisitionService,
            PurchaseOrderRepository poRepo, PurchaseOrderService poService,
            GoodsReceiptRepository grRepo, GoodsReceiptLineRepository grLineRepo, GoodsReceiptService grService,
            InventoryLocationRepository locationRepo, List<Product> products, List<Supplier1> suppliers) {

        var existingPo = poRepo.findAll().stream().filter(po -> M_ASSET_ONBOARDING_PO.equals(po.getNotes())).findFirst();
        if (existingPo.isPresent()) {
            var gr = grRepo.findAll().stream()
                .filter(g -> g.getPurchaseOrder().getId().equals(existingPo.get().getId())).findFirst()
                .orElseThrow(() -> new IllegalStateException("Asset-onboarding PO exists but its Goods Receipt is missing — investigate before re-running"));
            List<Long> lineIds = grLineRepo.findByGoodsReceiptIdOrderByIdAsc(gr.getId()).stream().map(l -> l.getId()).toList();
            log.info("Asset-onboarding Requisition/PO/Goods Receipt already exist — reusing {} line id(s)", lineIds.size());
            return lineIds;
        }

        InventoryLocation office = byLocationName(locationRepo, "Office Store Point");
        LocalDate today = LocalDate.now();

        Long reqId = requisitionService.create(new PurchaseRequisitionCreateRequest(office.getId(), today.minusDays(35), M_ASSET_ONBOARDING_REQ), ACTOR).id();
        var l1 = addLine(requisitionService, reqId, byName(products, "Laptop").getId(), "1");
        var l2 = addLine(requisitionService, reqId, byName(products, "External HDD 1TB").getId(), "1");
        requisitionService.submit(reqId, "office-clerk");
        requisitionService.approveLine(reqId, l1.id(), new PurchaseRequisitionResolutionRequest("Route to PO — asset onboarding"), "store-keeper");
        requisitionService.approveLine(reqId, l2.id(), new PurchaseRequisitionResolutionRequest("Route to PO — asset onboarding"), "store-keeper");

        Long poId = poService.create(new PurchaseOrderCreateRequest(byCode(suppliers, "SUP-006").id(), office.getId(),
            today.minusDays(30), today.minusDays(20), null, null, M_ASSET_ONBOARDING_PO), ACTOR).id();
        PurchaseOrderItemResponse poLine1 = poService.addLine(poId, new PurchaseOrderAddLineRequest(l1.id(), null, null, null, new BigDecimal("45000.00"), null, null));
        PurchaseOrderItemResponse poLine2 = poService.addLine(poId, new PurchaseOrderAddLineRequest(l2.id(), null, null, null, new BigDecimal("4200.00"), null, null));
        poService.order(poId, ACTOR);

        Long grId = grService.create(new GoodsReceiptCreateRequest(poId, today.minusDays(18), "Received in full — IT hardware for asset onboarding"), ACTOR).id();
        GoodsReceiptLineResponse grLine1 = grService.addLine(grId, new GoodsReceiptAddLineRequest(poLine1.id(), BigDecimal.ONE, null, null, null, null, null, null));
        GoodsReceiptLineResponse grLine2 = grService.addLine(grId, new GoodsReceiptAddLineRequest(poLine2.id(), BigDecimal.ONE, null, null, null, null, null, null));
        grService.confirm(grId, ACTOR);

        log.info("✓ Seeded asset-onboarding Requisition→PO→Goods Receipt chain (Laptop + External HDD 1TB)");
        return List.of(grLine1.id(), grLine2.id());
    }

    /**
     * 21 assets across every {@link com.cms.inventory.asset.model.enums.AssetStatus} except
     * {@code AVAILABLE} (14 IN_USE / 3 UNDER_MAINTENANCE / 2 RETIRED / 2 DISPOSED), drawn only
     * from the catalog's 20 {@code isAsset=true} products (10 "Computers" + 10 "Medical
     * Equipment") so categories stay realistic, with varied purchase value/date/useful
     * life/salvage so the Depreciation Summary report shows genuinely different book values per
     * category (some assets are even purchased further back than their useful life, to exercise
     * the calculator's salvage-value floor). Two assets (one Laptop, the disposed External HDD)
     * link back to {@code seedAssetOnboardingReceipt}'s real Goods Receipt lines; every other
     * asset is standalone ("already-owned, being onboarded"), matching the entity's dual creation
     * path 15-25 assets deep, per the plan.
     */
    private void seedAssets(AssetRepository assetRepo, AssetService assetService, ProductRepository productRepo,
                             InventoryLocationRepository locationRepo, List<Long> onboardingGrLineIds) {
        if (assetRepo.count() > 0) {
            log.info("Assets already exist — skipping this run.");
            return;
        }
        List<Product> products = productRepo.findAll();
        LocalDate today = LocalDate.now();

        record Seed(String tag, String serial, String productName, String locationName,
                    String purchaseValue, int monthsAgo, int usefulLifeMonths, String salvageValue,
                    String status, Long grLineId, String disposalReason, String disposalValue, Integer disposalMonthsAgo) {}

        List<Seed> seeds = List.of(
            // IT Assets — "Computers" category
            new Seed("AST-IT-001", "LT2024-001", "Laptop", "Office Store Point", "55000.00", 24, 36, "5000.00", "IN_USE", null, null, null, null),
            new Seed("AST-IT-002", "LT2025-014", "Laptop", "Office Store Point", "58000.00", 11, 36, "5000.00", "IN_USE", onboardingGrLineIds.get(0), null, null, null),
            new Seed("AST-IT-003", null, "Desktop Monitor", "Office Store Point", "12000.00", 18, 60, "1000.00", "IN_USE", null, null, null, null),
            new Seed("AST-IT-004", "LP-778812", "Laser Printer", "Office Store Point", "18000.00", 36, 48, "1500.00", "UNDER_MAINTENANCE", null, null, null, null),
            new Seed("AST-IT-005", "PJ-991200", "Projector", "Lab Requesting Point", "45000.00", 22, 60, "4000.00", "IN_USE", null, null, null, null),
            new Seed("AST-IT-006", null, "UPS 1kVA", "Office Store Point", "8000.00", 10, 36, "500.00", "IN_USE", null, null, null, null),
            new Seed("AST-IT-007", "WR-330045", "Wi-Fi Router", "Office Store Point", "5000.00", 48, 36, "200.00", "RETIRED", null, null, null, null),
            new Seed("AST-IT-008", "EH-556677", "External HDD 1TB", "Office Store Point", "4500.00", 36, 24, "200.00", "DISPOSED", onboardingGrLineIds.get(1), "Drive failure — beyond economical repair", "150.00", 2),
            // Medical Equipment
            new Seed("AST-MED-001", "ECG-ND-2201", "ECG Machine", "ICU Requesting Point", "250000.00", 24, 84, "20000.00", "IN_USE", null, null, null, null),
            new Seed("AST-MED-002", null, "Nebulizer", "Ward A Requesting Point", "15000.00", 12, 60, "1000.00", "IN_USE", null, null, null, null),
            new Seed("AST-MED-003", "IP-778001", "Infusion Pump", "ICU Requesting Point", "60000.00", 18, 72, "5000.00", "IN_USE", null, null, null, null),
            new Seed("AST-MED-004", null, "Wheelchair", "Ward B Requesting Point", "12000.00", 24, 60, "1000.00", "IN_USE", null, null, null, null),
            new Seed("AST-MED-005", null, "Wheelchair", "Ward A Requesting Point", "12500.00", 36, 60, "1000.00", "UNDER_MAINTENANCE", null, null, null, null),
            new Seed("AST-MED-006", "HB-100234", "Hospital Bed", "Ward A Requesting Point", "45000.00", 24, 96, "3000.00", "IN_USE", null, null, null, null),
            new Seed("AST-MED-007", "HB-100235", "Hospital Bed", "Ward B Requesting Point", "46000.00", 12, 96, "3000.00", "IN_USE", null, null, null, null),
            new Seed("AST-MED-008", "OXC-556", "Oxygen Concentrator", "ICU Requesting Point", "55000.00", 11, 60, "4000.00", "IN_USE", null, null, null, null),
            new Seed("AST-MED-009", null, "Pulse Oximeter", "OT Requesting Point", "8000.00", 6, 48, "500.00", "IN_USE", null, null, null, null),
            new Seed("AST-MED-010", "AUTO-8871", "Autoclave", "Lab Requesting Point", "120000.00", 24, 84, "10000.00", "IN_USE", null, null, null, null),
            new Seed("AST-MED-011", "SUC-4432", "Suction Machine", "OT Requesting Point", "35000.00", 30, 60, "2500.00", "UNDER_MAINTENANCE", null, null, null, null),
            new Seed("AST-MED-012", null, "Digital Weighing Scale", "Ward B Requesting Point", "6000.00", 60, 48, "300.00", "RETIRED", null, null, null, null),
            new Seed("AST-MED-013", "ECG-OLD-115", "ECG Machine", "OT Requesting Point", "240000.00", 72, 84, "20000.00", "DISPOSED", null, "Obsolete model, replaced by AST-MED-001 — parts no longer available", "15000.00", 3)
        );

        for (Seed s : seeds) {
            Long locationId = byLocationName(locationRepo, s.locationName()).getId();
            Long productId = byName(products, s.productName()).getId();
            AssetResponse created = assetService.create(new AssetRequest(
                productId, locationId, s.tag(), s.serial(), s.grLineId(),
                new BigDecimal(s.purchaseValue()), today.minusMonths(s.monthsAgo()), s.usefulLifeMonths(), new BigDecimal(s.salvageValue()),
                "Bulk demo asset"));
            if ("DISPOSED".equals(s.status())) {
                assetService.dispose(created.id(), new AssetDisposalRequest(
                    today.minusMonths(s.disposalMonthsAgo()), new BigDecimal(s.disposalValue()), s.disposalReason()), ACTOR);
            } else if (!"AVAILABLE".equals(s.status())) {
                assetService.updateStatus(created.id(), new AssetStatusUpdateRequest(s.status(), null));
            }
        }
        log.info("✓ Seeded {} assets (14 IN_USE / 3 UNDER_MAINTENANCE / 2 RETIRED / 2 DISPOSED)", assetRepo.count());
    }

    /** 6 schedules against a subset of assets — 4 RECURRING/2 ONE_OFF, 3 overdue/3 upcoming, one
     *  ({@code AST-MED-008}) exercising {@code markPerformed} so it carries real history
     *  ({@code lastPerformedDate} set, {@code nextDueDate} genuinely advanced by the recurrence
     *  interval) rather than every row being freshly created and untouched. */
    private void seedMaintenanceSchedules(AssetMaintenanceScheduleRepository scheduleRepo,
                                           AssetMaintenanceScheduleService scheduleService, List<Asset> assets) {
        if (scheduleRepo.count() > 0) {
            log.info("Maintenance schedules already exist — skipping this run.");
            return;
        }
        LocalDate today = LocalDate.now();

        scheduleService.create(new AssetMaintenanceScheduleRequest(
            assetByTag(assets, "AST-MED-001").getId(), "RECURRING", 90, today.minusDays(10), "Quarterly ECG calibration"));
        scheduleService.create(new AssetMaintenanceScheduleRequest(
            assetByTag(assets, "AST-MED-003").getId(), "RECURRING", 180, today.plusDays(20), "Half-yearly infusion pump service"));
        scheduleService.create(new AssetMaintenanceScheduleRequest(
            assetByTag(assets, "AST-MED-006").getId(), "ONE_OFF", null, today.plusDays(45), "One-time bed frame inspection"));
        scheduleService.create(new AssetMaintenanceScheduleRequest(
            assetByTag(assets, "AST-MED-010").getId(), "RECURRING", 30, today.minusDays(3), "Monthly autoclave pressure-valve check"));
        scheduleService.create(new AssetMaintenanceScheduleRequest(
            assetByTag(assets, "AST-IT-004").getId(), "ONE_OFF", null, today.minusDays(1), "Printer fuser unit replacement"));

        var oxygenSchedule = scheduleService.create(new AssetMaintenanceScheduleRequest(
            assetByTag(assets, "AST-MED-008").getId(), "RECURRING", 120, today.minusDays(60), "Quarterly oxygen concentrator filter service"));
        scheduleService.markPerformed(oxygenSchedule.id(), new AssetMaintenanceMarkPerformedRequest(today.minusDays(58), "Filter replaced, unit tested"));

        log.info("✓ Seeded {} maintenance schedules (recurring+one-off, overdue+upcoming)", scheduleRepo.count());
    }

    /** 4 contracts against a subset of assets — one long-active, one IT-asset active, one
     *  expiring soon, one already expired, so the renewal-reminder concept has real varied data. */
    private void seedServiceContracts(AssetServiceContractRepository contractRepo,
                                       AssetServiceContractService contractService, List<Asset> assets, List<Supplier1> suppliers) {
        if (contractRepo.count() > 0) {
            log.info("Service contracts already exist — skipping this run.");
            return;
        }
        LocalDate today = LocalDate.now();

        contractService.create(new AssetServiceContractRequest(
            assetByTag(assets, "AST-MED-001").getId(), byCode(suppliers, "SUP-003").id(), "SC-ECG-2026",
            today.minusMonths(6), today.plusMonths(6), today.plusMonths(6).minusDays(30), "Annual comprehensive maintenance — parts and labour", true));
        contractService.create(new AssetServiceContractRequest(
            assetByTag(assets, "AST-MED-010").getId(), byCode(suppliers, "SUP-004").id(), "SC-AUTO-2025",
            today.minusMonths(11), today.plusDays(20), today.plusDays(5), "Annual autoclave service contract — expiring soon", true));
        contractService.create(new AssetServiceContractRequest(
            assetByTag(assets, "AST-MED-006").getId(), byCode(suppliers, "SUP-002").id(), "SC-BED-2024",
            today.minusMonths(24), today.minusMonths(2), null, "Bed frame AMC — lapsed, not yet renewed", false));
        contractService.create(new AssetServiceContractRequest(
            assetByTag(assets, "AST-IT-001").getId(), byCode(suppliers, "SUP-006").id(), "SC-LT-2026",
            today.minusMonths(3), today.plusMonths(9), today.plusMonths(9).minusDays(15), "On-site laptop warranty extension", true));

        log.info("✓ Seeded {} service contracts (active×2/expiring-soon/expired)", contractRepo.count());
    }

    private Asset assetByTag(List<Asset> assets, String tag) {
        return assets.stream().filter(a -> tag.equals(a.getAssetTag())).findFirst()
            .orElseThrow(() -> new IllegalStateException("Asset '" + tag + "' not found — seedAssets must run first"));
    }

    // ── Small lookup helpers ─────────────────────────────────────────────

    private Product byName(List<Product> products, String name) {
        return products.stream().filter(p -> name.equals(p.getProductName())).findFirst()
            .orElseThrow(() -> new IllegalStateException("Product '" + name + "' not found — run InventoryBulkDemoDataSeeder first"));
    }

    private InventoryLocation byLocationName(InventoryLocationRepository locationRepo, String name) {
        return locationRepo.findAllByOrderByVirtualNameAsc().stream().filter(l -> name.equals(l.getVirtualName())).findFirst()
            .orElseThrow(() -> new IllegalStateException("Location '" + name + "' not found — run InventoryBulkDemoDataSeeder first"));
    }
}
