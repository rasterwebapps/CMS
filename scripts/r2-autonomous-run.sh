#!/usr/bin/env bash
# Cron entry point for the R2 autonomous overnight sessions (night/morning).
# Invoked by crontab lines tagged R2-AUTO-NIGHT / R2-AUTO-MORNING, which this
# script removes from crontab after it fires so they don't linger as dead
# one-shot entries.
set -uo pipefail

MODE="${1:?usage: r2-autonomous-run.sh <night|morning>}"
REPO_DIR="/home/raster/Idea Projects/SKSCMS"
LOG_DIR="$HOME/.r2-autonomous-logs"
mkdir -p "$LOG_DIR"

export PATH="/home/raster/.local/bin:/opt/gradle/gradle-8.7/bin:/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin:/usr/local/games:/usr/games:$PATH"

case "$MODE" in
  night)
    PROMPT_FILE="$REPO_DIR/scripts/r2-autonomous-prompt-night.md"
    LOG_FILE="$LOG_DIR/night-$(date +%F).log"
    CRON_TAG="R2-AUTO-NIGHT"
    ;;
  morning)
    PROMPT_FILE="$REPO_DIR/scripts/r2-autonomous-prompt-morning.md"
    LOG_FILE="$LOG_DIR/morning-$(date +%F).log"
    CRON_TAG="R2-AUTO-MORNING"
    ;;
  *)
    echo "unknown mode: $MODE" >&2
    exit 1
    ;;
esac

cd "$REPO_DIR" || exit 1

{
  echo "=== R2 autonomous session ($MODE) starting at $(date) ==="
  claude -p "$(cat "$PROMPT_FILE")" --dangerously-skip-permissions
  echo "=== R2 autonomous session ($MODE) finished at $(date) ==="
} >> "$LOG_FILE" 2>&1

# Deregister this one-shot cron line now that it has fired.
crontab -l 2>/dev/null | grep -v "$CRON_TAG" | crontab -
