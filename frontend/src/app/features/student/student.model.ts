export interface Student {
  id: number;
  rollNumber: string;
  firstName: string;
  lastName: string;
  fullName: string;
  email: string;
  phone?: string;
  programId: number;
  programName: string;
  semester: number;
  admissionDate: string;
  labBatch?: string;
  status: string;
  dateOfBirth?: string;
  gender?: string;
  nationality?: string;
  religion?: string;
  communityCategory?: string;
  caste?: string;
  bloodGroup?: string;
  fatherName?: string;
  motherName?: string;
  parentMobile?: string;
  postalAddress?: string;
  street?: string;
  city?: string;
  district?: string;
  state?: string;
  pincode?: string;
  createdAt: string;
  updatedAt: string;
}

export interface StudentRequest {
  rollNumber: string;
  firstName: string;
  lastName: string;
  email: string;
  phone?: string;
  programId: number;
  semester: number;
  admissionDate: string;
  labBatch?: string;
  status?: string;
  dateOfBirth?: string;
  gender?: string;
  nationality?: string;
  religion?: string;
  communityCategory?: string;
  caste?: string;
  bloodGroup?: string;
  fatherName?: string;
  motherName?: string;
  parentMobile?: string;
  address?: {
    postalAddress?: string;
    street?: string;
    city?: string;
    district?: string;
    state?: string;
    pincode?: string;
  };
}
