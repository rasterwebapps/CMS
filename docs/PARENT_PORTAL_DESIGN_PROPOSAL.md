# Parent Portal — Design Proposal (documentation-only, not built)

**Status:** Scoping only. No migration, entity, or code changes accompany this
document. Written during the 2026-09-15/16 overnight autonomous session (item
16 of `docs/ACADEMICS_PORTAL_AUTONOMOUS_SESSION_LOG.md`) as a substitute for a
specialist-review round the session couldn't hold live — a human must actually
sign off on the decisions below (particularly cardinality and the login-linking
mechanism) before any of this is implemented. Do not treat anything here as
final.

## Why this needed a design doc instead of a build attempt

Item 15 (Student Portal, shipped this session as OC-236/237/238/239) had a
clean, low-risk path to copy: `AppUser` already carries a 1:1
`student_id`/`faculty_id` `@OneToOne` link (`AppUser.java:52`), and
`AppUserService.create` already atomically provisions a Keycloak user + linked
`app_users` row. Parent Portal has no equivalent starting point:

- `Student` has only free-text `fatherName`/`fatherPhone`/`fatherEmail`/
  `motherName`/`motherPhone`/`motherEmail`/`parentMobile` columns
  (`backend/src/main/java/com/cms/model/Student.java:135-153`) — no
  `Guardian`/`Parent` entity, no join table, no FK anywhere in the schema.
- Those email/phone fields are unvalidated free text, not unique — they
  cannot safely double as a login-matching key the way `Student.email` does
  for the student login pattern.
- The real-world cardinality is genuinely many-to-many, not 1:1: one parent
  can have multiple children (siblings) enrolled, and a student commonly has
  two parents who may each want their own separate login.
- `docs/RELEASE_2_MILESTONES.md` R2-2.5 scoped this once as a one-line goal
  ("read-only access to ward's progress... restricted by `ROLE_PARENT`") with
  no schema, and R2-2.6 (Announcements/Messaging, also referenced by R2-2.5's
  "announcements" bullet) was never built either — confirmed via
  `find backend/src/main/java/com/cms -iname "*Announcement*.java"`, zero
  results.

None of this has a safe conservative default to fall back on the way Student
Portal did (self-scoped read-only was an obvious, low-risk call there). The
data model itself — who can see whom — is the decision, not a detail
downstream of it.

## Proposed data model

```
Guardian
  id                PK
  first_name
  last_name
  email             unique, not null   -- becomes the Keycloak login-matching key
  phone
  relationship_hint -- free text, e.g. "Father"/"Mother"/"Guardian" for display only

student_guardians   -- join table, many-to-many
  student_id        FK -> students.id
  guardian_id       FK -> guardians.id
  is_primary        boolean            -- which guardian receives default notifications, if ever built
  UNIQUE (student_id, guardian_id)

app_users.guardian_id   -- new nullable FK column, OneToOne, same shape as
                           the existing student_id/faculty_id columns
```

**Why a new `Guardian` entity rather than reusing the existing free-text
columns:** the free-text `fatherEmail`/`motherEmail` fields are per-student
denormalized text with no uniqueness constraint and no history — two
different students could have parents who share an email typo, or the same
real parent could be entered with slightly different spellings across
siblings' records. A dedicated entity with a unique email gives a clean
1:1 login-matching key and lets one real parent be represented once and
linked to multiple children, instead of re-deriving identity from
freeform text every time.

**Why many-to-many via a join table, not a second FK pair added to
`Student`:** `Student` already has father/mother email columns that assume
exactly two guardians in fixed roles. A join table supports the real cases
that assumption doesn't: single-guardian households, more than two legal
guardians, and one guardian with multiple enrolled children (common at a
nursing college with siblings or repeat family enrollment) — without
widening `Student` every time a new household shape shows up.

**Migration shape (not written yet):** three additions — `guardians` table,
`student_guardians` join table, `app_users.guardian_id` nullable FK column —
would ship as one new forward migration once approved, following this repo's
existing migration-column-verification and permission-migration-pattern hard
gates exactly as OC-236's `V514` and OC-239's `V515` did for Student Portal.

## Proposed auth / provisioning model

Mirror the Student Portal precedent as closely as the different cardinality
allows:

1. Admin provisions a `Guardian` row (name, unique email, phone) and links it
   to one or more existing `Student` rows via `student_guardians` — a new
   admin-facing screen, not built yet, analogous to how student logins are
   created via the existing `POST /user-management` + `CreateUserRequest`
   flow (`AppUserService.create`).
2. `CreateUserRequest` gains a `guardianId` field alongside its existing
   `studentId`/`facultyId` fields, and `AppUserService.create` gains the same
   atomic Keycloak-user + linked-row creation it already does for students —
   no new plumbing pattern, just a third case of the one that exists.
3. A new `PARENT` `app_roles` row (hierarchy tier TBD by whoever reviews this
   — suggest below `STUDENT`'s tier 5, since it's read-only over someone
   else's data rather than a first-party account) with its own dedicated
   permissions (`MY_WARD_ATTENDANCE_VIEW`, `MY_WARD_EXAM_RESULT_VIEW`, ...),
   created via the standard permission-migration pattern (DEV_ADMIN/
   SUPPORT_ADMIN catch-all sync block included).
4. Self-service endpoints resolve the caller's `guardian_id` via
   `AppUserRepository.findByKeycloakUsername` → `getLinkedGuardian()`
   (identical shape to `getLinkedStudent()`/`getLinkedFaculty()`), then join
   through `student_guardians` to get the caller's ward IDs, then reuse the
   existing `findByStudentId` repository methods already used by Student
   Portal — **never** a client-supplied `studentId` or `guardianId`, same
   rule OC-236 enforced for students.
5. **Multi-ward selection is a real UI decision, not just a backend one:** a
   guardian with two children needs a ward switcher on the dashboard (or a
   combined view) — Student Portal never needed this since a student only
   ever sees their own single record. Flag for whoever builds this: don't
   silently default to "first ward returned by the query."

## Proposed scope for a first slice (mirroring Student Portal's own scoping choice)

Student Portal shipped My Attendance + My Exam Results first and explicitly
deferred My Timetable and fee-status as separate, harder slices. Recommend
the same staged approach here, in this order:

1. **Ward's Attendance + Ward's Exam Results** — lowest risk, direct copy of
   the `findByStudentId` pattern already proven twice (Library, Student
   Portal), just resolved through one extra join (`guardian_id` →
   `student_guardians` → `student_id`) instead of a direct FK.
2. **Ward's fee payment status** (explicitly named in R2-2.5) — needs its own
   review: `StudentFeeAllocation`/`FeeDemand`/`FeeInstallment` carry
   financial data with their own existing permission tiers
   (`FEE_*` permissions) that were never designed with a non-staff, non-
   student third-party viewer in mind. Whether a guardian should see full
   installment/refund history or only a coarse paid/pending status is a real
   decision, not a default to assume.
3. **Announcements/messaging** — out of scope entirely until R2-2.6 is built;
   there is nothing to link a Parent Portal view to yet.
4. **Ward's Timetable** — same open question OC-239 left for Student Portal's
   own `MY_TIMETABLE_VIEW`, now doubled: does a guardian get to see a ward's
   full weekly schedule? Recommend deciding the student-facing question first
   (still pending a human `MY_TIMETABLE_VIEW` → `STUDENT` grant decision
   per OC-239) before extending it to guardians.

## What this document deliberately does not do

- No migration file, entity class, or endpoint has been written.
- No `PARENT` role or permission exists in any environment.
- No claim is made about which of the above choices is correct — only that
  they are the specific open decisions blocking a safe, unattended build the
  way Student Portal had one available.
