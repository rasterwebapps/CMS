#!/usr/bin/env bash
# Cron entry point for the 2026-09-15/16 Inventory/Stock Management overnight run
# (21:00 kickoff / ~02:00 midpoint / ~07:00 final, wrapping up by 10:00).
# Invoked by one-shot crontab lines tagged INVENTORY-AUTO-<PHASE>, which this script removes
# from crontab after it fires so it doesn't linger as a dead one-shot entry.
#
# Deliberately runs in the isolated worktree at WORKTREE_DIR, NOT the main repo checkout —
# another Claude Code session was found actively branch-switching in the main checkout earlier
# tonight (see docs/inventory-management/DECISION_LOG.md's 2026-09-15 "Bulk demo data" entry).
# Working in this worktree means tonight's run can never collide with whatever that (or any
# other concurrent) session is doing in the shared main checkout.
set -uo pipefail

PHASE="${1:?usage: inventory-overnight-run.sh <kickoff|midpoint|final>}"
WORKTREE_DIR="/home/raster/Idea Projects/SKSCMS/.worktrees/inventory-overnight"
PROMPT_FILE="$WORKTREE_DIR/scripts/inventory-overnight-prompt-$PHASE.md"
LOG_DIR="$HOME/.inventory-autonomous-logs"
LOG_FILE="$LOG_DIR/$PHASE-$(date +%F).log"
CRON_TAG="INVENTORY-AUTO-$(echo "$PHASE" | tr '[:lower:]' '[:upper:]')"
mkdir -p "$LOG_DIR"

export PATH="/home/raster/.local/bin:/opt/gradle/gradle-8.7/bin:/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin:/usr/local/games:/usr/games:$PATH"

if [ ! -f "$PROMPT_FILE" ]; then
  echo "unknown phase '$PHASE' — no prompt file at $PROMPT_FILE" >&2
  exit 1
fi

cd "$WORKTREE_DIR" || exit 1

{
  echo "=== Inventory autonomous session ($PHASE) starting at $(date) ==="
  claude -p "$(cat "$PROMPT_FILE")" --dangerously-skip-permissions
  echo "=== Inventory autonomous session ($PHASE) finished at $(date) ==="
} >> "$LOG_FILE" 2>&1

# Deregister this one-shot cron line now that it has fired.
crontab -l 2>/dev/null | grep -v "$CRON_TAG" | crontab -
