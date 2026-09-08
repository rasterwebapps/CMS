import { AttributeDataType } from '../category/category-attribute.model';

export interface Page<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
  first: boolean;
  last: boolean;
}

export interface ProductAttributeValueEntry {
  attributeId: number;
  attributeName: string;
  dataType: AttributeDataType;
  value: string | null;
}

export interface Product {
  id: number;
  productCode: string;
  productName: string;
  categoryId: number;
  categoryName: string;
  baseUomId: number;
  baseUomCode: string;
  baseUomName: string;
  reorderLevel?: number | null;
  reorderQty?: number | null;
  isAsset: boolean;
  isConsumable: boolean;
  isService: boolean;
  isLoanable: boolean;
  depreciationRate?: number | null;
  warrantyPeriodMonths?: number | null;
  description?: string;
  isActive: boolean;
  createdAt: string;
  updatedAt: string;
  aliases: string[];
  attributeValues: ProductAttributeValueEntry[];
}

export interface ProductAttributeValueRequest {
  attributeId: number;
  value?: string | null;
}

export interface ProductRequest {
  productCode: string;
  productName: string;
  categoryId: number;
  baseUomId: number;
  reorderLevel?: number | null;
  reorderQty?: number | null;
  isAsset?: boolean;
  isConsumable?: boolean;
  isService?: boolean;
  isLoanable?: boolean;
  depreciationRate?: number | null;
  warrantyPeriodMonths?: number | null;
  description?: string;
  isActive?: boolean;
  aliases?: string[];
  attributeValues?: ProductAttributeValueRequest[];
}

export interface ProductStatusUpdateRequest {
  isActive: boolean;
  reason?: string;
}

export interface ProductStatusUpdateResponse {
  id: number;
  isActive: boolean;
  updatedAt: string;
}
