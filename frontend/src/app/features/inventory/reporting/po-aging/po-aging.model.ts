export interface PurchaseOrderAgingBucketRow {
  bucketLabel: string;
  orderCount: number;
  totalValue: number;
}

export interface PurchaseOrderAgingReport {
  buckets: PurchaseOrderAgingBucketRow[];
  grandTotalOrderCount: number;
  grandTotalValue: number;
  generatedAt: string;
}
