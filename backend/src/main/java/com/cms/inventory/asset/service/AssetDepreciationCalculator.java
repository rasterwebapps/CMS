package com.cms.inventory.asset.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.Period;

/**
 * Standard straight-line depreciation, extracted out of {@link AssetService#toResponse} so the
 * exact same formula can be reused by the Asset Depreciation Summary report ({@code
 * com.cms.inventory.reporting}) without duplicating it — a second, drifted copy of a financial
 * calculation is exactly the kind of risk the "reuse over duplicate" discipline this module
 * follows throughout exists to avoid. Pure extraction, no behavior change: see the "Depreciation
 * slice" and "Asset Depreciation Summary Report slice" decision-log entries.
 */
public final class AssetDepreciationCalculator {

    private AssetDepreciationCalculator() {}

    public record Result(boolean applicable, BigDecimal accumulatedDepreciation, BigDecimal currentBookValue) {
        public static final Result NOT_APPLICABLE = new Result(false, null, null);
    }

    public static Result compute(BigDecimal purchaseValue, LocalDate purchaseDate, Integer usefulLifeMonths, BigDecimal salvageValue) {
        boolean applicable = purchaseValue != null && purchaseDate != null
            && usefulLifeMonths != null && usefulLifeMonths > 0;
        if (!applicable) {
            return Result.NOT_APPLICABLE;
        }

        BigDecimal salvage = salvageValue != null ? salvageValue : BigDecimal.ZERO;
        BigDecimal depreciableBase = purchaseValue.subtract(salvage);
        int monthsElapsed = Math.max(0, Math.min(usefulLifeMonths, monthsBetween(purchaseDate, LocalDate.now())));
        BigDecimal monthlyDepreciation = depreciableBase.divide(BigDecimal.valueOf(usefulLifeMonths), 4, RoundingMode.HALF_UP);
        BigDecimal accumulatedDepreciation = monthlyDepreciation.multiply(BigDecimal.valueOf(monthsElapsed))
            .min(depreciableBase.max(BigDecimal.ZERO))
            .setScale(2, RoundingMode.HALF_UP);
        BigDecimal currentBookValue = purchaseValue.subtract(accumulatedDepreciation).max(salvage)
            .setScale(2, RoundingMode.HALF_UP);

        return new Result(true, accumulatedDepreciation, currentBookValue);
    }

    /** Whole calendar months elapsed from {@code start} to {@code end}, never negative. */
    static int monthsBetween(LocalDate start, LocalDate end) {
        if (end.isBefore(start)) return 0;
        return Period.between(start, end).getYears() * 12 + Period.between(start, end).getMonths();
    }
}
