# Fee Increase Prevention

**Date:** May 7, 2026  
**BR-30 Update (2026-05-21):** The prevention logic is unchanged. `finalCalculatedFee` is now sourced from the multi-dimension fee structure group (quota × feeState × gender × studentType) via `/fee-structures/guideline` at enquiry creation time. The ceiling rule (`totalFee ≤ enquiry.finalCalculatedFee`) remains the same.

---

## Business Rule

During fee finalization, fees may only be **reduced (discounted)** — never increased. The original calculated fee is the upper bound.

---

## Backend (`EnquiryService.finalizeFees()`)

```java
BigDecimal originalCalculatedFee = enquiry.getFinalCalculatedFee();
if (originalCalculatedFee == null) {
    throw new IllegalStateException(
        "Cannot finalize fees: no calculated fee found for this enquiry.");
}
if (request.totalFee().compareTo(originalCalculatedFee) > 0) {
    throw new IllegalArgumentException(
        "Fee increase is not allowed. Only discounts can be applied during finalization.");
}
```

---

## Frontend (`fee-finalization.component.ts`)

```typescript
// Block submit if any year's final amount exceeds original
protected readonly anyYearExceedsOriginal = computed(() =>
  this.yearRows().some(r => r.finalAmount > r.originalAmount)
);

protected readonly canSubmit = computed(() =>
  !this.anyYearBelowZero() &&
  !this.anyYearExceedsOriginal() &&
  !this.discountExceedsTotal() &&
  !!this.selectedEnquiry() &&
  this.yearRows().length > 0
);

// Cap input at original amount
protected updateYearAmount(index: number, raw: string): void {
  const rows = this.yearRows().map((r, i) => {
    if (i === index) {
      const cappedVal = Math.min(parseFloat(raw) || 0, r.originalAmount);
      return { ...r, finalAmount: cappedVal };
    }
    return r;
  });
  // ...
}
```

---

## Edge Cases

| Scenario | Result |
|----------|--------|
| `finalCalculatedFee` is null | Rejected — 400 Bad Request |
| Exact match (no change) | Allowed |
| Discount (reduction) | Allowed |
| Increase | Rejected — 400 Bad Request |
| Frontend bypass attempt | Backend validation catches it |

---

## Files Modified

**Backend:**
- `src/main/java/com/cms/service/EnquiryService.java`
- `src/test/java/com/cms/service/EnquiryServiceTest.java` (4 new test cases added)

**Frontend:**
- `src/app/features/finance/fee-finalization/fee-finalization.component.ts`
- `src/app/features/finance/fee-finalization/fee-finalization.component.html`

**Documentation:**
- `docs/manual-test-cases/fee-increase-prevention.md`
