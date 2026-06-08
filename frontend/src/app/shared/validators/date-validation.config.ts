/**
 * Date Field Validation Configuration
 * Defines validation rules for all date fields in the application
 *
 * Validation Types:
 * A - Past dates only (up to and including today)
 * B - Future dates only (from today onwards)
 * C - Any date allowed (no restrictions)
 */

export type DateValidationType = 'A' | 'B' | 'C';

export interface DateFieldConfig {
  module: string;
  entity: string;
  field: string;
  type: DateValidationType;
  description: string;
}

export const DATE_VALIDATION_CONFIG: DateFieldConfig[] = [
  // Student Management
  {
    module: 'Student Management',
    entity: 'Student',
    field: 'dateOfBirth',
    type: 'A',
    description: 'Date of birth must be in the past'
  },
  {
    module: 'Student Management',
    entity: 'Student',
    field: 'admissionDate',
    type: 'A',
    description: 'Admission date must be in the past'
  },

  // Admission & Enquiry
  {
    module: 'Admission & Enquiry',
    entity: 'Enquiry',
    field: 'dateOfBirth',
    type: 'A',
    description: 'Date of birth must be in the past'
  },
  {
    module: 'Admission & Enquiry',
    entity: 'Enquiry',
    field: 'enquiryDate',
    type: 'A',
    description: 'Enquiry date must be in the past'
  },
  {
    module: 'Admission & Enquiry',
    entity: 'Admission',
    field: 'applicationDate',
    type: 'A',
    description: 'Application date must be in the past'
  },
  {
    module: 'Admission & Enquiry',
    entity: 'Admission',
    field: 'declarationDate',
    type: 'A',
    description: 'Declaration date must be in the past'
  },

  // Faculty Management
  {
    module: 'Faculty Management',
    entity: 'Faculty',
    field: 'dateOfBirth',
    type: 'A',
    description: 'Date of birth must be in the past'
  },
  {
    module: 'Faculty Management',
    entity: 'Faculty',
    field: 'joiningDate',
    type: 'C',
    description: 'Joining date can be any date'
  },

  // Finance
  {
    module: 'Finance',
    entity: 'FeePayment',
    field: 'paymentDate',
    type: 'A',
    description: 'Payment date must be in the past'
  },
  {
    module: 'Finance',
    entity: 'ScholarshipDisbursement',
    field: 'disbursementDate',
    type: 'C',
    description: 'Disbursement date can be any date'
  },

  // Academic Calendar & Scheduling
  {
    module: 'Academic Calendar',
    entity: 'AcademicYear',
    field: 'startDate',
    type: 'C',
    description: 'Academic year start date can be any date'
  },
  {
    module: 'Academic Calendar',
    entity: 'AcademicYear',
    field: 'endDate',
    type: 'C',
    description: 'Academic year end date can be any date'
  },
  {
    module: 'Academic Calendar',
    entity: 'TermInstance',
    field: 'startDate',
    type: 'C',
    description: 'Term start date can be any date'
  },
  {
    module: 'Academic Calendar',
    entity: 'TermInstance',
    field: 'endDate',
    type: 'C',
    description: 'Term end date can be any date'
  },
  {
    module: 'Academic Calendar',
    entity: 'CalendarEvent',
    field: 'startDate',
    type: 'C',
    description: 'Event start date can be any date'
  },
  {
    module: 'Academic Calendar',
    entity: 'CalendarEvent',
    field: 'endDate',
    type: 'C',
    description: 'Event end date can be any date'
  },

  // Examination
  {
    module: 'Examination',
    entity: 'Examination',
    field: 'date',
    type: 'B',
    description: 'Exam date must be from today onwards'
  },

  // Maintenance & Resources
  {
    module: 'Maintenance',
    entity: 'MaintenanceRequest',
    field: 'requestDate',
    type: 'B',
    description: 'Request date must be from today onwards'
  },
  {
    module: 'Maintenance',
    entity: 'MaintenanceRequest',
    field: 'scheduledDate',
    type: 'B',
    description: 'Scheduled date must be from today onwards'
  },
  {
    module: 'Maintenance',
    entity: 'MaintenanceRequest',
    field: 'completionDate',
    type: 'A',
    description: 'Completion date must be in the past'
  }
];

/**
 * Get validation type for a specific field
 * @param entity Entity name (e.g., 'Student', 'Enquiry')
 * @param field Field name (e.g., 'dateOfBirth', 'admissionDate')
 * @returns DateValidationType or 'C' (any date) if not found
 */
export function getDateValidationType(entity: string, field: string): DateValidationType {
  const config = DATE_VALIDATION_CONFIG.find(
    c => c.entity === entity && c.field === field
  );
  return config ? config.type : 'C';
}

/**
 * Get validation description for a specific field
 * @param entity Entity name
 * @param field Field name
 * @returns Description string
 */
export function getDateValidationDescription(entity: string, field: string): string {
  const config = DATE_VALIDATION_CONFIG.find(
    c => c.entity === entity && c.field === field
  );
  return config ? config.description : '';
}

