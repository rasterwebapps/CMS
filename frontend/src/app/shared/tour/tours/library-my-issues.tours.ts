import { TourDefinition, TourFlowMap } from '../tour.service';

// ─────────────────────────────────────────────────────────────────────────────
// My Library (self-service portal)
// ─────────────────────────────────────────────────────────────────────────────
export const LIBRARY_MY_ISSUES_TOUR: TourDefinition = {
  steps: [
    {
      popover: {
        title: '🎒 My Library',
        description: 'Your own borrowed books, borrowing history, and a way to search the whole catalogue.',
        side: 'over',
        align: 'center',
      },
    },
    {
      element: '#tour-mylib-tabs',
      popover: {
        title: 'Three Tabs',
        description: 'Currently Borrowed shows what you have out now (overdue ones flagged); Borrow History is your full past record; Search Catalogue lets you browse everything available.',
        side: 'bottom',
        align: 'start',
      },
    },
    {
      popover: {
        title: '✅ Overdue Alert',
        description: 'If anything is overdue, a banner at the top shows the count and the fine building up — return it to stop the fine from growing.',
        side: 'over',
        align: 'center',
      },
    },
  ],
};

export const LIBRARY_MY_ISSUES_FLOW_MAP: TourFlowMap = {
  funnel: [{ label: 'My Library', description: 'View your borrowed books, history, and search the catalogue.' }],
  currentIndex: 0,
  steps: [
    { label: 'Currently Borrowed', icon: 'checklist', detail: 'What you have out now — overdue items are flagged with their fine.' },
    { label: 'Borrow History', icon: 'open', detail: 'Your full past borrow/return record.' },
    { label: 'Search Catalogue', icon: 'search', detail: 'Browse everything currently available by subject, rack, or shelf.' },
  ],
};
