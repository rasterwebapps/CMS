import { test, expect } from '@playwright/test';
import { loginAs, hasCreds } from '../utils/login';

/**
 * Tier-A deep spec — dataset-correctness regression test for Fee Explorer
 * (`/student-fees`, fee-explorer.component.ts).
 *
 * Targets the exact bug just fixed there: the table is server-side paginated
 * (real totalElements from the backend), but the Program/Academic Year/Year
 * of Study filter dropdowns and the "N students" footer count were both
 * derived from `dataSource.data` -- the currently loaded PAGE, not the full
 * dataset. Symptom: a program/year that only appears on page 2+ never showed
 * up as a filter option at all, and the header/footer counts were really
 * just the page size mislabeled as a total.
 *
 * route-crawler.spec.ts would NOT catch this class of bug -- the screen
 * renders fine, returns 200s, it just silently shows wrong/incomplete data.
 * This spec doesn't assume specific seed data: it grabs a real Program value
 * from the LAST page of results (deliberately likely to be page-1-absent)
 * and checks it survives the round trip. Not a mathematical guarantee against
 * every possible regression (a program that happens to appear on every page
 * wouldn't distinguish old vs new behavior), but it directly exercises the
 * real mechanism of the bug rather than just re-testing that the fix exists.
 */

test.describe('Fee Explorer — dataset correctness (not just page-scoped)', () => {
  test.skip(!hasCreds('admin'), 'E2E_ADMIN_USER/PASS not configured — see e2e/.env.example');

  test('Program filter spans the whole dataset, not just the loaded page', async ({ page }) => {
    await loginAs(page, 'admin');
    await page.goto('/student-fees');
    await page.waitForLoadState('networkidle').catch(() => {});

    const rowCount = page.locator('.row-count');
    await expect(rowCount).toBeVisible({ timeout: 10_000 });
    const total = parseInt((await rowCount.innerText()).trim(), 10);

    test.skip(
      !(total > 25),
      `only ${total} student fee rows on this environment — not enough to span multiple pages ` +
        `(default page size 25), so this regression shape can't be exercised right now`
    );

    // Jump to the last page — grab a Program value unlikely to be on page 1.
    await page.getByRole('button', { name: 'Last page' }).click();
    await page.waitForLoadState('networkidle').catch(() => {});
    const lastPageProgram = (await page.locator('td.mat-column-programName .cell-text').first().innerText()).trim();
    expect(lastPageProgram, 'expected a real program name on the last page, not the "—" empty placeholder').not.toBe('—');
    expect(lastPageProgram).not.toBe('');

    // Back to page 1 — a real user would land here before ever touching the filter.
    await page.getByRole('button', { name: 'First page' }).click();
    await page.waitForLoadState('networkidle').catch(() => {});

    const programFilter = page.getByLabel('Filter by program');
    const optionTexts = (await programFilter.locator('option').allInnerTexts()).map((t) => t.trim());
    expect(
      optionTexts,
      `Program filter is missing "${lastPageProgram}" — a real value from a later page. That means options ` +
        `are being derived from only the loaded page, not the full dataset (the exact bug this spec targets).`
    ).toContain(lastPageProgram);

    // Selecting it must return real results, not a silent empty table.
    // `networkidle` is unreliable here -- this app polls in the background (notifications),
    // so the load-state can resolve before the filtered /explorer response actually replaces
    // the table, leaving stale page-1 rows in the DOM when we read them below. Wait for the
    // real filtered response instead.
    const filteredResponse = page.waitForResponse(
      (res) => res.url().includes('/student-fees/explorer') && res.url().includes('program=') && res.status() === 200
    );
    await programFilter.selectOption(lastPageProgram);
    await filteredResponse;

    await expect(rowCount).toBeVisible({ timeout: 10_000 });
    const filteredTotal = parseInt((await rowCount.innerText()).trim(), 10);
    expect(filteredTotal, `filtering by "${lastPageProgram}" returned 0 results despite it existing in the data`).toBeGreaterThan(0);

    // Correctness, not just non-emptiness: every row actually matches the filter.
    const visiblePrograms = await page.locator('td.mat-column-programName .cell-text').allInnerTexts();
    for (const p of visiblePrograms) {
      expect(p.trim(), `a row shown under the "${lastPageProgram}" filter has a different program`).toBe(lastPageProgram);
    }
  });

  test('Academic Year and Year of Study filters apply without error', async ({ page }) => {
    await loginAs(page, 'admin');
    await page.goto('/student-fees');
    await page.waitForLoadState('networkidle').catch(() => {});

    const failedResponses: string[] = [];
    page.on('response', (res) => {
      const type = res.request().resourceType();
      if ((type === 'xhr' || type === 'fetch') && res.status() >= 400) {
        failedResponses.push(`${res.status()} ${res.url()}`);
      }
    });

    for (const label of ['Filter by academic year', 'Filter by year of study']) {
      const filter = page.getByLabel(label);
      const options = await filter.locator('option[value]:not([value="ALL"])').all();
      if (options.length === 0) continue;
      const value = await options[0].getAttribute('value');
      await filter.selectOption(value!);
      await page.waitForLoadState('networkidle').catch(() => {});
    }

    expect(failedResponses, 'applying Academic Year / Year of Study filters errored').toEqual([]);
  });
});
