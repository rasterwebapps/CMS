export interface Enquiry {
  id: number;
  name: string;
  email: string;
  phone: string;
  programId: number;
  programName: string;
  enquiryDate: string;
  source: string;
  status: string;
  agentId: number | null;
  agentName: string | null;
  assignedTo: string;
  remarks: string;
  feeDiscussedAmount: number | null;
  convertedStudentId: number | null;
  createdAt: string;
  updatedAt: string;
}

export interface EnquiryRequest {
  name: string;
  email?: string;
  phone?: string;
  programId?: number;
  enquiryDate: string;
  source: string;
  status?: string;
  agentId?: number;
  assignedTo?: string;
  remarks?: string;
  feeDiscussedAmount?: number;
}
