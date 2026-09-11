export interface InventoryCurrencySettings {
  baseCurrencyCode: string | null;
  updatedAt: string | null;
  updatedBy: string | null;
}

export interface InventoryCurrencySettingsRequest {
  baseCurrencyCode: string;
}
