# Screen Compliance Audit — Night Run (2026-08-25 21:00 IST)

You are running fully autonomously and unattended tonight. Nobody is
available to answer questions. Do not stop to ask for input at any point —
make the most reasonable call yourself, write it down, and keep working.

**Decision authority for tonight: CLAUDE.md's generic instructions are the
tie-breaker for every ambiguous call, not a question to raise.** Every gate
in this task (badge audit, list-screen structural gate, resizable-column
markup, mlp spacing, Component Touch Rule, permission mapping, naming
conventions) is already fully specified in CLAUDE.md and in this repo's
established precedent (the OC-139 rollout log, prior badge/spacing fix
commits). When you hit a judgment call within one of these gates, resolve it
the way CLAUDE.md and that precedent already resolve it — do not treat
"needs a decision" as a reason to pause or leave something half-done. The
**only** things that get logged as "flag for human review" instead of fixed
are the narrow cases CLAUDE.md itself carves out as requiring a product/design
call outside the gates' own rules (badge-audit item 6's example: forcing a
genuinely distinct multi-state semantic into a binary ACTIVE/INACTIVE-shaped
component) — even those get a concrete written recommendation in the log, not
just a bare flag, and work continues on everything else regardless. Getting
to the stop condition with real progress committed always beats stopping
early because of an open question.

## The task

Audit every screen in the app against three things, and fix what's
clear-cut to fix:

1. Every screen still has a working Guided Tour + Flow Map (the pattern
   rolled out in OC-139..OC-160).
2. Every screen follows CLAUDE.md's mandatory hard gates (badge/status
   audit, list-screen structural gate, resizable-column cell markup gate,
   mlp-page spacing gate, operation-wise permission mapping).
3. Every multi-tab screen (Faculty View — Profile/Professional/Courses/
   Lab Schedules/Documents — is the flagged trigger example, but check
   every tabbed screen) gets the full Component Touch Rule applied
   tab-by-tab against the latest design instructions.

**Read `docs/SCREEN_COMPLIANCE_AUDIT_2026-08-25.md` in full before writing
any code.** It has the complete step-by-step plan (Step 0 rebuild-inventory,
Step 1 tour/flowmap, Step 2 hard-gate sweep, Step 3 multi-tab deep
verification), the reasoning for why this audit was scheduled now, the hard
rules, and the stop condition. This prompt is intentionally short — that
file is the actual spec, follow it precisely rather than improvising a
different shape.

Also read `frontend/src/app/shared/tour/README.md` (tour/flowmap mechanics)
and CLAUDE.md's "Mandatory Patterns" + "List-screen structural gate" +
"Resizable-column cell markup gate" + "Shared mlp-* list-page spacing
system" + "Component Touch Rule" sections in full — they are already loaded
in your context via CLAUDE.md auto-load, but re-read them deliberately
before starting the sweep since this task hinges on applying them precisely,
not from memory.

## First: commit the setup scaffold

Run `git status`. You should see 2 new uncommitted files from setup:
`docs/SCREEN_COMPLIANCE_AUDIT_2026-08-25.md` and this prompt file plus
`scripts/screen-audit-autonomous-run.sh`. Commit these as their own first
commit (`OC-168: docs(audit): add screen compliance audit plan + autonomous
session scaffold`) before starting Step 0, so there's a clean checkpoint to
diff against. Do not commit any *other* uncommitted changes you find in the
working tree unless you put them there yourself this session — if the tree
has pre-existing unrelated WIP (this has happened before, see OC-149's
Campus Infrastructure note in `docs/TOUR_FLOWMAP_AUTONOMOUS_SESSION_LOG.md`
for the exact failure mode), leave it untouched and log it, don't fold it
into your commits.

## Priority order

Follow the audit doc's Step 0 → 1 → 2 → 3 order exactly: rebuild the real
screen inventory first (the old tour/flowmap checklist is stale for
anything shipped after 2026-08-19), then close tour/flowmap gaps, then run
the hard-gate sweep across every screen, then do the deep multi-tab pass.
Work module by module (same order as the original rollout: Overview →
Admission → Student → Finance → Academics → Library → Core Infrastructure →
Inventory → Hostel → Reports → Preferences → User Management) so a partial
night still leaves broad, real coverage rather than one module perfectly
done and the rest untouched.

Batch sensibly: check/fix 3-6 screens, run
`npx tsc -p tsconfig.app.json --noEmit`, commit, move on. Don't let one
giant uncommitted working tree accumulate.

## Hard rules — repeated here for emphasis, full detail in the audit doc

- **Local dev only.** Never touch/deploy to 172.16.7.209 / 172.17.1.243.
  Never `git push`. Never open a PR. Commit locally to `main` only.
- JIRA parent is **OC-168** (already In Progress). Sub-tickets per logical
  group of fixes via `bash scripts/jira.sh create/start/review`.
- Commit style matching existing history: `OC-XXX: fix(<module>): ...` or
  `OC-XXX: feat(tour): <module> — ...`.
- Fix clear-cut, low-risk violations on sight (wrong CSS class pointing at
  a real existing class, missing matSort bindings, missing min-width:0,
  etc.). Flag — don't force — anything that would collapse a genuinely
  distinct semantic into a shared component, or requires a real design
  decision (mirrors CLAUDE.md badge-audit item 6).

## Progress tracking — continuous, not just at the end

Every time you complete, partially complete, block, or skip a screen or
gate-check, append one line to
`docs/SCREEN_COMPLIANCE_AUDIT_2026-08-25.md`'s Progress Log section with a
real timestamp from `date`, and fill in the Current Screen Inventory table
as Step 0 builds it.

## Stop condition

See the audit doc's Stop Condition section — same standard as every prior
autonomous session on this repo: leave the tree clean/committed/tsc-clean,
write a precise handoff line if you don't finish everything, and do not
attempt to schedule a follow-up cron session yourself (crontab edits need
the user).
