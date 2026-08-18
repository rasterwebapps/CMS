import { TourDefinition, TourFlowMap } from '../tour.service';

// Manage Exams → Exam Results, a small local funnel (schedule an exam, then review its results).
export const EXAMINATION_FUNNEL = [
  { label: 'Manage Exams', description: 'Schedule and manage course examinations — name, type, date, duration, and max marks.' },
  { label: 'Exam Results', description: 'View and filter recorded results for an examination.' },
];

// ─────────────────────────────────────────────────────────────────────────────
// Manage Exams
// ─────────────────────────────────────────────────────────────────────────────
export const EXAMINATION_LIST_TOUR: TourDefinition = {
  steps: [
    {
      popover: {
        title: '📝 Examination Schedule',
        description: 'Schedule and manage every course examination — name, type, date, duration, and max marks.',
        side: 'over',
        align: 'center',
      },
    },
    {
      element: '#tour-exam-toolbar',
      popover: {
        title: 'Search',
        description: 'Search examinations by name or course.',
        side: 'bottom',
        align: 'start',
      },
    },
    {
      element: '#tour-exam-table',
      popover: {
        title: 'Examinations',
        description: 'Each row is one scheduled examination. Edit or delete from the row actions, or Add Examination to schedule a new one.',
        side: 'top',
        align: 'start',
      },
    },
  ],
};

export const EXAMINATION_LIST_FLOW_MAP: TourFlowMap = {
  funnel: EXAMINATION_FUNNEL,
  currentIndex: 0,
  steps: [
    { label: 'Search', icon: 'search', detail: 'Find an examination by name or course.' },
    { label: 'Review Schedule', icon: 'checklist', detail: 'Each row is one exam: type, date, duration, and max marks.' },
    { label: 'Add / Edit', icon: 'open', detail: 'Schedule a new examination or edit an existing one.' },
  ],
};

// ─────────────────────────────────────────────────────────────────────────────
// Exam Results
// ─────────────────────────────────────────────────────────────────────────────
export const EXAM_RESULT_LIST_TOUR: TourDefinition = {
  steps: [
    {
      popover: {
        title: '📊 Exam Results',
        description: 'View and filter recorded results — marks, grade, and pass/fail status — for an examination.',
        side: 'over',
        align: 'center',
      },
    },
    {
      element: '#tour-er-toolbar',
      popover: {
        title: 'Search',
        description: 'Once results are showing, search by student name or roll number to find a specific result.',
        side: 'bottom',
        align: 'start',
      },
    },
    {
      element: '#tour-er-table',
      popover: {
        title: 'Results',
        description: 'Each row shows one student\'s marks obtained, grade, and result status for the selected examination.',
        side: 'top',
        align: 'start',
      },
    },
  ],
};

export const EXAM_RESULT_LIST_FLOW_MAP: TourFlowMap = {
  funnel: EXAMINATION_FUNNEL,
  currentIndex: 1,
  steps: [
    { label: 'Select an Examination', icon: 'search', detail: 'Results are scoped to one examination at a time.' },
    { label: 'Search', icon: 'checklist', detail: 'Find a student by name or roll number within the results.' },
    { label: 'Review', icon: 'open', detail: 'Marks obtained, grade, and status per student.' },
  ],
};
