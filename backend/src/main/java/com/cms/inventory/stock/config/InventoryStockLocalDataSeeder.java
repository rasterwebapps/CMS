package com.cms.inventory.stock.config;

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
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;

import com.cms.inventory.catalog.model.Product;
import com.cms.inventory.catalog.repository.ProductRepository;
import com.cms.inventory.stock.dto.StockMovementRequest;
import com.cms.inventory.stock.model.InventoryLocation;
import com.cms.inventory.stock.model.enums.LocationRole;
import com.cms.inventory.stock.repository.InventoryLocationRepository;
import com.cms.inventory.stock.service.StockMovementService;
import com.cms.model.Room;
import com.cms.repository.RoomRepository;

/**
 * Seeds demo Inventory Locations and a couple of opening-balance stock movements for the local
 * 'local' profile, so the Stock Tracking screens have something to show during development. Runs
 * after {@code InventoryCatalogLocalDataSeeder} (needs its Products) via {@code @Order} — see the
 * "own package namespace" decision in docs/inventory-management/DECISION_LOG.md. Skips gracefully
 * (rather than failing startup) if Rooms or Products aren't seeded yet in this environment, since
 * neither is guaranteed to exist before this runs.
 */
@Configuration
@Profile("local")
@ConditionalOnProperty(prefix = "cms.seed", name = "enabled", havingValue = "true")
public class InventoryStockLocalDataSeeder {

    private static final Logger log = LoggerFactory.getLogger(InventoryStockLocalDataSeeder.class);

    @Bean
    @Order(101)
    @Transactional
    CommandLineRunner seedInventoryStockData(
            RoomRepository roomRepo,
            InventoryLocationRepository locationRepo,
            ProductRepository productRepo,
            StockMovementService stockMovementService) {
        return args -> {
            if (locationRepo.count() > 0) {
                log.info("Inventory stock data already seeded, skipping...");
                return;
            }
            List<Room> rooms = roomRepo.findAll(PageRequest.of(0, 2)).getContent();
            List<Product> products = productRepo.findAll(PageRequest.of(0, 5)).getContent();
            if (rooms.isEmpty() || products.isEmpty()) {
                log.info("Skipping Inventory stock seed — no Rooms or Products available yet in this environment.");
                return;
            }

            log.info("🌱 Seeding demo Inventory Locations and opening stock balances...");

            InventoryLocation mainStore = locationRepo.save(location(rooms.get(0), "Main Store", LocationRole.STORE, "Central inventory store"));
            InventoryLocation labStore = rooms.size() > 1
                ? locationRepo.save(location(rooms.get(1), "Lab Requesting Point", LocationRole.REQUESTING_POINT, "Where labs draw consumables from"))
                : mainStore;
            log.info("✓ Created {} inventory location(s)", labStore == mainStore ? 1 : 2);

            int seeded = 0;
            for (Product product : products) {
                if (!Boolean.TRUE.equals(product.getIsConsumable())) continue; // only give consumables an opening balance
                stockMovementService.recordMovement(new StockMovementRequest(
                    product.getId(), mainStore.getId(), null, null,
                    "RECEIPT", null, new BigDecimal("50"), new BigDecimal("10.00"),
                    "Opening balance (local dev seed)"
                ), "seed-data");
                seeded++;
            }
            log.info("✓ Recorded opening balance for {} product(s) at '{}'", seeded, mainStore.getVirtualName());
        };
    }

    private static InventoryLocation location(Room room, String virtualName, LocationRole role, String description) {
        InventoryLocation loc = new InventoryLocation();
        loc.setRoom(room);
        loc.setVirtualName(virtualName);
        loc.setLocationRole(role);
        loc.setDescription(description);
        return loc;
    }
}
