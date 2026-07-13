import { TourDefinition } from '../tour.service';

export const LIBRARY_BOOK_LIST_TOUR: TourDefinition = {
  steps: [
    {
      popover: {
        title: '👋 Welcome to the Book Catalogue',
        description:
          'This is the Accession Register — the complete inventory of library books. Let\'s walk through the key controls.',
        side: 'over',
        align: 'center',
      },
    },
    {
      element: '#tour-lib-book-add-btn',
      popover: {
        title: 'Add a Book',
        description: 'Register a new book — accession number, barcode, title, author, shelf, and more.',
        side: 'bottom',
        align: 'end',
      },
    },
    {
      element: '#tour-lib-book-search',
      popover: {
        title: 'Search the Catalogue',
        description: 'Search by title, author, accession number, or call number.',
        side: 'bottom',
        align: 'start',
      },
    },
    {
      element: '#tour-lib-book-toolbar',
      popover: {
        title: 'Filters & Bulk Actions',
        description:
          'Filter by status, subject, rack or shelf. Select rows with the checkboxes to <strong>Transfer</strong> or <strong>Print Labels</strong> for several books at once.',
        side: 'bottom',
        align: 'start',
      },
    },
    {
      element: '#tour-lib-book-content',
      popover: {
        title: 'Row Actions',
        description:
          'Each row has its own <strong>View History</strong> (full borrow/return/transfer timeline), <strong>Print Barcode</strong>, and <strong>Transfer</strong> actions. Where the barcode label prints to (browser, a networked printer, or a USB label printer) is controlled from Library Settings — see the Settings tour for that.',
        side: 'top',
        align: 'start',
      },
    },
    {
      popover: {
        title: '✅ All done!',
        description: 'Click <em>Take a Tour</em> any time to replay this walkthrough.',
        side: 'over',
        align: 'center',
      },
    },
  ],
};

export const LIBRARY_SETTINGS_TOUR: TourDefinition = {
  steps: [
    {
      popover: {
        title: '👋 Welcome to Library Settings',
        description:
          'Configure loan periods, borrowing limits, fine rates, barcode labels, and how barcode printing reaches a printer.',
        side: 'over',
        align: 'center',
      },
    },
    {
      element: '#tour-lib-settings-loan',
      popover: {
        title: 'Loan Periods & Limits',
        description:
          'How many days students and faculty may keep a book, and the maximum number of books each may hold at once.',
        side: 'top',
        align: 'start',
      },
    },
    {
      element: '#tour-lib-settings-fine',
      popover: {
        title: 'Fine & Renewals',
        description: 'The overdue fine rate per day, and how many times an issue can be renewed.',
        side: 'top',
        align: 'start',
      },
    },
    {
      element: '#tour-lib-settings-barcode',
      popover: {
        title: 'Barcode Label Size',
        description: 'The physical sticker size (in mm) used for both on-screen preview and printed labels.',
        side: 'top',
        align: 'start',
      },
    },
    {
      element: '#tour-lib-settings-printer',
      popover: {
        title: 'Label Printer',
        description:
          'This is where the Print button\'s behavior is configured: <strong>Browser print dialog</strong> (default — no printer set up), <strong>Networked label printer</strong> (server sends the label directly to a printer\'s LAN IP), or <strong>USB label printer</strong> (the browser sends it to a local Browser Print agent). Set <strong>Labels per row</strong> to match whatever label roll is physically loaded — the same Print button on Book Catalogue and Journals routes through whichever mode is set here.',
        side: 'top',
        align: 'start',
      },
    },
    {
      popover: {
        title: '✅ All done!',
        description: 'Click <em>Take a Tour</em> any time to replay this walkthrough.',
        side: 'over',
        align: 'center',
      },
    },
  ],
};
