export interface InventoryDashboard {
  openPurchaseOrders: number;
  pendingPurchaseRequisitions: number;
  pendingWantedListItems: number;
  activeApprovalInstances: number;
  overdueGatePasses: number;
  overdueLoanableItems: number;
  openServiceTickets: number;
  urgentOpenServiceTickets: number;
  overAllocatedBudgets: number;
  consignmentOutstandingLiability: number;
  assetsUnderMaintenance: number;
  generatedAt: string;
}
