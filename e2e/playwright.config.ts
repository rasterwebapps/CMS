import { defineConfig, devices } from '@playwright/test';
import 'dotenv/config';

// Local-only regression gate — no cloud CI. Run against whatever E2E_BASE_URL points at
// (regression-gate.sh sets this to the 243 test server after deploying the branch under test).
export default defineConfig({
  testDir: './tests',
  fullyParallel: false, // shared 243 test-server state — role/user/master rows created by one spec must not race another
  retries: 0,
  workers: 1,
  reporter: [
    ['list'],
    ['html', { outputFolder: 'report', open: 'never' }],
    ['json', { outputFile: 'report/results.json' }],
  ],
  timeout: 30_000,
  use: {
    baseURL: process.env.E2E_BASE_URL || 'https://dev.raster.in:212',
    ignoreHTTPSErrors: process.env.E2E_IGNORE_HTTPS_ERRORS !== 'false',
    trace: 'retain-on-failure',
    screenshot: 'only-on-failure',
  },
  projects: [
    {
      name: 'chromium',
      // Desktop Chrome's default 1280x720 viewport was tripping the app shell's
      // compact/icon-rail sidebar breakpoint, which renders nav groups as a
      // hover flyout that can sit over the page content and eat clicks — not a
      // real bug, just not how an actual admin's desktop monitor renders this.
      use: { ...devices['Desktop Chrome'], viewport: { width: 1600, height: 900 } },
    },
  ],
});
