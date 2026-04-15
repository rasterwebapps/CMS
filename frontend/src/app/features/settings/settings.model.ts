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
