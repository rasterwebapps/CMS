import { test, expect } from '@playwright/test';
import { loginAs, hasCreds } from '../utils/login';

/**
 * Tier-A deep spec — automates the highest-priority slice of
 * docs/manual-test-cases/role-and-user-management.md (TC-RBAC-004/005/010).
 *
 * This exists specifically because "unable to create users, roles" is the
 * literal reported production failure. A green run here is the direct signal
 * that the create-user flow — form, role dropdown, hierarchy filter, save —
 * actually works end to end against a real deployed build, not just that the
 * screen renders.
 */

const FORBIDDEN_ROLE_RE = /DEV.?ADMIN|SUPPORT.?ADMIN|^ADMIN$/i;

test.describe('User Management — create user (TC-RBAC-004/005/010)', () => {
  test.skip(!hasCreds('admin'), 'E2E_ADMIN_USER/PASS not configured — see e2e/.env.example');

  test('User Management is reachable and Add User opens the create panel', async ({ page }) => {
    await loginAs(page, 'admin');
    await page.goto('/user-management');
    await expect(page.getByRole('heading', { name: /User\s*Management/i })).toBeVisible();

    await page.getByRole('button', { name: 'Add User' }).click();
    await expect(page.getByRole('heading', { name: 'Add User' })).toBeVisible();
  });

  test('role dropdown excludes DEV_ADMIN/SUPPORT_ADMIN/ADMIN for an ADMIN actor (TC-RBAC-005)', async ({ page }) => {
    await loginAs(page, 'admin');
    await page.goto('/user-management');
    await page.getByRole('button', { name: 'Add User' }).click();

    const roleSelect = page.locator('.panel-form select.field-select');
    const optionLabels = await roleSelect.locator('option').allInnerTexts();

    expect(optionLabels.length, 'role dropdown should offer at least one selectable role').toBeGreaterThan(1);
    for (const label of optionLabels) {
      expect(label, `"${label}" must not be assignable by an ADMIN actor`).not.toMatch(FORBIDDEN_ROLE_RE);
    }
  });

  test('create a user end to end and confirm it appears in the list (TC-RBAC-010)', async ({ page }) => {
    await loginAs(page, 'admin');
    await page.goto('/user-management');
    await page.getByRole('button', { name: 'Add User' }).click();

    const stamp = Date.now();
    const fullName = `E2E Regression ${stamp}`;
    const username = `e2e.regression.${stamp}`;

    await page.getByPlaceholder('e.g. Priya Sharma').fill(fullName);
    await page.getByPlaceholder('user@institution.edu').fill(`${username}@e2e.local`);
    await page.getByPlaceholder('e.g. priya.sharma').fill(username);
    await page.getByPlaceholder('Min. 8 characters').fill(`Regress1on${stamp}`);

    // Pick whichever non-forbidden role is first offered — this spec is about the save
    // path working, not a specific role's permission set.
    const roleSelect = page.locator('.panel-form select.field-select');
    const options = await roleSelect.locator('option').all();
    for (const opt of options) {
      const value = await opt.getAttribute('value');
      const label = (await opt.innerText()).trim();
      if (value && !FORBIDDEN_ROLE_RE.test(label)) {
        await roleSelect.selectOption(value);
        break;
      }
    }

    await page.getByRole('button', { name: 'Create User' }).click();

    // The create-user API call must not silently fail — this is the exact symptom reported live.
    await expect(page.getByText(fullName)).toBeVisible({ timeout: 10_000 });
  });
});
