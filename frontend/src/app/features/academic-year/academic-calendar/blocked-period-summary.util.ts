import { AppDayOfWeek, BlockedPeriod } from '../academic-year.model';

/** Shared by the Blocked Periods list tab and the day-detail flyout's "already blocked"
 *  sub-list -- both need to render the same one-line summary for a block. */
export function formatBlockSummary(block: BlockedPeriod, dayOfWeekLabels: Record<AppDayOfWeek, string>): string {
  if (block.blockType === 'ONE_OFF') {
    return `${block.specificDate} · ${block.periodName}`;
  }
  return `Every ${dayOfWeekLabels[block.dayOfWeek!]} · ${block.periodName} · ${block.rangeStartDate} to ${block.rangeEndDate}`;
}
