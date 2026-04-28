# Professional Data Table Alignment Standards (2026)

## Overview

This document defines the professional UX standards for data table alignment in the College Management System. These rules ensure optimal scannability, vertical rhythm, and visual clarity in financial and operational data tables.

## Implementation Date
April 28, 2026

---

## Core Principles

### 1. **Vertical Rhythm is Paramount**
All numeric data must align perfectly to enable instant magnitude comparison without reading every digit. Users should be able to scan down a column and immediately identify outliers or patterns.

### 2. **Scannability over Aesthetics**
Tables are productivity tools, not art. Every alignment decision prioritizes quick data comprehension over visual symmetry.

### 3. **Intentional Header Alignment**
Column headers **must mirror** the alignment of the data below them. Right-aligned numbers demand right-aligned headers to maintain visual coherence.

---

## Alignment Rules by Data Type

### Numeric Data → Right-Align
**Applies to**: Currency, counts, percentages, IDs, measurements

**Why**: Decimal points and comma separators line up vertically, allowing users to compare magnitude instinctively (thousands vs millions vs lakhs).

**CSS Implementation**: Automatic via column name matching

```scss
// Automatically right-aligned columns
.mat-column-totalAmount,
.mat-column-amountPaid,
.mat-column-totalFee,
.mat-column-totalPaid,
.mat-column-totalOutstanding,
.mat-column-paidAmount,
.mat-column-pendingAmount,
.mat-column-outstandingAmount,
.mat-column-finalizedNetFee,
.mat-column-commissionAmount,
.mat-column-amount,
.mat-column-count,
.mat-column-total
```

**HTML Example**:
```html
<ng-container matColumnDef="totalAmount">
  <th mat-header-cell *matHeaderCellDef mat-sort-header>Total Amount (₹)</th>
  <td mat-cell *matCellDef="let row">
    <span class="cell-currency">{{ row.totalAmount | inr:false:false }}</span>
  </td>
</ng-container>
```

**Required CSS Properties**:
- `text-align: right`
- `font-variant-numeric: tabular-nums` (prevents digit width variation)
- `.mat-sort-header-container { justify-content: flex-end; }` (aligns sort icon)

---

### Status Badges → Center-Align
**Applies to**: Status indicators, boolean flags, badges

**Why**: Status badges are visual anchors. Centering creates a clear vertical spine that breaks up text/number monotony and draws the eye to state changes.

**CSS Implementation**: Automatic via column name matching

```scss
// Automatically center-aligned columns
.mat-column-status,
.mat-column-paymentStatus,
.mat-column-isActive,
.mat-column-hasCommission
```

**HTML Example**:
```html
<ng-container matColumnDef="status">
  <th mat-header-cell *matHeaderCellDef mat-sort-header>Status</th>
  <td mat-cell *matCellDef="let row">
    <cms-status-badge [status]="row.status"></cms-status-badge>
  </td>
</ng-container>
```

**Exception**: If a badge appears inline next to text (not in its own column), keep it left-aligned following the text.

---

### Text Data → Left-Align
**Applies to**: Names, descriptions, addresses, free-form text

**Why**: Follows natural left-to-right reading pattern. Prevents "ragged left" edge that makes scanning difficult.

**CSS Implementation**: Default behavior (or automatic via column name)

```scss
// Automatically left-aligned columns
.mat-column-name,
.mat-column-studentName,
.mat-column-programName,
.mat-column-courseName,
.mat-column-departmentName,
.mat-column-description
```

**HTML Example**:
```html
<ng-container matColumnDef="studentName">
  <th mat-header-cell *matHeaderCellDef mat-sort-header>Student Name</th>
  <td mat-cell *matCellDef="let row">
    <span class="cell-name">{{ row.studentName }}</span>
  </td>
</ng-container>
```

---

### Dates → Left-Align (Fixed-Width Format)
**Applies to**: Date columns

**Why**: Dates are chronological identifiers, not numeric values. Left-alignment follows reading flow. Fixed-width format (e.g., `DD MMM YYYY`) prevents shifting.

**CSS Implementation**: Automatic via column name matching

```scss
.mat-column-date,
.mat-column-enquiryDate,
.mat-column-paymentDate,
.mat-column-dueDate
```

**HTML Example**:
```html
<ng-container matColumnDef="paymentDate">
  <th mat-header-cell *matHeaderCellDef mat-sort-header>Payment Date</th>
  <td mat-cell *matCellDef="let row">
    <span class="cell-date">{{ row.paymentDate | date:'dd MMM yyyy' }}</span>
  </td>
</ng-container>
```

---

## Empty State Handling

### Never Leave Cells Blank
**Bad**: `<td mat-cell></td>`  
**Good**: `<td mat-cell><span class="cell-empty"></span></td>`

**Why**: Blank cells feel incomplete, as if data is still loading. An explicit placeholder signals intentional absence.

### Use En-Dash (—) for Missing Data
**HTML**:
```html
<td mat-cell *matCellDef="let row">
  {{ row.value || '—' }}
</td>
```

**Or with CSS class**:
```html
<td mat-cell *matCellDef="let row">
  @if (row.value) {
    {{ row.value }}
  } @else {
    <span class="cell-empty"></span>
  }
</td>
```

The `.cell-empty` class automatically inserts the en-dash via `::before` pseudo-element, styled in muted color and center-aligned.

---

## Currency Display: Accounting Format

### Modern Pattern (Symbol in Header)
**Standard approach** (implemented): Currency symbol in column header only.

```html
<ng-container matColumnDef="totalAmount">
  <th mat-header-cell *matHeaderCellDef>Total Amount (₹)</th>
  <td mat-cell *matCellDef="let row">{{ row.totalAmount | inr:false:false }}</td>
</ng-container>
```

**Output**:
```
Total Amount (₹)
────────────────
  1,23,456
    45,000
  9,87,654
```

### Alternative: Accounting Format (Advanced)
**Future option**: Symbol at far left, number at far right within the same cell.

```html
<ng-container matColumnDef="totalAmount">
  <th mat-header-cell *matHeaderCellDef>Total Amount</th>
  <td mat-cell *matCellDef="let row">
    <div class="cell-accounting">
      <span class="currency-symbol">₹</span>
      <span class="currency-value">{{ row.totalAmount | inr:false:false }}</span>
    </div>
  </td>
</ng-container>
```

**Output**:
```
Total Amount
────────────────
₹      1,23,456
₹        45,000
₹      9,87,654
```

This "decoupling" clears the visual path for the eye to scan numbers without symbols acting as noise. Currently **not implemented** (use symbol-in-header approach instead).

---

## Tabular Figures (Monospaced Digits)

### Always Use `font-variant-numeric: tabular-nums`
**Why**: Prevents "jitter" effect where "1" takes less space than "8". Ensures perfect decimal alignment.

**Applied to**:
- `.cell-currency`
- `.cell-number`
- `.cell-code`
- All classes with `font-family: var(--cms-font-mono)`

**Example**:
```scss
.cell-currency {
  font-family: var(--cms-font-mono);
  font-variant-numeric: tabular-nums;
  font-weight: 600;
}
```

---

## Header Alignment Rules

### Mirror Data Alignment
Column headers **must** match the alignment of the data below:

| Data Type | Data Alignment | Header Alignment | Sort Icon Position |
|-----------|----------------|------------------|-------------------|
| Numbers   | Right          | Right            | Left of text      |
| Status    | Center         | Center           | Left of text      |
| Text      | Left           | Left             | Right of text     |

### Material Table Sort Header
Angular Material's `mat-sort-header` requires adjusting the flexbox container:

```scss
// Right-aligned headers
.mat-column-totalAmount {
  text-align: right;
  
  .mat-sort-header-container {
    justify-content: flex-end; // Moves sort icon to left of text
  }
}

// Center-aligned headers
.mat-column-status {
  text-align: center;
  
  .mat-sort-header-container {
    justify-content: center;
  }
}
```

---

## Manual Alignment Override

### Utility Classes
For one-off cases where column name doesn't match the standard:

```html
<!-- Force right alignment -->
<td mat-cell *matCellDef="let row" class="cell-align-right">
  {{ row.customNumeric | number }}
</td>

<!-- Force center alignment -->
<td mat-cell *matCellDef="let row" class="cell-align-center">
  <span class="badge">{{ row.label }}</span>
</td>

<!-- Force left alignment -->
<td mat-cell *matCellDef="let row" class="cell-align-left">
  {{ row.note }}
</td>
```

---

## Best Practices Checklist

### When Creating New Tables

- [ ] Numeric columns use right-alignment (`mat-column-*Amount`, `mat-column-*Total`)
- [ ] Status columns use center-alignment (`mat-column-status`)
- [ ] Text columns use left-alignment (default)
- [ ] Date columns use left-alignment with fixed-width format (`dd MMM yyyy`)
- [ ] Empty cells show en-dash (—) instead of blank
- [ ] Currency cells use `| inr:false:false` (no symbol)
- [ ] Currency column headers include `(₹)` suffix
- [ ] All numeric cells have `font-variant-numeric: tabular-nums`
- [ ] Column headers mirror data alignment
- [ ] Sort icons positioned correctly (left for right-aligned, right for left-aligned)

### Example Perfect Column

```html
<ng-container matColumnDef="totalAmount">
  <!-- ✅ Header: right-aligned, symbol in header -->
  <th mat-header-cell *matHeaderCellDef mat-sort-header>Total Amount (₹)</th>
  
  <!-- ✅ Cell: right-aligned via CSS, no symbol, tabular nums -->
  <td mat-cell *matCellDef="let row">
    <span class="cell-currency">{{ row.totalAmount | inr:false:false }}</span>
  </td>
</ng-container>
```

---

## Implementation Status

### ✅ Completed
- [x] Global CSS alignment rules (automatic via column name matching)
- [x] Tabular figures on all numeric classes
- [x] Empty state utility class (`.cell-empty`)
- [x] Header alignment helpers (`.mat-sort-header-container` adjustments)
- [x] Manual override utilities (`.cell-align-*`)
- [x] Symbol-in-header currency pattern (don't repeat symbol)

### 🚧 Future Considerations
- [ ] Accounting Format implementation (symbol at left, number at right)
- [ ] Accessibility: `aria-label` for currency cells without symbols
- [ ] Locale-specific alignment (RTL languages)

---

## Browser Compatibility

| Feature | Chrome | Firefox | Safari | Edge |
|---------|--------|---------|--------|------|
| `font-variant-numeric: tabular-nums` | ✅ 48+ | ✅ 34+ | ✅ 9.1+ | ✅ 79+ |
| `text-align: right` | ✅ All | ✅ All | ✅ All | ✅ All |
| Flexbox (sort headers) | ✅ 29+ | ✅ 28+ | ✅ 9+ | ✅ 12+ |

All target browsers (2026) fully support these features.

---

## References

- Material Design 3 Data Tables: https://m3.material.io/components/data-tables
- WCAG 2.1 Accessibility Standards for Financial Data
- Nielsen Norman Group: "Alignment in Tables" (2023)
- Edward Tufte: "The Visual Display of Quantitative Information" (alignment principles)
- CSS Fonts Level 4: `font-variant-numeric` specification

---

## Related Documentation

- [2026 Currency UX Implementation](./2026_CURRENCY_UX_IMPLEMENTATION.md)
- [Technical Standards (Section 2.6)](./TECHNICAL_STANDARDS.md#26-typography--text-formatting)
- [Copilot Instructions](../.github/copilot-instructions.md)

