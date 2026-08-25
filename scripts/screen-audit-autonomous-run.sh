#!/usr/bin/env bash
# One-shot cron entry point for the Screen Compliance Audit (OC-168), scheduled
# for 2026-08-25 21:00 IST. Invoked by a crontab line tagged AUDIT-AUTO-NIGHT,
# which this script removes from crontab after it fires so it doesn't linger
# as a dead one-shot entry. Modeled on scripts/tour-flowmap-autonomous-run.sh.
set -uo pipefail

REPO_DIR="/home/raster/Idea Projects/SKSCMS"
LOG_DIR="$HOME/.screen-audit-autonomous-logs"
mkdir -p "$LOG_DIR"

export PATH="/home/raster/.local/bin:/opt/gradle/gradle-8.7/bin:/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin:/usr/local/games:/usr/games:$PATH"

PROMPT_FILE="$REPO_DIR/scripts/screen-audit-autonomous-prompt-night.md"
LOG_FILE="$LOG_DIR/night-$(date +%F).log"
CRON_TAG="AUDIT-AUTO-NIGHT"

cd "$REPO_DIR" || exit 1

{
  echo "=== Screen Compliance Audit (OC-168) autonomous session starting at $(date) ==="
  claude -p "$(cat "$PROMPT_FILE")" --dangerously-skip-permissions
  echo "=== Screen Compliance Audit (OC-168) autonomous session finished at $(date) ==="
} >> "$LOG_FILE" 2>&1

# Deregister this one-shot cron line now that it has fired.
crontab -l 2>/dev/null | grep -v "$CRON_TAG" | crontab -
