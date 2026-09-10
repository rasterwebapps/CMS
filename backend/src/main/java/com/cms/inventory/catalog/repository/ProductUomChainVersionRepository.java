package com.cms.inventory.catalog.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.cms.inventory.catalog.model.ProductUomChainVersion;

@Repository
public interface ProductUomChainVersionRepository extends JpaRepository<ProductUomChainVersion, Long> {

    Optional<ProductUomChainVersion> findByProductIdAndIsActiveTrue(Long productId);

    List<ProductUomChainVersion> findByProductIdOrderByVersionNoDesc(Long productId);

    Optional<ProductUomChainVersion> findTopByProductIdOrderByVersionNoDesc(Long productId);
}
