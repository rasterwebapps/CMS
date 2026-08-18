import { TourDefinition, TourFlowMap } from '../tour.service';

// ─────────────────────────────────────────────────────────────────────────────
// Journal Explorer
// ─────────────────────────────────────────────────────────────────────────────
export const LIBRARY_PERIODICAL_LIST_TOUR: TourDefinition = {
  steps: [
    {
      popover: {
        title: '📰 Journals & Periodicals',
        description: 'The journal subscription register — national and international periodicals, the book catalogue\'s counterpart for journals.',
        side: 'over',
        align: 'center',
      },
    },
    {
      element: '#tour-libjrn-add',
      popover: {
        title: 'Add or Import',
        description: 'Add a single journal entry, or bulk-import many from an Excel file.',
        side: 'bottom',
        align: 'end',
      },
    },
    {
      element: '#tour-libjrn-toolbar',
      popover: {
        title: 'Search & Filters',
        description: 'Search by journal name, organization, accession number, or volume; filter by type and subscription status. Select rows to print labels in bulk.',
        side: 'bottom',
        align: 'start',
      },
    },
    {
      element: '#tour-libjrn-table',
      popover: {
        title: 'Row Actions',
        description: 'View History, Print Barcode, Edit, or Delete from each row. Barcode printer routing is configured in Library Settings.',
        side: 'top',
        align: 'start',
      },
    },
  ],
};

export const LIBRARY_PERIODICAL_LIST_FLOW_MAP: TourFlowMap = {
  funnel: [{ label: 'Journal Explorer', description: 'Search, register, and manage the journal/periodical subscription register.' }],
  currentIndex: 0,
  steps: [
    { label: 'Search & Filter', icon: 'search', detail: 'By journal name, organization, accession number, volume, type, or subscription status.' },
    { label: 'Add or Import', icon: 'open', detail: 'Add a single entry, or bulk-import from Excel.' },
    { label: 'Row Actions', icon: 'checklist', detail: 'View History, Print Barcode, Edit, or Delete per journal.' },
    { label: 'Print Labels', icon: 'send', detail: 'Select rows and print barcode labels in bulk.' },
  ],
};
