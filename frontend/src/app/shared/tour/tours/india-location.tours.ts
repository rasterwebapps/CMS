import { TourDefinition, TourFlowMap } from '../tour.service';

export const INDIA_LOCATION_LIST_TOUR: TourDefinition = {
  steps: [
    {
      popover: {
        title: '👋 Welcome to Location Master',
        description:
          'This screen lets you manage countries, states / UTs, and their districts — used in student and faculty address forms throughout the application.',
        side: 'over',
        align: 'center',
      },
    },
    {
      element: '#tour-location-header',
      popover: {
        title: 'Page Header',
        description:
          'Admins see <strong>Add State / UT</strong> and <strong>Add Country</strong> buttons here to register new geographical entries.',
        side: 'bottom',
        align: 'start',
      },
    },
    {
      element: '#tour-location-search',
      popover: {
        title: 'Search Locations',
        description:
          'Type a state name, country, or code to instantly filter the list.',
        side: 'bottom',
        align: 'start',
      },
    },
    {
      element: '#tour-location-tabs',
      popover: {
        title: 'Card / Table View Toggle',
        description:
          'Switch between <strong>Card view</strong> (country to state to district hierarchy) and <strong>Table view</strong> (flat state list). Your preference is saved.',
        side: 'bottom',
        align: 'start',
      },
    },
    {
      element: '#tour-location-table',
      popover: {
        title: 'Location Table',
        description:
          'This content area shows the managed location hierarchy. Admins can add, edit, or delete countries, states, and districts from card or table view.',
        side: 'top',
        align: 'start',
      },
    },
    {
      popover: {
        title: '✅ All done!',
        description:
          'You now know how to navigate the Location Master screen. Click <em>Take a Tour</em> any time to replay this walkthrough.',
        side: 'over',
        align: 'center',
      },
    },
  ],
};

export const INDIA_LOCATION_LIST_FLOW_MAP: TourFlowMap = {
  funnel: [
    { label: 'Location Master', description: 'Master list of Indian states, districts, and cities used for address fields across the system.' },
  ],
  currentIndex: 0,
  steps: [
    { label: 'Search Locations', icon: 'search', detail: 'Search states, districts, or cities by name.' },
    { label: 'Card / Table View', icon: 'open', detail: 'Switch between a card layout and a sortable table.' },
    { label: 'Location Table', icon: 'checklist', detail: 'Browse the full hierarchy of states, districts, and cities.' },
    { label: 'Add / Edit', icon: 'send', detail: 'Add or edit location entries used across student, faculty, and institution address fields.' },
  ],
};

