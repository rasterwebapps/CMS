export interface Page<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
  first: boolean;
  last: boolean;
}

export interface SystemConfiguration {
  id: number;
  configKey: string;
  configValue: string;
  description: string;
  dataType: string;
  category: string;
  isEditable: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface SystemConfigurationRequest {
  configKey: string;
  configValue: string;
  description?: string;
  dataType: string;
  category: string;
  isEditable?: boolean;
}
