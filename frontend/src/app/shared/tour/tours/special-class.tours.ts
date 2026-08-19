import { TourDefinition, TourFlowMap } from '../tour.service';

// My Special Classes → Special Class Approvals, a small local funnel (a
// faculty request gets reviewed by an admin) — same idiom as Examination's
// Manage Exams → Exam Results. The other timetable-operations screens
// (Resource Timetable, Faculty Absence, Staff Session Swap) are independent
// of these two and of each other, so they each get their own single-entry
// funnel instead of sharing a fake group-wide one.
const SPECIAL_CLASS_FUNNEL = [
  { label: 'My Special Classes', description: 'Request an ad-hoc special/remedial class, or a whole-day repeat, and track its approval status.' },
  { label: 'Special Class Approvals', description: 'Review and approve or reject faculty requests for ad-hoc special/remedial classes.' },
];

// ─────────────────────────────────────────────────────────────────────────────
// My Special Classes (faculty-facing request list)
// ─────────────────────────────────────────────────────────────────────────────
export const MY_SPECIAL_CLASSES_TOUR: TourDefinition = {
  steps: [
    {
      popover: {
        title: '➕ My Special Classes',
        description: 'Request an ad-hoc special/remedial class, or a whole-day repeat, and track its approval status.',
        side: 'over',
        align: 'center',
      },
    },
    {
      element: '#tour-msc-request',
      popover: {
        title: 'Request Special Class',
        description: 'Pick a single date, or repeat across a whole day\'s periods, for a subject and venue — an admin must approve it before it goes live.',
        side: 'bottom',
        align: 'end',
      },
    },
    {
      element: '#tour-msc-table',
      popover: {
        title: 'Your Requests',
        description: 'Every request you\'ve made, with its Pending / Approved / Rejected status. Approved requests you haven\'t taught yet can still be cancelled.',
        side: 'top',
        align: 'start',
      },
    },
  ],
};

export const MY_SPECIAL_CLASSES_FLOW_MAP: TourFlowMap = {
  funnel: SPECIAL_CLASS_FUNNEL,
  currentIndex: 0,
  steps: [
    { label: 'Request', icon: 'search', detail: 'Choose a single-date or day-repeat special class — subject, venue, and date(s).' },
    { label: 'Awaiting Approval', icon: 'checklist', detail: 'The request sits Pending until an admin approves or rejects it.' },
    { label: 'Track Status', icon: 'open', detail: 'See Approved / Rejected status here, with the rejection reason if declined.' },
    { label: 'Cancel if Needed', icon: 'send', detail: 'An approved class that hasn\'t happened yet can still be cancelled from this list.' },
  ],
};

// ─────────────────────────────────────────────────────────────────────────────
// Special Class Approvals (admin-facing approval queue)
// ─────────────────────────────────────────────────────────────────────────────
export const SPECIAL_CLASS_APPROVALS_TOUR: TourDefinition = {
  steps: [
    {
      popover: {
        title: '✅ Special Class Approvals',
        description: 'Review and approve or reject faculty requests for ad-hoc special/remedial classes and whole-day repeats.',
        side: 'over',
        align: 'center',
      },
    },
    {
      element: '#tour-aql-table',
      popover: {
        title: 'Pending Requests',
        description: 'Every request awaiting a decision, with who requested it, for whom, and when. A Day Repeat tag marks requests that are part of a whole-day batch.',
        side: 'top',
        align: 'start',
      },
    },
    {
      popover: {
        title: 'Approve or Reject',
        description: 'Approve schedules the class immediately. Reject requires a reason, which the requesting faculty can see. Both act on the entire day-repeat batch at once when applicable.',
        side: 'over',
        align: 'center',
      },
    },
  ],
};

export const SPECIAL_CLASS_APPROVALS_FLOW_MAP: TourFlowMap = {
  funnel: SPECIAL_CLASS_FUNNEL,
  currentIndex: 1,
  steps: [
    { label: 'Review Queue', icon: 'checklist', detail: 'Every pending request, with requester, subject, date, and venue.' },
    { label: 'Approve', icon: 'open', detail: 'Schedules the class immediately — the whole day-repeat batch if applicable.' },
    { label: 'Reject', icon: 'send', detail: 'Requires a reason, visible to the requesting faculty; also applies to the whole batch if applicable.' },
  ],
};
