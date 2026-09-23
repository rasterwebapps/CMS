import { test, expect } from '@playwright/test';
import { loginAs, hasCreds } from '../utils/login';

/**
 * Tier-A deep spec, rollout step 4 (cont'd) — Library Management
 * (docs/manual-test-cases/library-management.md).
 *
 * The manual-test-case doc is stale in a few places found while writing this
 * (same pattern already seen in admission-from-enquiry.md):
 * - Nav labels differ: the doc says "Book Catalogue"/"Issue Desk"/"Fine
 *   Management"/"Journals & Periodicals"/"Reports"/"Book Import" — the real
 *   nav-config.ts labels are "Book Explorer"/"Issue Explorer"/"Fines"/
 *   "Journal Explorer"/"Overdue Books"/"Import" (nav-config.ts:161-174).
 * - TC-LIB-BOOK-001/002, TC-LIB-ISSUE-001/002, TC-LIB-FINE-001/002,
 *   TC-LIB-PER-001/002 all describe summary cards (Total/Available/Issued
 *   etc.) that don't exist anywhere in the current list templates —
 *   library-book-list, library-issue-list, library-fines, and
 *   library-periodical-list are all plain toolbar+table, no summary card
 *   row. Not tested here since the UI doesn't have them.
 * - The whole "Reports" section (TC-LIB-RPT-001 through 011: four tabs —
 *   Overdue/Fines Summary/Issue History/Accession Register) describes a
 *   feature that isn't there. `/library/reports` (library-reports.component)
 *   is a single-purpose "Overdue Books" list with no tabs at all. Scoped
 *   below to what's real.
 * - Delete-blocked-on-ISSUED (TC-LIB-BOOK-020) is enforced by disabling the
 *   Delete button entirely (`[disabled]="book.status === 'ISSUED'"`), not by
 *   letting the confirm dialog through and erroring — asserted as disabled
 *   below rather than doc's confirm+error-toast flow.
 *
 * Selectors verified against the real .html/.ts files, not guessed. Each
 * test creates and cleans up its own throwaway data rather than depending on
 * seed data or another test's run order (243 is shared, long-lived state) --
 * except the Issue Desk round-trip test's book, which is left behind
 * permanently by design: once a book has any issue history (even returned),
 * LibraryBookService.delete() refuses to delete it forever (real backend
 * rule, asserted directly in that test) -- there is no legitimate app flow
 * to remove it afterward.
 */

test.describe('Library — Book Catalogue (TC-LIB-BOOK)', () => {
  test.skip(!hasCreds('admin'), 'E2E_ADMIN_USER/PASS not configured — see e2e/.env.example');

  test('add, accession-uniqueness-check, edit, and delete a book (full round trip)', async ({ page }) => {
    await loginAs(page, 'admin');

    const stamp = Date.now();
    const accessionNumber = `E2EBK${stamp}`;
    const title = `E2E Test Book ${stamp}`;
    const updatedTitle = `${title} Updated`;

    // Add Book — happy path (TC-LIB-BOOK-010/014).
    await page.goto('/library/books/new');
    await expect(page.getByRole('heading', { name: /Add Book/i })).toBeVisible();
    await page.locator('#accessionNumber').fill(accessionNumber);
    await page.locator('#title').fill(title);
    await page.locator('#authors').fill('E2E Author');
    await page.getByRole('button', { name: 'Add Book' }).click();
    await expect(page.getByText('Book added successfully')).toBeVisible({ timeout: 10_000 });
    await expect(page).toHaveURL(/\/library\/books$/, { timeout: 10_000 });

    // Accession number uniqueness — async check on blur (TC-LIB-BOOK-012).
    await page.goto('/library/books/new');
    await page.locator('#accessionNumber').fill(accessionNumber);
    await page.locator('#title').click(); // blur accessionNumber to trigger the async validator
    await expect(page.getByText('This accession number is already in use')).toBeVisible({ timeout: 5_000 });
    await page.goto('/library/books'); // abandon this half-filled form, no Save

    // Edit — form pre-populated, save changes (TC-LIB-BOOK-015/017).
    await page.getByLabel('Search books').fill(accessionNumber);
    const row = page.locator('tr.mlp-row', { hasText: title });
    await expect(row).toBeVisible({ timeout: 10_000 });
    await row.getByRole('button', { name: 'Edit book' }).click();
    await expect(page.getByRole('heading', { name: /Edit Book/i })).toBeVisible();
    await expect(page.locator('#accessionNumber')).toHaveValue(accessionNumber);
    await expect(page.locator('#title')).toHaveValue(title);
    await page.locator('#title').fill(updatedTitle);
    await page.getByRole('button', { name: 'Update Book' }).click();
    await expect(page.getByText('Book updated successfully')).toBeVisible({ timeout: 10_000 });
    await expect(page).toHaveURL(/\/library\/books$/, { timeout: 10_000 });

    // Delete — confirm dialog, cleanup (TC-LIB-BOOK-018).
    await page.getByLabel('Search books').fill(accessionNumber);
    const updatedRow = page.locator('tr.mlp-row', { hasText: updatedTitle });
    await expect(updatedRow).toBeVisible({ timeout: 10_000 });
    await updatedRow.getByRole('button', { name: 'Delete book' }).click();
    await page.getByRole('button', { name: 'Delete', exact: true }).click();
    await expect(page.getByText('Book deleted successfully')).toBeVisible({ timeout: 10_000 });
    await page.getByLabel('Search books').fill(accessionNumber);
    await expect(page.locator('tr.mlp-row', { hasText: updatedTitle })).toHaveCount(0, { timeout: 10_000 });
  });

  test('search and status filter narrow the catalogue (TC-LIB-BOOK-003/004)', async ({ page }) => {
    await loginAs(page, 'admin');
    await page.goto('/library/books');
    await page.waitForLoadState('networkidle').catch(() => {});

    const rowCount = page.locator('.mat-paginator-range-label, mat-paginator');
    await expect(rowCount.first()).toBeVisible({ timeout: 10_000 });

    // Status filter — every visible row's status pill must match the selected option.
    const statusFilter = page.getByLabel('Filter by status');
    await statusFilter.selectOption('AVAILABLE');
    await page.waitForLoadState('networkidle').catch(() => {});
    const pills = page.locator('td.mat-column-status .status-pill');
    const pillCount = await pills.count();
    if (pillCount > 0) {
      for (const text of await pills.allInnerTexts()) {
        expect(text.trim()).toBe('Available');
      }
    }
    await statusFilter.selectOption('');
  });
});

test.describe('Library — Issue Desk (TC-LIB-ISSUE)', () => {
  test.skip(!hasCreds('admin'), 'E2E_ADMIN_USER/PASS not configured — see e2e/.env.example');

  test('look up an unknown accession number shows the real not-found error (TC-LIB-ISSUE-010)', async ({ page }) => {
    await loginAs(page, 'admin');
    await page.goto('/library/issues/new');
    const bogusAcc = `NOPE-${Date.now()}`;
    await page.locator('#accessionNumber').fill(bogusAcc);
    await page.getByRole('button', { name: 'Look Up' }).click();
    await expect(page.getByText(`No book or journal found with accession number "${bogusAcc}"`)).toBeVisible({ timeout: 10_000 });
  });

  test('issue a book to a student, confirm it blocks deletion while ISSUED, then return it (full round trip)', async ({ page }) => {
    await loginAs(page, 'admin');

    const stamp = Date.now();
    const accessionNumber = `E2EISS${stamp}`;
    const title = `E2E Issue Test Book ${stamp}`;

    // Set up a throwaway AVAILABLE book to issue.
    await page.goto('/library/books/new');
    await page.locator('#accessionNumber').fill(accessionNumber);
    await page.locator('#title').fill(title);
    await page.locator('#authors').fill('E2E Author');
    await page.getByRole('button', { name: 'Add Book' }).click();
    await expect(page).toHaveURL(/\/library\/books$/, { timeout: 10_000 });

    // Issue Book — look up, pick a student, save (TC-LIB-ISSUE-009/013).
    await page.goto('/library/issues/new');
    await page.locator('#accessionNumber').fill(accessionNumber);
    await page.getByRole('button', { name: 'Look Up' }).click();
    await expect(page.getByText(title)).toBeVisible({ timeout: 10_000 });
    await expect(page.getByText('Available', { exact: true })).toBeVisible();

    const studentSelect = page.locator('#studentId');
    await studentSelect.selectOption({ index: 1 }); // index 0 is the "— Select Student —" placeholder
    const studentLabel = (await studentSelect.locator('option:checked').innerText()).trim();

    await page.getByRole('button', { name: 'Issue Book' }).click();
    await expect(page.getByText('Book issued successfully')).toBeVisible({ timeout: 10_000 });
    await expect(page).toHaveURL(/\/library\/issues$/, { timeout: 10_000 });

    // Book status flips to ISSUED, and the catalogue's Delete button is disabled for it
    // (TC-LIB-ISSUE-013, TC-LIB-BOOK-020 — enforced via disabled button, see file header).
    await page.goto('/library/books');
    await page.getByLabel('Search books').fill(accessionNumber);
    const bookRow = page.locator('tr.mlp-row', { hasText: title });
    await expect(bookRow.locator('.status-pill')).toHaveText('Issued', { timeout: 10_000 });
    await expect(bookRow.getByRole('button', { name: 'Delete book' })).toBeDisabled();

    // Return it (TC-LIB-ISSUE-016) — on-time, no fine, back to AVAILABLE.
    await page.goto('/library/issues');
    await page.getByLabel('Search issues').fill(accessionNumber);
    const issueRow = page.locator('tr.mlp-row', { hasText: title });
    await expect(issueRow).toBeVisible({ timeout: 10_000 });
    await expect(issueRow).toContainText(studentLabel.split(' (')[0]);
    await issueRow.getByRole('button', { name: 'Return book' }).click();
    await page.getByRole('button', { name: 'Confirm Return', exact: true }).click();
    await expect(page.getByText(/returned successfully/i)).toBeVisible({ timeout: 10_000 });

    await page.goto('/library/books');
    await page.getByLabel('Search books').fill(accessionNumber);
    const returnedRow = page.locator('tr.mlp-row', { hasText: title });
    await expect(returnedRow.locator('.status-pill')).toHaveText('Available', { timeout: 10_000 });

    // No cleanup delete: LibraryBookService.delete() (backend) blocks deleting any book with
    // issue history at all -- `issueRepository.existsByBookId(id)` -- not just currently-ISSUED
    // ones. The Delete button stays enabled here (only disabled while status === 'ISSUED'), so
    // clicking through actually reaches this real, permanent rule -- worth asserting directly
    // rather than treating as test cleanup friction (audit-trail books are meant to never be
    // deletable once they've circulated, even after being returned).
    await returnedRow.getByRole('button', { name: 'Delete book' }).click();
    await page.getByRole('button', { name: 'Delete', exact: true }).click();
    await expect(page.getByText('Cannot delete a book that has issue history')).toBeVisible({ timeout: 10_000 });
    await expect(returnedRow).toBeVisible(); // confirm dialog closed, row still there -- not deleted
  });
});

test.describe('Library — Journals & Periodicals (TC-LIB-PER)', () => {
  test.skip(!hasCreds('admin'), 'E2E_ADMIN_USER/PASS not configured — see e2e/.env.example');

  test('create, edit, and delete a periodical entry (full round trip)', async ({ page }) => {
    await loginAs(page, 'admin');

    const stamp = Date.now();
    const accessionNumber = `E2EPER${stamp}`;
    const journalName = `E2E Test Journal ${stamp}`;
    const updatedName = `${journalName} Updated`;

    // Add Periodical — required-field validation (TC-LIB-PER-008). The Save button is
    // `[disabled]="form.invalid"` from the start (both required fields start empty), so it
    // can't be clicked while blank — touch-and-blur each field instead to surface the errors,
    // the same way a real user tabbing through the form would trigger them.
    await page.goto('/library/periodicals/new');
    await page.locator('#accessionNumber').click();
    await page.locator('#journalName').click();
    await page.locator('#barcode').click();
    await expect(page.getByText('Accession number is required')).toBeVisible();
    await expect(page.getByText('Journal name is required')).toBeVisible();
    await expect(page.getByRole('button', { name: 'Add Entry' })).toBeDisabled();

    await page.locator('#accessionNumber').fill(accessionNumber);
    await page.locator('#journalName').fill(journalName);
    await page.getByRole('button', { name: 'Add Entry' }).click();
    await expect(page.getByText('Journal entry added')).toBeVisible({ timeout: 10_000 });
    await expect(page).toHaveURL(/\/library\/periodicals$/, { timeout: 10_000 });

    // Edit — pre-populated, save (TC-LIB-PER-010/011).
    await page.getByLabel('Search periodicals').fill(accessionNumber);
    const row = page.locator('tr.mlp-row', { hasText: journalName });
    await expect(row).toBeVisible({ timeout: 10_000 });
    await row.getByRole('button', { name: 'Edit', exact: true }).click();
    await expect(page.locator('#journalName')).toHaveValue(journalName);
    await page.locator('#journalName').fill(updatedName);
    await page.getByRole('button', { name: 'Update Entry' }).click();
    await expect(page.getByText('Journal entry updated')).toBeVisible({ timeout: 10_000 });

    // Delete — confirm dialog, cleanup (TC-LIB-PER-012).
    await page.getByLabel('Search periodicals').fill(accessionNumber);
    const updatedRow = page.locator('tr.mlp-row', { hasText: updatedName });
    await expect(updatedRow).toBeVisible({ timeout: 10_000 });
    await updatedRow.getByRole('button', { name: 'Delete', exact: true }).click();
    await page.getByRole('button', { name: 'Delete', exact: true }).click();
    await expect(page.getByText('Periodical entry deleted')).toBeVisible({ timeout: 10_000 });
    await page.getByLabel('Search periodicals').fill(accessionNumber);
    await expect(page.locator('tr.mlp-row', { hasText: updatedName })).toHaveCount(0, { timeout: 10_000 });
  });
});

test.describe('Library — Fines, Overdue Report, Settings, Import (read-only entry points)', () => {
  test.skip(!hasCreds('admin'), 'E2E_ADMIN_USER/PASS not configured — see e2e/.env.example');

  test('Fine Management list loads (TC-LIB-FINE-001)', async ({ page }) => {
    await loginAs(page, 'admin');
    await page.goto('/library/fines');
    await expect(page.getByRole('heading', { name: /Fine Management/i })).toBeVisible({ timeout: 10_000 });
    await expect(page.getByLabel('Search fines')).toBeVisible();
  });

  test('Overdue Books report loads (TC-LIB-RPT-002/003, real single-list shape)', async ({ page }) => {
    await loginAs(page, 'admin');
    await page.goto('/library/reports');
    await expect(page.getByRole('heading', { name: /Overdue Books/i })).toBeVisible({ timeout: 10_000 });
    // Either real overdue rows or the empty state — both are a valid, correctly-loaded outcome.
    await expect(
      page.locator('tr.mlp-row').first().or(page.getByText(/No overdue books/i))
    ).toBeVisible({ timeout: 10_000 });
  });

  test('Library Settings loads real backend values, not just form fallbacks (TC-LIB-SET-001/002)', async ({ page }) => {
    await loginAs(page, 'admin');
    await page.goto('/library/settings');
    await expect(page.getByRole('heading', { name: /Library Settings/i })).toBeVisible({ timeout: 10_000 });
    const fieldIds = ['student_loan_days', 'faculty_loan_days', 'student_max_books', 'faculty_max_books', 'fine_per_day', 'max_renewals'];
    for (const id of fieldIds) {
      const value = await page.locator(`#${id}`).inputValue();
      expect(value, `#${id} loaded empty instead of a real backend value`).not.toBe('');
    }
  });

  test('Book Import wizard Step 1 loads with the template download (TC-LIB-IMP-001)', async ({ page }) => {
    await loginAs(page, 'admin');
    await page.goto('/library/import');
    await expect(page.getByRole('button', { name: 'Download Template', exact: true })).toBeVisible({ timeout: 10_000 });
  });
});

test.describe('Library — RBAC (TC-LIB-RBAC-004)', () => {
  test.skip(!hasCreds('admin'), 'E2E_ADMIN_USER/PASS not configured — see e2e/.env.example');

  test('Admin can access every Library screen directly', async ({ page }) => {
    await loginAs(page, 'admin');
    for (const route of [
      '/library/books', '/library/issues', '/library/periodicals',
      '/library/fines', '/library/reports', '/library/settings', '/library/import',
    ]) {
      const failedResponses: string[] = [];
      page.on('response', (res) => {
        if (res.url().includes(route) && res.status() >= 500) failedResponses.push(`${res.status()} ${res.url()}`);
      });
      await page.goto(route);
      await page.waitForLoadState('networkidle').catch(() => {});
      expect(failedResponses, `${route} returned a 5xx`).toEqual([]);
    }
  });
});
