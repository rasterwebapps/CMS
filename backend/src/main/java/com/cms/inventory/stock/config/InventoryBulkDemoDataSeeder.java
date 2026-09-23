package com.cms.inventory.stock.config;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;

import com.cms.inventory.catalog.model.Brand;
import com.cms.inventory.catalog.model.Category;
import com.cms.inventory.catalog.model.Product;
import com.cms.inventory.catalog.model.Uom;
import com.cms.inventory.catalog.model.enums.StockTrackingMode;
import com.cms.inventory.catalog.repository.BrandRepository;
import com.cms.inventory.catalog.repository.CategoryRepository;
import com.cms.inventory.catalog.repository.ProductRepository;
import com.cms.inventory.catalog.repository.UomRepository;
import com.cms.inventory.issue.dto.LoanableItemIssueCreateRequest;
import com.cms.inventory.issue.dto.LoanableItemIssueReturnRequest;
import com.cms.inventory.indent.dto.StockIndentAddLineRequest;
import com.cms.inventory.indent.dto.StockIndentCreateRequest;
import com.cms.inventory.indent.dto.StockIndentResolutionRequest;
import com.cms.inventory.indent.dto.StockIndentResponse;
import com.cms.inventory.indent.dto.StockIndentReturnLineRequest;
import com.cms.inventory.issue.service.LoanableItemIssueService;
import com.cms.inventory.indent.service.StockIndentService;
import com.cms.inventory.stock.dto.StockMovementRequest;
import com.cms.inventory.stock.dto.StockTransferAddLineRequest;
import com.cms.inventory.stock.dto.StockTransferCreateRequest;
import com.cms.inventory.stock.dto.StockTransferResponse;
import com.cms.inventory.stock.model.InventoryLocation;
import com.cms.inventory.stock.model.enums.LocationRole;
import com.cms.inventory.stock.repository.InventoryLocationRepository;
import com.cms.inventory.stock.service.StockMovementService;
import com.cms.inventory.stock.service.StockTransferService;
import com.cms.model.Room;
import com.cms.repository.RoomRepository;

/**
 * One-off bulk demo-data generator for the Stock Management nav group — masters, sister/store
 * locations (respecting the {@code LocationRole} gate), 100+ products across realistic nursing-
 * college categories, a mix of batch-tracked and untracked opening balances, and Stock Indents
 * / Stock Transfers / Loanable Item Issues in varied lifecycle states, so every list
 * screen under Stock Management has real, varied data to click through during a manual QA pass.
 * Deliberately its own opt-in flag ({@code cms.seed.bulk-inventory-demo=true}), separate from the
 * always-on {@code cms.seed.enabled} seeders, so it never fires on a normal local boot — run once
 * intentionally. Idempotent per phase (masters/locations/products always safe to re-run; stock
 * movements/indents/transfers/loanable issues each separately skip once their own table has
 * any rows) rather than gated on a single product count, so a run that fails partway through (as
 * the first one did — see the "Bulk demo data" DECISION_LOG entry) can simply be re-run to finish.
 * See docs/inventory-management/DECISION_LOG.md's 2026-09-15 "Bulk demo data" entry.
 */
@Configuration
@Profile("local")
@ConditionalOnProperty(prefix = "cms.seed", name = "bulk-inventory-demo", havingValue = "true")
public class InventoryBulkDemoDataSeeder {

    private static final Logger log = LoggerFactory.getLogger(InventoryBulkDemoDataSeeder.class);

    @Bean
    @Order(150)
    @Transactional
    CommandLineRunner seedBulkInventoryDemoData(
            RoomRepository roomRepo,
            CategoryRepository categoryRepo,
            BrandRepository brandRepo,
            UomRepository uomRepo,
            ProductRepository productRepo,
            InventoryLocationRepository locationRepo,
            StockMovementService stockMovementService,
            StockIndentService indentService,
            StockTransferService transferService,
            LoanableItemIssueService loanableItemIssueService,
            com.cms.inventory.stock.repository.StockBalanceRepository balanceRepo,
            com.cms.inventory.indent.repository.StockIndentRepository indentRepo,
            com.cms.inventory.stock.repository.StockTransferRepository transferRepo,
            com.cms.inventory.issue.repository.LoanableItemIssueRepository loanableRepo) {
        return args -> {
            log.info("🌱 Seeding BULK inventory demo data (masters, locations, 100+ products, batches, requests, transfers)...");

            Map<String, Category> categories = ensureCategories(categoryRepo);
            List<Brand> brands = ensureBrands(brandRepo);
            List<Uom> uoms = uomRepo.findByIsActiveTrueOrderByNameAsc();
            if (uoms.isEmpty()) {
                log.warn("No active UOMs found — skipping bulk inventory seed (run the base catalog seeder first).");
                return;
            }

            List<Room> rooms = roomRepo.findAll(PageRequest.of(0, 12)).getContent();
            if (rooms.isEmpty()) {
                log.warn("No Rooms found — skipping bulk inventory seed.");
                return;
            }
            Locations locations = ensureLocations(locationRepo, rooms);

            seedProducts(productRepo, categories, brands, uoms);
            // Re-read the FULL catalog rather than using seedProducts' own return value — that
            // return is only the products actually created THIS run (empty once they already
            // exist from a prior run), but every phase below needs the whole current catalog to
            // pick demo lines from, not just this run's delta.
            List<Product> products = productRepo.findAll();
            log.info("✓ {} product(s) now in catalog", products.size());

            List<Product> consumables = products.stream().filter(p -> Boolean.TRUE.equals(p.getIsConsumable())).toList();
            if (balanceRepo.count() == 0) {
                seedStockMovements(stockMovementService, productRepo, consumables, locations.mainStore());
                log.info("✓ Posted opening balances/batches for {} consumable product(s)", consumables.size());
            } else {
                log.info("Stock balances already exist — skipping opening-balance seeding this run.");
            }

            // Neither StockIndentAddLineRequest nor StockTransferAddLineRequest carries a
            // batch/serial number — approveLine()/complete() then fail requireTrackingModeCompliance
            // for a BATCH/SERIAL-tracked product (see docs/inventory-management/DECISION_LOG.md's
            // 2026-09-15 "Bulk demo data" entry — flagged there as a real gap, not fixed here).
            // Route demo Indents/Transfers only through untracked products until that's fixed.
            List<Product> untrackedConsumables = consumables.stream()
                .filter(p -> p.getTrackingMode() == StockTrackingMode.NONE).toList();
            if (indentRepo.count() == 0) {
                seedIndents(indentService, untrackedConsumables, locations);
            } else {
                log.info("Stock indents already exist — skipping this run.");
            }
            if (transferRepo.count() == 0) {
                seedTransfers(transferService, stockMovementService, untrackedConsumables, locations);
            } else {
                log.info("Stock transfers already exist — skipping this run.");
            }

            List<Product> loanables = products.stream().filter(p -> Boolean.TRUE.equals(p.getIsLoanable())).toList();
            if (loanableRepo.count() == 0) {
                seedLoanableIssues(loanableItemIssueService, loanables, locations);
            } else {
                log.info("Loanable item issues already exist — skipping this run.");
            }

            log.info("✓ Bulk inventory demo data seeding complete.");
        };
    }

    // ── Locations ──────────────────────────────────────────────────────────

    private record Locations(InventoryLocation mainStore, List<InventoryLocation> sisters) {}

    private Locations ensureLocations(InventoryLocationRepository locationRepo, List<Room> rooms) {
        List<InventoryLocation> existing = locationRepo.findAllByOrderByVirtualNameAsc();
        InventoryLocation mainStore = existing.stream().filter(l -> "Main Store".equals(l.getVirtualName())).findFirst()
            .orElseGet(() -> locationRepo.save(location(rooms.get(0), "Main Store", LocationRole.STORE, "Central inventory store")));

        List<String> sisterNames = List.of(
            "Lab Requesting Point", "Ward A Requesting Point", "Ward B Requesting Point",
            "OT Requesting Point", "ICU Requesting Point", "Housekeeping Requesting Point", "Office Store Point");
        List<InventoryLocation> sisters = new ArrayList<>();
        int roomIdx = 1;
        for (String name : sisterNames) {
            InventoryLocation loc = existing.stream().filter(l -> name.equals(l.getVirtualName())).findFirst().orElse(null);
            if (loc == null) {
                Room room = rooms.get(roomIdx % rooms.size());
                loc = locationRepo.save(location(room, name, LocationRole.REQUESTING_POINT, "Draws stock from Main Store"));
            }
            sisters.add(loc);
            roomIdx++;
        }
        InventoryLocation pharmacyStore = existing.stream().filter(l -> "Pharmacy Store".equals(l.getVirtualName())).findFirst()
            .orElseGet(() -> locationRepo.save(location(rooms.get(rooms.size() - 1), "Pharmacy Store", LocationRole.STORE, "Pharmacy-owned stock, separate from Main Store")));
        sisters.add(pharmacyStore); // usable as a second store-ish location for transfer variety, not a requesting point

        log.info("✓ {} inventory location(s) in place (Main Store + {} others)", 2 + sisters.size() - 1, sisters.size());
        return new Locations(mainStore, sisters);
    }

    private static InventoryLocation location(Room room, String virtualName, LocationRole role, String description) {
        InventoryLocation loc = new InventoryLocation();
        loc.setRoom(room);
        loc.setVirtualName(virtualName);
        loc.setLocationRole(role);
        loc.setDescription(description);
        return loc;
    }

    // ── Masters: Categories / Brands ─────────────────────────────────────────

    private Map<String, Category> ensureCategories(CategoryRepository categoryRepo) {
        List<Category> existing = categoryRepo.findAllByOrderByNameAsc();
        Map<String, Category> byName = new java.util.HashMap<>();
        for (Category c : existing) byName.put(c.getName(), c);

        List<String[]> wanted = List.of(
            new String[]{"Nursing Consumables", null},
            new String[]{"Housekeeping Supplies", null},
            new String[]{"Office Supplies", null},
            new String[]{"Linen & Bedding", null},
            new String[]{"Pharmacy Stock", null}
        );
        for (String[] w : wanted) {
            byName.computeIfAbsent(w[0], n -> categoryRepo.save(category(n, null, n + " (demo data)")));
        }
        log.info("✓ {} categories in place", categoryRepo.count());
        return byName;
    }

    private static Category category(String name, Category parent, String description) {
        Category c = new Category();
        c.setName(name);
        c.setParentCategory(parent);
        c.setDescription(description);
        return c;
    }

    private List<Brand> ensureBrands(BrandRepository brandRepo) {
        List<String> wanted = List.of("3M", "Johnson & Johnson", "BD", "Dell", "HP",
            "Philips Healthcare", "GE Healthcare", "Medline", "Local Vendor", "Generic");
        List<Brand> brands = new ArrayList<>();
        for (String name : wanted) {
            Brand b = brandRepo.findByIsActiveTrueOrderByNameAsc().stream().filter(x -> x.getName().equals(name)).findFirst()
                .orElseGet(() -> {
                    Brand nb = new Brand();
                    nb.setName(name);
                    nb.setDescription(name + " (demo data)");
                    return brandRepo.save(nb);
                });
            brands.add(b);
        }
        log.info("✓ {} brands in place", brandRepo.count());
        return brands;
    }

    // ── Products ──────────────────────────────────────────────────────────

    private record Seed(String prefix, String name, boolean asset, boolean consumable, boolean loanable, boolean batchTracked) {}

    private List<Product> seedProducts(ProductRepository productRepo, Map<String, Category> categories, List<Brand> brands, List<Uom> uoms) {
        List<Seed> seeds = new ArrayList<>();
        // Nursing Consumables (30)
        for (String n : List.of("Syringe 5ml", "Syringe 10ml", "IV Cannula 18G", "IV Cannula 20G", "IV Cannula 22G",
                "Gauze Roll", "Cotton Roll", "Surgical Gloves S", "Surgical Gloves M", "Surgical Gloves L",
                "Face Mask", "N95 Mask", "Alcohol Swabs", "Crepe Bandage", "Foley Catheter 14",
                "Foley Catheter 16", "Nasogastric Tube", "Suction Catheter", "Digital Thermometer", "BP Cuff",
                "Disposable Bedsheet", "Urine Bag", "IV Infusion Set", "Blood Collection Tube", "Cannula Fixer Tape",
                "Adhesive Tape", "Hand Sanitizer 500ml", "Surface Disinfectant 5L", "Wound Dressing Kit", "Tongue Depressor")) {
            seeds.add(new Seed("NUR", n, false, true, false, seeds.size() % 4 == 0));
        }
        // Lab Consumables / Chemicals (15)
        for (String n : List.of("Distilled Water 1L", "Formalin 10%", "Methylene Blue Stain", "Litmus Paper Strips",
                "pH Test Strips", "Petri Dishes (pack)", "Glass Pipette 10ml", "Glass Beaker 250ml", "Measuring Cylinder 100ml",
                "Microscope Slides (box)", "Cover Slips (box)", "Iodine Solution", "Hydrogen Peroxide 500ml", "Acetone 1L",
                "Glucose Test Strips")) {
            seeds.add(new Seed("LAB", n, false, true, false, seeds.size() % 5 == 0));
        }
        // Glassware (5)
        for (String n : List.of("Conical Flask 250ml", "Beaker Set", "Measuring Jar 500ml", "Glass Funnel", "Burette 50ml")) {
            seeds.add(new Seed("GLS", n, false, true, false, false));
        }
        // IT Assets (10)
        for (String n : List.of("Laptop", "Desktop Monitor", "Laser Printer", "Projector", "UPS 1kVA",
                "Keyboard", "Wireless Mouse", "Webcam", "Wi-Fi Router", "External HDD 1TB")) {
            boolean loanable = n.equals("Projector");
            seeds.add(new Seed("ITA", n, true, false, loanable, false));
        }
        // Medical Equipment (10) — mostly assets, some loanable
        for (String n : List.of("ECG Machine", "Nebulizer", "Infusion Pump", "Wheelchair", "Hospital Bed",
                "Oxygen Concentrator", "Pulse Oximeter", "Autoclave", "Suction Machine", "Digital Weighing Scale")) {
            boolean loanable = n.equals("Wheelchair") || n.equals("Nebulizer") || n.equals("Infusion Pump");
            seeds.add(new Seed("MED", n, true, false, loanable, false));
        }
        // Housekeeping Supplies (10)
        for (String n : List.of("Mop", "Broom", "Floor Cleaner 5L", "Toilet Cleaner 1L", "Garbage Bags (roll)",
                "Dustbin", "Wiping Cloth (pack)", "Air Freshener", "Glass Cleaner 500ml", "Cleaning Bucket")) {
            seeds.add(new Seed("HSK", n, false, true, false, false));
        }
        // Office Supplies (10)
        for (String n : List.of("A4 Paper Ream", "Ball Pen (box)", "Stapler", "File Folder", "Whiteboard Marker",
                "Printer Ink Cartridge", "Envelope (pack)", "Notebook", "Highlighter", "Correction Fluid")) {
            seeds.add(new Seed("OFC", n, false, true, false, false));
        }
        // Linen & Bedding (5)
        for (String n : List.of("Bedsheet", "Pillow Cover", "Blanket", "Patient Gown", "Privacy Curtain")) {
            seeds.add(new Seed("LIN", n, false, true, false, false));
        }
        // Pharmacy Stock (10) — batch/expiry tracked
        for (String n : List.of("Paracetamol Tablets", "ORS Sachet", "Antiseptic Cream", "Cough Syrup", "Vitamin Tablets",
                "Insulin Vial", "Normal Saline 500ml", "Betadine Solution", "Calamine Lotion", "Eye Drops")) {
            seeds.add(new Seed("PHM", n, false, true, false, true));
        }

        Map<String, Category> categoryByPrefix = Map.of(
            "NUR", categories.get("Nursing Consumables"), "LAB", categories.get("Chemicals"),
            "GLS", categories.get("Glassware"), "ITA", categories.get("Computers"),
            "MED", categories.get("Medical Equipment"), "HSK", categories.get("Housekeeping Supplies"),
            "OFC", categories.get("Office Supplies"), "LIN", categories.get("Linen & Bedding"),
            "PHM", categories.get("Pharmacy Stock"));

        List<Product> created = new ArrayList<>();
        int i = 0;
        for (Seed s : seeds) {
            i++;
            String code = s.prefix() + "-" + String.format("%04d", i);
            if (productRepo.existsByProductCodeIgnoreCase(code)) continue;
            Product p = new Product();
            p.setProductCode(code);
            p.setProductName(s.name());
            p.setCategory(categoryByPrefix.getOrDefault(s.prefix(), categories.values().iterator().next()));
            p.setBaseUom(uoms.get(i % uoms.size()));
            p.setBrand(brands.get(i % brands.size()));
            p.setIsAsset(s.asset());
            p.setIsConsumable(s.consumable());
            p.setIsService(false);
            p.setIsLoanable(s.loanable());
            p.setTrackingMode(s.batchTracked() ? StockTrackingMode.BATCH : StockTrackingMode.NONE);
            if (s.consumable()) {
                p.setReorderLevel(new BigDecimal(10 + (i % 20)));
                p.setReorderQty(new BigDecimal(50 + (i % 50)));
            }
            if (s.asset()) {
                p.setDepreciationRate(new BigDecimal("15.00"));
                p.setWarrantyPeriodMonths(24);
            }
            p.setDescription(s.name() + " (bulk demo data)");
            created.add(productRepo.save(p));
        }
        return created;
    }

    // ── Stock movements: opening balances + batches ───────────────────────

    private void seedStockMovements(StockMovementService stockMovementService, ProductRepository productRepo,
                                     List<Product> consumables, InventoryLocation mainStore) {
        ThreadLocalRandom rnd = ThreadLocalRandom.current();
        int lot = 1;
        for (Product product : consumables) {
            BigDecimal qty = new BigDecimal(20 + rnd.nextInt(180));
            BigDecimal unitCost = new BigDecimal(5 + rnd.nextInt(500)).setScale(2);
            String batchNo = null;
            LocalDate expiry = null;
            if (product.getTrackingMode() == StockTrackingMode.BATCH) {
                batchNo = "LOT-2026-" + String.format("%04d", lot++);
                expiry = LocalDate.now().plusMonths(6 + rnd.nextInt(18));
            }
            stockMovementService.recordMovement(new StockMovementRequest(
                product.getId(), null, mainStore.getId(), batchNo, expiry,
                "RECEIPT", null, qty, unitCost,
                "Opening balance (bulk demo seed)", null
            ), "bulk-seed");
        }
    }

    // ── Stock Indents (varied states) ───────────────────────────────

    private void seedIndents(StockIndentService service, List<Product> consumables, Locations locations) {
        if (consumables.size() < 10) return;
        InventoryLocation wardA = locations.sisters().get(1); // Ward A Requesting Point
        InventoryLocation wardB = locations.sisters().get(2); // Ward B Requesting Point
        InventoryLocation ot = locations.sisters().get(3);    // OT Requesting Point
        InventoryLocation icu = locations.sisters().get(4);   // ICU Requesting Point
        InventoryLocation housekeeping = locations.sisters().get(5);
        InventoryLocation officePoint = locations.sisters().get(6);
        InventoryLocation mainStore = locations.mainStore();

        // 1) DRAFT — never submitted
        StockIndentResponse draft = service.create(new StockIndentCreateRequest(wardA.getId(), mainStore.getId(), LocalDate.now(), "Weekly ward restock"), "bulk-seed");
        addLine(service, draft.id(), consumables.get(0), new BigDecimal("10"));
        addLine(service, draft.id(), consumables.get(1), new BigDecimal("5"));

        // 2) SUBMITTED — lines still pending (awaiting approval)
        StockIndentResponse submitted = service.create(new StockIndentCreateRequest(wardB.getId(), mainStore.getId(), LocalDate.now(), "Monthly consumables"), "bulk-seed");
        addLine(service, submitted.id(), consumables.get(2), new BigDecimal("8"));
        addLine(service, submitted.id(), consumables.get(3), new BigDecimal("12"));
        addLine(service, submitted.id(), consumables.get(4), new BigDecimal("6"));
        service.submit(submitted.id(), "ward-b-clerk");

        // 3) COMPLETED — every line approved
        StockIndentResponse completed = service.create(new StockIndentCreateRequest(ot.getId(), mainStore.getId(), LocalDate.now().minusDays(3), "OT pre-op supplies"), "bulk-seed");
        var l1 = addLine(service, completed.id(), consumables.get(5), new BigDecimal("15"));
        var l2 = addLine(service, completed.id(), consumables.get(6), new BigDecimal("20"));
        service.submit(completed.id(), "ot-incharge");
        service.approveLine(completed.id(), l1.id(), new StockIndentResolutionRequest("Issued"), "store-keeper");
        service.approveLine(completed.id(), l2.id(), new StockIndentResolutionRequest("Issued"), "store-keeper");

        // 4) COMPLETED with one rejected line
        StockIndentResponse mixed = service.create(new StockIndentCreateRequest(icu.getId(), mainStore.getId(), LocalDate.now().minusDays(5), "ICU request"), "bulk-seed");
        var m1 = addLine(service, mixed.id(), consumables.get(7), new BigDecimal("4"));
        var m2 = addLine(service, mixed.id(), consumables.get(8), new BigDecimal("500")); // deliberately large, gets rejected
        service.submit(mixed.id(), "icu-incharge");
        service.approveLine(mixed.id(), m1.id(), new StockIndentResolutionRequest("Issued"), "store-keeper");
        service.rejectLine(mixed.id(), m2.id(), new StockIndentResolutionRequest("Quantity exceeds available stock"), "store-keeper");

        // 5) COMPLETED with an Internal Return on one line
        StockIndentResponse returned = service.create(new StockIndentCreateRequest(housekeeping.getId(), mainStore.getId(), LocalDate.now().minusDays(7), "Housekeeping supplies"), "bulk-seed");
        var r1 = addLine(service, returned.id(), consumables.get(9), new BigDecimal("30"));
        service.submit(returned.id(), "housekeeping-lead");
        service.approveLine(returned.id(), r1.id(), new StockIndentResolutionRequest("Issued"), "store-keeper");
        service.returnLine(returned.id(), r1.id(), new StockIndentReturnLineRequest(new BigDecimal("5"), "Unused, returning surplus"), "housekeeping-lead");

        // 6) CANCELLED — drafted then abandoned
        StockIndentResponse cancelled = service.create(new StockIndentCreateRequest(officePoint.getId(), mainStore.getId(), LocalDate.now(), "Office stationery — cancelled"), "bulk-seed");
        addLine(service, cancelled.id(), consumables.get(10), new BigDecimal("3"));
        service.cancel(cancelled.id());

        log.info("✓ Seeded 6 Stock Indents (DRAFT/SUBMITTED/COMPLETED/rejected-line/returned/CANCELLED)");
    }

    private com.cms.inventory.indent.dto.StockIndentItemResponse addLine(StockIndentService service, Long requestId, Product product, BigDecimal qty) {
        return service.addLine(requestId, new StockIndentAddLineRequest(product.getId(), null, qty, null));
    }

    // ── Stock Transfers (varied states) ────────────────────────────────────

    private void seedTransfers(StockTransferService service, StockMovementService stockMovementService, List<Product> consumables, Locations locations) {
        if (consumables.size() < 15) return;
        InventoryLocation mainStore = locations.mainStore();
        InventoryLocation wardA = locations.sisters().get(1);
        InventoryLocation icu = locations.sisters().get(4);
        InventoryLocation wardB = locations.sisters().get(2);
        InventoryLocation pharmacyStore = locations.sisters().get(locations.sisters().size() - 1);

        // Ward B needs stock of its own before it can return any of it — give it a small opening
        // balance first (a sister location otherwise never receives an opening balance; only Main
        // Store does, per seedStockMovements above).
        Product returnProduct = consumables.get(15);
        stockMovementService.recordMovement(new StockMovementRequest(
            returnProduct.getId(), null, wardB.getId(), null, null,
            "RECEIPT", null, new BigDecimal("20"), new BigDecimal("15.00"),
            "Ward B pre-existing stock (bulk demo seed, so it has something to return)", null
        ), "bulk-seed");

        // 1) DRAFT — never completed
        StockTransferResponse draft = service.create(new StockTransferCreateRequest(mainStore.getId(), wardA.getId(), LocalDate.now(), "Draft transfer, not yet sent"), "bulk-seed");
        addTransferLine(service, draft.id(), consumables.get(11), new BigDecimal("10"));

        // 2) COMPLETED — Main Store -> ICU
        StockTransferResponse completed = service.create(new StockTransferCreateRequest(mainStore.getId(), icu.getId(), LocalDate.now().minusDays(2), "ICU stock top-up"), "bulk-seed");
        addTransferLine(service, completed.id(), consumables.get(12), new BigDecimal("25"));
        addTransferLine(service, completed.id(), consumables.get(13), new BigDecimal("15"));
        service.complete(completed.id(), "store-keeper");

        // 3) CANCELLED
        StockTransferResponse cancelled = service.create(new StockTransferCreateRequest(mainStore.getId(), pharmacyStore.getId(), LocalDate.now(), "Cancelled — wrong destination"), "bulk-seed");
        addTransferLine(service, cancelled.id(), consumables.get(14), new BigDecimal("5"));
        service.cancel(cancelled.id());

        // 4) COMPLETED — sister returning surplus back to the store (REQUESTING_POINT -> STORE, exercises the symmetric role gate)
        StockTransferResponse returnToStore = service.create(new StockTransferCreateRequest(wardB.getId(), mainStore.getId(), LocalDate.now().minusDays(1), "Ward B returning unused surplus to Main Store"), "bulk-seed");
        addTransferLine(service, returnToStore.id(), consumables.get(15), new BigDecimal("8"));
        service.complete(returnToStore.id(), "ward-b-clerk");

        log.info("✓ Seeded 4 Stock Transfers (DRAFT/COMPLETED/CANCELLED/sister-to-store COMPLETED)");
    }

    private void addTransferLine(StockTransferService service, Long transferId, Product product, BigDecimal qty) {
        service.addLine(transferId, new StockTransferAddLineRequest(product.getId(), null, qty, null, null, null));
    }

    // ── Loanable Item Issues ────────────────────────────────────────────────

    private void seedLoanableIssues(LoanableItemIssueService service, List<Product> loanables, Locations locations) {
        if (loanables.isEmpty()) return;
        InventoryLocation mainStore = locations.mainStore();

        var issued = service.create(new LoanableItemIssueCreateRequest(
            loanables.get(0).getId(), mainStore.getId(), "Nurse A. Kumar", "9876543210",
            LocalDate.now().minusDays(2), LocalDate.now().plusDays(5), "Good condition", "Ward round use"), "bulk-seed");

        var overdue = service.create(new LoanableItemIssueCreateRequest(
            loanables.get(loanables.size() > 1 ? 1 : 0).getId(), mainStore.getId(), "Nurse B. Iyer", "9876500000",
            LocalDate.now().minusDays(15), LocalDate.now().minusDays(3), "Good condition", "Should have been returned"), "bulk-seed");

        var returned = service.create(new LoanableItemIssueCreateRequest(
            loanables.get(loanables.size() > 2 ? 2 : 0).getId(), mainStore.getId(), "Nurse C. Reddy", "9876511111",
            LocalDate.now().minusDays(10), LocalDate.now().minusDays(2), "Good condition", "Returned on time"), "bulk-seed");
        service.markReturned(returned.id(), new LoanableItemIssueReturnRequest("Good condition", "Returned clean"), "store-keeper");

        log.info("✓ Seeded 3 Loanable Item Issues (issued / overdue / returned) — ids {}, {}, {}", issued.id(), overdue.id(), returned.id());
    }
}
