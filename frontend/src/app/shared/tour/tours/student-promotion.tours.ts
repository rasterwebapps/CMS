import { TourDefinition, TourFlowMap } from '../tour.service';

// ─────────────────────────────────────────────────────────────────────────────
// Student Promotion
// ─────────────────────────────────────────────────────────────────────────────
export const STUDENT_PROMOTION_TOUR: TourDefinition = {
  steps: [
    {
      popover: {
        title: '🎓 Student Promotion',
        description: 'Review academic eligibility and promote a whole cohort to its next term — the sole path for next-term enrollment and fee-demand creation.',
        side: 'over',
        align: 'center',
      },
    },
    {
      element: '#tour-sp-select',
      popover: {
        title: '1. Select Cohort & Terms',
        description: 'Pick a cohort — its current term is usually auto-detected, with the next term suggested. Use "Choose different terms manually" for anything unusual.',
        side: 'bottom',
        align: 'start',
      },
    },
    {
      element: '#tour-sp-preview',
      popover: {
        title: '2. Review Eligibility',
        description: 'Every enrolled student, with attendance, pending arrears, and any hard block (unresolved arrears at the Final Year gate, exceeded max duration). Set each student\'s decision — Promote, Promote with arrears, Detain, Graduate, or Exclude.',
        side: 'top',
        align: 'start',
      },
    },
    {
      element: '#tour-sp-execute',
      popover: {
        title: '3. Execute',
        description: 'Optionally generate course registrations and fee demands for promoted students in the same pass, then Execute Promotion.',
        side: 'top',
        align: 'start',
      },
    },
    {
      popover: {
        title: '✅ Result Summary',
        description: 'Counts of Promoted / With Arrears / Detained / Graduated / Excluded, plus any decisions the backend rejected and why.',
        side: 'over',
        align: 'center',
      },
    },
  ],
};

export const STUDENT_PROMOTION_FLOW_MAP: TourFlowMap = {
  funnel: [{ label: 'Student Promotion', description: 'Review academic eligibility and promote a cohort to its next term.' }],
  currentIndex: 0,
  steps: [
    { label: 'Select Cohort & Terms', icon: 'search', detail: 'Pick a cohort — its current and next term are usually auto-detected.' },
    { label: 'Review Eligibility', icon: 'checklist', detail: 'Attendance, arrears, and hard blocks per student, with an editable decision.' },
    { label: 'Execute', icon: 'send', detail: 'Optionally generate course registrations and fee demands, then execute.' },
    { label: 'Result', icon: 'receipt', detail: 'Promoted/detained/graduated/excluded counts and any rejected decisions.' },
  ],
};
