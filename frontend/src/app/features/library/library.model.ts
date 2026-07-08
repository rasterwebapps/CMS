export interface Page<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
  first: boolean;
  last: boolean;
}

export type BookStatus = 'AVAILABLE' | 'ISSUED' | 'LOST' | 'DAMAGED' | 'WITHDRAWN';
export type BookSourceOfSupply = 'PURCHASE' | 'DONATION' | 'EXCHANGE';

export interface LibraryBook {
  id: number;
  accessionNumber: string;
  entryDate?: string;
  title: string;
  authors: string;
  publisher?: string;
  yearOfPublication?: string;
  edition?: string;
  isbn?: string;
  collation?: string;
  series?: string;
  callNumber?: string;
  libraryId: number;
  libraryName?: string;
  rackId?: number;
  rackName?: string;
  shelfId?: number;
  shelfName?: string;
  subjectCategory?: string;
  sourceOfSupply?: BookSourceOfSupply;
  vendorDonorName?: string;
  billNumber?: string;
  billDate?: string;
  priceRs?: number;
  status: BookStatus;
  remarks?: string;
  createdAt: string;
  updatedAt: string;
}

export interface LibraryBookRequest {
  accessionNumber?: string;
  entryDate?: string;
  title: string;
  authors: string;
  publisher?: string;
  yearOfPublication?: string;
  edition?: string;
  isbn?: string;
  collation?: string;
  series?: string;
  callNumber?: string;
  libraryId: number;
  shelfId?: number;
  subjectCategory?: string;
  sourceOfSupply?: BookSourceOfSupply;
  vendorDonorName?: string;
  billNumber?: string;
  billDate?: string;
  priceRs?: number;
  status?: BookStatus;
  remarks?: string;
}

// ── Library / Rack / Shelf masters ──────────────────────────────

export interface Library {
  id: number;
  name: string;
  code: string;
  address?: string;
  isActive: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface LibraryRack {
  id: number;
  libraryId: number;
  libraryName?: string;
  name: string;
  code: string;
  description?: string;
  isActive: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface LibraryRackRequest {
  libraryId: number;
  name: string;
  code: string;
  description?: string;
  isActive?: boolean;
}

export interface LibraryShelf {
  id: number;
  rackId: number;
  rackName?: string;
  libraryId: number;
  libraryName?: string;
  name: string;
  code: string;
  description?: string;
  isActive: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface LibraryShelfRequest {
  rackId: number;
  name: string;
  code: string;
  description?: string;
  isActive?: boolean;
}

// ── Book transfer ────────────────────────────────────────────

export interface LibraryBookTransferRequest {
  newShelfId: number;
  notes?: string;
}

export interface LibraryBookBulkTransferRequest {
  bookIds: number[];
  newShelfId: number;
  notes?: string;
}

export interface LibraryBookTransferFailure {
  bookId: number;
  reason: string;
}

export interface LibraryBookTransferResult {
  succeededBookIds: number[];
  failed: LibraryBookTransferFailure[];
}

export interface LibraryBookShelfTransfer {
  id: number;
  bookId: number;
  oldLibraryName?: string;
  oldRackName?: string;
  oldShelfName?: string;
  newLibraryName: string;
  newRackName?: string;
  newShelfName?: string;
  transferredAt: string;
  transferredBy?: string;
  notes?: string;
}

export interface LibraryBookImportRowError {
  sheet: string;
  rowNumber: number;
  column: string;
  message: string;
  severity: 'ERROR' | 'WARNING';
}

export interface LibraryBookImportValidationResult {
  totalRows: number;
  validRows: number;
  invalidRows: number;
  errors: LibraryBookImportRowError[];
  warnings: LibraryBookImportRowError[];
}

export interface LibraryBookImportExecuteResult {
  booksImported: number;
  booksSkipped: number;
  errors: LibraryBookImportRowError[];
}

export const BOOK_STATUS_OPTIONS: { value: BookStatus; label: string; colorClass: string }[] = [
  { value: 'AVAILABLE',  label: 'Available',  colorClass: 'status--available' },
  { value: 'ISSUED',     label: 'Issued',     colorClass: 'status--issued' },
  { value: 'LOST',       label: 'Lost',       colorClass: 'status--lost' },
  { value: 'DAMAGED',    label: 'Damaged',    colorClass: 'status--damaged' },
  { value: 'WITHDRAWN',  label: 'Withdrawn',  colorClass: 'status--withdrawn' },
];

export const BOOK_SOURCE_OPTIONS: { value: BookSourceOfSupply; label: string }[] = [
  { value: 'PURCHASE',  label: 'Purchase' },
  { value: 'DONATION',  label: 'Donation' },
  { value: 'EXCHANGE',  label: 'Exchange' },
];

// ── Settings ─────────────────────────────────────────────────

export interface LibrarySetting {
  id: number;
  settingKey: string;
  settingValue: string;
  displayName: string;
  description?: string;
  dataType: 'INTEGER' | 'DECIMAL' | 'STRING';
  updatedAt: string;
}

export interface LibrarySettingUpdateRequest {
  settingValue: string;
}

// ── Periodicals ───────────────────────────────────────────────

export type JournalType = 'NATIONAL' | 'INTERNATIONAL';
export type SubscriptionStatus = 'ACTIVE' | 'EXPIRED';

export interface LibraryPeriodical {
  id: number;
  accessionNumber?: string;
  journalName: string;
  journalType: JournalType;
  organization?: string;
  volumeNumber?: string;
  issueNumber?: string;
  monthRange?: string;
  year?: number;
  copiesCount: number;
  subscriptionStatus: SubscriptionStatus;
  status: BookStatus;
  receivedDate?: string;
  receivedBy?: string;
  remarks?: string;
  createdAt: string;
  updatedAt: string;
}

export interface LibraryPeriodicalRequest {
  accessionNumber?: string;
  journalName: string;
  journalType?: JournalType;
  organization?: string;
  volumeNumber?: string;
  issueNumber?: string;
  monthRange?: string;
  year?: number;
  copiesCount?: number;
  subscriptionStatus?: SubscriptionStatus;
  status?: BookStatus;
  receivedDate?: string;
  receivedBy?: string;
  remarks?: string;
}

export const JOURNAL_TYPE_OPTIONS: { value: JournalType; label: string }[] = [
  { value: 'NATIONAL',      label: 'National' },
  { value: 'INTERNATIONAL', label: 'International' },
];

export const SUBSCRIPTION_STATUS_OPTIONS: { value: SubscriptionStatus; label: string }[] = [
  { value: 'ACTIVE',  label: 'Active' },
  { value: 'EXPIRED', label: 'Expired' },
];

export const MONTH_RANGE_OPTIONS: string[] = [
  'Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun',
  'Jul', 'Aug', 'Sep', 'Oct', 'Nov', 'Dec',
  'Jan-Feb', 'Mar-Apr', 'May-Jun', 'Jul-Aug', 'Sep-Oct', 'Nov-Dec',
  'Jan-Mar', 'Apr-Jun', 'Jul-Sep', 'Oct-Dec',
  'Jan-Jun', 'Jul-Dec',
  'Jan-Dec',
];

// ── Circulation ───────────────────────────────────────────────

export type IssueStatus = 'ISSUED' | 'RETURNED' | 'OVERDUE' | 'LOST';
export type LibraryMemberType = 'STUDENT' | 'FACULTY';
export type FineStatus = 'PENDING' | 'WAIVED' | 'COLLECTED';
export type LibraryItemType = 'BOOK' | 'JOURNAL';

export const LIBRARY_ITEM_TYPE_OPTIONS: { value: LibraryItemType; label: string }[] = [
  { value: 'BOOK',    label: 'Book' },
  { value: 'JOURNAL', label: 'Journal' },
];

export interface LibraryFine {
  id: number;
  overdueDays: number;
  finePerDay: number;
  totalFine: number;
  status: FineStatus;
  waivedBy?: string;
  collectedAt?: string;
  remarks?: string;
}

export interface LibraryIssue {
  id: number;
  itemType: LibraryItemType;
  bookId?: number;
  periodicalId?: number;
  accessionNumber: string;
  itemTitle: string;
  itemDetail: string;
  callNumber?: string;
  shelfLocation?: string;
  memberType: LibraryMemberType;
  studentId?: number;
  studentName?: string;
  studentRollNumber?: string;
  facultyId?: number;
  facultyName?: string;
  facultyEmployeeCode?: string;
  issuedDate: string;
  dueDate: string;
  returnedDate?: string;
  renewalCount: number;
  lastRenewedDate?: string;
  status: IssueStatus;
  issuedBy: string;
  returnedTo?: string;
  remarks?: string;
  fine?: LibraryFine;
  createdAt: string;
  updatedAt: string;
}

export interface LibraryIssueRequest {
  itemType: LibraryItemType;
  bookId?: number;
  periodicalId?: number;
  memberType: LibraryMemberType;
  studentId?: number;
  facultyId?: number;
  issuedDate?: string;
  remarks?: string;
}

export interface LibraryCirculationLookup {
  itemType: LibraryItemType;
  itemId: number;
  accessionNumber: string;
  title: string;
  detail: string;
  callNumber?: string;
  shelfLocation?: string;
  status: BookStatus;
}

export interface LibraryReturnRequest {
  remarks?: string;
}

export interface LibraryRenewRequest {
  remarks?: string;
}

export const ISSUE_STATUS_OPTIONS: { value: IssueStatus; label: string; colorClass: string }[] = [
  { value: 'ISSUED',   label: 'Issued',   colorClass: 'issue-status--issued' },
  { value: 'RETURNED', label: 'Returned', colorClass: 'issue-status--returned' },
  { value: 'OVERDUE',  label: 'Overdue',  colorClass: 'issue-status--overdue' },
  { value: 'LOST',     label: 'Lost',     colorClass: 'issue-status--lost' },
];

export const FINE_STATUS_OPTIONS: { value: FineStatus; label: string; colorClass: string }[] = [
  { value: 'PENDING',   label: 'Pending',   colorClass: 'fine-status--pending' },
  { value: 'WAIVED',    label: 'Waived',    colorClass: 'fine-status--waived' },
  { value: 'COLLECTED', label: 'Collected', colorClass: 'fine-status--collected' },
];

export interface LibraryFineDetail {
  id: number;
  issueId: number;
  itemType: LibraryItemType;
  accessionNumber: string;
  itemTitle: string;
  memberType: LibraryMemberType;
  memberName: string;
  memberCode?: string;
  issuedDate: string;
  dueDate: string;
  returnedDate?: string;
  overdueDays: number;
  finePerDay: number;
  totalFine: number;
  status: FineStatus;
  waivedBy?: string;
  collectedAt?: string;
  remarks?: string;
  createdAt: string;
}

export interface LibraryFineActionRequest {
  remarks?: string;
}

export const SUBJECT_CATEGORY_OPTIONS: string[] = [
  'Anatomy & Physiology',
  'Biochemistry',
  'Microbiology',
  'Pathology',
  'Pharmacology',
  'Nursing Fundamentals',
  'Medical-Surgical Nursing',
  'Obstetrics & Gynaecology',
  'Paediatric Nursing',
  'Mental Health Nursing',
  'Community Health Nursing',
  'Midwifery',
  'Nutrition',
  'Psychology',
  'English',
  'Computer Science',
  'Management',
  'Research',
  'General',
];
