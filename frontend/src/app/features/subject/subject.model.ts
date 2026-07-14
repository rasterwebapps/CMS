import { Speciality } from '../speciality/speciality.model';

export interface Page<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
  first: boolean;
  last: boolean;
}

export interface Subject {
  id: number;
  name: string;
  code: string;
  credits: number;
  theoryCredits: number;
  labCredits: number;
  speciality: Speciality | null;
  termNumber: number;
  isActive: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface SubjectRequest {
  name: string;
  code: string;
  credits: number;
  theoryCredits: number;
  labCredits: number;
  specialityId: number | null;
  termNumber: number;
  isActive?: boolean;
}

export interface SubjectStatusUpdateRequest {
  isActive: boolean;
  reason?: string;
}

export interface SubjectStatusUpdateResponse {
  id: number;
  isActive: boolean;
  updatedAt: string;
}
