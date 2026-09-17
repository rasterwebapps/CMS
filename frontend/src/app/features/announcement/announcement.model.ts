export type AnnouncementAudienceType = 'ROLE' | 'COHORT' | 'SECTION' | 'ALL';

export interface AnnouncementAudience {
  audienceType: AnnouncementAudienceType;
  audienceRefId: number | null;
  audienceLabel: string;
}

export interface Announcement {
  id: number;
  title: string;
  body: string;
  createdBy: string;
  publishedAt: string;
  audiences: AnnouncementAudience[];
  read: boolean;
}
