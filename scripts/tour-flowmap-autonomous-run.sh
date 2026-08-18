#!/usr/bin/env bash
# Cron entry point for the Tour+FlowMap autonomous overnight sessions (night/morning).
# Invoked by crontab lines tagged TOUR-AUTO-NIGHT / TOUR-AUTO-MORNING, which this
# script removes from crontab after it fires so they don't linger as dead
# one-shot entries. Modeled on scripts/r2-autonomous-run.sh.
set -uo pipefail

MODE="${1:?usage: tour-flowmap-autonomous-run.sh <night|morning>}"
REPO_DIR="/home/raster/Idea Projects/SKSCMS"
LOG_DIR="$HOME/.tour-autonomous-logs"
mkdir -p "$LOG_DIR"

export PATH="/home/raster/.local/bin:/opt/gradle/gradle-8.7/bin:/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin:/usr/local/games:/usr/games:$PATH"

case "$MODE" in
  night)
    PROMPT_FILE="$REPO_DIR/scripts/tour-flowmap-autonomous-prompt-night.md"
    LOG_FILE="$LOG_DIR/night-$(date +%F).log"
    CRON_TAG="TOUR-AUTO-NIGHT"
    ;;
  morning)
    PROMPT_FILE="$REPO_DIR/scripts/tour-flowmap-autonomous-prompt-morning.md"
    LOG_FILE="$LOG_DIR/morning-$(date +%F).log"
    CRON_TAG="TOUR-AUTO-MORNING"
    ;;
  *)
    echo "unknown mode: $MODE" >&2
    exit 1
    ;;
esac

cd "$REPO_DIR" || exit 1

{
  echo "=== Tour+FlowMap autonomous session ($MODE) starting at $(date) ==="
  claude -p "$(cat "$PROMPT_FILE")" --dangerously-skip-permissions
  echo "=== Tour+FlowMap autonomous session ($MODE) finished at $(date) ==="
} >> "$LOG_FILE" 2>&1

# Deregister this one-shot cron line now that it has fired.
crontab -l 2>/dev/null | grep -v "$CRON_TAG" | crontab -
