import { test, expect } from '@playwright/test';
import { loginAs, hasCreds } from '../utils/login';

/**
 * Tier-A deep spec, rollout step 2 (see e2e/README.md): every CLAUDE.md-mandated
 * master screen shares one pattern — `entry-form-*` create form, `<id>-name`/`<id>-code`
 * fields, `.field-error` rendered next to the field, `uniqueFieldValidator` doing a
 * real-time async `/name-exists` check, and an `mlp-page` list with an `Add <Label>`
 * button. One parameterized spec covers all of them instead of N near-identical files —
 * verified against real component markup (blood-group/speciality/community/referral-type
 * form + list .html), not guessed selectors.
 *
 * Directly targets the reported "sometimes other masters screen updates or creations
 * doesn't work" failure: this is the create round-trip actually happening against a
 * real deployed build, not just the screen rendering (route-crawler.spec.ts covers that).
 */

interface MasterConfig {
  label: string;
  listRoute: string;
  addButtonName: string;
  nameFieldId: string;
  codeFieldId: string;
}

const MASTERS: MasterConfig[] = [
  { label: 'Blood Group', listRoute: '/blood-groups', addButtonName: 'Add Blood Group', nameFieldId: 'bg-name', codeFieldId: 'bg-code' },
  { label: 'Speciality', listRoute: '/specialities', addButtonName: 'Add Speciality', nameFieldId: 'sp-name', codeFieldId: 'sp-code' },
  { label: 'Community', listRoute: '/communities', addButtonName: 'Add Community', nameFieldId: 'community-name', codeFieldId: 'community-code' },
  { label: 'Referral Type', listRoute: '/referral-types', addButtonName: 'Add Referral Type', nameFieldId: 'rt-name', codeFieldId: 'rt-code' },
];

for (const master of MASTERS) {
  const stamp = Date.now();
  const name = `E2E Master ${stamp}`;
  const code = `E2E${stamp}`;

  test.describe(`${master.label} master`, () => {
    test.skip(!hasCreds('admin'), 'E2E_ADMIN_USER/PASS not configured — see e2e/.env.example');

    test(`create round-trip: new ${master.label} saves and appears in the list`, async ({ page }) => {
      await loginAs(page, 'admin');
      await page.goto(master.listRoute);
      // Match on visible text, not accessible name — these "Add X" buttons carry an
      // aria-label that doesn't match their text ("Add new blood group" vs "Add Blood
      // Group"), which silently breaks role-based name matching.
      await page.getByRole('button').filter({ hasText: master.addButtonName }).click();

      await page.locator(`#${master.nameFieldId}`).fill(name);
      await page.locator(`#${master.codeFieldId}`).fill(code);
      await page.locator('.btn-submit').click();

      // Uniform success signal across masters (toast wording varies / is sometimes absent —
      // see referral-type "Created" vs community "Community created" vs speciality no toast at
      // all): the form always navigates back to the list and the row is really there.
      await expect(page).toHaveURL(new RegExp(`${master.listRoute}$`), { timeout: 10_000 });
      // exact:true — a lingering row-action tooltip ("Deactivate " + name) contains the
      // bare name as a substring and made this ambiguous under default matching.
      await expect(page.getByText(name, { exact: true })).toBeVisible({ timeout: 10_000 });
    });

    test(`real-time uniqueness: re-typing the just-created ${master.label} name flags it before submit`, async ({ page }) => {
      await loginAs(page, 'admin');
      await page.goto(`${master.listRoute}/new`);

      const nameField = page.locator(`#${master.nameFieldId}`);
      await nameField.fill(name);
      await nameField.blur();

      const fieldGroup = page.locator('.field-group', { has: nameField });
      await expect(fieldGroup.locator('.field-error')).toBeVisible({ timeout: 10_000 });
    });
  });
}
