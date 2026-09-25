# Software Requirements Specification — Referral & Commission Management

**Module:** Referral & Commission Management
**Application:** OneCMS / College Management System
**Client:** SKSCON / SKS College Of Nursing
**Built by:** Raster / Raster Images Pvt. Ltd.
**Release:** Release 1, Milestone 4 (Finance & Asset Management) — R1-M4.1a (Referral Type Master) and R1-M4.4 (Commission Explorer & OneBook Payment Gateway Integration)

---

## 1. Introduction

### 1.1 Purpose
This document specifies the requirements for the referral-capture-to-commission-payout flow: how an enquiry's referral source is categorized, how a commission is calculated and tracked against it, and how that commission (along with fee refunds and scholarship disbursements) can be pushed to OneBook, SKSCON's external accounting/payment application, for actual money movement.

### 1.2 Scope
In scope:
- `ReferralType` master (replaces the old `EnquirySource` enum).
- `StaffReferrer` master, scoped to an `Institution`.
- Commission calculation and storage on `Enquiry` (fields, not a separate commission entity until payout).
- Commission Explorer screen: view, approve, reject/reopen, and settle (record payout) for commission.
- `CommissionPayout` payout-history rows.
- OneBook integration: outbound payment-register push (commission, fee refund, scholarship disbursement) and the two inbound webhook callbacks.
- Settings → Integrations screen for OneBook credentials.

Out of scope (documented in sibling modules):
- Fee structure / fee collection mechanics (see `docs/requirements/fee-structure-collection/`).
- The enquiry status workflow and fee finalization narrative (see `docs/requirements/enquiry-admission-workflow/`).
- Scholarship application/disbursement lifecycle itself (only the OneBook push of an already-approved disbursement is covered here).
- Agent master and `AgentCommissionGuideline` (per-agent, per-program commission override) — these live under the `Agent` master module; only `Agent.commissionAmount`'s role as a `CommissionSource.AGENT` override is referenced here.

### 1.3 References
- `docs/RELEASE_1_MILESTONES.md`, sections R1-M4.1a (lines ~446-463) and R1-M4.4 (lines ~546-572).
- `docs/BUSINESS_REQUIREMENTS.md`, BR-4, BR-5, BR-33, BR-34.

---

## 2. Overall Description

### 2.1 Product Perspective
This module sits between the Enquiry module (source of referral data and commission accrual) and the Fee Refund / Scholarship Disbursement modules (other payment types that share the OneBook push mechanism). It is a distinct screen (Commission Explorer) plus two supporting master screens (Referral Type, Staff Referrer) plus a shared integration layer (`OneBookIntegrationService`, `OneBookConfigService`, `OneBookWebhookService`).

### 2.2 Actors / User Classes
- **Front Office / Cashier** — selects referral type (and agent/staff/faculty referrer) on the enquiry form; read-only visibility into Commission Explorer (`COMMISSION_VIEW`).
- **Admin / College Admin / Manager** (`COMMISSION_MANAGE`) — approves, rejects (with reason), reopens rejected commissions, and pushes to OneBook.
- **Accounting / Finance role** (`COMMISSION_SETTLE`) — records the actual payout once a commission is approved/`PAYMENT_REQUESTED`.
- **System Admin** (`SYSTEM_CONFIG_MANAGE`) — configures OneBook credentials in Settings → Integrations.
- **DEV_ADMIN / SUPPORT_ADMIN** — full access to all of the above via the mandatory catch-all permission sync.
- **OneBook** (external system) — calls back into OneCMS via two authenticated webhook endpoints; not a UI actor.

### 2.3 Operating Environment
Angular frontend (`frontend/src/app/features/referral-type/`, `staff-referrer/`, `commission-explorer/`, `settings/integrations/`), Spring Boot backend, PostgreSQL via Flyway, DB-driven RBAC. OneBook is an external HTTPS API secured with JWT (outbound) and a shared secret header (inbound).

### 2.4 Constraints / Assumptions
- `onebook.enabled` defaults to `false`; all credential config keys seed blank. Until a real OneBook org/branch/login is entered, commissions are settled manually via `recordPayout` (cash/other mode), never transmitted.
- OneBook JWT is not cached — a fresh token is requested on every push (documented simplicity trade-off).
- OneBook's synchronous response never carries the created register's ID or final payment outcome — both arrive later via the two webhook callbacks, correlated only by the `invoiceNumber` OneCMS itself generated.
- Referral commission is architecturally separate from the student's own fee — it must never affect `feeGuidelineTotal`/`finalCalculatedFee`/receipts (BR-5); enforced entirely on the Enquiry/Fee module side, referenced here as a hard constraint this module relies on.

---

## 3. Functional Requirements

| ID | Description | Priority | Dependencies |
|---|---|---|---|
| FR-COMM-1 | Admin can CRUD `ReferralType` records (name, code, commissionAmount, hasCommission, description, active flag) with real-time name/code uniqueness checks. | Must | none |
| FR-COMM-2 | Referral types default-seed 8 system-defined values (WALK_IN, PHONE, ONLINE, AGENT_REFERRAL, STAFF, ALUMNI, PARENT, ADVERTISEMENT) and can be soft-deactivated but not hard-deleted once referenced. | Must | FR-COMM-1 |
| FR-COMM-3 | Admin can CRUD `StaffReferrer` records, each bound to an `Institution` (FK, not free text), with name/employee-code uniqueness scoped per institution. | Must | Institution master (BR-33) |
| FR-COMM-4 | System resolves a commission amount server-side at enquiry time: Agent override (if `Agent.commissionAmount > 0`) takes precedence over the selected `ReferralType.commissionAmount`; otherwise ₹0 / `NOT_APPLICABLE`. | Must | Enquiry module |
| FR-COMM-5 | Commission Explorer lists enquiries with an accrued/tracked commission, filterable by status, source, referral type, agent, date range, and free-text search; paginated server-side. | Must | FR-COMM-4 |
| FR-COMM-6 | `COMMISSION_MANAGE` users can approve a `PENDING` (or retry a `FAILED`) commission; if OneBook is enabled this transmits immediately, otherwise it moves to `PAYMENT_REQUESTED`. | Must | FR-COMM-5, OneBook integration (if enabled) |
| FR-COMM-7 | `COMMISSION_MANAGE` users can reject a `PENDING` commission with a mandatory reason, and reopen a `REJECTED` commission back to `PENDING`. | Must | FR-COMM-6 |
| FR-COMM-8 | `COMMISSION_SETTLE` (or `COMMISSION_MANAGE`) users can record a payout against a `PAYMENT_REQUESTED`/`PARTIAL` commission; payout amount must exactly equal the outstanding commission amount (full-amount-only settlement). | Must | FR-COMM-6 |
| FR-COMM-9 | System pushes commission, fee-refund, and scholarship-disbursement payments to OneBook as a "payment register" via a JWT-authenticated outbound call, generating a unique invoice number per push. | Should (config-gated, off by default) | onebook.enabled=true, valid credentials |
| FR-COMM-10 | System exposes two authenticated inbound webhook endpoints so OneBook can report register acceptance and final payment completion/failure, correlated by `invoiceNumber`. | Should | FR-COMM-9 |
| FR-COMM-11 | Outbound push is blocked if the recipient is missing bank details (name/account/bank/IFSC); enquiry-sourced refunds are always blocked (no bank details captured on Enquiry). | Must (when OneBook enabled) | Student bank-detail fields |
| FR-COMM-12 | Admin (`SYSTEM_CONFIG_MANAGE`) can view/edit OneBook integration credentials from Settings → Integrations, with password show/hide. | Must | none |

## 4. External Interface Requirements

### 4.1 Screens
- Referral Type List/Form (`/referral-types` route) — card+table toggle, server-side pagination, uniqueness-validated form.
- Staff Referrer List/Form — institution-scoped, export (Excel/PDF).
- Commission Explorer — list with side-flyout for payout recording; approve/reject/settle actions gated by permission.
- Settings → Integrations — OneBook credential form.

### 4.2 API Endpoints (high-level; full list in FRD.md)
- `/referral-types/**`, `/staff-referrers/**`, `/commission-explorer/**`, `/webhooks/onebook/**`.

### 4.3 Key DB Entities
`ReferralType`, `StaffReferrer`, `Institution`, `CommissionPayout`, `OneBookPaymentRequest`, plus commission fields on `Enquiry` (`commissionAmount`, `commissionSource`, `commissionPaymentStatus`, `commissionPaidAmount`, `commissionNumber`, rejection fields).

## 5. Non-Functional Requirements

- **Performance:** Commission Explorer list is server-side paginated (`Page<CommissionExplorerResponse>`), default page size 25, sorted by `updatedAt` desc.
- **Security/RBAC:** Every mutating endpoint is `@PreAuthorize`-gated by a DB-driven permission (`REFERRAL_TYPE_MANAGE`, `STAFF_REFERRER_MANAGE`, `COMMISSION_MANAGE`, `COMMISSION_SETTLE`, `SYSTEM_CONFIG_MANAGE`). Inbound OneBook webhooks are authenticated by a shared secret header (`X-OneBook-Secret`), not JWT/session — appropriate since the caller is an external server, not a browser user. Bank account/IFSC/PAN/Aadhaar fields on `StaffReferrer` are sensitive PII; access is limited to `STAFF_REFERRER_MANAGE`/`VIEW` holders.
- **Auditability:** `CommissionPayout` rows retain payer, mode, transaction reference, remarks, and `paidBy`. Commission rejection stamps reason/by/at on the `Enquiry`. `OneBookPaymentRequest` retains the full raw JSON response from OneBook (`onebook_raw_response` JSONB) for audit/troubleshooting.

## 6. Known Gaps / Not Yet Implemented

- **Scholarship OneBook failures have no visible lifecycle end.** Unlike commission (`FAILED` status, retryable in Commission Explorer) and fee refund (`PAYMENT_FAILED`, retryable in Fee Refund List), a failed/rejected OneBook callback for a scholarship disbursement is logged server-side only — no field on `StudentScholarship`/`ScholarshipDisbursement` reflects it and no screen surfaces it for retry. Explicitly tracked as a follow-up, not yet designed.
- **No post-push cancellation/edit flow.** OneBook's edit/delete-register and fetch-by-id endpoints are not wired up; nothing in OneCMS cancels or edits an already-transmitted payment register. Rejection is backend-and-UI-gated to `PENDING` only.
- **Supplier Master Sync is out of scope.** That part of OneBook's shared API spec is pharmacy-only and is not implemented for OneCMS.
- **Integration is dormant by default.** `onebook.enabled=false` and all credential keys seed blank; until a college's real OneBook org/branch/login is entered, all commission settlement is manual (cash/other mode via `recordPayout`).
