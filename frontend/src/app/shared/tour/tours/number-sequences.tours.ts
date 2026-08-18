import { TourDefinition, TourFlowMap } from '../tour.service';

export const NUMBER_SEQUENCES_TOUR: TourDefinition = {
  steps: [
    {
      popover: {
        title: '👋 Welcome to Number Sequences',
        description:
          'This read-only screen shows all auto-generated number series used across the application — receipt numbers, admission numbers, roll numbers, and more.',
        side: 'over',
        align: 'center',
      },
    },
    {
      element: '#tour-numseq-search',
      popover: {
        title: 'Search Sequences',
        description:
          'Type a series name, scope key, or generated number to filter the list instantly.',
        side: 'bottom',
        align: 'start',
      },
    },
    {
      element: '#tour-numseq-table',
      popover: {
        title: 'Sequence Registry',
        description:
          'Each row shows the <strong>Series Name</strong> (e.g., ADM, REC), the <strong>Scope Key</strong> (e.g., 2025-26), the last generated number, a preview of the next number, and when it was last updated.',
        side: 'top',
        align: 'start',
      },
    },
    {
      popover: {
        title: '📌 Read-only Screen',
        description:
          'Number sequences are managed automatically by the system. You cannot edit them here — they increment whenever an admission, receipt, or roll number is generated.',
        side: 'over',
        align: 'center',
      },
    },
    {
      popover: {
        title: '✅ All done!',
        description:
          'You now know how to read the Number Sequences registry. Click <em>Take a Tour</em> any time to replay this walkthrough.',
        side: 'over',
        align: 'center',
      },
    },
  ],
};

export const NUMBER_SEQUENCES_FLOW_MAP: TourFlowMap = {
  funnel: [
    { label: 'Number Sequences', description: 'Read-only registry of every auto-generated number series used across the application.' },
  ],
  currentIndex: 0,
  steps: [
    { label: 'Search Sequences', icon: 'search', detail: 'Type a series name, scope key, or generated number to filter the list instantly.' },
    { label: 'Sequence Registry', icon: 'checklist', detail: 'Each row shows the series name, scope key, last generated number, next-number preview, and last update.' },
    { label: 'Read-Only', icon: 'open', detail: 'This screen is for reference only — sequences are managed automatically by the system, not edited manually.' },
    { label: 'Auto-Increment', icon: 'receipt', detail: 'Sequences increment automatically whenever an admission, receipt, or roll number is generated.' },
  ],
};

