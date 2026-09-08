package com.cms.inventory.reporting.service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cms.inventory.asset.model.Asset;
import com.cms.inventory.asset.model.enums.AssetStatus;
import com.cms.inventory.asset.repository.AssetRepository;
import com.cms.inventory.asset.service.AssetDepreciationCalculator;
import com.cms.inventory.catalog.model.Category;
import com.cms.inventory.reporting.dto.AssetDepreciationCategoryRow;
import com.cms.inventory.reporting.dto.AssetDepreciationSummaryReportResponse;

/**
 * Phase 8's fourth Reporting slice — total purchase value / accumulated depreciation / current
 * book value grouped by category, for every asset still on the register (not {@code DISPOSED}).
 * Reuses {@link AssetDepreciationCalculator} — the exact same straight-line formula {@code
 * AssetService} already applies per-asset — rather than a second, duplicated computation, so the
 * two screens can never drift apart. A plain "as of today" snapshot, no period/monthly
 * breakdown, matching the Stock Valuation Report's own scope decision. See the "Asset
 * Depreciation Summary Report slice" decision-log entry for why this was judged buildable
 * without further product input.
 */
@Service
@Transactional(readOnly = true)
public class AssetDepreciationSummaryReportService {

    private final AssetRepository assetRepository;

    public AssetDepreciationSummaryReportService(AssetRepository assetRepository) {
        this.assetRepository = assetRepository;
    }

    public AssetDepreciationSummaryReportResponse get() {
        List<Asset> assets = assetRepository.findAllWithCategoryExcludingStatus(AssetStatus.DISPOSED);

        Map<Long, AssetDepreciationCategoryRow> rowsByCategory = new LinkedHashMap<>();
        for (Asset asset : assets) {
            Category category = asset.getProduct().getCategory();
            AssetDepreciationCalculator.Result depreciation = AssetDepreciationCalculator.compute(
                asset.getPurchaseValue(), asset.getPurchaseDate(), asset.getUsefulLifeMonths(), asset.getSalvageValue());

            BigDecimal purchaseValue = asset.getPurchaseValue() != null ? asset.getPurchaseValue() : BigDecimal.ZERO;
            BigDecimal accumulatedDepreciation = depreciation.applicable() ? depreciation.accumulatedDepreciation() : BigDecimal.ZERO;
            BigDecimal currentBookValue = depreciation.applicable() ? depreciation.currentBookValue() : BigDecimal.ZERO;

            AssetDepreciationCategoryRow existing = rowsByCategory.get(category.getId());
            if (existing == null) {
                rowsByCategory.put(category.getId(), new AssetDepreciationCategoryRow(
                    category.getId(), category.getName(), 1, purchaseValue, accumulatedDepreciation, currentBookValue));
            } else {
                rowsByCategory.put(category.getId(), new AssetDepreciationCategoryRow(
                    category.getId(), category.getName(), existing.assetCount() + 1,
                    existing.totalPurchaseValue().add(purchaseValue),
                    existing.totalAccumulatedDepreciation().add(accumulatedDepreciation),
                    existing.totalCurrentBookValue().add(currentBookValue)));
            }
        }

        List<AssetDepreciationCategoryRow> categories = new ArrayList<>(rowsByCategory.values());
        categories.sort((a, b) -> a.categoryName().compareToIgnoreCase(b.categoryName()));

        long grandTotalAssetCount = categories.stream().mapToLong(AssetDepreciationCategoryRow::assetCount).sum();
        BigDecimal grandTotalPurchaseValue = sum(categories, AssetDepreciationCategoryRow::totalPurchaseValue);
        BigDecimal grandTotalAccumulatedDepreciation = sum(categories, AssetDepreciationCategoryRow::totalAccumulatedDepreciation);
        BigDecimal grandTotalCurrentBookValue = sum(categories, AssetDepreciationCategoryRow::totalCurrentBookValue);

        return new AssetDepreciationSummaryReportResponse(
            categories, grandTotalAssetCount, grandTotalPurchaseValue, grandTotalAccumulatedDepreciation,
            grandTotalCurrentBookValue, Instant.now());
    }

    private static BigDecimal sum(List<AssetDepreciationCategoryRow> rows, java.util.function.Function<AssetDepreciationCategoryRow, BigDecimal> extractor) {
        return rows.stream().map(extractor).reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
