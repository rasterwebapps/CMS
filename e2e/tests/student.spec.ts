import { test, expect } from '@playwright/test';
import { loginAs, hasCreds } from '../utils/login';

/**
 * Tier-A deep spec, rollout step 4 — Student Management
 * (docs/manual-test-cases/student-management.md, TC-STU-004/005).
 *
 * Students are core to nearly everything else in the app (fees, attendance,
 * exams, admissions all key off a real student record), so a real create
 * round-trip here is high-value coverage. Selectors verified against
 * student-form.component.html/.ts, not guessed — including the two
 * different "empty option" styles the same form mixes: Program's
 * `[ngValue]="null" disabled` vs Admission Category/Gender's plain
 * `value=""` (no disabled attribute), which need different Playwright
 * filters to skip correctly (masters.spec.ts and enquiry.spec.ts both
 * hit variants of this same Angular quirk already).
 */

test.describe('Student — create (TC-STU-005)', () => {
  test.skip(!hasCreds('admin'), 'E2E_ADMIN_USER/PASS not configured — see e2e/.env.example');

  test('a new student with required fields saves and appears in the list', async ({ page }) => {
    await loginAs(page, 'admin');

    const stamp = Date.now();
    const firstName = 'E2E';
    const lastName = `Student${stamp}`;
    const fullName = `${firstName} ${lastName}`;

    await page.goto('/students/new');

    await page.locator('#stu-roll').fill(`E2E${stamp}`);
    await page.locator('#stu-admission').fill('2026-06-01');
    await page.locator('#stu-first').fill(firstName);
    await page.locator('#stu-last').fill(lastName);
    await page.locator('#stu-email').fill(`e2e.student.${stamp}@e2e.local`);

    // Program: [ngValue]="null" disabled placeholder.
    const programSelect = page.locator('#stu-program');
    const firstProgram = await programSelect.locator('option:not([disabled])').first().getAttribute('value');
    await programSelect.selectOption(firstProgram!);

    // Admission Category / Gender: plain value="" placeholder, not disabled.
    const admissionCategorySelect = page.locator('#stu-admission-category');
    const firstCategory = await admissionCategorySelect.locator('option:not([value=""])').first().getAttribute('value');
    await admissionCategorySelect.selectOption(firstCategory!);

    const genderSelect = page.locator('#stu-gender');
    const firstGender = await genderSelect.locator('option:not([value=""])').first().getAttribute('value');
    await genderSelect.selectOption(firstGender!);

    // Status (default 'ACTIVE') and Year of Study (default 1) already carry valid defaults.

    const rollNumber = `E2E${stamp}`;
    await page.locator('button[type="submit"]').click();
    await expect(page).toHaveURL(/\/students$/, { timeout: 15_000 });

    // The list sorts by admission number ascending (auto-generated, unrelated to creation
    // order) with 25/page — a freshly created student isn't reliably on page 1. Search by the
    // roll number this spec set explicitly, the way a real user would confirm their save worked.
    await page.getByLabel('Search students').fill(rollNumber);
    await expect(page.locator('.cell-name', { hasText: fullName })).toBeVisible({ timeout: 10_000 });
  });
});
