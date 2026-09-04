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

/** Declares that a specific calendar date runs a DIFFERENT weekday's timetable than its own
 *  actual weekday (a compensatory working day, e.g. "this Saturday runs Monday's schedule").
 *  {@code mappedDate} is unique institution-wide; the mapped date always fully suppresses its
 *  own actual-weekday sessions and substitutes the borrowed weekday's instead. */
export interface DayMapping {
  id: number;
  termInstanceId: number;
  mappedDate: string;
  borrowedDayOfWeek: AppDayOfWeek;
  reason: string;
  createdAt: string;
  updatedAt: string;
}

export interface DayMappingRequest {
  termInstanceId: number;
  mappedDate: string;
  borrowedDayOfWeek: AppDayOfWeek;
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

/** Live checklist data shown before advancing a term's status — every field is system-verified
 *  from real state; fields irrelevant to targetStatus come back empty/zero rather than missing. */
export interface TermAdvanceChecklist {
  targetStatus: TermInstanceStatus;
  cohortsWithoutCurriculum: string[];
  outstandingFeeDemandCount: number;
  outstandingFeeDemandAmount: number;
  draftTimetableSessionCount: number;
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
  rollNumber: string | null;
  cohortId: number;
  cohortCode: string;
  termInstanceId: number;
  termInstanceLabel: string;
  termNumber: number;
  yearOfStudy: number;
  status: EnrollmentStatus;
}

export interface GenerateEnrollmentsResponse {
  enrollmentsCreated: number;
}

export type RegistrationStatus = 'REGISTERED' | 'DROPPED' | 'COMPLETED';

export type ElectiveSelectionMode = 'STUDENT_CHOICE' | 'INSTITUTION_DECIDED';

export interface ElectiveBulkAssignmentResponse {
  eligibleStudentCount: number;
  assignedCount: number;
  blockedCount: number;
}

export interface ElectiveGroupSummary {
  electiveGroupId: number;
  electiveGroupName: string;
  selectionMode: ElectiveSelectionMode;
  termNumber: number;
  eligibleCount: number;
  assignedCount: number;
  scheduled: boolean;
}

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
  /** Faculty explicitly widened onto this subject on top of the Speciality-match rule — additive
   *  only (see backend FacultyEligibility). Empty means Speciality-match-only. */
  subjectEligibleFacultyIds: number[];
  termNumber: number;
  isActive: boolean;
  curriculumTermCourseId: number | null;
  isElective: boolean;
  subjectType: 'CORE' | 'FOUNDATIONAL' | 'ELECTIVE';
  electiveGroupId: number | null;
  electiveGroupName: string | null;
  electiveGroupSelectionMode: ElectiveSelectionMode | null;
  theoryHours: number;
  labHours: number;
  clinicalHours: number;
  /** Subject's on-campus clinical session block size (consecutive periods), set in Subject Master —
   *  read-only here; Clinical Shift Duration below can't be set shorter than this many periods. */
  clinicalSessionBlockPeriods: number;
  /** Configurable off-campus clinical shift length/travel buffer — null means this offering has
   *  no shift-based clinical component (on-campus-only clinical uses the standard Period grid). */
  clinicalShiftDurationMinutes: number | null;
  clinicalTravelBufferMinutes: number | null;
  createdAt: string;
  updatedAt: string;
  /** CourseOffering has no cohort FK of its own — it's keyed by curriculum version, which can be
   *  shared by more than one cohort's admission year on the same (program, course). Usually a
   *  single name; more than one means this exact row is shared across cohorts. Empty when no
   *  cohort is currently enrolled against this offering's curriculum version + semester. */
  cohortNames: string[];
}

/** Deactivating (isActive: false) is blocked server-side when the offering already has sessions
 *  placed in Skeleton Builder or batches with students rostered — reactivating has no such guard. */
export interface CourseOfferingStatusUpdateRequest {
  isActive: boolean;
  reason?: string;
}

export interface CourseOfferingStatusUpdateResponse {
  id: number;
  isActive: boolean;
  updatedAt: string;
}

/** FULL = every expected Theory row and Batch coordinator slot is filled. PARTIAL = at least one
 *  filled, at least one not. NONE = at least one expected, none filled. NOT_APPLICABLE = nothing
 *  to assign yet (no cohort resolves and no active batches exist). */
export type OfferingAssignmentStatus = 'FULL' | 'PARTIAL' | 'NONE' | 'NOT_APPLICABLE';

/** Per-offering roll-up of who's currently assigned (see backend CourseOfferingFacultySummaryDto) —
 *  backs the Assign Faculty list table's Faculty and Status columns. Present for every offering in
 *  the term now. assignedFacultyNames stays Theory-only (render "Unassigned" when empty);
 *  assignmentStatus additionally covers Lab/Clinical coordinators. */
export interface CourseOfferingFacultySummary {
  offeringId: number;
  assignedFacultyNames: string[];
  assignmentStatus: OfferingAssignmentStatus;
}

export interface FacultyCapacitySpreadLoadSuggestion {
  alternateFacultyId: number;
  alternateFacultyName: string;
  alternateSpareCapacityHours: number;
  courseOfferingId: number;
  subjectName: string;
}

/** One candidate in an eligible-faculty picker (offering-level or section-level) — mirrors the
 *  backend's EligibleFacultyCandidateDto exactly. Sorted most-free-first by the backend; render in
 *  the order received. capacityTier === 'NONE' means no cap is configured for this candidate at
 *  any tier — show "no cap configured" rather than a 0h remaining figure. */
export interface EligibleFacultyCandidate {
  facultyId: number;
  facultyName: string;
  specialityMatch: boolean;
  viaEligibleList: boolean;
  currentlyAssigned: boolean;
  currentDemandHours: number;
  capacityHours: number;
  capacityTier: string;
  remainingHours: number;
  overCapacity: boolean;
}

/** Live pre-save capacity check for the Course Offering edit dialog's Faculty picker — mirrors
 *  the backend's FacultyCapacityCheckResult exactly. */
export interface FacultyCapacityCheckResult {
  overCapacity: boolean;
  currentDemandHours: number;
  offeringHours: number;
  projectedTotalHours: number;
  capacityHours: number;
  dailyCap: number;
  capacityTier: string;
  workingDaysInTerm: number;
  suggestedMinDailyHours: number;
  spreadLoad: FacultyCapacitySpreadLoadSuggestion[];
}

/** One cohort's faculty assignment for an offering — cohortId is always set (cohortName exists
 *  because a CourseOffering can be shared by more than one cohort on the same curriculum version,
 *  each assigned independently). cohortSectionId/sectionLabel are null when that cohort's Theory
 *  delivery has no active section split (a single whole-cohort row), or set to identify exactly
 *  which section this row covers when it does. */
export interface SectionFacultyAssignment {
  cohortId: number;
  cohortSectionId: number | null;
  cohortName: string;
  sectionLabel: string | null;
  facultyId: number | null;
  facultyName: string | null;
  /** Null when no row exists yet (shows "Unassigned") -- checked against the current row on save,
   *  rejected with a conflict if it no longer matches (someone else changed it since). */
  version: number | null;
}

/** applicable=false means this offering's cohort couldn't be uniquely resolved (none currently
 *  enrolled against this offering's curriculum version + semester) — reason explains why, sections
 *  is empty. Otherwise every cohort using the offering gets at least one row, always — one per
 *  active section if split, or exactly one whole-cohort row if not; there's no "nothing to show"
 *  case anymore now that Section Faculty is the sole assignment mechanism. */
export interface CourseOfferingSectionFacultyResponse {
  applicable: boolean;
  reason: string | null;
  sections: SectionFacultyAssignment[];
}

/** One committed CohortSection for a term, with its Class Incharge if assigned — structurally
 *  created in Capacity Planner, staffed here (Assign Faculty), same split as batch coordinators
 *  and Section Faculty. No fallback: a section with facultyId null simply has no incharge yet. */
export interface ClassInchargeAssignment {
  cohortSectionId: number;
  cohortName: string;
  sectionLabel: string;
  classroomName: string;
  facultyId: number | null;
  facultyName: string | null;
}

export interface GenerateCourseOfferingsResponse {
  offeringsCreated: number;
  activeCohortCount: number;
  cohortsWithoutCurriculumVersion: string[];
  cohortsWithoutProgramTotalTerms: number;
  offeringsAlreadyExisting: number;
  subjectsWithoutFacultyPool: string[];
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
  termNumber: number;
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


