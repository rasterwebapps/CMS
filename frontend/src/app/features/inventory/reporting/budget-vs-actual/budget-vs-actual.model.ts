export interface BudgetVsActualLocationRow {
  locationId: number;
  locationName: string;
  budgetCount: number;
  allocatedAmount: number;
  consumedAmount: number;
  remainingAmount: number;
  overAllocated: boolean;
}

export interface BudgetVsActualReport {
  locations: BudgetVsActualLocationRow[];
  grandTotalBudgetCount: number;
  grandTotalAllocatedAmount: number;
  grandTotalConsumedAmount: number;
  grandTotalRemainingAmount: number;
  generatedAt: string;
}
