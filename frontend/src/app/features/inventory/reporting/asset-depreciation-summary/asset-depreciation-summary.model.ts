export interface AssetDepreciationCategoryRow {
  categoryId: number;
  categoryName: string;
  assetCount: number;
  totalPurchaseValue: number;
  totalAccumulatedDepreciation: number;
  totalCurrentBookValue: number;
}

export interface AssetDepreciationSummaryReport {
  categories: AssetDepreciationCategoryRow[];
  grandTotalAssetCount: number;
  grandTotalPurchaseValue: number;
  grandTotalAccumulatedDepreciation: number;
  grandTotalCurrentBookValue: number;
  generatedAt: string;
}
