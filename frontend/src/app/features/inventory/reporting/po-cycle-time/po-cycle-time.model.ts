export interface PurchaseOrderCycleTimeSupplierRow {
  supplierId: number;
  supplierName: string;
  completedOrderCount: number;
  averageCycleDays: number;
}

export interface PurchaseOrderCycleTimeReport {
  suppliers: PurchaseOrderCycleTimeSupplierRow[];
  grandTotalCompletedOrderCount: number;
  overallAverageCycleDays: number;
  generatedAt: string;
}
