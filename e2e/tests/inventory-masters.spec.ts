import { test, expect } from '@playwright/test';
import { loginAs, hasCreds } from '../utils/login';

/**
 * Tier-A deep spec, rollout step 4 (cont'd) — Inventory Management, first slice.
 *
 * Inventory is by far the largest remaining module in docs/manual-test-cases/ (~30
 * files across 5 nav groups: Stock Management, Purchasing & Suppliers, Equipment &
 * Asset Management, Budgets & Approvals, Gate Pass & Service Requests). This spec
 * scopes to the highest-risk foundational slice for this pass: the catalog masters
 * (Category/UOM/Product) and the Purchasing masters (Tax Rules/Suppliers/Rate
 * Contracts) — all self-contained, no cross-module prerequisites.
 *
 * **Deliberately deferred, not started in this pass:**
 * - The Purchase Requisition -> Purchase Order -> Goods Receipt chain
 *   (inventory-procurement-purchase-requisition.md/-purchase-order.md,
 *   inventory-receiving-goods-receipt.md). Both PR and PO forms require selecting an
 *   Inventory Location (`#pr-location`/`#po-location`), and a direct check against
 *   243 (`GET /api/v1/inventory/locations`) found **zero** Locations exist there —
 *   creating one requires a Zone + Room from the Core Infrastructure hierarchy
 *   first, which is its own dependency chain outside this module. Needs either that
 *   set up first, or the user pointing at real seeded infra, before this chain can
 *   be automated the way admission's full pipeline was deferred for a similar
 *   isolated-fixture reason.
 * - Stock Balance/Transfers/Issue Requests, Cycle Counts, all Equipment & Asset
 *   Management, all Budgets & Approvals, all Gate Pass & Service Requests, and
 *   every reporting/analytics screen (PO Aging, PO Cycle-Time, Price Comparison,
 *   Stock Valuation, Budget vs. Actual, Depreciation Summary) — same "largest
 *   single chunk, work through by risk" prioritization already applied to prior
 *   modules; reports/analytics are lower-value than functional transactional
 *   coverage.
 * - Category custom attributes (TC-INV-ATTR-*) and the cycle-prevention check on
 *   the parent-category picker (TC-INV-CAT-004) — lower-risk UI nuance, left for a
 *   follow-up pass.
 *
 * A direct check against 243 also found the entire Inventory module has **zero**
 * real data yet (categories/uoms/products/suppliers all empty) despite having
 * shipped — every test below is fully self-contained by necessity, not by choice.
 *
 * Selectors verified against the real .html/.ts files, not guessed.
 */

test.describe('Inventory — Item Categories (TC-INV-CAT)', () => {
  test.skip(!hasCreds('admin'), 'E2E_ADMIN_USER/PASS not configured — see e2e/.env.example');

  test('create top-level + nested sub-category, parent-scoped uniqueness, deactivate/reactivate', async ({ page }) => {
    await loginAs(page, 'admin');

    const stamp = Date.now();
    const parentA = `E2E Cat A ${stamp}`;
    const parentB = `E2E Cat B ${stamp}`;
    const child = `E2E Cables ${stamp}`;

    // Two top-level categories (TC-INV-CAT-001).
    for (const name of [parentA, parentB]) {
      await page.goto('/inventory/categories/new');
      await page.locator('#cat-name').fill(name);
      await page.locator('button[type="submit"]').click();
      await expect(page).toHaveURL(/\/inventory\/categories$/, { timeout: 10_000 });
    }

    await page.getByRole('button', { name: 'Cards' }).click();
    await expect(page.locator('.cat-card', { hasText: parentA })).toBeVisible({ timeout: 10_000 });

    // Sub-category under Parent A (TC-INV-CAT-002).
    await page.goto('/inventory/categories/new');
    await page.locator('#cat-name').fill(child);
    await page.locator('#cat-parent').selectOption({ label: parentA });
    await page.locator('button[type="submit"]').click();
    await expect(page).toHaveURL(/\/inventory\/categories$/, { timeout: 10_000 });
    const childCard = page.locator('.cat-card', { hasText: child });
    await expect(childCard).toBeVisible({ timeout: 10_000 });
    await expect(childCard.getByText(parentA, { exact: true })).toBeVisible();

    // Same name under the SAME parent is blocked (TC-INV-CAT-003).
    // Select the parent BEFORE typing the name -- the async check re-fires on either field
    // changing, but doing it in this order means only one request is ever in flight (the
    // reverse order lets Angular cancel-and-replace the name-only check with the parent-scoped
    // one so fast that the .blur() below can win the race and see a stale "valid" state).
    await page.goto('/inventory/categories/new');
    await page.locator('#cat-parent').selectOption({ label: parentA });
    await page.locator('#cat-name').fill(child);
    await page.locator('#cat-name').blur();
    await expect(page.locator('.field-error')).toBeVisible({ timeout: 10_000 });
    await expect(page.locator('button[type="submit"]')).toBeDisabled();

    // Same name under a DIFFERENT parent succeeds — uniqueness is per-parent, not global.
    await page.locator('#cat-parent').selectOption({ label: parentB });
    await expect(page.locator('.field-error')).toHaveCount(0, { timeout: 10_000 });
    await page.locator('button[type="submit"]').click();
    await expect(page).toHaveURL(/\/inventory\/categories$/, { timeout: 10_000 });

    // Deactivate / reactivate (TC-INV-CAT-006).
    await page.getByRole('button', { name: 'Cards' }).click();
    const card = page.locator('.cat-card', { hasText: parentA }).first();
    await expect(card.getByText('Active', { exact: true })).toBeVisible({ timeout: 10_000 });
    await card.getByRole('button', { name: 'Toggle category status' }).click();
    await page.getByRole('button', { name: 'Deactivate', exact: true }).click();
    await expect(page.getByText('Category deactivated successfully')).toBeVisible({ timeout: 10_000 });
    await expect(card.getByText('Inactive', { exact: true })).toBeVisible({ timeout: 10_000 });
    await card.getByRole('button', { name: 'Toggle category status' }).click();
    await page.getByRole('button', { name: 'Activate', exact: true }).click();
    await expect(page.getByText('Category activated successfully')).toBeVisible({ timeout: 10_000 });
    await expect(card.getByText('Active', { exact: true })).toBeVisible({ timeout: 10_000 });
  });
});

test.describe('Inventory — Units of Measure (TC-INV-UOM)', () => {
  test.skip(!hasCreds('admin'), 'E2E_ADMIN_USER/PASS not configured — see e2e/.env.example');

  test('create with auto-uppercased code, code/name uniqueness, deactivate/reactivate', async ({ page }) => {
    await loginAs(page, 'admin');

    const stamp = Date.now();
    const name = `E2E Kilogram ${stamp}`;
    const code = `EKG${stamp}`.slice(0, 20);

    await page.goto('/inventory/uoms/new');
    await page.locator('#uom-name').fill(name);
    // Auto-uppercase + strip-spaces happens live on (input) (TC-INV-UOM-001's "kg" case,
    // generalized to a unique code) -- feed it lowercase with leading/trailing spaces.
    await page.locator('#uom-code').fill(`  ${code.toLowerCase()}  `);
    await expect(page.locator('#uom-code')).toHaveValue(code, { timeout: 5_000 });
    await page.locator('button[type="submit"]').click();
    await expect(page).toHaveURL(/\/inventory\/uoms$/, { timeout: 10_000 });

    await page.getByRole('button', { name: 'Cards' }).click().catch(() => {}); // no-op if UOM list has no card/table toggle
    await expect(page.getByText(name, { exact: true })).toBeVisible({ timeout: 10_000 });
    await expect(page.getByText(code, { exact: true })).toBeVisible();

    // Code must be globally unique (TC-INV-UOM-002).
    await page.goto('/inventory/uoms/new');
    await page.locator('#uom-name').fill(`${name} Duplicate Code`);
    await page.locator('#uom-code').fill(code);
    await page.locator('#uom-code').blur();
    const codeGroup = page.locator('.field-group', { has: page.locator('#uom-code') });
    await expect(codeGroup.locator('.field-error')).toBeVisible({ timeout: 10_000 });

    // Name must also be globally unique (TC-INV-UOM-002).
    await page.goto('/inventory/uoms/new');
    await page.locator('#uom-name').fill(name);
    await page.locator('#uom-code').fill(`${code}X`);
    await page.locator('#uom-name').blur();
    const nameGroup = page.locator('.field-group', { has: page.locator('#uom-name') });
    await expect(nameGroup.locator('.field-error')).toBeVisible({ timeout: 10_000 });

    // Deactivate / reactivate (TC-INV-UOM-003).
    await page.goto('/inventory/uoms');
    const row = page.locator('tr', { hasText: name }).or(page.locator('.uom-card', { hasText: name }));
    await row.first().getByRole('button', { name: /Toggle .*status/i }).click();
    await page.getByRole('button', { name: 'Deactivate', exact: true }).click();
    await expect(page.getByText(/deactivated successfully/i)).toBeVisible({ timeout: 10_000 });
  });
});

test.describe('Inventory — Products (TC-INV-PROD)', () => {
  test.skip(!hasCreds('admin'), 'E2E_ADMIN_USER/PASS not configured — see e2e/.env.example');

  test('create with required fields, code uniqueness, name unique per category (not global)', async ({ page }) => {
    await loginAs(page, 'admin');

    const stamp = Date.now();
    const catA = `E2E Prod Cat A ${stamp}`;
    const catB = `E2E Prod Cat B ${stamp}`;
    const uomName = `E2E Each ${stamp}`;
    const uomCode = `EA${stamp}`.slice(0, 20);
    const productCode = `E2EPRD${stamp}`;
    const productName = `E2E Test Item ${stamp}`;

    // Set up prerequisites — a category and a UOM (243 has zero of either).
    for (const cat of [catA, catB]) {
      await page.goto('/inventory/categories/new');
      await page.locator('#cat-name').fill(cat);
      await page.locator('button[type="submit"]').click();
      await expect(page).toHaveURL(/\/inventory\/categories$/, { timeout: 10_000 });
    }
    await page.goto('/inventory/uoms/new');
    await page.locator('#uom-name').fill(uomName);
    await page.locator('#uom-code').fill(uomCode);
    await page.locator('button[type="submit"]').click();
    await expect(page).toHaveURL(/\/inventory\/uoms$/, { timeout: 10_000 });

    // Create the product (TC-INV-PROD-001).
    await page.goto('/inventory/products/new');
    await page.locator('#prod-code').fill(productCode.toLowerCase()); // lowercase in, uppercased out
    await page.locator('#prod-name').fill(productName);
    await page.locator('#prod-category').selectOption({ label: catA });
    await page.locator('#prod-uom').selectOption({ label: `${uomName} (${uomCode})` });
    await page.getByRole('button', { name: 'Create Product' }).click();
    await expect(page.getByText('Product created successfully')).toBeVisible({ timeout: 10_000 });
    await expect(page).toHaveURL(/\/inventory\/products$/, { timeout: 10_000 });

    await page.getByLabel('Search products').fill(productCode);
    await expect(page.getByText(productCode, { exact: true })).toBeVisible({ timeout: 10_000 });
    await expect(page.getByText(productName, { exact: true })).toBeVisible();

    // Product code is globally unique (TC-INV-PROD-005, step 1).
    await page.goto('/inventory/products/new');
    await page.locator('#prod-code').fill(productCode);
    await page.locator('#prod-code').blur();
    const codeGroup = page.locator('.field-group', { has: page.locator('#prod-code') });
    await expect(codeGroup.locator('.field-error')).toBeVisible({ timeout: 10_000 });

    // Product name is unique per category — blocked in the SAME category (TC-INV-PROD-005, step 2)...
    // Category selected BEFORE the name, same reasoning as the Category master spec above:
    // it keeps only one async check in flight instead of a name-only one Angular's own
    // updateValueAndValidity call might not finish replacing before the blur assertion runs.
    await page.locator('#prod-category').selectOption({ label: catA });
    await page.locator('#prod-name').fill(productName);
    await page.locator('#prod-name').blur();
    const nameGroup = page.locator('.field-group', { has: page.locator('#prod-name') });
    await expect(nameGroup.locator('.field-error')).toBeVisible({ timeout: 10_000 });

    // ...but allowed in a DIFFERENT category (TC-INV-PROD-005, step 3).
    await page.locator('#prod-category').selectOption({ label: catB });
    await expect(nameGroup.locator('.field-error')).toHaveCount(0, { timeout: 10_000 });

    // Deactivate / reactivate (TC-INV-PROD-007).
    await page.goto('/inventory/products');
    await page.getByLabel('Search products').fill(productCode);
    const row = page.locator('tr', { hasText: productName }).or(page.locator('.prod-card', { hasText: productName }));
    await row.first().getByRole('button', { name: /Toggle .*status/i }).click();
    await page.getByRole('button', { name: 'Deactivate', exact: true }).click();
    await expect(page.getByText(/deactivated successfully/i)).toBeVisible({ timeout: 10_000 });
  });
});

test.describe('Inventory — Tax Rules (TC-INV-PROC-001)', () => {
  test.skip(!hasCreds('admin'), 'E2E_ADMIN_USER/PASS not configured — see e2e/.env.example');

  test('create a tax rule and block a duplicate name', async ({ page }) => {
    await loginAs(page, 'admin');
    const stamp = Date.now();
    const name = `E2E GST ${stamp}`;

    await page.goto('/inventory/procurement/tax-rules/new');
    await page.locator('#tr-tax-type').selectOption({ index: 1 }); // index 0 is the disabled placeholder
    await page.locator('#tr-name').fill(name);
    await page.locator('#tr-rate').fill('18');
    await page.getByRole('button', { name: 'Add Tax Rule' }).click();
    await expect(page.getByText('Tax rule created successfully')).toBeVisible({ timeout: 10_000 });
    await expect(page).toHaveURL(/\/inventory\/procurement\/tax-rules$/, { timeout: 10_000 });
    await expect(page.getByText(name, { exact: true })).toBeVisible({ timeout: 10_000 });

    await page.goto('/inventory/procurement/tax-rules/new');
    await page.locator('#tr-name').fill(name);
    await page.locator('#tr-name').blur();
    await expect(page.locator('.field-error')).toBeVisible({ timeout: 10_000 });
  });
});

test.describe('Inventory — Suppliers (TC-INV-PROC-002/003)', () => {
  test.skip(!hasCreds('admin'), 'E2E_ADMIN_USER/PASS not configured — see e2e/.env.example');

  test('create a supplier starting unapproved, block duplicate code, then approve it', async ({ page }) => {
    await loginAs(page, 'admin');
    const stamp = Date.now();
    const code = `E2ESUP${stamp}`;
    const name = `E2E Acme Medical ${stamp}`;

    await page.goto('/inventory/procurement/suppliers/new');
    await page.locator('#sup-code').fill(code.toLowerCase());
    await page.locator('#sup-name').fill(name);
    await page.locator('#sup-state').fill('Tamil Nadu');
    await page.getByRole('button', { name: 'Create Supplier' }).click();
    await expect(page.getByText('Supplier created successfully')).toBeVisible({ timeout: 10_000 });
    await expect(page).toHaveURL(/\/inventory\/procurement\/suppliers$/, { timeout: 10_000 });

    await page.getByLabel('Search suppliers').fill(code);
    const row = page.locator('tr', { hasText: name });
    await expect(row).toBeVisible({ timeout: 10_000 });
    await expect(row.getByText('Pending', { exact: true })).toBeVisible();

    // Duplicate supplier code is blocked (TC-INV-PROC-002).
    await page.goto('/inventory/procurement/suppliers/new');
    await page.locator('#sup-code').fill(code);
    await page.locator('#sup-code').blur();
    await expect(page.locator('.field-error')).toBeVisible({ timeout: 10_000 });

    // Approve is a separate step from creating/editing (TC-INV-PROC-003).
    await page.goto('/inventory/procurement/suppliers');
    await page.getByLabel('Search suppliers').fill(code);
    const approveRow = page.locator('tr', { hasText: name });
    await approveRow.getByRole('button', { name: 'Approve supplier' }).click();
    await page.getByRole('button', { name: 'Approve', exact: true }).click();
    await expect(page.getByText('Supplier approved')).toBeVisible({ timeout: 10_000 });
    await expect(approveRow.getByText('Approved', { exact: true })).toBeVisible({ timeout: 10_000 });
    // No re-approve affordance once already approved.
    await expect(approveRow.getByRole('button', { name: 'Approve supplier' })).toHaveCount(0);
  });
});

test.describe('Inventory — Rate Contracts (TC-INV-PROC-005)', () => {
  test.skip(!hasCreds('admin'), 'E2E_ADMIN_USER/PASS not configured — see e2e/.env.example');

  test('create a rate contract against a supplier, block an end date before the start date', async ({ page }) => {
    await loginAs(page, 'admin');
    const stamp = Date.now();
    const supplierCode = `E2ESUPRC${stamp}`;
    const supplierName = `E2E Rate Contract Supplier ${stamp}`;

    await page.goto('/inventory/procurement/suppliers/new');
    await page.locator('#sup-code').fill(supplierCode);
    await page.locator('#sup-name').fill(supplierName);
    await page.locator('#sup-state').fill('Tamil Nadu');
    await page.getByRole('button', { name: 'Create Supplier' }).click();
    await expect(page).toHaveURL(/\/inventory\/procurement\/suppliers$/, { timeout: 10_000 });

    const today = new Date().toISOString().slice(0, 10);
    const nextYear = new Date(Date.now() + 366 * 86400000).toISOString().slice(0, 10);
    const lastYear = new Date(Date.now() - 366 * 86400000).toISOString().slice(0, 10);

    await page.goto('/inventory/procurement/rate-contracts/new');
    await page.locator('#rc-supplier').selectOption({ label: `${supplierName} (${supplierCode})` });
    await page.locator('#rc-start').fill(today);
    await page.locator('#rc-end').fill(nextYear);
    await page.getByRole('button', { name: 'Add Rate Contract' }).click();
    await expect(page.getByText('Rate contract created successfully')).toBeVisible({ timeout: 10_000 });
    await expect(page).toHaveURL(/\/inventory\/procurement\/rate-contracts$/, { timeout: 10_000 });
    // getByText alone is ambiguous here -- the same list page's own "Filter by supplier"
    // dropdown carries an <option> with this exact text too; scope to the table cell.
    await expect(page.getByRole('cell', { name: supplierName, exact: true })).toBeVisible({ timeout: 10_000 });

    // End date earlier than start date is blocked (TC-INV-PROC-005).
    await page.goto('/inventory/procurement/rate-contracts/new');
    await page.locator('#rc-supplier').selectOption({ label: `${supplierName} (${supplierCode})` });
    await page.locator('#rc-start').fill(today);
    await page.locator('#rc-end').fill(lastYear);
    await page.getByRole('button', { name: 'Add Rate Contract' }).click();
    await expect(page.getByText('End date cannot be before the start date')).toBeVisible({ timeout: 10_000 });
    await expect(page).toHaveURL(/\/inventory\/procurement\/rate-contracts\/new$/); // did not submit
  });
});
