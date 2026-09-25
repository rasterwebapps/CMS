# Functional Requirements Document — Referral & Commission Management

**Application:** OneCMS / College Management System | **Client:** SKSCON / SKS College Of Nursing | **Built by:** Raster / Raster Images Pvt. Ltd.

---

## 1. Overview

Covers three backend feature areas sharing one business thread — referral capture → commission tracking → OneBook payout — plus the two supporting masters (Referral Type, Staff Referrer/Institution) and the Integrations settings screen.

## 2. Actors & Permissions

Exact permission strings (grepped from controllers and migrations):

| Permission | Grants | Seeded to (catch-all) |
|---|---|---|
| `REFERRAL_TYPE_VIEW` | Read referral types | all admin-tier roles |
| `REFERRAL_TYPE_MANAGE` | Create/update/delete/status-toggle referral types | DEV_ADMIN, SUPPORT_ADMIN, ADMIN, COLLEGE_ADMIN (V172) |
| `REFERRAL_TYPE_CREATE` / `REFERRAL_TYPE_EDIT` / `REFERRAL_TYPE_DELETE` / `REFERRAL_TYPE_EXPORT` | Granular V242 screen-level permissions (Master category, tier 4) — coexist with the broader `REFERRAL_TYPE_MANAGE` under the tiered permission model | per Permission Model V2 (BR-39) |
| `STAFF_REFERRER_VIEW` | Read staff referrers | DEV_ADMIN, SUPPORT_ADMIN, ADMIN, COLLEGE_ADMIN, FRONT_OFFICE, CASHIER (read-only) |
| `STAFF_REFERRER_MANAGE` | Full CRUD on staff referrers | DEV_ADMIN, SUPPORT_ADMIN, ADMIN, COLLEGE_ADMIN |
| `STAFF_REFERRER_EXPORT` | Export staff referrer list (Excel/PDF) | — (grep controller only; verify role grants via Role Management) |
| `INSTITUTION_VIEW` / `INSTITUTION_MANAGE` | Read/manage the Institution master (V232) | — |
| `COMMISSION_VIEW` | Read-only Commission Explorer access | DEV_ADMIN, SUPPORT_ADMIN, ADMIN, COLLEGE_ADMIN, FRONT_OFFICE, CASHIER |
| `COMMISSION_MANAGE` | Approve / reject / reopen a commission; also allowed to record payout | DEV_ADMIN, SUPPORT_ADMIN, ADMIN, COLLEGE_ADMIN |
| `COMMISSION_SETTLE` | Record commission payout settlement (separate from approve/reject) | DEV_ADMIN, SUPPORT_ADMIN, ADMIN, COLLEGE_ADMIN (catch-all only; further role grants via DB Role Management) |
| `COMMISSION_EXPORT` | Export Commission Explorer list | — |
| `SETTINGS_MANAGE` | Gates all of `SystemConfigurationController`, including OneBook integration config (Settings → Integrations) — note: BR-34's text names this `SYSTEM_CONFIG_MANAGE`; the shipped code actually enforces `SETTINGS_MANAGE`. Documented here from code as the authoritative value. | — |

A user without `COMMISSION_VIEW`/`COMMISSION_MANAGE` (i.e. `COMMISSION_SETTLE`-only) sees Commission Explorer filtered to only `PAYMENT_REQUESTED`/`PARTIAL` rows (settlement queue only) — enforced in `CommissionExplorerService.findAll`/`findPage` via `PermSecurityBean`.

OneBook's two inbound webhooks (`/webhooks/onebook/posting-track-update`, `/posting-track-completion`) are **not** permission-gated (no logged-in user) — authenticated instead by the `X-OneBook-Secret` header checked against `onebook.webhook_secret`.

## 3. Screens & UI Behavior

### 3.1 Referral Type List/Form (`frontend/src/app/features/referral-type/`)
- List: server-side paginated (`/referral-types/page`), card+table toggle, search.
- Form fields: Name*, Code* (uniqueness-checked live against `/referral-types/name-exists` and `/code-exists`), Commission Amount* (BigDecimal), Has Commission (toggle), Description, Active (toggle). Preview card shown while editing.
- System-defined seeded types (`isSystemDefined=true`) — code does not prevent editing/deactivating them via the API observed; no special UI lock was found in the controller/service read, so treat as documented-but-not-independently-verified for the UI layer.

### 3.2 Staff Referrer List/Form (`frontend/src/app/features/staff-referrer/`)
- Form fields: Name*, Phone, Email, Employee Code* (max 50, uniqueness-checked per-institution via `/staff-referrers/name-exists` and `/employee-code-exists`, both requiring `institutionId`), Institution* (FK dropdown, required), Commission Amount, Active, PAN Number, Aadhaar Number, Bank Account Number, Bank IFSC Code, Bank Branch, Bank Name, Bank Account Holder, Bank Account Type.
- List: search, export (Excel/PDF) via `STAFF_REFERRER_EXPORT`.

### 3.3 Commission Explorer (`frontend/src/app/features/commission-explorer/`)
- List columns include: student name, admission number, program/course, referral type, commission source, agent/staff/faculty name, commission amount, paid amount, outstanding, payment status, OneBook reference/status/txn-id (when applicable), rejection reason/by/at (when `REJECTED`).
- Filters: status, source, referral type, agent, date range, free-text search.
- Row actions: Approve, Reject (opens reason dialog), Reopen (only visible on `REJECTED` rows), Record Payout (side-flyout form: amount, payout date, payment mode, transaction reference, remarks) — payout amount pre-filled/validated to equal outstanding.
- Export button (`COMMISSION_EXPORT`).

### 3.4 Settings → Integrations (`frontend/src/app/features/settings/integrations/`)
- OneBook credential form: enabled toggle, API URL, username, password (show/hide), org ID, branch ID, app name, paper name, zone name, webhook secret, integration date (informational).

## 4. Functional Workflows

### 4.1 Referral Type CRUD
`POST/PUT /referral-types` → validate name/code uniqueness → save. `PATCH /{id}/status`, `PUT /{id}/deactivate`, `PUT /{id}/reactivate` toggle `isActive`.

### 4.2 Staff Referrer CRUD
`POST/PUT /staff-referrers` → validate name/employee-code uniqueness scoped to `institutionId` → save with bank/PAN/Aadhaar fields.

### 4.3 Commission lifecycle (see BRD §4.2 for the full state diagram)
1. `POST /commission-explorer/{enquiryId}/approve` — valid only from `PENDING`/`FAILED`; if `onebook.enabled`, calls `OneBookIntegrationService.pushCommissionPayment`; else sets `PAYMENT_REQUESTED`.
2. `POST /commission-explorer/{enquiryId}/reject` — valid only from `PENDING`; body `{reason}` required non-blank; stamps rejection metadata.
3. `POST /commission-explorer/{enquiryId}/reopen` — valid only from `REJECTED`; resets to `PENDING`.
4. `POST /commission-explorer/{enquiryId}/payouts` — valid only from `PAYMENT_REQUESTED`/`PARTIAL`; body `{amount, payoutDate, paymentMode, transactionReference, remarks}`; **amount must exactly equal outstanding** or the request is rejected (`IllegalArgumentException`); creates a `CommissionPayout` row linked to the resolved Agent/StaffReferrer/Faculty via `Enquiry.commissionSource`; sets `commissionPaidAmount = commissionAmount` and status `PAID`.

### 4.4 OneBook outbound push (`OneBookIntegrationService`)
1. `authenticate()` → `POST {api_url}/authserver/api/auth` `{Username, password, branchId, organizationId, zoneName}` → JWT (not cached).
2. Generate invoice number via `ApplicationNumberSequenceService` (`COM-yyyy-NNNNN` / `RFD-yyyy-NNNNN` / `DSB-yyyy-NNNNN`).
3. Create `OneBookPaymentRequest` row (`PENDING`) with recipient bank details, amount, `invoiceNumber`, internal `referenceId` (`OB-yyyyMMdd-XXXXXXXX`).
4. `POST {api_url}/one-book/api/payment-registers-add-from-other-applications`, `Authorization: Bearer {jwt}`, body = one-element JSON array (field mapping per BR-34 table). 2xx → `TRANSMITTED`; else → `FAILED`.
5. **Bank-detail guard**: pre-call check for recipient name/account/bank/IFSC; enquiry-sourced refunds always blocked.

### 4.5 OneBook inbound callbacks (`OneBookWebhookService`)
- `processPostingTrackUpdate(payload)` — matches by `invoiceNumber`/`documentNumber`; stores `oneBookPaymentRegisterId`, comment.
- `processPostingTrackCompletion(payload)` — maps OneBook status → internal status (`PAID`/`FAILED`/`PROCESSING`); on `PAID`, propagates to the source entity (Enquiry commission, FeeRefund, or creates the ScholarshipDisbursement record from stored request metadata) and sets its number field to the already-generated invoice number.
- Both process each array entry independently; unmatched entries are logged and skipped, not failed.

## 5. API Endpoints

| Method | Path | Request / Response (shape) | Permission |
|---|---|---|---|
| GET | `/referral-types` | `?activeOnly=` → `List<ReferralTypeResponse>` | none (read) |
| GET | `/referral-types/page` | `?search=&page=&size=&sort=` → `Page<ReferralTypeResponse>` | none |
| GET | `/referral-types/{id}` | → `ReferralTypeResponse` | none |
| POST | `/referral-types` | `ReferralTypeRequest` → `ReferralTypeResponse` (201) | `REFERRAL_TYPE_MANAGE` |
| PUT | `/referral-types/{id}` | `ReferralTypeRequest` → `ReferralTypeResponse` | `REFERRAL_TYPE_MANAGE` |
| DELETE | `/referral-types/{id}` | → 204 | `REFERRAL_TYPE_MANAGE` |
| PATCH | `/referral-types/{id}/status` | `ActiveStatusUpdateRequest` → `ActiveStatusUpdateResponse` | `REFERRAL_TYPE_MANAGE` |
| PUT | `/referral-types/{id}/deactivate` \| `/reactivate` | → `ReferralTypeResponse` | `REFERRAL_TYPE_MANAGE` |
| GET | `/referral-types/name-exists` \| `/code-exists` | `?value=&excludeId=` → `Boolean` | `REFERRAL_TYPE_MANAGE` |
| GET | `/staff-referrers` | `?active=` → `List<StaffReferrerResponse>` | none (read) |
| GET | `/staff-referrers/page` | → `Page<StaffReferrerResponse>` | none |
| GET | `/staff-referrers/{id}` | → `StaffReferrerResponse` | none |
| POST | `/staff-referrers` | `StaffReferrerRequest` → `StaffReferrerResponse` (201) | `STAFF_REFERRER_MANAGE` |
| PUT | `/staff-referrers/{id}` | → `StaffReferrerResponse` | `STAFF_REFERRER_MANAGE` |
| DELETE | `/staff-referrers/{id}` | → 204 | `STAFF_REFERRER_MANAGE` |
| PATCH | `/staff-referrers/{id}/status` | → `ActiveStatusUpdateResponse` | `STAFF_REFERRER_MANAGE` |
| PUT | `/staff-referrers/{id}/deactivate` \| `/reactivate` | → `StaffReferrerResponse` | `STAFF_REFERRER_MANAGE` |
| GET | `/staff-referrers/export` | `?format=excel|pdf&search=&sort=&direction=` → file bytes | `STAFF_REFERRER_EXPORT` |
| GET | `/staff-referrers/name-exists` \| `/employee-code-exists` | `?value=&institutionId=&excludeId=` → `Boolean` | `STAFF_REFERRER_MANAGE` |
| GET | `/commission-explorer` | filters (`status,source,referralTypeId,agentId,fromDate,toDate,search`) → `Page<CommissionExplorerResponse>` | `COMMISSION_VIEW`\|`COMMISSION_MANAGE`\|`COMMISSION_SETTLE` |
| GET | `/commission-explorer/export` | → file bytes | `COMMISSION_EXPORT` |
| POST | `/commission-explorer/{enquiryId}/approve` | → `CommissionExplorerResponse` | `COMMISSION_MANAGE` |
| POST | `/commission-explorer/{enquiryId}/reject` | `CommissionRejectionRequest{reason}` → `CommissionExplorerResponse` | `COMMISSION_MANAGE` |
| POST | `/commission-explorer/{enquiryId}/reopen` | → `CommissionExplorerResponse` | `COMMISSION_MANAGE` |
| POST | `/commission-explorer/{enquiryId}/payouts` | `CommissionPayoutRequest{amount,payoutDate,paymentMode,transactionReference,remarks}` → `CommissionExplorerResponse` | `COMMISSION_SETTLE`\|`COMMISSION_MANAGE` |
| PUT | `/webhooks/onebook/posting-track-update` | Array of `OneBookPostingTrackUpdatePayload` → `{"message":"true"}` | `X-OneBook-Secret` header |
| PUT | `/webhooks/onebook/posting-track-completion` | Array of `OneBookPostingTrackCompletionPayload` → `{"message":"true"}` | `X-OneBook-Secret` header |
| (various) | `/system-configurations/**` | OneBook + other config keys | `SETTINGS_MANAGE` |
| (various) | `/institutions/**` | Institution master CRUD | `INSTITUTION_VIEW`/`INSTITUTION_MANAGE` |

## 6. Data Model

**`referral_types`** — `id`, `name` (unique), `code` (unique), `commission_amount` (NUMERIC(10,2)), `has_commission` (BOOLEAN), `description`, `is_active`, `is_system_defined`, `created_at`, `updated_at`.

**`staff_referrers`** — `id`, `name`, `phone`, `email`, `employee_code` (NOT NULL), `institution_id` (FK → `institutions`, NOT NULL), `commission_amount` (NUMERIC(12,2)), `is_active`, `pan_number`, `aadhaar_number`, `bank_account_number`, `bank_ifsc_code`, `bank_branch`, `bank_name`, `bank_account_holder`, `bank_account_type` (enum), `created_at`, `updated_at`. Unique on `(institution_id, LOWER(name))` and `(institution_id, LOWER(employee_code))`.

**`institutions`** — `id`, `name`, `code`, `description`, `is_active` (V232).

**`commission_payouts`** — `id`, `enquiry_id` (FK, NOT NULL), `agent_id` (FK, nullable), `staff_referrer_id` (FK, nullable), `referred_faculty_id` (FK, nullable), `amount` (NUMERIC(12,2)), `payout_date`, `payment_mode` (enum), `transaction_reference`, `remarks`, `paid_by`, `created_at`, `updated_at`.

**`enquiries`** (relevant commission columns only) — `referral_type_id` (FK), `commission_amount`, `commission_source` (enum: `AGENT`/`STAFF_REFERRER`/`FACULTY_REFERRER`/`REFERRAL_TYPE`/`NONE`), `commission_payment_status` (enum: `NOT_APPLICABLE`/`PENDING`/`PAYMENT_REQUESTED`/`PARTIAL`/`PAID`/`TRANSMITTED`/`PROCESSING`/`FAILED`/`REJECTED`), `commission_paid_amount`, `commission_number`, `commission_rejection_reason`, `commission_rejected_by`, `commission_rejected_at`, `referred_staff_id`, `referred_faculty_id`, `agent_id`.

**`onebook_payment_requests`** — `id`, `reference_id` (unique, internal `OB-yyyyMMdd-XXXXXXXX`), `payment_type` (`COMMISSION`/`REFUND`/`SCHOLARSHIP`), `entity_id`, `entity_table`, recipient bank fields, `amount`, `status`, `error_message`, `transmitted_at`, `onebook_txn_id`, `onebook_status`, `onebook_paid_date`, `onebook_payment_mode`, `onebook_remarks`, `onebook_raw_response` (JSONB), `invoice_number` (V236), `onebook_payment_number`, `onebook_bank_name`, `onebook_payment_by`, `onebook_batch_number`, `approved_by`, `approved_at`, timestamps. Indexed on `(entity_id, payment_type)`, `status`, `reference_id`, `invoice_number`.

**`system_configurations`** — generic key/value store; `onebook.*` keys (`enabled`, `allow_cash_in_cms`, `api_url`, `username`, `password`, `org_id`, `branch_id`, `app_name`, `paper_name`, `webhook_secret`, `zone_name`).

## 7. Edge Cases & Validation Rules

- Approve/reject/reopen/payout each hard-enforce their required source status server-side (`IllegalStateException` on mismatch) — the UI must not be the only gate.
- Rejection reason is mandatory; blank/whitespace-only is rejected.
- Payout amount must equal outstanding exactly — over- or under-payment requests are rejected (`IllegalArgumentException`); there is no partial-commission-payout path.
- A `COMMISSION_SETTLE`-only user's list view is implicitly filtered to `PAYMENT_REQUESTED` when no explicit status filter is supplied.
- OneBook push is blocked pre-flight (before any HTTP call) on missing recipient bank details; enquiry-sourced refunds are unconditionally blocked since `Enquiry` has no bank-detail fields.
- Webhook entries are processed independently — a malformed/unmatched entry in a batch is logged and skipped without failing sibling entries or the HTTP response (always 200 `{"message":"true"}` once the secret check passes).
- Institution-scoped uniqueness means the same referrer name can legitimately exist at two different institutions.
- `REFERRAL_TYPE_MANAGE` gates even the `name-exists`/`code-exists` uniqueness-check endpoints (they are not anonymous/public), unlike some other masters' uniqueness endpoints in this codebase — verified directly from the controller's `@PreAuthorize` placement.

## 8. Known Gaps / Deferred

- Scholarship disbursement OneBook failure has no dedicated status field or retry screen (see BRD/SRS).
- No cancel/edit of a transmitted OneBook register.
- `REFERRAL_TYPE_MANAGE` vs. the newer granular `REFERRAL_TYPE_CREATE`/`EDIT`/`DELETE`/`EXPORT` permissions (V242) appear to coexist; how the tiered permission model (BR-39) reconciles the broad `_MANAGE` permission with the granular ones for this specific screen was not traced end-to-end in `PermSecurityBean` — flagged as an area to verify if inconsistent behavior is reported.
