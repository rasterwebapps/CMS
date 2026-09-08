package com.cms.inventory.asset.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import com.cms.inventory.asset.model.Asset;

@Repository
public interface AssetRepository extends JpaRepository<Asset, Long>, JpaSpecificationExecutor<Asset> {

    boolean existsByAssetTagIgnoreCase(String assetTag);

    boolean existsByAssetTagIgnoreCaseAndIdNot(String assetTag, Long id);
}
