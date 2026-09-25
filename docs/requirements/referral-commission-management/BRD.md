# Business Requirements Document — Referral & Commission Management

**Application:** OneCMS / College Management System | **Client:** SKSCON / SKS College Of Nursing | **Built by:** Raster / Raster Images Pvt. Ltd.

---

## 1. Executive Summary / Business Objective

SKSCON works with agents, staff of sister institutions, and its own faculty who refer prospective students. The college needs to (a) categorize *how* every enquiry arrived without hardcoding source types in application code, (b) track any commission owed to the referring party strictly separately from the student's own fee, and (c) run that commission — along with fee refunds and scholarship disbursements — through the college's external accounting system (OneBook) for actual payment, rather than settling everything by hand. This module delivers the Referral Type master, the Staff Referrer master, the Commission Explorer approval/settlement workflow, and the OneBook payment-gateway integration.

## 2. Stakeholders

- **Front Office / Admissions staff** — record the referral source at enquiry time.
- **Admissions Manager / College Admin** — approve or reject commission payouts.
- **Accounts/Finance team** — settle (pay out) approved commissions, either manually or via OneBook.
- **Agents, Staff Referrers, Faculty Referrers** — the external/internal parties owed a commission.
- **OneBook** — SKSCON's external accounting application that actually moves money and reports outcomes back.
- **System Administrator** — configures OneBook credentials and Institution master data.

## 3. Business Rules

| ID | Rule | Rationale |
|---|---|---|
| BR-COMM-1 | Referral categorization is a standalone, admin-managed `ReferralType` master (not a hardcoded enum); `Enquiry.source` was removed and `referralType` FK is the sole tracking field. | Lets the college add/retire referral categories without a code change. |
| BR-COMM-2 | Each `ReferralType` carries `hasCommission` + `commissionAmount`; when `hasCommission=true`, the amount pre-fills as an editable referral additional amount on the enquiry form. | Encodes which referral channels are commission-bearing at the master-data level. |
| BR-COMM-3 | Only active referral types appear in the enquiry form dropdown; types can be deactivated (soft delete) but not force-deleted once referenced. | Preserves historical enquiry data integrity while letting stale categories be retired. |
| BR-COMM-4 | Selecting `AGENT_REFERRAL` as the referral type reveals an Agent picker on the enquiry form. | Only agent referrals need a specific-agent link for commission attribution. |
| BR-COMM-5 | Commission is resolved server-side: an Agent's own `commissionAmount` override (if positive) wins over the Referral Type's amount; otherwise commission is ₹0 and status is `NOT_APPLICABLE`. Client-submitted commission values are never trusted. | Prevents a manipulated client payload from inflating a payout; keeps a single source of truth. |
| BR-COMM-6 | Commission is tracked entirely separately from student fee — it must never increase `feeGuidelineTotal`, `finalCalculatedFee`, fee finalization totals, payment dues, receipts, or student fee allocations. | A referral payout is an institutional expense, not a student charge; conflating the two would corrupt fee accounting. |
| BR-COMM-7 | Staff Referrers must be linked to a known sister-concern `Institution` (admin-managed master), not free text; name/employee-code uniqueness is scoped per institution, not global. | Prevents duplicate/ambiguous referrer records across institutions sharing common names. |
| BR-COMM-8 | Commission lifecycle: `PENDING` → approve → (`PAYMENT_REQUESTED` if OneBook disabled, or `TRANSMITTED`/`PROCESSING`/`PAID`/`FAILED` if OneBook enabled) → settle. `PENDING` can instead be `REJECTED` (reason required) → `reopen` back to `PENDING`. | Gives admins a deliberate approve/reject gate before money moves, with an audit trail for rejections. |
| BR-COMM-9 | Settlement (`recordPayout`) requires the payout amount to exactly equal the outstanding commission — no partial payouts. | Matches the "full-amount-only payout settlement" design decision; avoids reconciliation complexity of partial commission payments. |
| BR-COMM-10 | `COMMISSION_SETTLE` is a distinct permission from `COMMISSION_MANAGE` — a user can be granted payout-recording rights without approve/reject authority, or vice versa. | Operation-wise permission separation (segregation of duties between approval and disbursement). |
| BR-COMM-11 | Three payment types (commission, fee refund, scholarship disbursement) route through one shared OneBook "payment register" mechanism; OneCMS never self-marks a payment as paid — it transmits and waits for OneBook's callback. | OneBook is the system of record for actual money movement; OneCMS must not claim a payment succeeded that it didn't itself execute. |
| BR-COMM-12 | A push is blocked before any API call if the recipient is missing bank name/account/IFSC/account-holder; enquiry-sourced refunds (no bank details captured) are always blocked from OneBook push. | Prevents transmitting an unpayable register to OneBook. |
| BR-COMM-13 | The integration is config-gated (`onebook.enabled`, default `false`) and all credentials seed blank — dormant until a college's real OneBook login is entered. | Integration must not activate accidentally in a fresh/uncofigured environment. |
| BR-COMM-14 | Inbound OneBook callbacks are correlated purely by the `invoiceNumber` OneCMS itself generated and sent — the one identifier guaranteed unique across both systems. | OneBook's synchronous response carries no register ID, so a self-generated correlation key is required. |

## 4. Business Process / Workflow Narrative

### 4.1 Referral capture (at enquiry time)
1. Front office selects a `ReferralType` (required field) on the enquiry form.
2. If the type's `hasCommission=true`, the commission amount is pre-filled (editable) as the referral additional amount; if `AGENT_REFERRAL`, an Agent dropdown appears.
3. On save, the backend recomputes commission server-side (Agent override → Referral Type amount → ₹0), setting `Enquiry.commissionAmount`, `commissionSource` (`AGENT`/`STAFF_REFERRER`/`FACULTY_REFERRER`/`REFERRAL_TYPE`/`NONE`), and `commissionPaymentStatus` (`NOT_APPLICABLE` or `PENDING`).

### 4.2 Commission Explorer lifecycle
```
PENDING --approve--> PAYMENT_REQUESTED (OneBook disabled)
PENDING --approve--> TRANSMITTED -> PROCESSING -> PAID   (OneBook enabled, success path)
                                              \-> FAILED (retryable: re-approve)
PENDING --reject(reason)--> REJECTED --reopen--> PENDING
PAYMENT_REQUESTED / PARTIAL --recordPayout(full amount)--> PAID
```
- Approve is only valid from `PENDING` or `FAILED` (retry).
- Reject is only valid from `PENDING`, requires a non-blank reason, and stamps `commissionRejectedBy`/`commissionRejectedAt`.
- Settlement (`recordPayout`) requires the request amount to equal the current outstanding balance exactly; it creates a `CommissionPayout` row linked to the resolved agent/staff-referrer/faculty and marks the enquiry `PAID`.

### 4.3 OneBook push (when enabled)
1. Generate an invoice number (`COM-yyyy-NNNNN` for commission, similarly `RFD-`/`DSB-` for refund/scholarship) via `ApplicationNumberSequenceService` — generated once, reused verbatim as the domain entity's own number only on confirmed success.
2. Authenticate: `POST {api_url}/authserver/api/auth` with username/password/branchId/organizationId/zoneName → JWT (not cached, fetched per push).
3. Create an `OneBookPaymentRequest` row (`PENDING`) with recipient bank details and the generated invoice/reference numbers.
4. `POST {api_url}/one-book/api/payment-registers-add-from-other-applications` with the JWT and a one-element array payload. Any 2xx → `TRANSMITTED`; non-2xx/exception → `FAILED`. The response carries no register ID.
5. OneBook later calls back:
   - `PUT /webhooks/onebook/posting-track-update` — delivers OneBook's own register ID (`oneBookPaymentRegisterId`).
   - `PUT /webhooks/onebook/posting-track-completion` — delivers final status (`SUCCESS`/`COMPLETED`/`PAID` → `PAID`; `FAILED`/`REJECTED`/`CANCELLED`/`ERROR` → `FAILED`; anything else → `PROCESSING`), payment number, bank name, mode, transaction number, date, paid-by, batch number.
6. On `PAID`, the commission's `Enquiry.commissionPaymentStatus` becomes `PAID` and `commissionNumber` is set to the invoice number generated in step 1.

### 4.4 Calculation Formula
```
Commission Payable = Agent.commissionAmount (if Agent linked AND > 0)
                    OR ReferralType.commissionAmount (if hasCommission = true)
                    OR ₹0  (commissionPaymentStatus = NOT_APPLICABLE)
```
This value is never added to the student's fee total.

## 5. Success Criteria

Not formally defined with numeric KPIs in project documentation — inferred from feature completeness: every enquiry with a commission-bearing referral type has a trackable, auditable commission record from accrual through settlement, and (when configured) that settlement can flow through OneBook without manual bank transfers being tracked outside the system.

## 6. Assumptions & Constraints

- Assumes OneBook's published Payment Register API contract as the integration source of truth (per BR-34's note that an earlier placeholder-field-name build was confirmed wrong and replaced).
- Assumes only one active `AUTO_EXCESS`-style special case does not apply here (that's the fee-refund module) — commission payouts are always full-amount.
- Institution scoping for Staff Referrer assumes SKSCON's sister-concern institutions are a bounded, admin-managed list, not open text.
- JWT not being cached is a documented simplicity trade-off, not an oversight — noted as acceptable given OneBook push frequency is low (per-commission/refund/scholarship, not high-volume).

## 7. Known Gaps / Deferred

- Scholarship disbursement failure/rejection has no UI surface or retry path (see SRS §6).
- No cancel/edit of an already-transmitted OneBook register.
- Supplier Master Sync (pharmacy-only) not implemented.
