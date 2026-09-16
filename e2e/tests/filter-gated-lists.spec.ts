import { test, expect } from '@playwright/test';
import { loginAs, hasCreds } from '../utils/login';

/**
 * Tier-A deep spec, rollout step 3 — regression coverage for a specific bug shape
 * that has shipped twice: a list screen whose data load is gated behind a
 * mandatory filter dropdown 500s/400s the moment a real value is picked
 * (OC-242 "Attendance List always 400s, never loads records"; OC-237
 * "Exam Results screen 500s on every real result load"). The route crawler
 * only proves the screen renders at rest — it never touches the filter, so
 * it would NOT have caught either of these. This spec picks a real option
 * and watches the resulting API call.
 */

interface FilterGatedList {
  label: string;
  listRoute: string;
  filterAriaLabel: string;
}

const SCREENS: FilterGatedList[] = [
  { label: 'Attendance', listRoute: '/attendance', filterAriaLabel: 'Filter by subject' },
  { label: 'Exam Results', listRoute: '/exam-results', filterAriaLabel: 'Filter by examination' },
];

for (const screen of SCREENS) {
  test(`${screen.label} list: selecting the required filter loads data without a 4xx/5xx`, async ({ page }) => {
    test.skip(!hasCreds('admin'), 'E2E_ADMIN_USER/PASS not configured — see e2e/.env.example');
    await loginAs(page, 'admin');

    const failedResponses: string[] = [];
    page.on('response', (res) => {
      const type = res.request().resourceType();
      if ((type === 'xhr' || type === 'fetch') && res.status() >= 400) {
        failedResponses.push(`${res.status()} ${res.url()}`);
      }
    });

    await page.goto(screen.listRoute);
    const filter = page.getByLabel(screen.filterAriaLabel);
    await expect(filter).toBeVisible({ timeout: 10_000 });

    const options = await filter.locator('option[value]:not([value=""])').all();
    test.skip(options.length === 0, `no options available in "${screen.filterAriaLabel}" on this environment`);

    // Select by value, not { index: 0 } — index 0 is the disabled "Select a subject" placeholder.
    const firstValue = await options[0].getAttribute('value');
    await filter.selectOption(firstValue!);
    await page.waitForLoadState('networkidle').catch(() => {});

    expect(failedResponses, `${screen.label} list errored after selecting a real filter value`).toEqual([]);

    const errorText = page.getByText(/\b(500|Internal Server Error|Unexpected error|Something went wrong)\b/i);
    await expect(errorText).not.toBeVisible();
  });
}
