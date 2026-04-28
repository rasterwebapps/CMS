# Global Date Formatting Standard

## Overview

The College Management System uses a single, configurable date format across the entire application. All dates are displayed in **DD-MM-YYYY** format (e.g., `28-04-2026`) by default, which can be changed globally from a single configuration file.

## Implementation Date
April 28, 2026

---

## Why Standardized Date Formatting?

### Problems with Inconsistent Dates
❌ **Before**: Multiple date formats throughout the app
- `mediumDate` → "Apr 28, 2026" (US format)
- `'dd MMM yyyy'` → "28 Apr 2026"
- `'dd MMM yy'` → "28 Apr 26"
- `'dd/MM/yyyy'` → "28/04/2026"
- No consistent format across screens

### Benefits of Standardization
✅ **After**: Single global format
- **Consistency**: All dates look identical everywhere
- **Professionalism**: Tables and reports feel cohesive
- **User Experience**: Users learn one format, not five
- **Configurability**: Change format globally in one place
- **Localization**: Format matches regional expectations (DD-MM-YYYY for India)

---

## Implementation

### 1. Date Format Configuration
**File**: `frontend/src/app/shared/config/date-format.config.ts`

**Single source of truth** for all date formats:

```typescript
export const DATE_FORMATS = {
  standard: 'dd-MM-yyyy',          // 28-04-2026 (tables, forms, displays)
  short: 'dd-MM-yy',               // 28-04-26 (compact displays)
  long: 'dd-MM-yyyy',              // 28-04-2026 (detailed views)
  dateTime: 'dd-MM-yyyy hh:mm a',  // 28-04-2026 02:30 PM (timestamps, local TZ, AM/PM)
  time: 'hh:mm a',                 // 02:30 PM (time-only displays, local TZ, AM/PM)
  rangeSeparator: ' – ',            // en-dash for date ranges
} as const;
```

**To change format globally**, edit this file:

```typescript
// Example: Switch to slash separator
standard: 'dd/MM/yyyy',  // 28/04/2026

// Example: Switch to readable format
standard: 'dd MMM yyyy',  // 28 Apr 2026

// Example: Switch to ISO format
standard: 'yyyy-MM-dd',  // 2026-04-28
```

### 2. AppDatePipe
**File**: `frontend/src/app/shared/pipes/app-date.pipe.ts`

Custom pipe that uses the global configuration:

```typescript
@Pipe({
  name: 'appDate',
  standalone: true,
})
export class AppDatePipe implements PipeTransform {
  transform(value: Date | string | number | null, format: DateFormatType = 'standard'): string {
    // Uses DATE_FORMATS[format] internally
  }
}
```

**Benefits**:
- Single import, consistent behavior
- Automatic handling of null/undefined → `'—'` (en-dash)
- Type-safe format selection
- Uses `en-IN` locale for Indian conventions

---

## Usage Guide

### Basic Usage (Standard Format)

**Template**:
```html
<td mat-cell *matCellDef="let row">
  {{ row.paymentDate | appDate }}
</td>
```

**Output**: `28-04-2026`

### Short Format (Compact Tables)

**Template**:
```html
<td mat-cell *matCellDef="let row">
  {{ row.dueDate | appDate:'short' }}
</td>
```

**Output**: `28-04-26`

### DateTime Format (Timestamps)

**Template**:
```html
<td mat-cell *matCellDef="let row">
  {{ row.createdAt | appDate:'dateTime' }}
</td>
```

**Output**: `28-04-2026 14:30`

### Date Ranges

**Template**:
```html
<span>
  {{ startDate | appDate }} {{ rangeSeparator }} {{ endDate | appDate }}
</span>
```

**Output**: `28-04-2026 – 15-05-2026`

Or use the helper:
```typescript
import { DATE_FORMATS } from '../config/date-format.config';

const range = `${startDate | appDate}${DATE_FORMATS.rangeSeparator}${endDate | appDate}`;
```

### Null/Empty Dates

**Template**:
```html
<td mat-cell *matCellDef="let row">
  {{ row.optionalDate | appDate }}
</td>
```

**Output**: `—` (en-dash, not blank)

---

## Component Setup

### Import the Pipe

```typescript
import { AppDatePipe } from '../../../shared/pipes/app-date.pipe';

@Component({
  selector: 'app-student-list',
  standalone: true,
  imports: [
    AppDatePipe,  // ← Add this
    MatTableModule,
    // ...other imports
  ],
  templateUrl: './student-list.component.html',
})
export class StudentListComponent {
  // Component logic
}
```

### Replace Old Date Pipes

**Before** (inconsistent):
```html
{{ date | date:'mediumDate' }}
{{ date | date:'dd MMM yyyy' }}
{{ date | date:'dd/MM/yyyy' }}
```

**After** (consistent):
```html
{{ date | appDate }}
{{ date | appDate }}
{{ date | appDate }}
```

All produce: `28-04-2026`

---

## Migration Checklist

### For Existing Components

- [ ] Import `AppDatePipe` in component
- [ ] Add `AppDatePipe` to `imports[]` array
- [ ] Replace `| date:'mediumDate'` with `| appDate`
- [ ] Replace `| date:'dd MMM yyyy'` with `| appDate`
- [ ] Replace `| date:'dd/MM/yyyy'` with `| appDate`
- [ ] Replace `| date:'dd MMM yy'` with `| appDate:'short'`
- [ ] Replace `| date:'dd MMM yyyy, HH:mm'` with `| appDate:'dateTime'`
- [ ] Verify empty dates show en-dash (—)

### For New Components

```html
<!-- ✅ CORRECT: Use appDate pipe -->
<ng-container matColumnDef="paymentDate">
  <th mat-header-cell *matHeaderCellDef mat-sort-header>Payment Date</th>
  <td mat-cell *matCellDef="let row">
    <span class="cell-date">{{ row.paymentDate | appDate }}</span>
  </td>
</ng-container>

<!-- ❌ INCORRECT: Don't use Angular's date pipe directly -->
<td mat-cell *matCellDef="let row">
  {{ row.paymentDate | date:'dd/MM/yyyy' }}
</td>
```

---

## Format Options Reference

| Format Type | String | Example Output | Use Case |
|-------------|--------|----------------|----------|
| `standard` | `'dd-MM-yyyy'` | `28-04-2026` | Tables, forms, most displays |
| `short` | `'dd-MM-yy'` | `28-04-26` | Compact tables, mobile views |
| `long` | `'dd-MM-yyyy'` | `28-04-2026` | Reports, certificates |
| `dateTime` | `'dd-MM-yyyy HH:mm'` | `28-04-2026 14:30` | Audit logs, payment timestamps |

### Customization Examples

Want a different format? Edit `date-format.config.ts`:

```typescript
// Readable format
standard: 'dd MMM yyyy',  // 28 Apr 2026

// Slash separator
standard: 'dd/MM/yyyy',  // 28/04/2026

// ISO format
standard: 'yyyy-MM-dd',  // 2026-04-28

// Full month name
long: 'dd MMMM yyyy',  // 28 April 2026

// 12-hour time
dateTime: 'dd-MM-yyyy hh:mm a',  // 28-04-2026 02:30 PM
```

---

## Angular DatePipe Format Reference

For reference when customizing `date-format.config.ts`:

| Symbol | Meaning | Example |
|--------|---------|---------|
| `dd` | Day of month (2 digits) | `01`, `28`, `31` |
| `d` | Day of month (1-2 digits) | `1`, `28`, `31` |
| `MM` | Month (2 digits) | `01`, `04`, `12` |
| `M` | Month (1-2 digits) | `1`, `4`, `12` |
| `MMM` | Month short name | `Jan`, `Apr`, `Dec` |
| `MMMM` | Month full name | `January`, `April`, `December` |
| `yy` | Year (2 digits) | `26` |
| `yyyy` | Year (4 digits) | `2026` |
| `EEE` | Day of week short | `Mon`, `Tue`, `Wed` |
| `EEEE` | Day of week full | `Monday`, `Tuesday` |
| `HH` | Hour 24-hour (00-23) | `00`, `14`, `23` |
| `hh` | Hour 12-hour (01-12) | `01`, `02`, `12` |
| `mm` | Minute | `00`, `30`, `59` |
| `ss` | Second | `00`, `30`, `59` |
| `a` | AM/PM marker | `AM`, `PM` |

---

## Examples from the Application

### Fee Collection Table

**Before** (inconsistent):
```html
<td>{{ row.nextDueDate | date:'dd MMM yy' }}</td>
<td>{{ row.paymentDate | date:'dd MMM yyyy' }}</td>
```

**After** (consistent):
```html
<td>{{ row.nextDueDate | appDate:'short' }}</td>
<td>{{ row.paymentDate | appDate }}</td>
```

**Output**:
- Due Date: `28-04-26`
- Payment Date: `28-04-2026`

### Academic Year Card

**Before**:
```html
{{ ay.startDate | date:'mediumDate' }} – {{ ay.endDate | date:'mediumDate' }}
```
Output: `Apr 1, 2026 – Mar 31, 2027` (US format, inconsistent)

**After**:
```html
{{ ay.startDate | appDate }}{{ DATE_FORMATS.rangeSeparator }}{{ ay.endDate | appDate }}
```
Output: `01-04-2026 – 31-03-2027` (Indian format, consistent)

### Payment Receipt

**Before**:
```html
<span>{{ receipt.paymentDate | date:'dd MMM yyyy' }}</span>
```
Output: `28 Apr 2026`

**After**:
```html
<span>{{ receipt.paymentDate | appDate }}</span>
```
Output: `28-04-2026`

---

## Best Practices

### 1. Always Use `appDate` for Display
```html
<!-- ✅ GOOD -->
{{ date | appDate }}

<!-- ❌ BAD -->
{{ date | date:'dd/MM/yyyy' }}
```

### 2. Use Appropriate Format Type
```html
<!-- ✅ GOOD: Use 'short' for compact tables -->
<td>{{ row.dueDate | appDate:'short' }}</td>

<!-- ❌ BAD: Using standard in narrow columns -->
<td style="width: 80px">{{ row.dueDate | appDate }}</td>
```

### 3. Handle Empty Dates Gracefully
```html
<!-- ✅ GOOD: Pipe handles null automatically -->
{{ row.optionalDate | appDate }}  <!-- Shows '—' if null -->

<!-- ❌ BAD: Manual null check -->
{{ row.optionalDate ? (row.optionalDate | appDate) : '—' }}
```

### 4. Date Ranges
```html
<!-- ✅ GOOD: Use rangeSeparator constant -->
{{ start | appDate }}{{ DATE_FORMATS.rangeSeparator }}{{ end | appDate }}

<!-- ❌ BAD: Hardcoded separator -->
{{ start | appDate }} - {{ end | appDate }}
```

---

## Alignment in Tables

Dates follow **left-alignment** as per the professional data table standards:

```html
<ng-container matColumnDef="paymentDate">
  <!-- Header: left-aligned (automatic via CSS) -->
  <th mat-header-cell *matHeaderCellDef mat-sort-header>Payment Date</th>
  
  <!-- Cell: left-aligned (automatic via CSS) -->
  <td mat-cell *matCellDef="let row">
    <span class="cell-date">{{ row.paymentDate | appDate }}</span>
  </td>
</ng-container>
```

**Why left-align?**
- Dates are chronological identifiers, not numeric values
- Left alignment follows natural reading flow
- Fixed-width format (`DD-MM-YYYY`) prevents shifting
- See `docs/DATA_TABLE_ALIGNMENT_STANDARDS.md` for full details

---

## Build Status

✅ **All features implemented and tested**
- Date format configuration created
- AppDatePipe created and functional
- Build successful (zero errors)
- Ready for migration

---

## Future Enhancements

### User-Specific Date Preferences
Allow users to choose their preferred format:

```typescript
// In user profile service
export class UserPreferenceService {
  dateFormat = signal<'dd-MM-yyyy' | 'dd/MM/yyyy' | 'yyyy-MM-dd'>('dd-MM-yyyy');
  
  getDateFormat(): string {
    return this.dateFormat();
  }
}

// In AppDatePipe
constructor(private userPref: UserPreferenceService) {}

transform(value: Date, format: DateFormatType = 'standard'): string {
  const formatString = this.userPref.getDateFormat();
  // Use user preference instead of global config
}
```

### Timezone Support
Add timezone handling for multi-region deployments:

```typescript
export const DATE_FORMATS = {
  standard: 'dd-MM-yyyy',
  timezone: 'Asia/Kolkata',  // Indian Standard Time
};
```

---

## Documentation References

- [Data Table Alignment Standards](./DATA_TABLE_ALIGNMENT_STANDARDS.md) — Date alignment rules
- [Technical Standards](./TECHNICAL_STANDARDS.md) — Formatting conventions
- [Copilot Instructions](../.github/copilot-instructions.md) — AI code generation rules
- [Angular DatePipe Docs](https://angular.io/api/common/DatePipe) — Format string reference

---

## Summary

🎯 **One Format, Everywhere**
- Default: `DD-MM-YYYY` (Indian standard)
- Change globally in one file
- Automatic consistency across all screens
- Professional, cohesive user experience

📝 **Simple Migration**
```typescript
// Old
{{ date | date:'mediumDate' }}

// New
{{ date | appDate }}
```

🚀 **Benefits**
- Consistency across the application
- Easy to change globally
- Automatic null handling
- Type-safe format selection
- Indian locale support

