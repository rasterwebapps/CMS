#!/usr/bin/env bash
# Cron entry point for tonight's Academics+Portals autonomous checkpoints.
# Invoked by crontab lines tagged ACAD-AUTO-<slot>, which this script removes
# from crontab after it fires so they don't linger as dead one-shot entries.
set -uo pipefail

SLOT="${1:?usage: academics-portal-autonomous-run.sh <slot, e.g. 2100>}"
REPO_DIR="/home/raster/Idea Projects/SKSCMS"
LOG_DIR="$HOME/.academics-portal-autonomous-logs"
mkdir -p "$LOG_DIR"

export PATH="/home/raster/.local/bin:/opt/gradle/gradle-8.7/bin:/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin:/usr/local/games:/usr/games:$PATH"

PROMPT_FILE="$REPO_DIR/scripts/academics-portal-autonomous-prompt.md"
LOG_FILE="$LOG_DIR/${SLOT}-$(date +%F).log"
CRON_TAG="ACAD-AUTO-${SLOT}"

cd "$REPO_DIR" || exit 1

{
  echo "=== Academics+Portals autonomous session (slot $SLOT) starting at $(date) ==="
  claude -p "$(cat "$PROMPT_FILE")" --dangerously-skip-permissions
  echo "=== Academics+Portals autonomous session (slot $SLOT) finished at $(date) ==="
} >> "$LOG_FILE" 2>&1

# Deregister this one-shot cron line now that it has fired.
crontab -l 2>/dev/null | grep -v "$CRON_TAG" | crontab -
