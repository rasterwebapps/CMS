import { Department } from '../department/department.model';

export type LabType =
  | 'COMPUTER'
  | 'PHYSICS'
  | 'CHEMISTRY'
  | 'ELECTRONICS'
  | 'BIOLOGY'
  | 'LANGUAGE'
  | 'MECHANICAL'
  | 'OTHER';

export type LabStatus = 'ACTIVE' | 'INACTIVE' | 'UNDER_MAINTENANCE';

export type LabInChargeRole = 'LAB_INCHARGE' | 'TECHNICIAN';

export interface Lab {
  id: number;
  name: string;
  labType: LabType;
  department: Department;
  building?: string;
  roomNumber?: string;
  capacity: number;
  status: LabStatus;
  createdAt: string;
  updatedAt: string;
}

export interface LabRequest {
  name: string;
  labType: LabType;
  departmentId: number;
  building?: string;
  roomNumber?: string;
  capacity: number;
  status: LabStatus;
}

export interface LabInChargeAssignment {
  id: number;
  labId: number;
  assigneeId: number;
  assigneeName: string;
  role: LabInChargeRole;
  assignedDate: string;
  createdAt: string;
  updatedAt: string;
}

export interface LabInChargeAssignmentRequest {
  assigneeId: number;
  assigneeName: string;
  role: LabInChargeRole;
  assignedDate: string;
}

export const LAB_TYPES: { value: LabType; label: string }[] = [
  { value: 'COMPUTER', label: 'Computer' },
  { value: 'PHYSICS', label: 'Physics' },
  { value: 'CHEMISTRY', label: 'Chemistry' },
  { value: 'ELECTRONICS', label: 'Electronics' },
  { value: 'BIOLOGY', label: 'Biology' },
  { value: 'LANGUAGE', label: 'Language' },
  { value: 'MECHANICAL', label: 'Mechanical' },
  { value: 'OTHER', label: 'Other' },
];

export const LAB_STATUSES: { value: LabStatus; label: string }[] = [
  { value: 'ACTIVE', label: 'Active' },
  { value: 'INACTIVE', label: 'Inactive' },
  { value: 'UNDER_MAINTENANCE', label: 'Under Maintenance' },
];

export const LAB_INCHARGE_ROLES: { value: LabInChargeRole; label: string }[] = [
  { value: 'LAB_INCHARGE', label: 'Lab In-Charge' },
  { value: 'TECHNICIAN', label: 'Technician' },
];
