import { test, expect } from '@playwright/test';
import { loginAs, hasCreds } from '../utils/login';

/**
 * Tier-A deep spec, rollout step 2b — automates
 * docs/manual-test-cases/master-lifecycle-status-management.md (TC-MASTER-LIFE-001/002
 * pattern: activate/deactivate a master row from the list screen, PATCH .../status,
 * confirm dialog, status badge + toast update).
 *
 * Uses Blood Group as the vehicle (selectors verified against
 * blood-group-list.component.html/.ts) — the confirm dialog is the shared
 * ConfirmDialogComponent every master list uses, so this shape generalizes.
 * Creates its own throwaway row rather than depending on masters.spec.ts's
 * run order or on seed data being present.
 */

test.describe('Blood Group master — activate/deactivate lifecycle (TC-MASTER-LIFE-001)', () => {
  test.skip(!hasCreds('admin'), 'E2E_ADMIN_USER/PASS not configured — see e2e/.env.example');

  test('deactivating then reactivating a blood group updates status and shows success feedback', async ({ page }) => {
    await loginAs(page, 'admin');

    const stamp = Date.now();
    const name = `E2E Lifecycle ${stamp}`;
    const code = `E2EL${stamp}`;

    await page.goto('/blood-groups/new');
    await page.locator('#bg-name').fill(name);
    await page.locator('#bg-code').fill(code);
    await page.locator('.btn-submit').click();
    await expect(page).toHaveURL(/\/blood-groups$/, { timeout: 10_000 });

    const card = page.locator('.mlp-card', { hasText: name });
    await expect(card).toBeVisible({ timeout: 10_000 });
    await expect(card.getByText('Active', { exact: true })).toBeVisible();

    // Deactivate
    await card.getByRole('button', { name: 'Deactivate blood group' }).click();
    await page.getByRole('dialog').getByRole('button', { name: 'Deactivate', exact: true }).click();
    await expect(page.getByText(/Blood group deactivated successfully/i)).toBeVisible({ timeout: 10_000 });
    await expect(card.getByText('Inactive', { exact: true })).toBeVisible({ timeout: 10_000 });

    // Reactivate
    await card.getByRole('button', { name: 'Activate blood group' }).click();
    await page.getByRole('dialog').getByRole('button', { name: 'Activate', exact: true }).click();
    await expect(page.getByText(/Blood group activated successfully/i)).toBeVisible({ timeout: 10_000 });
    await expect(card.getByText('Active', { exact: true })).toBeVisible({ timeout: 10_000 });
  });
});
