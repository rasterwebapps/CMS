export interface Page<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
  first: boolean;
  last: boolean;
}

export type LoanableItemIssueStatus = 'ISSUED' | 'RETURNED';

export interface LoanableItemIssueCreateRequest {
  productId: number;
  locationId: number;
  borrowerName: string;
  borrowerContact?: string;
  issueDate?: string;
  expectedReturnDate: string;
  conditionOnIssue?: string;
  notes?: string;
}

export interface LoanableItemIssueReturnRequest {
  conditionOnReturn?: string;
  notes?: string;
}

export interface LoanableItemIssue {
  id: number;
  productId: number;
  productCode: string;
  productName: string;
  locationId: number;
  locationVirtualName: string;
  borrowerName: string;
  borrowerContact: string | null;
  status: LoanableItemIssueStatus;
  issueDate: string;
  expectedReturnDate: string;
  actualReturnDate: string | null;
  overdue: boolean;
  conditionOnIssue: string | null;
  conditionOnReturn: string | null;
  notes: string | null;
  issuedBy: string | null;
  issuedAt: string;
  returnedBy: string | null;
  returnedAt: string | null;
}
