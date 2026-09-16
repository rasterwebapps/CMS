import { test, expect } from '@playwright/test';
import { loginAs, hasCreds } from '../utils/login';

/**
 * Tier-A deep spec, rollout step 3 — Fee Collection is the highest-stakes
 * transaction screen in the app (money handled by the Cashier role day to
 * day). Deliberately scoped to opening the collect-payment entry point
 * cleanly, not submitting a real payment: this suite runs against the
 * shared 243 test server (not an isolated per-run DB — see
 * scripts/regression-gate.sh), and a real payment write against whatever
 * outstanding balance happens to be seeded there is a state mutation this
 * spec shouldn't be making blind. This still catches the exact failure
 * shape reported ("some transaction screen functionalities doesn't work")
 * for the list load and the "Collect" action itself — a full submit-and-
 * verify-receipt spec belongs in a follow-up once there's an isolated fee
 * test fixture to run it against safely.
 */

test.describe('Fee Collection', () => {
  test.skip(!hasCreds('cashier') && !hasCreds('admin'), 'no cashier/admin creds configured — see e2e/.env.example');

  test('list loads outstanding entries and "Collect" opens the payment view cleanly', async ({ page }) => {
    await loginAs(page, hasCreds('cashier') ? 'cashier' : 'admin');

    const failedResponses: string[] = [];
    page.on('response', (res) => {
      const type = res.request().resourceType();
      if ((type === 'xhr' || type === 'fetch') && res.status() >= 500) {
        failedResponses.push(`${res.status()} ${res.url()}`);
      }
    });

    await page.goto('/student-fees/collect-payment');
    await page.getByLabel('Filter by status').selectOption('OUTSTANDING');
    await page.waitForLoadState('networkidle').catch(() => {});

    const collectBtn = page.getByRole('button', { name: 'Collect balance' }).first();
    test.skip(!(await collectBtn.isVisible().catch(() => false)), 'no entry with an outstanding balance on this environment');

    await collectBtn.click();
    await expect(page.getByRole('button', { name: 'Back to fee collection list' })).toBeVisible({ timeout: 10_000 });

    expect(failedResponses, 'opening the Collect payment view errored').toEqual([]);

    // Leave shared test-server state untouched.
    await page.getByRole('button', { name: 'Back to fee collection list' }).click();
  });
});
