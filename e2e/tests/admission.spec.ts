import { test, expect } from '@playwright/test';
import { loginAs, hasCreds } from '../utils/login';

/**
 * Tier-A deep spec, rollout step 3 — Admission.
 *
 * Scope note: the real "Admission" flow (docs/manual-test-cases/admission-from-enquiry.md)
 * is stale — it describes a `/admissions/new` screen that no longer exists in
 * app.routes.ts. The current path, verified against real code, is:
 *   Enquiry -> fee finalization -> document submission -> document verification
 *   -> admission-completion queue (only lists DOCUMENTS_VERIFIED enquiries,
 *      admission-completion-list.component.ts) -> "Complete" -> /enquiries/:id/convert
 *   (enquiry-convert.component.ts, an 800+ line form).
 *
 * Driving an enquiry through that entire pipeline (real fee finalization, a
 * real document upload, a real verification approval) to get a
 * DOCUMENTS_VERIFIED row to convert is a large, separate piece of work that
 * deserves its own careful pass rather than a rushed, likely-fragile spec
 * bolted on here — tracked as the next open item in e2e/README.md.
 *
 * What this spec DOES cover, which the route crawler alone does not: if the
 * environment already has a DOCUMENTS_VERIFIED enquiry sitting in the queue
 * (real colleges will, in steady state), clicking "Complete" must actually
 * reach the Create Admission screen without erroring — skips cleanly if the
 * queue is empty rather than reporting a false pass or a false fail.
 */

test.describe('Admission — completion queue entry point', () => {
  test.skip(!hasCreds('admin'), 'E2E_ADMIN_USER/PASS not configured — see e2e/.env.example');

  test('"Complete" on a DOCUMENTS_VERIFIED enquiry reaches Create Admission cleanly', async ({ page }) => {
    await loginAs(page, 'admin');

    const failedResponses: string[] = [];
    page.on('response', (res) => {
      const type = res.request().resourceType();
      if ((type === 'xhr' || type === 'fetch') && res.status() >= 500) {
        failedResponses.push(`${res.status()} ${res.url()}`);
      }
    });

    await page.goto('/enquiries/admission-completion');
    await page.waitForLoadState('networkidle').catch(() => {});

    const completeBtn = page.getByRole('button', { name: 'Complete admission' }).first();
    test.skip(
      !(await completeBtn.isVisible().catch(() => false)),
      'no DOCUMENTS_VERIFIED enquiry in the queue on this environment'
    );

    await completeBtn.click();
    await expect(page.getByRole('heading', { name: /Create\s*Admission/i })).toBeVisible({ timeout: 15_000 });
    expect(failedResponses, 'opening Create Admission from the queue errored').toEqual([]);
  });
});
