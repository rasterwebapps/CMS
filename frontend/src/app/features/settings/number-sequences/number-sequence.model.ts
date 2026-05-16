export interface NumberSequence {
  id: number;
  seriesCode: string;
  seriesName: string;
  scopeType: string;
  scopeKey: string;
  prefix: string;
  sequencePadding: number;
  lastSequence: number;
  lastGeneratedNumber: string;
  nextPreviewNumber: string;
  description: string | null;
  createdAt: string;
  updatedAt: string;
}

