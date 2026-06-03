# Transaction ID Mandatory Validation

**Date:** May 5, 2026

---

## Business Rule

Transaction ID (reference number) is **mandatory** when paying via UPI, Bank Transfer, or Cheque. It is optional for Cash, Card, Net Banking, Demand Draft, and Scholarship.

---

## Backend

### Custom Validation Annotation
- `TransactionReferenceRequired.java` — class-level Bean Validation annotation
- `TransactionReferenceValidator.java` — validates `transactionReference` is present when payment mode requires it

### DTOs Updated
- `EnquiryPaymentRequest.java` — added `@TransactionReferenceRequired`
- `TermFeePaymentRequest.java` — added `@TransactionReferenceRequired` + `transactionReference` field (was missing)
- `CollectPaymentRequest.java` — added `@TransactionReferenceRequired`

### Model
- `TermFeePayment.java` — added `transactionReference` field (column `transaction_reference`)

### Response DTO
- `TermFeePaymentDto.java` — added `transactionReference` to response

### Services
- `TermFeePaymentServiceImpl.java` — set + map `transactionReference`
- `FeeReportService.java` — include `transactionReference` in reports

### Database Migration
- `V96__add_transaction_reference_to_term_fee_payments.sql` — adds `transaction_reference VARCHAR(255)` (nullable for backward compat)

> Other payment tables (`fee_payments`, `enquiry_payments`, `fee_installments`) already had this column.

---

## Frontend

### Shared Validator
- `frontend/src/app/shared/validators/transaction-reference-validator.ts` — `transactionReferenceRequiredValidator()` Angular validator; dynamically re-validates when payment mode changes

### Components Updated
All three payment collection flows updated:
1. `fee-collection.component.ts/.html` — Finance module fee collection
2. `collect-payment-dialog.component.ts/.html` — Student detail collect payment dialog
3. `fee-payment-dialog.component.ts` — Academic year fee demand record payment (inline template)

### TypeScript Interfaces
- `academic-year.model.ts` — added `transactionReference?: string` to `TermFeePaymentRequest` and `TermFeePayment`

---

## Error Messages

| Context | Message |
|---------|---------|
| Frontend | "Transaction reference is required for this payment mode" |
| Backend (400) | "Transaction reference is required for UPI, Bank Transfer, and Cheque payments" |
| Hint text | "Required for UPI, Bank Transfer, and Cheque payments" |

---

## Files Changed

**Backend (10 files):** `TransactionReferenceRequired.java`, `TransactionReferenceValidator.java`, `EnquiryPaymentRequest.java`, `TermFeePaymentRequest.java`, `CollectPaymentRequest.java`, `TermFeePayment.java`, `TermFeePaymentDto.java`, `TermFeePaymentServiceImpl.java`, `FeeReportService.java`, `V96__...sql`

**Frontend (7 files):** `transaction-reference-validator.ts`, `fee-collection.component.ts/.html`, `collect-payment-dialog.component.ts/.html`, `fee-payment-dialog.component.ts`, `academic-year.model.ts`

**Documentation:** `docs/manual-test-cases/transaction-id-validation.md`
