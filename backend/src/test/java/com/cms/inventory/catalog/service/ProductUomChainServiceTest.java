package com.cms.inventory.catalog.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.cms.exception.ResourceNotFoundException;
import com.cms.inventory.catalog.dto.ProductUomChainSaveRequest;
import com.cms.inventory.catalog.dto.ProductUomLevelRequest;
import com.cms.inventory.catalog.model.Product;
import com.cms.inventory.catalog.model.ProductUomChainVersion;
import com.cms.inventory.catalog.model.ProductUomLevel;
import com.cms.inventory.catalog.model.Uom;
import com.cms.inventory.catalog.repository.ProductUomChainVersionRepository;
import com.cms.inventory.catalog.repository.ProductUomLevelRepository;

@ExtendWith(MockitoExtension.class)
class ProductUomChainServiceTest {

    @Mock private ProductUomChainVersionRepository versionRepository;
    @Mock private ProductUomLevelRepository levelRepository;
    @Mock private ProductService productService;
    @Mock private UomService uomService;
    private ProductUomChainService service;

    private final Uom tablet = uom(1L, "TABLET");
    private final Uom strip = uom(2L, "STRIP");
    private final Uom box = uom(3L, "BOX");
    private final Product paracetamol = product(10L, tablet);

    @BeforeEach
    void setUp() {
        service = new ProductUomChainService(versionRepository, levelRepository, productService, uomService);
    }

    // ── getActiveVersion / listVersions ─────────────────────────────────────

    @Test
    void shouldReturnNullActiveVersionWhenNoneConfigured() {
        when(versionRepository.findByProductIdAndIsActiveTrue(10L)).thenReturn(Optional.empty());
        assertThat(service.getActiveVersion(10L)).isNull();
    }

    @Test
    void shouldReturnActiveVersionWithLevelsSortedByRank() {
        ProductUomChainVersion version = version(100L, paracetamol, 1, true);
        when(versionRepository.findByProductIdAndIsActiveTrue(10L)).thenReturn(Optional.of(version));
        when(levelRepository.findByChainVersionIdOrderByLevelRankAsc(100L)).thenReturn(List.of(
            level(1L, version, tablet, 0, "1", false),
            level(2L, version, strip, 1, "10", true)));

        var res = service.getActiveVersion(10L);
        assertThat(res.versionNo()).isEqualTo(1);
        assertThat(res.isActive()).isTrue();
        assertThat(res.levels()).extracting("levelRank").containsExactly(0, 1);
        assertThat(res.levels().get(1).isDefaultPurchase()).isTrue();
    }

    @Test
    void shouldListVersionsForProduct() {
        when(versionRepository.findByProductIdOrderByVersionNoDesc(10L))
            .thenReturn(List.of(version(101L, paracetamol, 2, true), version(100L, paracetamol, 1, false)));
        when(levelRepository.findByChainVersionIdOrderByLevelRankAsc(101L)).thenReturn(List.of());
        when(levelRepository.findByChainVersionIdOrderByLevelRankAsc(100L)).thenReturn(List.of());
        assertThat(service.listVersions(10L)).extracting("versionNo").containsExactly(2, 1);
    }

    // ── requireActiveLevel ───────────────────────────────────────────────────

    @Test
    void shouldReturnLevelWhenActiveAndBelongsToProduct() {
        ProductUomChainVersion version = version(100L, paracetamol, 1, true);
        ProductUomLevel level = level(2L, version, strip, 1, "10", false);
        when(levelRepository.findById(2L)).thenReturn(Optional.of(level));
        assertThat(service.requireActiveLevel(10L, 2L)).isSameAs(level);
    }

    @Test
    void shouldThrowWhenLevelBelongsToDifferentProduct() {
        ProductUomChainVersion version = version(100L, paracetamol, 1, true);
        ProductUomLevel level = level(2L, version, strip, 1, "10", false);
        when(levelRepository.findById(2L)).thenReturn(Optional.of(level));
        assertThatThrownBy(() -> service.requireActiveLevel(999L, 2L))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("does not belong to this product");
    }

    @Test
    void shouldThrowWhenLevelVersionIsNotActive() {
        ProductUomChainVersion version = version(100L, paracetamol, 1, false);
        ProductUomLevel level = level(2L, version, strip, 1, "10", false);
        when(levelRepository.findById(2L)).thenReturn(Optional.of(level));
        assertThatThrownBy(() -> service.requireActiveLevel(10L, 2L))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("retired pack configuration");
    }

    @Test
    void shouldThrowWhenLevelIdDoesNotExist() {
        when(levelRepository.findById(9L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.requireActiveLevel(10L, 9L)).isInstanceOf(ResourceNotFoundException.class);
    }

    // ── saveVersion — validation ─────────────────────────────────────────────

    @Test
    void shouldRejectChainMissingBaseLevel() {
        when(productService.findOrThrow(10L)).thenReturn(paracetamol);
        var req = new ProductUomChainSaveRequest(List.of(levelReq(2L, 1, "10", false)));
        assertThatThrownBy(() -> service.saveVersion(10L, req, "admin"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("must include level 0");
    }

    @Test
    void shouldRejectBaseLevelNotMatchingProductBaseUom() {
        when(productService.findOrThrow(10L)).thenReturn(paracetamol);
        var req = new ProductUomChainSaveRequest(List.of(levelReq(2L, 0, "1", false)));
        assertThatThrownBy(() -> service.saveVersion(10L, req, "admin"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("must be the product's base unit");
    }

    @Test
    void shouldRejectBaseLevelWithFactorOtherThanOne() {
        when(productService.findOrThrow(10L)).thenReturn(paracetamol);
        var req = new ProductUomChainSaveRequest(List.of(levelReq(1L, 0, "2", false)));
        assertThatThrownBy(() -> service.saveVersion(10L, req, "admin"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("base level's conversion factor must be 1");
    }

    @Test
    void shouldRejectDuplicateLevelRanks() {
        when(productService.findOrThrow(10L)).thenReturn(paracetamol);
        var req = new ProductUomChainSaveRequest(List.of(
            levelReq(1L, 0, "1", false),
            levelReq(2L, 0, "10", false)));
        assertThatThrownBy(() -> service.saveVersion(10L, req, "admin"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("unique rank");
    }

    @Test
    void shouldRejectDuplicateUnitsOfMeasure() {
        when(productService.findOrThrow(10L)).thenReturn(paracetamol);
        var req = new ProductUomChainSaveRequest(List.of(
            levelReq(1L, 0, "1", false),
            levelReq(1L, 1, "10", false)));
        assertThatThrownBy(() -> service.saveVersion(10L, req, "admin"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("different unit of measure");
    }

    @Test
    void shouldRejectMoreThanOneDefaultPurchaseLevel() {
        when(productService.findOrThrow(10L)).thenReturn(paracetamol);
        var req = new ProductUomChainSaveRequest(List.of(
            levelReq(1L, 0, "1", false),
            levelReq(2L, 1, "10", true),
            levelReq(3L, 2, "100", true)));
        assertThatThrownBy(() -> service.saveVersion(10L, req, "admin"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Only one level can be marked");
    }

    // ── saveVersion — create / reactivate ────────────────────────────────────

    @Test
    void shouldCreateNewVersionAndDeactivateThePreviousActiveOne() {
        when(productService.findOrThrow(10L)).thenReturn(paracetamol);
        when(uomService.findOrThrow(1L)).thenReturn(tablet);
        when(uomService.findOrThrow(2L)).thenReturn(strip);

        ProductUomChainVersion oldActive = version(100L, paracetamol, 1, true);
        when(versionRepository.findByProductIdOrderByVersionNoDesc(10L)).thenReturn(List.of(oldActive));
        // The one existing version's levels don't match what's being submitted (different factor),
        // so no reactivation candidate matches and a new version must be created.
        when(levelRepository.findByChainVersionIdOrderByLevelRankAsc(100L))
            .thenReturn(List.of(level(1L, oldActive, tablet, 0, "1", false), level(2L, oldActive, strip, 1, "10", false)));

        when(versionRepository.save(any(ProductUomChainVersion.class))).thenAnswer(inv -> {
            ProductUomChainVersion v = inv.getArgument(0);
            if (v.getId() == null) v.setId(200L);
            return v;
        });
        // toResponse() re-queries the newly saved version's levels by id.
        ProductUomChainVersion newVersion = version(200L, paracetamol, 2, true);
        when(levelRepository.findByChainVersionIdOrderByLevelRankAsc(200L)).thenReturn(List.of(
            level(3L, newVersion, tablet, 0, "1", false), level(4L, newVersion, strip, 1, "15", false)));

        var req = new ProductUomChainSaveRequest(List.of(
            levelReq(1L, 0, "1", false),
            levelReq(2L, 1, "15", false)));
        var res = service.saveVersion(10L, req, "admin");

        assertThat(res.versionNo()).isEqualTo(2);
        assertThat(oldActive.getIsActive()).isFalse();
        verify(versionRepository, times(1)).save(eq(oldActive));
        // One save for deactivating the old version, one for persisting the new one.
        verify(versionRepository, times(2)).save(any(ProductUomChainVersion.class));
    }

    @Test
    void shouldReactivateAMatchingPriorVersionInsteadOfDuplicating() {
        when(productService.findOrThrow(10L)).thenReturn(paracetamol);

        ProductUomChainVersion v2Active = version(101L, paracetamol, 2, true);
        ProductUomChainVersion v1Inactive = version(100L, paracetamol, 1, false);
        when(versionRepository.findByProductIdOrderByVersionNoDesc(10L)).thenReturn(List.of(v2Active, v1Inactive));

        // v2 (active, 15/strip) does not match the submitted 10/strip configuration...
        when(levelRepository.findByChainVersionIdOrderByLevelRankAsc(101L))
            .thenReturn(List.of(level(5L, v2Active, tablet, 0, "1", false), level(6L, v2Active, strip, 1, "15", false)));
        // ...but v1 (inactive, 10/strip) exactly matches it, so it should be reactivated.
        when(levelRepository.findByChainVersionIdOrderByLevelRankAsc(100L))
            .thenReturn(List.of(level(1L, v1Inactive, tablet, 0, "1", false), level(2L, v1Inactive, strip, 1, "10", false)));
        when(versionRepository.save(any(ProductUomChainVersion.class))).thenAnswer(inv -> inv.getArgument(0));

        var req = new ProductUomChainSaveRequest(List.of(
            levelReq(1L, 0, "1", false),
            levelReq(2L, 1, "10", false)));
        var res = service.saveVersion(10L, req, "admin");

        assertThat(res.id()).isEqualTo(100L);
        assertThat(res.versionNo()).isEqualTo(1);
        assertThat(v1Inactive.getIsActive()).isTrue();
        assertThat(v2Active.getIsActive()).isFalse();
        // Reactivation must not create a brand new version row.
        verify(versionRepository, never()).save(argThatIsNewVersion());
    }

    private ProductUomChainVersion argThatIsNewVersion() {
        return org.mockito.ArgumentMatchers.argThat(v -> v.getId() == null);
    }

    private ProductUomLevelRequest levelReq(Long uomId, int rank, String factor, boolean isDefaultPurchase) {
        return new ProductUomLevelRequest(uomId, rank, new BigDecimal(factor), isDefaultPurchase);
    }

    private Uom uom(Long id, String code) {
        Uom u = new Uom();
        u.setId(id);
        u.setCode(code);
        u.setName(code);
        return u;
    }

    private Product product(Long id, Uom baseUom) {
        Product p = new Product();
        p.setId(id);
        p.setProductCode("LAB-000" + id);
        p.setProductName("Test Product " + id);
        p.setBaseUom(baseUom);
        return p;
    }

    private ProductUomChainVersion version(Long id, Product product, int versionNo, boolean active) {
        ProductUomChainVersion v = new ProductUomChainVersion();
        v.setId(id);
        v.setProduct(product);
        v.setVersionNo(versionNo);
        v.setIsActive(active);
        return v;
    }

    private ProductUomLevel level(Long id, ProductUomChainVersion version, Uom uom, int rank, String factor, boolean isDefaultPurchase) {
        ProductUomLevel l = new ProductUomLevel();
        l.setId(id);
        l.setChainVersion(version);
        l.setUom(uom);
        l.setLevelRank(rank);
        l.setFactorToBase(new BigDecimal(factor));
        l.setIsDefaultPurchase(isDefaultPurchase);
        return l;
    }
}
