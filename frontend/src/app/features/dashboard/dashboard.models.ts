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
  // Phase 4 — optional month-over-month deltas powering the KPI trend pills.
  // Backend will populate these in a follow-up; until then the trend pill is hidden.
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
  /** Phase 4 — optional last-week conversion rate powering the KPI trend pill. */
  conversionRateLastWeek?: number;
}

// ── Phase 4 additions ──────────────────────────────────────────────

export type KpiTrendDirection = 'up' | 'down' | 'neutral';

/**
 * One entry in the Recent Activity feed shown on the admin dashboard.
 * Backend endpoint: GET /api/dashboard/activity?limit=10
 */
export interface ActivityItem {
  id: number;
  entityType: 'ENQUIRY' | 'ADMISSION' | 'STUDENT' | 'PAYMENT' | 'DOCUMENT';
  entityId: number;
  action: string;
  actor: string;
  timestamp: string; // ISO-8601
  linkPath: string;
}

/**
 * Single academic-calendar event rendered in the admin calendar strip.
 * Backend endpoint: GET /api/academic-years/current/events
 */
export interface CalendarEvent {
  id: number;
  label: string;
  date: string; // YYYY-MM-DD
  type: 'EXAM' | 'HOLIDAY' | 'EVENT' | 'DEADLINE' | 'OTHER';
}

/** A single timetable entry shown in the faculty "My Classes Today" widget. */
export interface FacultyClass {
  courseCode: string;
  courseName: string;
  batch: string;
  startTime: string; // HH:MM
  endTime: string;   // HH:MM
  room: string;
  attendanceMarked: boolean;
  /** Optional course id used to deep-link the "Mark Attendance" CTA. */
  courseId?: number;
}

/** A single attendance session awaiting marking by a faculty member. */
export interface AttendanceSession {
  id: number;
  courseCode: string;
  courseName: string;
  date: string;
  studentCount: number;
  linkPath: string;
}

/** An upcoming lab booking shown in the faculty Lab Schedule widget. */
export interface LabSlot {
  labName: string;
  date: string;
  startTime: string;
  endTime: string;
  batch: string;
}

/**
 * Aggregated payload for the faculty dashboard.
 * Backend endpoint: GET /api/dashboard/faculty
 */
export interface FacultyDashboard {
  todayClasses: FacultyClass[];
  pendingAttendanceCount: number;
  pendingAttendanceSessions: AttendanceSession[];
  upcomingLabSlots: LabSlot[];
  assignedCourseCount: number;
  totalStudentCount: number;
}
