export interface DashboardSummary {
  totalStudents: number;
  totalFaculty: number;
  totalDepartments: number;
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
}

export interface DashboardTrendPoint {
  month: string;
  value: number;
}

export interface DashboardTrends {
  enrolmentTrend: DashboardTrendPoint[];
  feeCollectionTrend: DashboardTrendPoint[];
}

export interface FrontOfficeEnquiryItem {
  id: number;
  name: string;
  programName: string | null;
  referralTypeName: string | null;
  status: string;
  enquiryDate: string;
}

export interface FrontOfficeDashboard {
  todayEnquiryCount: number;
  totalEnquiryCount: number;
  pendingAdmissionsCount: number;
  feeCollectedToday: number;
  conversionsThisWeek: number;
  conversionRate: number;
  enquiryFunnel: Record<string, number>;
  todaysEnquiries: FrontOfficeEnquiryItem[];
  pendingActionItems: string[];
}
