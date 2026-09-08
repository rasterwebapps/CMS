export interface StockValuationCategoryRow {
  categoryId: number;
  categoryName: string;
  productCount: number;
  totalValue: number;
}

export interface StockValuationReport {
  categories: StockValuationCategoryRow[];
  grandTotalProductCount: number;
  grandTotalValue: number;
  generatedAt: string;
}
