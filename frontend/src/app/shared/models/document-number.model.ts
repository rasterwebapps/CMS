/** Shared response shape for every document type's "Regenerate Numbers" admin action
 *  (Purchase Order, Quotation Request, Goods Receipt, Supplier Return) — mirrors the backend's
 *  DocumentNumberChange/DocumentNumberRegenerationResult records. */
export interface DocumentNumberChange {
  entityId: number;
  entityLabel: string;
  oldNumber: string | null;
  newNumber: string;
}

export interface DocumentNumberRegenerationResult {
  totalChanged: number;
  changes: DocumentNumberChange[];
}
