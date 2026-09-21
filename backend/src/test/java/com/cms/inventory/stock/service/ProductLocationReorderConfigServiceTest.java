package com.cms.inventory.stock.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.cms.exception.ResourceNotFoundException;
import com.cms.inventory.catalog.model.Product;
import com.cms.inventory.catalog.repository.ProductRepository;
import com.cms.inventory.stock.dto.ProductLocationReorderConfigRequest;
import com.cms.inventory.stock.model.InventoryLocation;
import com.cms.inventory.stock.model.ProductLocationReorderConfig;
import com.cms.inventory.stock.model.enums.LocationRole;
import com.cms.inventory.stock.repository.InventoryLocationRepository;
import com.cms.inventory.stock.repository.ProductLocationReorderConfigRepository;

@ExtendWith(MockitoExtension.class)
class ProductLocationReorderConfigServiceTest {

    @Mock private ProductLocationReorderConfigRepository configRepository;
    @Mock private ProductRepository productRepository;
    @Mock private InventoryLocationRepository locationRepository;
    private ProductLocationReorderConfigService service;

    private final Product product = product(10L);

    @BeforeEach
    void setUp() {
        service = new ProductLocationReorderConfigService(configRepository, productRepository, locationRepository);
        lenient().when(configRepository.save(any(ProductLocationReorderConfig.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void shouldRejectWhenLocationIsAStore() {
        InventoryLocation store = location(1L, "Main Store", LocationRole.STORE);
        when(productRepository.findById(10L)).thenReturn(Optional.of(product));
        when(locationRepository.findById(1L)).thenReturn(Optional.of(store));

        var req = new ProductLocationReorderConfigRequest(10L, 1L, BigDecimal.TEN, BigDecimal.ONE, null, true, null);
        assertThatThrownBy(() -> service.create(req))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("reorder configuration applies to requesting-point locations only");
    }

    @Test
    void shouldRejectWhenMaxStockQtyBelowReorderLevel() {
        InventoryLocation ward = location(2L, "Ward A", LocationRole.REQUESTING_POINT);
        when(productRepository.findById(10L)).thenReturn(Optional.of(product));
        when(locationRepository.findById(2L)).thenReturn(Optional.of(ward));

        var req = new ProductLocationReorderConfigRequest(10L, 2L, new BigDecimal("20"), BigDecimal.ONE, new BigDecimal("5"), false, null);
        assertThatThrownBy(() -> service.create(req))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("cannot be less than the reorder level");
    }

    @Test
    void shouldRejectAutoIndentEnabledWithoutDefaultSupplyingLocation() {
        InventoryLocation ward = location(2L, "Ward A", LocationRole.REQUESTING_POINT);
        when(productRepository.findById(10L)).thenReturn(Optional.of(product));
        when(locationRepository.findById(2L)).thenReturn(Optional.of(ward));

        var req = new ProductLocationReorderConfigRequest(10L, 2L, BigDecimal.TEN, BigDecimal.ONE, null, true, null);
        assertThatThrownBy(() -> service.create(req))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("no default supplying store configured");
    }

    @Test
    void shouldRejectDuplicateActiveConfigForSamePair() {
        InventoryLocation ward = location(2L, "Ward A", LocationRole.REQUESTING_POINT);
        when(productRepository.findById(10L)).thenReturn(Optional.of(product));
        when(locationRepository.findById(2L)).thenReturn(Optional.of(ward));
        when(configRepository.existsByProductIdAndLocationIdAndIsActiveTrue(10L, 2L)).thenReturn(true);

        var req = new ProductLocationReorderConfigRequest(10L, 2L, BigDecimal.TEN, BigDecimal.ONE, null, false, null);
        assertThatThrownBy(() -> service.create(req))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("already exists");
    }

    @Test
    void shouldCreateWhenAutoIndentEnabledAndDefaultSupplyingLocationIsSet() {
        InventoryLocation store = location(1L, "Main Store", LocationRole.STORE);
        InventoryLocation ward = location(2L, "Ward A", LocationRole.REQUESTING_POINT);
        ward.setDefaultSupplyingLocation(store);
        when(productRepository.findById(10L)).thenReturn(Optional.of(product));
        when(locationRepository.findById(2L)).thenReturn(Optional.of(ward));

        var req = new ProductLocationReorderConfigRequest(10L, 2L, BigDecimal.TEN, BigDecimal.ONE, new BigDecimal("50"), true, null);
        var response = service.create(req);

        assertThat(response.autoIndentEnabled()).isTrue();
        assertThat(response.defaultSupplyingLocationVirtualName()).isEqualTo("Main Store");
    }

    @Test
    void shouldThrowWhenConfigNotFound() {
        when(configRepository.findById(99L)).thenReturn(Optional.empty());
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

    private InventoryLocation location(Long id, String name, LocationRole role) {
        InventoryLocation l = new InventoryLocation();
        l.setId(id);
        l.setVirtualName(name);
        l.setLocationRole(role);
        return l;
    }
}
