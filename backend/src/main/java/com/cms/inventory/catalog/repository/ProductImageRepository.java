package com.cms.inventory.catalog.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.cms.inventory.catalog.model.ProductImage;

@Repository
public interface ProductImageRepository extends JpaRepository<ProductImage, Long> {

    List<ProductImage> findByProductIdOrderByIsPrimaryDescCreatedAtAsc(Long productId);

    Optional<ProductImage> findFirstByProductIdAndIsPrimaryTrue(Long productId);
}
