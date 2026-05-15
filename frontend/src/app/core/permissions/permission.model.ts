/** Matches com.cms.dto.WidgetConfigDto — shared by role-level and user-level configs. */
export interface WidgetConfigDto {
  key:        string;
  order:      number;
  colSpan:    number;
  rowSpan:    number;
  configJson: string | null;
}

export interface AppRoleResponse {
  id: number;
  name: string;
  displayName: string;
  hierarchyLevel: number;
  isSystemRole: boolean;
  description: string | null;
  permissionCodes: string[];
  /** Ordered dashboard widget configs (key + span metadata) for this role. */
  dashboardWidgets: WidgetConfigDto[];
}

export interface AppUserResponse {
  id: number;
  keycloakUsername: string;
  email: string;
  fullName: string;
  roleName: string;
  roleDisplayName: string;
  hierarchyLevel: number;
  isActive: boolean;
  createdBy: string | null;
  createdAt: string;
}

export interface CreateUserRequest {
  keycloakUsername: string;
  email: string;
  fullName: string;
  password: string;
  roleName: string;
}

export interface UpdateUserRequest {
  fullName: string;
  email: string;
  roleName: string;
  isActive: boolean;
}

export interface AllPermissionsResponse {
  category: string;
  code: string;
  displayName: string;
}

/** Grouped view for the permission matrix editor. */
export interface PermissionGroup {
  category: string;
  permissions: AllPermissionsResponse[];
}
