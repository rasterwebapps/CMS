export type PromotionOutcome = 'PROMOTED' | 'PROMOTED_WITH_ARREARS' | 'DETAINED_REPEAT' | 'GRADUATED' | 'EXCLUDED';
export type ExamOutcome = 'PASS' | 'FAIL';

export interface AttendanceReport {
  studentId: number;
  studentName: string;
  rollNumber: string;
  subjectId: number;
  subjectName: string;
  subjectCode: string;
  type: string;
  totalClasses: number;
  classesAttended: number;
  attendancePercentage: number;
  thresholdPercentage: number;
  lowAttendance: boolean;
}

export interface PromotionArrearSubject {
  subjectId: number;
  subjectName: string | null;
  subjectCode: string | null;
}

export interface SubjectExamOutcome {
  subjectId: number;
  subjectName: string;
  subjectCode: string;
  outcome: ExamOutcome | null;
}

export interface StudentPromotionPreviewRow {
  studentId: number;
  studentName: string;
  rollNumber: string;
  enrollmentId: number;
  subjectAttendance: AttendanceReport[];
  subjectExamOutcomes: SubjectExamOutcome[];
  carriedArrearSubjects: PromotionArrearSubject[];
  newArrearSubjects: PromotionArrearSubject[];
  totalArrearSubjects: PromotionArrearSubject[];
  recommendedOutcome: PromotionOutcome | null;
  blockReasons: string[];
}

/** A term instance a cohort currently has ENROLLED students in (or a suggested destination term,
 *  where enrolledCount is always 0 since no one is enrolled there yet). */
export interface CohortTermOption {
  termInstanceId: number;
  termLabel: string;
  enrolledCount: number;
}

export interface PromotionPreviewRequest {
  cohortId: number;
  fromTermInstanceId: number;
  toTermInstanceId: number;
}

export interface PromotionPreviewResponse {
  cohortId: number;
  cohortCode: string;
  fromTermInstanceId: number;
  fromTermLabel: string;
  toTermInstanceId: number;
  toTermLabel: string;
  programTotalTerms: number | null;
  maxDurationYears: number;
  students: StudentPromotionPreviewRow[];
}

export interface PromotionDecisionInput {
  studentId: number;
  outcome: PromotionOutcome;
  remarks: string | null;
}

export interface PromotionExecuteRequest {
  cohortId: number;
  fromTermInstanceId: number;
  toTermInstanceId: number;
  decisions: PromotionDecisionInput[];
  generateCourseRegistrations: boolean;
  generateFeeDemands: boolean;
}

export interface PromotionRejectedDecision {
  studentId: number;
  reason: string;
}

export interface PromotionExecuteResponse {
  promotedCount: number;
  promotedWithArrearsCount: number;
  detainedCount: number;
  graduatedCount: number;
  excludedCount: number;
  rejectedDecisions: PromotionRejectedDecision[];
  courseRegistrationsGenerated: number | null;
  feeDemandsGenerated: number | null;
}

export interface StudentPromotionDecisionDto {
  id: number;
  studentId: number;
  studentName: string;
  rollNumber: string;
  cohortId: number;
  cohortCode: string;
  fromTermInstanceId: number;
  fromTermLabel: string;
  toTermInstanceId: number | null;
  toTermLabel: string | null;
  outcome: PromotionOutcome;
  arrearSubjects: PromotionArrearSubject[];
  decidedBy: string;
  decidedAt: string;
  remarks: string | null;
}

/** Editable row built from a StudentPromotionPreviewRow for the review table. */
export interface PromotionEditableRow {
  preview: StudentPromotionPreviewRow;
  outcome: PromotionOutcome;
  remarks: string;
}
