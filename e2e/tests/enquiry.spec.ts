import { test, expect } from '@playwright/test';
import { loginAs, hasCreds } from '../utils/login';

/**
 * Tier-A deep spec, rollout step 3 — Enquiry is the front door of the whole
 * admissions pipeline (44KB of manual test cases, the largest single doc in
 * docs/manual-test-cases/) and its form (enquiry-form.component.ts) gates
 * submission on a real fee-structure lookup: onSubmit() silently no-ops if
 * `feeNotFound()` or `totalFees() <= 0` for the chosen program/course/state/
 * studentType/quota combination — read directly from the component, not
 * guessed. That gate is itself a plausible source of "creation doesn't work"
 * if a given environment's fee structures aren't configured for a
 * combination — this spec tries a few programs and reports clearly if none
 * of them have a usable fee structure, which is a real, actionable finding
 * either way (not a false pass).
 *
 * Country/state/district (cms-country-state-district-selector) auto-default
 * to India/Tamil Nadu/Salem — verified in the selector component, no
 * interaction needed. Quota and student type already carry form defaults.
 */

test.describe('Enquiry — create (front door of admissions)', () => {
  test.skip(!hasCreds('admin'), 'E2E_ADMIN_USER/PASS not configured — see e2e/.env.example');

  test('a new enquiry with a real fee structure saves and appears in the list', async ({ page }) => {
    await loginAs(page, 'admin');

    const stamp = Date.now();
    const name = `E2E Enquiry ${stamp}`;

    await page.goto('/enquiries/new');
    await page.locator('#enq-name').fill(name);
    await page.locator('#enq-phone').fill('9876500000');
    await page.locator('#enq-dob').fill('2005-01-01');
    await page.locator('#enq-gender').selectOption({ index: 1 });

    // Referral: pick the first option that isn't Agent/Staff (those pull in an extra
    // required person-search field this spec isn't targeting). Filter on [disabled], not
    // [value=""] — Angular's [ngValue]="null" placeholder doesn't render value="".
    const referralSelect = page.locator('#enq-referral');
    const referralOptions = await referralSelect.locator('option:not([disabled])').all();
    for (const opt of referralOptions) {
      const label = (await opt.innerText()).trim();
      if (label && !/agent|staff/i.test(label)) {
        await referralSelect.selectOption({ label });
        break;
      }
    }

    // Try programs in order until one resolves a real fee structure — see file header.
    const programSelect = page.locator('#enq-program');
    const programOptions = await programSelect.locator('option:not([disabled])').all();
    const programLabels = (await Promise.all(programOptions.map((o) => o.innerText()))).map((s) => s.trim());

    let feeResolved = false;
    for (const label of programLabels.slice(0, 3)) {
      await programSelect.selectOption({ label });

      // The select itself is [disabled]="courses().length === 0" (enquiry-form.component.html),
      // so "not disabled" already guarantees a real course exists at index 1 — no need to
      // separately count options (course's placeholder isn't marked [disabled] itself, unlike
      // program/referral, so it can't be filtered out the same way).
      const courseSelect = page.locator('#enq-course');
      if (!(await courseSelect.isDisabled())) {
        await courseSelect.selectOption({ index: 1 });
      }

      const feeVal = page.locator('.fee-banner-val');
      const feeNotFound = page.locator('.fee-banner--not-found');
      await Promise.race([
        feeVal.waitFor({ state: 'visible', timeout: 8_000 }).catch(() => {}),
        feeNotFound.waitFor({ state: 'visible', timeout: 8_000 }).catch(() => {}),
      ]);

      if (await feeVal.isVisible().catch(() => false)) {
        feeResolved = true;
        break;
      }
    }

    expect(
      feeResolved,
      `none of the first ${Math.min(3, programLabels.length)} programs (${programLabels.slice(0, 3).join(', ')}) ` +
        `resolved a fee structure — either configure one, or this is a real finding: enquiry creation is ` +
        `blocked for these combinations on this environment`
    ).toBe(true);

    await page.locator('.btn-submit', { hasText: 'Save Enquiry' }).click();

    await expect(page).toHaveURL(/\/enquiries$/, { timeout: 15_000 });
    await expect(page.getByText(name)).toBeVisible({ timeout: 10_000 });
  });
});
