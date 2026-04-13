export type Designation =
  | 'PROFESSOR'
  | 'ASSOCIATE_PROFESSOR'
  | 'ASSISTANT_PROFESSOR'
  | 'LECTURER'
  | 'SENIOR_LECTURER'
  | 'LAB_INSTRUCTOR'
  | 'TEACHING_ASSISTANT'
  | 'GUEST_FACULTY'
  | 'VISITING_FACULTY'
  | 'HOD';

export type FacultyStatus =
  | 'ACTIVE'
  | 'ON_LEAVE'
  | 'SABBATICAL'
  | 'RESIGNED'
  | 'RETIRED'
  | 'TERMINATED';

export interface Faculty {
  id: number;
  employeeCode: string;
  firstName: string;
  lastName: string;
  fullName: string;
  email: string;
  phone?: string;
  departmentId: number;
  departmentName: string;
  designation: Designation;
  specialization?: string;
  labExpertise?: string;
  joiningDate: string;
  status: FacultyStatus;
  createdAt: string;
  updatedAt: string;
}

export interface FacultyRequest {
  employeeCode: string;
  firstName: string;
  lastName: string;
  email: string;
  phone?: string;
  departmentId: number;
  designation: Designation;
  specialization?: string;
  labExpertise?: string;
  joiningDate: string;
  status?: FacultyStatus;
}

export const DESIGNATION_OPTIONS: { value: Designation; label: string }[] = [
  { value: 'PROFESSOR', label: 'Professor' },
  { value: 'ASSOCIATE_PROFESSOR', label: 'Associate Professor' },
  { value: 'ASSISTANT_PROFESSOR', label: 'Assistant Professor' },
  { value: 'LECTURER', label: 'Lecturer' },
  { value: 'SENIOR_LECTURER', label: 'Senior Lecturer' },
  { value: 'LAB_INSTRUCTOR', label: 'Lab Instructor' },
  { value: 'TEACHING_ASSISTANT', label: 'Teaching Assistant' },
  { value: 'GUEST_FACULTY', label: 'Guest Faculty' },
  { value: 'VISITING_FACULTY', label: 'Visiting Faculty' },
  { value: 'HOD', label: 'Head of Department' },
];

export const FACULTY_STATUS_OPTIONS: { value: FacultyStatus; label: string }[] = [
  { value: 'ACTIVE', label: 'Active' },
  { value: 'ON_LEAVE', label: 'On Leave' },
  { value: 'SABBATICAL', label: 'Sabbatical' },
  { value: 'RESIGNED', label: 'Resigned' },
  { value: 'RETIRED', label: 'Retired' },
  { value: 'TERMINATED', label: 'Terminated' },
];
