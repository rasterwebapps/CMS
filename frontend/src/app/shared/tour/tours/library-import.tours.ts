import { TourDefinition, TourFlowMap } from '../tour.service';

// ─────────────────────────────────────────────────────────────────────────────
// Import (Books / Journals)
// ─────────────────────────────────────────────────────────────────────────────
export const LIBRARY_IMPORT_TOUR: TourDefinition = {
  steps: [
    {
      popover: {
        title: '⬆️ Bulk Import',
        description: 'Bulk-import book or journal records from an Excel file straight into the Accession Register.',
        side: 'over',
        align: 'center',
      },
    },
    {
      element: '#tour-libimp-steps',
      popover: {
        title: 'Three Steps',
        description: 'Download Template gets you a pre-formatted Excel sheet (with a reference tab of valid values). Upload & Validate checks your filled sheet before anything is written. Results shows what happened.',
        side: 'bottom',
        align: 'start',
      },
    },
    {
      popover: {
        title: '✅ Validate First',
        description: 'Validate Only checks every row without importing — fix the errors it finds, or tick "Skip errored rows" to import everything valid and leave the rest out.',
        side: 'over',
        align: 'center',
      },
    },
  ],
};

export const LIBRARY_IMPORT_FLOW_MAP: TourFlowMap = {
  funnel: [{ label: 'Import', description: 'Bulk-import book or journal records from an Excel file.' }],
  currentIndex: 0,
  steps: [
    { label: 'Download Template', icon: 'open', detail: 'A pre-formatted Excel sheet with a reference tab of valid values.' },
    { label: 'Upload & Validate', icon: 'search', detail: 'Check every row for errors before writing anything.' },
    { label: 'Fix or Skip', icon: 'checklist', detail: 'Fix flagged rows and re-upload, or skip errored rows and import the rest.' },
    { label: 'Import', icon: 'send', detail: 'Writes the valid rows to the Accession Register.' },
  ],
};
