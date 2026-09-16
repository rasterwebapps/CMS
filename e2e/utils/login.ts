import { Page, expect } from '@playwright/test';

export type Role = 'admin' | 'collegeAdmin' | 'cashier' | 'faculty' | 'student';

const CREDS: Record<Role, { user?: string; pass?: string }> = {
  admin: { user: process.env.E2E_ADMIN_USER, pass: process.env.E2E_ADMIN_PASS },
  collegeAdmin: { user: process.env.E2E_COLLEGE_ADMIN_USER, pass: process.env.E2E_COLLEGE_ADMIN_PASS },
  cashier: { user: process.env.E2E_CASHIER_USER, pass: process.env.E2E_CASHIER_PASS },
  faculty: { user: process.env.E2E_FACULTY_USER, pass: process.env.E2E_FACULTY_PASS },
  student: { user: process.env.E2E_STUDENT_USER, pass: process.env.E2E_STUDENT_PASS },
};

export function hasCreds(role: Role): boolean {
  return Boolean(CREDS[role].user && CREDS[role].pass);
}

/**
 * Logs into the app as `role` via the Keycloak-hosted login redirect.
 * Uses Keycloak's standard login.ftl field ids (#username/#password/#kc-login),
 * which theme customizations (see keycloak-custom-login-theme.md) leave intact.
 */
export async function loginAs(page: Page, role: Role): Promise<void> {
  const { user, pass } = CREDS[role];
  if (!user || !pass) {
    throw new Error(
      `No credentials configured for role "${role}" — set E2E_${role.toUpperCase()}_USER/PASS in e2e/.env`
    );
  }

  // Pre-empt the 9-step onboarding tour (tour.service.ts) rather than racing its
  // dim-panel overlay after the fact — it reads this exact localStorage key/value
  // (PREF_KEY = 'cms_tour_show_onboarding') on every route change to decide whether
  // to auto-start, so setting it before first navigation keeps it off for the whole
  // run, on every screen, not just the one loginAs happens to land on first.
  await page.addInitScript(() => {
    try {
      localStorage.setItem('cms_tour_show_onboarding', 'false');
    } catch {
      /* private mode / storage disabled — ignore */
    }
  });

  await page.goto('/');
  await page.waitForURL(/realms\/cms\/protocol\/openid-connect/, { timeout: 15_000 }).catch(() => {
    // already has a valid session (storageState reuse) — nothing to do
  });

  const usernameField = page.locator('#username');
  if (await usernameField.isVisible().catch(() => false)) {
    await usernameField.fill(user);
    await page.locator('#password').fill(pass);
    await page.locator('#kc-login, button[type="submit"]').first().click();
  }

  // Confirms the redirect back into Angular succeeded, not just the Keycloak form submit.
  await expect(page).not.toHaveURL(/protocol\/openid-connect/, { timeout: 15_000 });

  // Defensive fallback — the addInitScript above should already keep the onboarding
  // tour (tour-tooltip.component.html) off; if it still renders for some other
  // reason, its full-page dim overlay blocks every click, so clear it here too.
  const hardCloseTour = page.getByRole('button', { name: "Don't show again" });
  if (await hardCloseTour.isVisible({ timeout: 1_500 }).catch(() => false)) {
    await hardCloseTour.click();
  }
}
