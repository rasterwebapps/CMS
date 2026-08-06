export interface Page<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
  first: boolean;
  last: boolean;
}

export interface AcademicYear {
  id: number;
  name: string;
  startDate: string;
  endDate: string;
  isCurrent: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface CohortSummary {
  id: number;
  cohortCode: string;
  displayName: string;
  courseName: string;
  courseCode: string;
  totalSeats: number | null;
  managementPercentage: number | null;
  managementSeats: number | null;
  counsellingSeats: number | null;
  hasStudents: boolean;
  counsellingClosed: boolean;
  counsellingClosedDate: string | null;
  managementClosed: boolean;
  managementClosedDate: string | null;
}

export interface CohortSeatsRequest {
  totalSeats: number | null;
  managementPercentage: number | null;
}

export interface CohortSeatAllocationRequest extends CohortSeatsRequest {
  courseId: number;
}

export interface SeatAvailabilityResponse {
  available: boolean;
  filled: number;
  total: number | null;
  full: boolean;
  closed: boolean;
  overManagementQuota: boolean;
}

export interface CohortLapsedRow {
  cohortId: number;
  courseName: string;
  courseCode: string;
  counsellingSeats: number;
  filledCounselling: number;
  lapsedSeats: number;
  counsellingClosed: boolean;
}

export interface CohortLapsedSummary {
  cohorts: CohortLapsedRow[];
  totalCounsellingSeats: number;
  totalFilledCounselling: number;
  totalLapsedSeats: number;
  lapsedPercentage: number;
}

export interface AcademicYearRequest {
  name: string;
  startDate: string;
  endDate: string;
  isCurrent: boolean;
  cohortSeatAllocations?: CohortSeatAllocationRequest[];
}

export type CalendarEventType = 'HOLIDAY' | 'EXAM' | 'CULTURAL' | 'SPORTS' | 'WORKSHOP' | 'OTHER';
export type HolidayCategory = 'GOVERNMENT' | 'LOCAL' | 'INSTITUTIONAL';

export interface CalendarEvent {
  id: number;
  title: string;
  description?: string;
  startDate: string;
  endDate: string;
  eventType: CalendarEventType;
  holidayCategory: HolidayCategory | null;
  academicYear: AcademicYear;
  createdAt: string;
  updatedAt: string;
  /** Distinct period ids currently auto-blocked for this event (only ever non-empty for
   *  eventType === 'HOLIDAY'). */
  blockedPeriodIds: number[];
  /** Non-null only when this event was seeded from a recurring Holiday Template -- drives the
   *  "delete this occurrence only" vs "delete this and all future occurrences" choice. */
  sourceHolidayTemplateId: number | null;
  sourceHolidayTemplateName: string | null;
}

export interface CalendarEventRequest {
  title: string;
  description?: string;
  startDate: string;
  endDate: string;
  eventType: CalendarEventType;
  academicYearId: number;
  holidayCategory?: HolidayCategory | null;
  /** Only meaningful when eventType === 'HOLIDAY' -- which periods to auto-block for every date
   *  in [startDate, endDate]. Omitted/empty means "whole day" (every active period). */
  blockedPeriodIds?: number[];
  /** When true (with a non-null recurrence), creates/updates a linked Holiday Template anchored
   *  to this event's own startDate. When false on an event that already repeats, the linked
   *  template is deactivated (this event reverts to one-time; siblings are untouched). */
  repeats?: boolean;
  recurrence?: EventRecurrenceRequest | null;
}

export type AppDayOfWeek = 'MONDAY' | 'TUESDAY' | 'WEDNESDAY' | 'THURSDAY' | 'FRIDAY' | 'SATURDAY';
export type HolidayRecurrenceType = 'DAILY' | 'WEEKLY' | 'MONTHLY' | 'YEARLY';
export type WeekOfMonth = 'FIRST' | 'SECOND' | 'THIRD' | 'FOURTH' | 'LAST';

/** Mirrors the backend's EventRecurrenceRequest -- the "Repeats" configuration chosen inline on
 *  the Add/Edit Event form (a simplified iOS/Google Calendar Repeat picker). */
export interface EventRecurrenceRequest {
  recurrenceType: HolidayRecurrenceType;
  intervalCount?: number;
  endDate?: string | null;
  month?: number | null;
  dayOfMonth?: number | null;
  weekOfMonth?: WeekOfMonth | null;
  dayOfWeek?: AppDayOfWeek | null;
}

export type BlockType = 'ONE_OFF' | 'RECURRING';

export interface BlockedPeriod {
  id: number;
  periodId: number;
  periodName: string;
  blockType: BlockType;
  specificDate: string | null;
  dayOfWeek: AppDayOfWeek | null;
  rangeStartDate: string | null;
  rangeEndDate: string | null;
  reason: string;
  createdAt: string;
  updatedAt: string;
  /** Non-null only when this block was auto-generated from a HOLIDAY CalendarEvent -- drives the
   *  "Auto · Holiday" tag; deleting the row (with no special-cased UI) is the unblock override. */
  sourceCalendarEventId: number | null;
}

export interface BlockedPeriodRequest {
  periodId: number;
  blockType: BlockType;
  specificDate?: string | null;
  dayOfWeek?: AppDayOfWeek | null;
  rangeStartDate?: string | null;
  rangeEndDate?: string | null;
  reason: string;
}

export type TermType = 'ODD' | 'EVEN';
export type TermInstanceStatus = 'PLANNED' | 'OPEN' | 'LOCKED';
export type LateFeeType = 'FLAT' | 'PER_DAY';

export interface TermInstance {
  id: number;
  academicYearId: number;
  academicYearName: string;
  termType: TermType;
  startDate: string;
  endDate: string;
  status: TermInstanceStatus;
  createdAt: string;
  updatedAt: string;
}

export interface TermInstanceUpdateRequest {
  startDate?: string | null;
  endDate?: string | null;
  status?: TermInstanceStatus | null;
}

export interface TermBillingSchedule {
  id: number;
  academicYearId: number;
  academicYearName: string;
  termType: TermType;
  dueDate: string;
  lateFeeType: LateFeeType;
  lateFeeAmount: number;
  graceDays: number;
  createdAt: string;
  updatedAt: string;
}

export interface TermBillingScheduleRequest {
  academicYearId: number;
  termType: TermType;
  dueDate: string;
  lateFeeType: LateFeeType;
  lateFeeAmount: number;
  graceDays: number;
}

export interface TermDatesRequest {
  startDate: string;
  endDate: string;
}

export interface TermBillingDetailsRequest {
  dueDate: string;
  lateFeeType: LateFeeType;
  lateFeeAmount: number;
  graceDays: number;
}

/**
 * Updates the academic year's own dates together with both terms' dates and billing details in
 * one request, so the backend can validate the full combined target state atomically. Submitting
 * these as three separate sequential calls (AY, then terms, then billing) deadlocked whenever an
 * edit shrank or widened the academic year and its term together in the same save — each call
 * validated its own dates against the other's still-persisted, not-yet-updated value.
 */
export interface AcademicYearFullUpdateRequest {
  name: string;
  startDate: string;
  endDate: string;
  isCurrent: boolean;
  oddTerm: TermDatesRequest;
  evenTerm: TermDatesRequest;
  oddBilling: TermBillingDetailsRequest;
  evenBilling: TermBillingDetailsRequest;
}

export type EnrollmentStatus = 'ENROLLED' | 'COMPLETED' | 'DROPPED';

export interface StudentTermEnrollment {
  id: number;
  studentId: number;
  studentName: string;
  cohortId: number;
  cohortCode: string;
  termInstanceId: number;
  termInstanceLabel: string;
  semesterNumber: number;
  yearOfStudy: number;
  status: EnrollmentStatus;
}

export interface GenerateEnrollmentsResponse {
  enrollmentsCreated: number;
}

export type RegistrationStatus = 'REGISTERED' | 'DROPPED' | 'COMPLETED';

export interface CourseOffering {
  id: number;
  termInstanceId: number;
  termInstanceLabel: string;
  curriculumVersionId: number;
  curriculumVersionName: string;
  subjectId: number;
  subjectName: string;
  subjectCode: string;
  subjectSpecialityId: number | null;
  subjectSpecialityName: string | null;
  semesterNumber: number;
  facultyId: number | null;
  sectionLabel: string | null;
  isActive: boolean;
  curriculumTermCourseId: number | null;
  isElective: boolean;
  subjectType: 'CORE' | 'FOUNDATIONAL' | 'ELECTIVE';
  electiveGroupId: number | null;
  electiveGroupName: string | null;
  labHours: number;
  clinicalHours: number;
  createdAt: string;
  updatedAt: string;
}

export interface CourseOfferingUpdateRequest {
  facultyId?: number | null;
  sectionLabel?: string | null;
}

export interface GenerateCourseOfferingsResponse {
  offeringsCreated: number;
}

export interface CourseRegistration {
  id: number;
  enrollmentId: number;
  studentId: number;
  studentName: string;
  cohortCode: string;
  courseOfferingId: number;
  subjectName: string;
  subjectCode: string;
  semesterNumber: number;
  status: RegistrationStatus;
  createdAt: string;
  updatedAt: string;
}

export interface GenerateCourseRegistrationsResponse {
  registrationsCreated: number;
}

export type DemandStatus = 'UNPAID' | 'PARTIAL' | 'PAID' | 'WAIVED';

export interface FeeDemand {
  id: number;
  enrollmentId: number;
  studentId: number;
  studentName: string;
  cohortCode: string;
  termInstanceId: number;
  termInstanceLabel: string;
  academicYearId: number;
  academicYearName: string;
  totalAmount: number;
  dueDate: string;
  paidAmount: number;
  outstandingAmount: number;
  status: DemandStatus;
}

export interface GenerateDemandsResponse {
  demandsCreated: number;
  yearlySkipped: number;
}

export type PaymentMode =
  | 'CASH'
  | 'CARD'
  | 'UPI'
  | 'CHEQUE'
  | 'DEMAND_DRAFT'
  | 'BANK_TRANSFER'
  | 'SCHOLARSHIP';


