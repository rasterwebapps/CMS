import { describe, it, expect } from 'vitest';
import {
  getCounsellingStateWarning,
  isCounsellingCategory,
  isTamilNaduState,
} from './counselling-state.util';

describe('counselling-state.util', () => {
  describe('isTamilNaduState', () => {
    it('matches Tamil Nadu regardless of case or extra whitespace', () => {
      expect(isTamilNaduState('Tamil Nadu')).toBe(true);
      expect(isTamilNaduState('  tamil   nadu ')).toBe(true);
      expect(isTamilNaduState('TAMILNADU')).toBe(true);
    });

    it('returns false for non-Tamil Nadu or blank states', () => {
      expect(isTamilNaduState('Kerala')).toBe(false);
      expect(isTamilNaduState('Karnataka')).toBe(false);
      expect(isTamilNaduState('Uttar Pradesh')).toBe(false);
      expect(isTamilNaduState('')).toBe(false);
      expect(isTamilNaduState('   ')).toBe(false);
      expect(isTamilNaduState(null)).toBe(false);
      expect(isTamilNaduState(undefined)).toBe(false);
    });
  });

  describe('isCounsellingCategory', () => {
    it('identifies COUNSELLING and MANAGEMENT_TO_COUNSELLING', () => {
      expect(isCounsellingCategory('COUNSELLING')).toBe(true);
      expect(isCounsellingCategory('MANAGEMENT_TO_COUNSELLING')).toBe(true);
      expect(isCounsellingCategory('MANAGEMENT')).toBe(false);
      expect(isCounsellingCategory('LAPSED')).toBe(false);
      expect(isCounsellingCategory('')).toBe(false);
      expect(isCounsellingCategory(null)).toBe(false);
    });
  });

  describe('getCounsellingStateWarning', () => {
    it('returns null when state is Tamil Nadu across all contexts', () => {
      expect(getCounsellingStateWarning('Counselling Quota', 'Tamil Nadu', 'enquiry')).toBeNull();
      expect(getCounsellingStateWarning('Counselling Quota', 'Tamil Nadu', 'convert')).toBeNull();
      expect(
        getCounsellingStateWarning('Management to Counselling', 'Tamil Nadu', 'student'),
      ).toBeNull();
    });

    it('returns descriptive warning when state is outside Tamil Nadu', () => {
      const enqMsg = getCounsellingStateWarning('Counselling Quota', 'Kerala', 'enquiry');
      expect(enqMsg).toContain('Kerala');
      expect(enqMsg).toContain('outside Tamil Nadu');
      expect(enqMsg).toContain('Tamil Nadu counselling fee slab');

      const convMsg = getCounsellingStateWarning('Counselling Quota', 'Kerala', 'convert');
      expect(convMsg).toContain('Kerala');
      expect(convMsg).toContain('creating this admission');

      const stuMsg = getCounsellingStateWarning(
        'Management to Counselling',
        'Kerala',
        'student',
      );
      expect(stuMsg).toContain('Kerala');
      expect(stuMsg).toContain('Management to Counselling');
    });

    it('returns missing-state warning when state is blank or null', () => {
      expect(getCounsellingStateWarning('Counselling Quota', '', 'enquiry')).toContain(
        'not selected',
      );
      expect(getCounsellingStateWarning('Counselling Quota', null, 'convert')).toContain(
        'not selected',
      );
      expect(
        getCounsellingStateWarning('Management to Counselling', '   ', 'student'),
      ).toContain('not specified');
    });
  });
});
