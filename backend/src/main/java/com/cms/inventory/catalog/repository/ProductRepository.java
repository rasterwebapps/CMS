package com.cms.inventory.catalog.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import com.cms.inventory.catalog.model.Product;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long>, JpaSpecificationExecutor<Product> {

    boolean existsByProductCodeIgnoreCase(String productCode);
    boolean existsByProductCodeIgnoreCaseAndIdNot(String productCode, Long id);

    boolean existsByProductNameIgnoreCaseAndCategoryId(String productName, Long categoryId);
    boolean existsByProductNameIgnoreCaseAndCategoryIdAndIdNot(String productName, Long categoryId, Long id);

    boolean existsByBarcodeIgnoreCase(String barcode);
    boolean existsByBarcodeIgnoreCaseAndIdNot(String barcode, Long id);

    /** Used by the barcode-scan lookup workflow — see ProductService.findByBarcode. */
    Optional<Product> findByBarcodeIgnoreCase(String barcode);
}
