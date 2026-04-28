import { TourStep } from '../tour-step.model';

/**
 * Seven-step onboarding tour that introduces new users to CMS.
 *
 * The tour is anchored to persistent app-shell elements so it runs
 * correctly on any route. The alignment-demo steps teach users about
 * right-aligned currency, centred status badges, and left-aligned names
 * using an inline preview inside the tooltip itself — no specific feature
 * screen is required.
 *
 * Feature teams can define their own tour arrays and register them with
 * `TourService.registerTour('my-feature', MY_FEATURE_STEPS)` following
 * the same pattern.
 */
export const ONBOARDING_TOUR_STEPS: TourStep[] = [
  {
    id: 'onboarding-welcome',
    title: 'Welcome to CMS',
    body: 'This short tour will walk you through the key areas of the College Management System. Click "Next" whenever you\'re ready to continue.',
    targetSelector: '.app-toolbar',
    placement: 'bottom',
  },
  {
    id: 'onboarding-navigation',
    title: 'Navigation Sidebar',
    body: 'All modules — Preferences, Admissions, Finance, and more — are organised here. Expand a section to navigate to any screen. You can also search by name using the "Find menu…" box.',
    targetSelector: '.sidenav-nav',
    placement: 'right',
  },
  {
    id: 'onboarding-search',
    title: 'Try the Menu Search',
    body: 'Type any module name in the search box below. The sidebar filters in real time. Try typing a letter now to continue.',
    targetSelector: '.sidenav-search-input',
    placement: 'right',
    advanceOn: { event: 'input' },
    validatorFn: (e) => ((e.target as HTMLInputElement).value.trim().length > 0),
  },
  {
    id: 'onboarding-alignment-currency',
    title: 'Data Alignment — Currency',
    body: 'In every CMS data table, numeric and currency values are right-aligned for financial scannability. Notice how amounts like fee totals appear on the right side of their column.',
    alignmentHint: 'right',
  },
  {
    id: 'onboarding-alignment-status',
    title: 'Data Alignment — Status Badges',
    body: 'Status badges (Active, Pending, Paid, etc.) are always centred in their column for visual balance. Colour-coded badges let you scan a table at a glance.',
    alignmentHint: 'center',
  },
  {
    id: 'onboarding-alignment-names',
    title: 'Data Alignment — Names & Text',
    body: 'Names, codes, and free-text fields are left-aligned for natural readability. This three-rule system — right, centre, left — is consistent across every table in CMS.',
    alignmentHint: 'left',
  },
  {
    id: 'onboarding-complete',
    title: "You're All Set!",
    body: 'Explore the Preferences section to set up departments, programs, and fee structures. You can replay this tour at any time by clicking the Help icon in the sidebar footer.',
    targetSelector: '.sidenav-footer',
    placement: 'right',
  },
];
