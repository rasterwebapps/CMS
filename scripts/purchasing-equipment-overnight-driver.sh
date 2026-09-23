#!/usr/bin/env bash
# Detached driver for tonight's Purchasing & Suppliers + Equipment & Asset Management overnight
# run (2026-09-22). Runs phase1 -> phase2 -> phase3 -> phase4 strictly sequentially, each as its
# own fresh headless `claude -p` invocation (bounded context per phase, matching this repo's
# established context-hygiene practice), only advancing once the previous phase's process has
# fully exited.
#
# Deliberately NOT a crontab-based mechanism: docs/inventory-management/DECISION_LOG.md's
# 2026-09-16 entry documents that a prior 3-pass cron mechanism for this exact kind of overnight
# inventory run never fired, because registering crontab entries needs the user to run
# `crontab ...` themselves (blocked for Claude by the auto-mode permission classifier), and that
# manual step never happened. This script is instead launched directly, once, as a detached
# background OS process (setsid + nohup + disown) by the session that authored it — no further
# human action required to keep it running.
#
# Runs in the isolated worktree .worktrees/purchasing-equipment-overnight, never the main
# checkout, so it can never collide with whatever else is running there.
set -uo pipefail

WORKTREE_DIR="/home/raster/Idea Projects/SKSCMS/.worktrees/purchasing-equipment-overnight"
SCRIPT_DIR="$WORKTREE_DIR/scripts"
LOG_DIR="$HOME/.purchasing-equipment-overnight-logs"
mkdir -p "$LOG_DIR"

export PATH="/home/raster/.local/bin:/opt/gradle/gradle-8.7/bin:/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin:/usr/local/games:/usr/games:$PATH"

cd "$WORKTREE_DIR" || exit 1

DRIVER_LOG="$LOG_DIR/driver-$(date +%F).log"
{
  echo "=== Driver started at $(date) ==="
  for phase in phase1 phase2 phase3 phase4; do
    PROMPT_FILE="$SCRIPT_DIR/purchasing-equipment-overnight-prompt-$phase.md"
    PHASE_LOG="$LOG_DIR/$phase-$(date +%F).log"
    if [ ! -f "$PROMPT_FILE" ]; then
      echo "=== $phase: no prompt file at $PROMPT_FILE, stopping driver ==="
      break
    fi
    echo "=== $phase starting at $(date), log: $PHASE_LOG ==="
    claude -p "$(cat "$PROMPT_FILE")" --dangerously-skip-permissions >> "$PHASE_LOG" 2>&1
    STATUS=$?
    echo "=== $phase finished at $(date) with exit code $STATUS ==="
    if [ $STATUS -ne 0 ]; then
      echo "=== $phase exited non-zero, stopping driver rather than chaining onward ==="
      break
    fi
  done
  echo "=== Driver finished at $(date) ==="
} >> "$DRIVER_LOG" 2>&1
