#!/usr/bin/env bash
# =============================================================================
#  Local regression gate — no cloud CI, no GitHub Actions.
#
#  Point it at a branch, get one PASS/FAIL verdict before you decide to deploy
#  that branch to 209 (live). It:
#    1. Checks out the branch into an isolated worktree (never touches your
#       current working tree / uncommitted work).
#    2. Runs the full backend test suite (./gradlew test).
#    3. Deploys the branch to the 243 test server (scripts/deploy-243.sh) —
#       this doubles as the migration-boot verification hard gate: a real
#       Flyway-migrated Postgres boot, not gradle's create-drop schema. A
#       checksum mismatch or bad migration fails LOUD here instead of
#       crash-looping the backend silently in prod.
#    4. Runs the Playwright e2e suite (e2e/) against the freshly deployed 243
#       build: the whole-app route crawler + the hand-authored deep specs
#       seeded from docs/manual-test-cases/.
#
#  Usage:
#    ./scripts/regression-gate.sh <branch-name>
#
#  Requires e2e/.env to be filled in first (copy from e2e/.env.example) with
#  real Keycloak test-account credentials for the 243 server.
# =============================================================================

set -euo pipefail

BRANCH="${1:-}"
if [[ -z "$BRANCH" ]]; then
  echo "Usage: $0 <branch-name>" >&2
  exit 1
fi

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
WORKTREE_DIR="$REPO_ROOT/.worktrees/regression-gate"
REPORT_DIR="$REPO_ROOT/.worktrees/regression-gate-reports/$(date +%Y%m%d-%H%M%S)-${BRANCH//\//_}"
mkdir -p "$REPORT_DIR"

SUMMARY_FILE="$REPORT_DIR/summary.txt"
STEP_STATUS=()

log_step() {
  echo ""
  echo "════════════════════════════════════════════════════════════"
  echo "  $1"
  echo "════════════════════════════════════════════════════════════"
}

record() {
  local name="$1" status="$2"
  STEP_STATUS+=("$name: $status")
  echo "$name: $status" >>"$SUMMARY_FILE"
}

# ── Step 0: isolated worktree for the target branch ─────────────────────────
log_step "Checking out '$BRANCH' into an isolated worktree"
if [[ -d "$WORKTREE_DIR" ]]; then
  git -C "$REPO_ROOT" worktree remove --force "$WORKTREE_DIR" 2>/dev/null || rm -rf "$WORKTREE_DIR"
fi
git -C "$REPO_ROOT" fetch origin "$BRANCH" 2>/dev/null || true
git -C "$REPO_ROOT" worktree add "$WORKTREE_DIR" "$BRANCH"

# ── Step 1: backend test suite ───────────────────────────────────────────────
log_step "Backend: ./gradlew test"
if (cd "$WORKTREE_DIR/backend" && ./gradlew test --console=plain >"$REPORT_DIR/gradle-test.log" 2>&1); then
  record "backend-tests" "PASS"
else
  record "backend-tests" "FAIL (see $REPORT_DIR/gradle-test.log)"
fi

# ── Step 2: deploy branch to 243 (also = migration-boot verification) ───────
log_step "Deploying '$BRANCH' to 243 test server (full rebuild)"
if (cd "$WORKTREE_DIR" && ./scripts/deploy-243.sh full 2>&1 | tee "$REPORT_DIR/deploy-243.log"); then
  # deploy-243.sh doesn't itself exit non-zero on a bad health check line, so
  # check the captured health-check output explicitly.
  if grep -qE 'Backend API: *(4|5)[0-9]{2}|Keycloak \(OIDC\): *(4|5)[0-9]{2}|Frontend \(HTTPS\): *0' "$REPORT_DIR/deploy-243.log"; then
    record "deploy-243-health" "FAIL — a service didn't come up clean after deploy (checksum mismatch / crash-loop class of bug). See $REPORT_DIR/deploy-243.log"
  else
    record "deploy-243-health" "PASS"
  fi
else
  record "deploy-243-health" "FAIL (deploy script errored — see $REPORT_DIR/deploy-243.log)"
fi

# ── Step 3: e2e suite against the freshly deployed build ────────────────────
log_step "Running Playwright e2e suite against 243"
if [[ ! -f "$REPO_ROOT/e2e/.env" ]]; then
  echo "WARNING: e2e/.env not found — copy e2e/.env.example and fill in 243 test credentials." | tee -a "$SUMMARY_FILE"
  record "e2e-suite" "SKIPPED (no e2e/.env)"
else
  cp "$REPO_ROOT/e2e/.env" "$WORKTREE_DIR/e2e/.env"
  if (cd "$WORKTREE_DIR/e2e" && npm install --no-audit --no-fund >"$REPORT_DIR/e2e-install.log" 2>&1 \
      && npx playwright install --with-deps chromium >>"$REPORT_DIR/e2e-install.log" 2>&1 \
      && npx playwright test >"$REPORT_DIR/e2e-test.log" 2>&1); then
    record "e2e-suite" "PASS"
  else
    record "e2e-suite" "FAIL (see $REPORT_DIR/e2e-test.log, HTML report at $WORKTREE_DIR/e2e/report/index.html)"
  fi
  cp -r "$WORKTREE_DIR/e2e/report" "$REPORT_DIR/e2e-html-report" 2>/dev/null || true
fi

# ── Verdict ───────────────────────────────────────────────────────────────────
log_step "VERDICT"
FAILED=0
for line in "${STEP_STATUS[@]}"; do
  echo "  $line"
  [[ "$line" == *"FAIL"* ]] && FAILED=1
done
echo ""
echo "Full report: $REPORT_DIR"

if [[ $FAILED -eq 1 ]]; then
  echo ""
  echo "  ✗ NOT SAFE TO DEPLOY '$BRANCH' TO 209 — see failures above."
  exit 1
else
  echo ""
  echo "  ✓ All gates passed — '$BRANCH' is clear to deploy to 209 (still a manual decision, this script never deploys to 209 itself)."
  exit 0
fi
