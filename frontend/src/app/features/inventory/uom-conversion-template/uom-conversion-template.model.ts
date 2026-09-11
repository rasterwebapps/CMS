export interface Page<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
  first: boolean;
  last: boolean;
}

/** One level of a template's chain (e.g. "Strip", factor 10 -> the template's base unit). Same
 *  shape as ProductUomLevel — levelRank 0 is always the template's own base unit. */
export interface UomConversionTemplateLevel {
  uomId: number;
  uomCode: string;
  uomName: string;
  levelRank: number;
  factorToBase: number;
  isDefaultPurchase: boolean;
}

export interface UomConversionTemplate {
  id: number;
  name: string;
  description?: string;
  baseUomId: number;
  baseUomCode: string;
  baseUomName: string;
  isActive: boolean;
  levels: UomConversionTemplateLevel[];
  createdAt: string;
  updatedAt: string;
}

export interface UomConversionTemplateLevelRequest {
  uomId: number;
  levelRank: number;
  factorToBase: number;
  isDefaultPurchase?: boolean;
}

export interface UomConversionTemplateRequest {
  name: string;
  description?: string;
  baseUomId: number;
  isActive?: boolean;
  levels: UomConversionTemplateLevelRequest[];
}

export interface UomConversionTemplateStatusUpdateRequest {
  isActive: boolean;
  reason?: string;
}

export interface UomConversionTemplateStatusUpdateResponse {
  id: number;
  isActive: boolean;
  updatedAt: string;
}
