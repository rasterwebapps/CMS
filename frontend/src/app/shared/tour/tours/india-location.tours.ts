import { TourDefinition } from '../tour.service';

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
          'Type a state name, country, or district to instantly filter across all tabs.',
        side: 'bottom',
        align: 'start',
      },
    },
    {
      element: '#tour-location-tabs',
      popover: {
        title: 'States / Districts / Countries Tabs',
        description:
          'Switch between <strong>States</strong>, <strong>Districts</strong>, and <strong>Countries</strong> tabs to manage each level of the location hierarchy independently.',
        side: 'bottom',
        align: 'start',
      },
    },
    {
      element: '#tour-location-table',
      popover: {
        title: 'Location Table',
        description:
          'Each row shows the entry\'s name, code, and parent (where applicable). Admins can use the action icons to <strong>Edit</strong> or <strong>Delete</strong> an entry.',
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

