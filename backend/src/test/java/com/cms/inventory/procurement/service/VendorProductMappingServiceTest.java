package com.cms.inventory.procurement.service;

import static org.assertj.core.api.Assertions.assertThat;
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

import com.cms.inventory.catalog.model.Product;
import com.cms.inventory.catalog.repository.ProductRepository;
import com.cms.inventory.catalog.repository.UomRepository;
import com.cms.inventory.procurement.dto.VendorProductMappingRequest;
import com.cms.inventory.procurement.dto.VendorProductMappingResponse;
import com.cms.inventory.procurement.model.Supplier;
import com.cms.inventory.procurement.model.VendorProductMapping;
import com.cms.inventory.procurement.repository.RateContractRepository;
import com.cms.inventory.procurement.repository.SupplierRepository;
import com.cms.inventory.procurement.repository.VendorProductMappingRepository;

/**
 * Covers the "Vendor part-number/name on VendorProductMapping" Phase 1 item — the supplier's own
 * identifier/name for a product, stored and returned alongside our own, blank-trimmed to null
 * like every other optional free-text field in this module.
 */
@ExtendWith(MockitoExtension.class)
class VendorProductMappingServiceTest {

    @Mock private VendorProductMappingRepository mappingRepository;
    @Mock private SupplierRepository supplierRepository;
    @Mock private ProductRepository productRepository;
    @Mock private UomRepository uomRepository;
    @Mock private RateContractRepository rateContractRepository;
    @Mock private CurrencyExchangeRateService currencyExchangeRateService;

    private VendorProductMappingService service;

    private Supplier supplier;
    private Product product;

    @BeforeEach
    void setUp() {
        service = new VendorProductMappingService(mappingRepository, supplierRepository, productRepository,
            uomRepository, rateContractRepository, currencyExchangeRateService);

        supplier = new Supplier();
        supplier.setId(1L);
        supplier.setSupplierName("Acme Supplies");

        product = new Product();
        product.setId(2L);
        product.setProductName("Sodium Chloride");

        lenient().when(supplierRepository.findById(1L)).thenReturn(Optional.of(supplier));
        lenient().when(productRepository.findById(2L)).thenReturn(Optional.of(product));
        lenient().when(mappingRepository.existsBySupplierIdAndProductIdAndIsActiveTrue(1L, 2L)).thenReturn(false);
        lenient().when(mappingRepository.save(any(VendorProductMapping.class))).thenAnswer(inv -> {
            VendorProductMapping m = inv.getArgument(0);
            m.setId(100L);
            return m;
        });
    }

    private VendorProductMappingRequest request(String vendorPartNumber, String vendorProductName) {
        return new VendorProductMappingRequest(1L, 2L, null, vendorPartNumber, vendorProductName,
            new BigDecimal("10.00"), "INR", null, null, null, null, null);
    }

    @Test
    void storesAndReturnsVendorPartNumberAndName() {
        VendorProductMappingResponse response = service.create(request("ACM-SC-001", "Table Salt (Lab Grade)"));

        assertThat(response.vendorPartNumber()).isEqualTo("ACM-SC-001");
        assertThat(response.vendorProductName()).isEqualTo("Table Salt (Lab Grade)");
    }

    @Test
    void trimsVendorPartNumberAndName() {
        VendorProductMappingResponse response = service.create(request("  ACM-SC-001  ", "  Table Salt  "));

        assertThat(response.vendorPartNumber()).isEqualTo("ACM-SC-001");
        assertThat(response.vendorProductName()).isEqualTo("Table Salt");
    }

    @Test
    void blankVendorPartNumberAndNameBecomeNull() {
        VendorProductMappingResponse response = service.create(request("   ", ""));

        assertThat(response.vendorPartNumber()).isNull();
        assertThat(response.vendorProductName()).isNull();
    }

    @Test
    void vendorPartNumberAndNameAreOptional() {
        VendorProductMappingResponse response = service.create(request(null, null));

        assertThat(response.vendorPartNumber()).isNull();
        assertThat(response.vendorProductName()).isNull();
    }
}
