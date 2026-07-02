export interface Page<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
  first: boolean;
  last: boolean;
}

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

