import { TourStep } from '../tour-step.model';

export const ONBOARDING_TOUR_STEPS: TourStep[] = [
  {
    id: 'onboarding-welcome',
    title: 'Welcome to CMS!',
    body: 'This quick tour covers the key areas of the College Management System. Use the arrow keys or the buttons below to navigate. It only takes a minute.',
  },
  {
    id: 'onboarding-toolbar',
    title: 'App Toolbar',
    body: 'The toolbar shows the current section and gives you quick access to global search, notifications, theme switcher, and your account menu.',
    targetSelector: '.app-toolbar',
    placement: 'bottom',
  },
  {
    id: 'onboarding-navigation',
    title: 'Navigation Sidebar',
    body: 'All modules — Preferences, Admissions, Finance, Curriculum, and more — live here. Expand any group to go to a screen. Click the pin icon in the toolbar to collapse the sidebar to an icon rail.',
    targetSelector: '.sidenav-nav',
    placement: 'right',
  },
  {
    id: 'onboarding-search',
    title: 'Menu Search',
    body: 'Can\'t find a module? Type its name here and the sidebar filters in real time. Try it any time to quickly jump to any screen.',
    targetSelector: '.sidenav-search-input',
    placement: 'right',
  },
  {
    id: 'onboarding-alignment-currency',
    title: 'Table Conventions — Currency',
    body: 'In every CMS data table, numeric and currency values are right-aligned for easy financial scanning. Fee totals and payment amounts always appear on the right.',
    alignmentHint: 'right',
  },
  {
    id: 'onboarding-alignment-status',
    title: 'Table Conventions — Status',
    body: 'Status badges (Active, Pending, Paid, etc.) are always centred. Colour-coded badges let you scan a table at a glance without reading every cell.',
    alignmentHint: 'center',
  },
  {
    id: 'onboarding-alignment-names',
    title: 'Table Conventions — Names & Text',
    body: 'Names, codes, and text fields are left-aligned for natural readability. This three-rule system — right, centre, left — is consistent across every table in CMS.',
    alignmentHint: 'left',
  },
  {
    id: 'onboarding-footer',
    title: 'Sidebar Footer',
    body: 'The footer holds your keyboard shortcuts guide (?) and the Help Tour button so you can replay this tour at any time. The OneCMS logo links to the Raster support site.',
    targetSelector: '.sidenav-footer',
    placement: 'right',
  },
  {
    id: 'onboarding-complete',
    title: "You're All Set!",
    body: 'Start by opening Preferences to configure departments, programs, and fee structures. Explore the sidebar to discover every module. Welcome aboard!',
  },
];
