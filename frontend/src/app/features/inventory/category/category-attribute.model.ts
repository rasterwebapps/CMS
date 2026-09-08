export type AttributeDataType = 'TEXT' | 'NUMBER' | 'DATE' | 'BOOLEAN' | 'ENUM';

export interface CategoryAttribute {
  id: number;
  categoryId: number;
  name: string;
  dataType: AttributeDataType;
  enumOptions?: string | null;
  isRequired: boolean;
  displayOrder: number;
  createdAt: string;
  updatedAt: string;
}

export interface CategoryAttributeRequest {
  name: string;
  dataType: AttributeDataType;
  enumOptions?: string | null;
  isRequired?: boolean;
  displayOrder?: number;
}
