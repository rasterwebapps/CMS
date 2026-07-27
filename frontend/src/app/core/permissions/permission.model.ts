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
  studentId?: number | null;
  facultyId?: number | null;
}

export interface UpdateUserRequest {
  fullName: string;
  email: string;
  roleName: string;
  isActive: boolean;
}

export interface AllPermissionsResponse {
  id: number;
  category: string;
  code: string;
  displayName: string;
  /** 1=Dev Only, 2=Support+, 3=Hold Only, 4=Delegatable */
  tier: number;
  /** Screen name this permission belongs to — set by the backend. */
  screenLabel: string | null;
}

/** Grouped view for the permission matrix editor. */
export interface PermissionGroup {
  category: string;
  permissions: AllPermissionsResponse[];
}

export interface TierChangeItem {
  id: number;
  tier: number;
}

export interface ImpactedRole {
  roleId: number;
  roleName: string;
  roleDisplayName: string;
  userCount: number;
}

/** A pending tier change that will revoke the permission from one or more roles that no longer qualify. */
export interface TierImpactEntry {
  permissionId: number;
  code: string;
  displayName: string;
  currentTier: number;
  newTier: number;
  revokedFrom: ImpactedRole[];
}
