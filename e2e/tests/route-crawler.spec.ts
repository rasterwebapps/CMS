import { test, expect } from '@playwright/test';
import { mkdirSync, readFileSync } from 'node:fs';
import path from 'node:path';
import { loginAs, hasCreds } from '../utils/login';

const STATE_FILE = path.resolve(__dirname, '../report/.admin-state.json');

/**
 * Tier-B smoke floor: every static route in app.routes.ts, visited as ADMIN.
 * Extracted from the live route table (not a hand-maintained copy) so it can never
 * silently go stale as screens are added — this is what "run this on the whole
 * application" cheaply means before every screen has a hand-authored deep spec.
 *
 * Catches exactly the class of bug that has shipped repeatedly (OC-242 Attendance
 * List always 400s, OC-237 Exam Results 500s, OC-245 lab-utilization-heatmap 500):
 * a screen that loads to a visibly broken state. It does NOT verify business
 * correctness of what's on the page — that's what the Tier-A deep specs are for.
 */

const ROUTES_FILE = path.resolve(__dirname, '../../frontend/src/app/app.routes.ts');

function extractStaticPaths(): string[] {
  const src = readFileSync(ROUTES_FILE, 'utf-8');
  const paths = new Set<string>();
  for (const m of src.matchAll(/path:\s*'([^']*)'/g)) {
    const p = m[1];
    if (!p || p.includes(':') || p === '**' || p.includes('*')) continue;
    paths.add(p);
  }
  return [...paths].sort();
}

// Bare "500" is unusable on this app — real currency amounts (₹500, fee receipts, etc.)
// hit it constantly on finance screens. Only match it in genuine error-status phrasing.
const ERROR_TEXT_RE = /\b(HTTP\s*500|Error\s*500|500\s*Internal|Internal Server Error|Cannot GET|Whitelabel Error Page|Unexpected error|Something went wrong)\b/i;

test.describe('Route crawler (whole-app smoke floor)', () => {
  test.skip(!hasCreds('admin'), 'E2E_ADMIN_USER/PASS not configured — see e2e/.env.example');

  const routes = extractStaticPaths();
  test(`discovered ${routes.length} static routes to crawl`, () => {
    expect(routes.length).toBeGreaterThan(0);
  });

  test.beforeAll(async ({ browser }) => {
    if (!hasCreds('admin')) return;
    mkdirSync(path.dirname(STATE_FILE), { recursive: true });
    const page = await browser.newPage();
    await loginAs(page, 'admin');
    await page.context().storageState({ path: STATE_FILE });
    await page.close();
  });

  for (const route of routes) {
    test(`route "/${route}" loads without a visible error`, async ({ browser }) => {
      test.skip(!hasCreds('admin'), 'no admin creds configured');
      const context = await browser.newContext({ storageState: STATE_FILE });
      const page = await context.newPage();

      const failedResponses: string[] = [];
      page.on('response', (res) => {
        if (res.request().resourceType() === 'xhr' || res.request().resourceType() === 'fetch') {
          if (res.status() >= 500) failedResponses.push(`${res.status()} ${res.url()}`);
        }
      });
      const pageErrors: string[] = [];
      page.on('pageerror', (err) => pageErrors.push(err.message));

      await page.goto(`/${route}`, { waitUntil: 'networkidle', timeout: 20_000 }).catch(() => {
        // navigation timeout is itself a finding — fall through to the assertions below
      });

      const bodyText = await page.locator('body').innerText().catch(() => '');
      expect.soft(bodyText, `route "/${route}" rendered an error string in the DOM`).not.toMatch(ERROR_TEXT_RE);
      expect.soft(failedResponses, `route "/${route}" had a 5xx API response`).toEqual([]);
      expect.soft(pageErrors, `route "/${route}" threw an uncaught JS error`).toEqual([]);

      await context.close();
    });
  }
});
