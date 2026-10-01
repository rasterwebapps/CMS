import { formatIstTime } from './print-receipt.utils';

describe('formatIstTime', () => {
  it('converts a UTC instant to IST clock-time (UTC+5:30)', () => {
    expect(formatIstTime('2026-07-18T10:15:00Z')).toBe('03:45 PM');
  });

  it('rolls over correctly when the IST offset crosses into the next hour', () => {
    expect(formatIstTime('2026-07-18T00:00:00Z')).toBe('05:30 AM');
  });

  it('returns an empty string for null, so callers can omit the time cleanly', () => {
    expect(formatIstTime(null)).toBe('');
  });

  it('returns an empty string for undefined', () => {
    expect(formatIstTime(undefined)).toBe('');
  });

  it('returns an empty string for an empty string input', () => {
    expect(formatIstTime('')).toBe('');
  });
});
