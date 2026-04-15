export interface LabUtilizationReport {
  totalLabs: number;
  totalSchedules: number;
  averageSchedulesPerLab: number;
  totalEquipment: number;
  equipmentByStatus: Record<string, number>;
  labsByStatus: Record<string, number>;
}

export interface AttendanceAnalyticsReport {
  totalStudents: number;
  totalAttendanceRecords: number;
  attendanceByStatus: Record<string, number>;
  attendanceByType: Record<string, number>;
}
