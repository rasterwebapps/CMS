export interface DashboardSummary {
  totalStudents: number;
  totalFaculty: number;
  totalSpecialities: number;
  totalSubjects: number;
  totalPrograms: number;
  totalLabs: number;
  totalEquipment: number;
  totalExaminations: number;
  totalFeePayments: number;
  totalMaintenanceRequests: number;
  totalAttendanceRecords: number;
  equipmentByStatus: Record<string, number>;
  maintenanceByStatus: Record<string, number>;
  studentsByStatus: Record<string, number>;
  attendanceByStatus: Record<string, number>;
  enquiryFunnel: Record<string, number>;
  feeCollectedThisMonth: number;
  feeOutstanding: number;
  studentsDelta?: number;
  feeCollectedDelta?: number;
  feeOutstandingDelta?: number;
  totalEnquiriesThisMonth?: number;
  admissionsThisMonth?: number;
  pendingDocumentsCount?: number;
}

export interface DashboardTrendPoint {
  month: string;
  value: number;
}

export interface DashboardTrends {
  enrolmentTrend: DashboardTrendPoint[];
  feeCollectionTrend: DashboardTrendPoint[];
}

export type KpiTrendDirection = 'up' | 'down' | 'neutral';

export interface ActivityItem {
  id: number;
  entityType: 'ENQUIRY' | 'ADMISSION' | 'STUDENT' | 'PAYMENT' | 'DOCUMENT';
  entityId: number;
  action: string;
  actor: string;
  timestamp: string; // ISO-8601
  linkPath: string;
}
