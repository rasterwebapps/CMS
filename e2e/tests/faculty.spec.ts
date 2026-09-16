import { test, expect } from '@playwright/test';
import { loginAs, hasCreds } from '../utils/login';

/**
 * Tier-A deep spec, rollout step 4 — Faculty Management
 * (docs/manual-test-cases/faculty-management.md).
 *
 * Selectors verified against faculty-form.component.html/.ts. Both
 * Speciality and Designation use the `[ngValue]="null" disabled` placeholder
 * style — filtered on :not([disabled]), same as elsewhere in this suite.
 *
 * Searches by the employee code this spec sets explicitly rather than
 * assuming a fresh faculty lands on page 1 — student.spec.ts hit exactly
 * that assumption failing (list sorted by an unrelated auto-generated field).
 */

test.describe('Faculty — create', () => {
  test.skip(!hasCreds('admin'), 'E2E_ADMIN_USER/PASS not configured — see e2e/.env.example');

  test('a new faculty member with required fields saves and appears in the list', async ({ page }) => {
    await loginAs(page, 'admin');

    const stamp = Date.now();
    const employeeCode = `E2E${stamp}`;
    const firstName = 'E2E';
    const lastName = `Faculty${stamp}`;
    const fullName = `${firstName} ${lastName}`;

    await page.goto('/faculty/new');

    await page.locator('#fac-code').fill(employeeCode);
    await page.locator('#fac-first').fill(firstName);
    await page.locator('#fac-last').fill(lastName);
    await page.locator('#fac-email').fill(`e2e.faculty.${stamp}@e2e.local`);

    const specialitySelect = page.locator('#fac-dept');
    const firstSpeciality = await specialitySelect.locator('option:not([disabled])').first().getAttribute('value');
    await specialitySelect.selectOption(firstSpeciality!);

    const designationSelect = page.locator('#fac-designation');
    const firstDesignation = await designationSelect.locator('option:not([disabled])').first().getAttribute('value');
    await designationSelect.selectOption(firstDesignation!);

    await page.locator('#fac-joining').fill('2026-06-01');

    await page.locator('button[type="submit"]').click();
    await expect(page).toHaveURL(/\/faculty$/, { timeout: 15_000 });

    await page.getByLabel('Search faculty').fill(employeeCode);
    await expect(page.locator('.cell-name', { hasText: fullName })).toBeVisible({ timeout: 10_000 });
  });
});
