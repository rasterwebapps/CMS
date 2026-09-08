package com.cms.inventory.asset.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.cms.inventory.asset.model.Asset;
import com.cms.inventory.asset.model.enums.AssetStatus;

@Repository
public interface AssetRepository extends JpaRepository<Asset, Long>, JpaSpecificationExecutor<Asset> {

    boolean existsByAssetTagIgnoreCase(String assetTag);

    boolean existsByAssetTagIgnoreCaseAndIdNot(String assetTag, Long id);

    /** Every asset not in the given status, with its product and category eagerly fetched —
     *  the Asset Depreciation Summary report's raw input, avoiding an N+1 per-asset category
     *  lookup. See the "Asset Depreciation Summary Report slice" decision-log entry. */
    @Query("SELECT a FROM Asset a JOIN FETCH a.product p JOIN FETCH p.category WHERE a.status <> :excludedStatus")
    List<Asset> findAllWithCategoryExcludingStatus(@Param("excludedStatus") AssetStatus excludedStatus);
}
