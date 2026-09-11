package com.cms.inventory.catalog.config;

import java.math.BigDecimal;
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

import com.cms.inventory.catalog.model.Category;
import com.cms.inventory.catalog.model.CategoryAttribute;
import com.cms.inventory.catalog.model.Product;
import com.cms.inventory.catalog.model.ProductAlias;
import com.cms.inventory.catalog.model.ProductAttributeValue;
import com.cms.inventory.catalog.model.Uom;
import com.cms.inventory.catalog.model.enums.AttributeDataType;
import com.cms.inventory.catalog.repository.CategoryAttributeRepository;
import com.cms.inventory.catalog.repository.CategoryRepository;
import com.cms.inventory.catalog.repository.ProductRepository;
import com.cms.inventory.catalog.repository.UomRepository;

/**
 * Seeds demo Category/Uom/Product data for the Inventory Catalog screens when running with the
 * 'local' profile, so the screens have something to show during development. Deliberately kept
 * as its own small seeder under the inventory package namespace rather than folded into the
 * monolithic {@code com.cms.config.LocalDataSeeder} — see the "own package namespace" decision in
 * docs/inventory-management/DECISION_LOG.md. Demo data intentionally reflects a nursing-college
 * context (matching the rest of the local seed data) — the module's own code stays generic; only
 * this local-dev sample data is deployment-flavoured, same as the existing Speciality demo rows.
 */
@Configuration
@Profile("local")
@ConditionalOnProperty(prefix = "cms.seed", name = "enabled", havingValue = "true")
public class InventoryCatalogLocalDataSeeder {

    private static final Logger log = LoggerFactory.getLogger(InventoryCatalogLocalDataSeeder.class);

    @Bean
    @Order(100)
    @Transactional
    CommandLineRunner seedInventoryCatalogData(
            CategoryRepository categoryRepo,
            CategoryAttributeRepository attributeRepo,
            UomRepository uomRepo,
            ProductRepository productRepo) {
        return args -> {
            if (categoryRepo.count() > 0) {
                log.info("Inventory catalog already seeded, skipping...");
                return;
            }

            log.info("🌱 Seeding demo Inventory Catalog data (Categories, UOMs, Products)...");

            // ── Units of Measure ──────────────────────────────────────────
            Uom uomEach = uomRepo.save(uom("EA", "Each"));
            Uom uomBox10 = uomRepo.save(uom("BOX10", "Box of 10"));
            Uom uomKg = uomRepo.save(uom("KG", "Kilogram"));
            Uom uomLtr = uomRepo.save(uom("LTR", "Litre"));
            log.info("✓ Created 4 units of measure");

            // ── Categories ─────────────────────────────────────────────────
            Category labConsumables = categoryRepo.save(category("Lab Consumables", null, "Items consumed in labs and skill stations"));
            Category chemicals = categoryRepo.save(category("Chemicals", labConsumables, "Reagents and chemical consumables"));
            Category glassware = categoryRepo.save(category("Glassware", labConsumables, "Lab glassware and reusable containers"));
            Category itAssets = categoryRepo.save(category("IT Assets", null, "Computers and IT equipment"));
            Category computers = categoryRepo.save(category("Computers", itAssets, "Desktops and laptops"));
            Category medicalEquipment = categoryRepo.save(category("Medical Equipment", null, "Clinical/skills-lab equipment"));
            log.info("✓ Created 6 categories (2 top-level with sub-categories, 1 standalone)");

            // ── Category Attributes ──────────────────────────────────────────
            CategoryAttribute shelfLife = attributeRepo.save(attribute(chemicals, "Shelf Life", AttributeDataType.TEXT, null, false, 1));
            CategoryAttribute storageCondition = attributeRepo.save(attribute(chemicals, "Storage Condition", AttributeDataType.ENUM,
                "Room Temperature, Refrigerated, Frozen", true, 2));
            CategoryAttribute warrantyProvider = attributeRepo.save(attribute(computers, "Warranty Provider", AttributeDataType.TEXT, null, false, 1));
            log.info("✓ Created 3 category attributes");

            // ── Products ──────────────────────────────────────────────────
            productRepo.save(product(
                "CHM-0001", "Sodium Chloride", chemicals, uomKg,
                new BigDecimal("5"), new BigDecimal("20"),
                false, true, false, false, null, null,
                "General-purpose laboratory-grade salt",
                List.of("NaCl", "Common Salt"),
                List.of(attrValue(shelfLife, "24 months"), attrValue(storageCondition, "Room Temperature"))
            ));

            productRepo.save(product(
                "CHM-0002", "Ethanol 70%", chemicals, uomLtr,
                new BigDecimal("2"), new BigDecimal("10"),
                false, true, false, false, null, null,
                "Disinfectant-grade ethanol solution",
                List.of("Rubbing Alcohol"),
                List.of(attrValue(shelfLife, "12 months"), attrValue(storageCondition, "Room Temperature"))
            ));

            productRepo.save(product(
                "GLS-0001", "Test Tubes", glassware, uomBox10,
                new BigDecimal("3"), new BigDecimal("10"),
                false, true, false, false, null, null,
                "Borosilicate glass test tubes, box of 10",
                List.of(), List.of()
            ));

            productRepo.save(product(
                "ITA-0001", "Desktop Computer", computers, uomEach,
                null, null,
                true, false, false, false, new BigDecimal("15.00"), 36,
                "Standard lab/office desktop workstation",
                List.of(), List.of(attrValue(warrantyProvider, "Dell ProSupport"))
            ));

            productRepo.save(product(
                "MED-0001", "Digital BP Monitor", medicalEquipment, uomEach,
                null, null,
                true, false, false, true, new BigDecimal("10.00"), 24,
                "Digital blood pressure monitor for skills-lab practice",
                List.of("Sphygmomanometer (Digital)"), List.of()
            ));

            log.info("✓ Created 5 products with aliases and attribute values");
        };
    }

    private static Uom uom(String code, String name) {
        Uom u = new Uom();
        u.setCode(code);
        u.setName(name);
        return u;
    }

    private static Category category(String name, Category parent, String description) {
        Category c = new Category();
        c.setName(name);
        c.setParentCategory(parent);
        c.setDescription(description);
        return c;
    }

    private static CategoryAttribute attribute(Category category, String name, AttributeDataType dataType,
                                                String enumOptions, boolean required, int displayOrder) {
        CategoryAttribute a = new CategoryAttribute();
        a.setCategory(category);
        a.setName(name);
        a.setDataType(dataType);
        a.setEnumOptions(enumOptions);
        a.setIsRequired(required);
        a.setDisplayOrder(displayOrder);
        return a;
    }

    /** All demo attribute values here are TEXT or ENUM, so the plain text column is what they use. */
    private static ProductAttributeValue attrValue(CategoryAttribute attribute, String value) {
        ProductAttributeValue v = new ProductAttributeValue();
        v.setAttribute(attribute);
        v.setTextValue(value);
        return v;
    }

    private static Product product(String code, String name, Category category, Uom uom,
                                    BigDecimal reorderLevel, BigDecimal reorderQty,
                                    boolean isAsset, boolean isConsumable, boolean isService, boolean isLoanable,
                                    BigDecimal depreciationRate, Integer warrantyMonths,
                                    String description, List<String> aliases, List<ProductAttributeValue> attributeValues) {
        Product p = new Product();
        p.setProductCode(code);
        p.setProductName(name);
        p.setCategory(category);
        p.setBaseUom(uom);
        p.setReorderLevel(reorderLevel);
        p.setReorderQty(reorderQty);
        p.setIsAsset(isAsset);
        p.setIsConsumable(isConsumable);
        p.setIsService(isService);
        p.setIsLoanable(isLoanable);
        p.setDepreciationRate(depreciationRate);
        p.setWarrantyPeriodMonths(warrantyMonths);
        p.setDescription(description);

        for (String aliasName : aliases) {
            ProductAlias alias = new ProductAlias();
            alias.setProduct(p);
            alias.setAliasName(aliasName);
            p.getAliases().add(alias);
        }
        for (ProductAttributeValue value : attributeValues) {
            value.setProduct(p);
            p.getAttributeValues().add(value);
        }
        return p;
    }
}
