#!/usr/bin/env bash
# =============================================================================
#  CMS Deployment Script — deploys to 172.17.1.243 (dev.raster.in:212)
#
#  Usage:
#    ./scripts/deploy-243.sh            → full rebuild (frontend + backend)
#    ./scripts/deploy-243.sh frontend   → rebuild frontend only
#    ./scripts/deploy-243.sh backend    → rebuild backend only
# =============================================================================

set -e

# ── Configuration ─────────────────────────────────────────────────────────────
SERVER="172.17.1.243"
SERVER_USER="raster"
SERVER_PASS="ra5terpass@1234"
REMOTE_DIR="~/skscms"
LOCAL_DIR="$(cd "$(dirname "$0")/.." && pwd)"   # project root
COMPOSE_FILE="$REMOTE_DIR/docker-compose.yml"
COMPOSE_CMD="docker compose -f $COMPOSE_FILE"

SSH_OPTS="-o StrictHostKeyChecking=no"

# ── Helpers ───────────────────────────────────────────────────────────────────
rsync_to_server() {
  local src="$1"
  local dst="$2"
  shift 2
  SSHPASS="$SERVER_PASS" sshpass -e rsync -avz --progress \
    "$@" \
    -e "ssh $SSH_OPTS" \
    "$src" "$SERVER_USER@$SERVER:$dst"
}

ssh_run() {
  SSHPASS="$SERVER_PASS" sshpass -e ssh $SSH_OPTS "$SERVER_USER@$SERVER" \
    "echo '$SERVER_PASS' | sudo -S $1 2>&1"
}

print_step() {
  echo ""
  echo "──────────────────────────────────────────"
  echo "  $1"
  echo "──────────────────────────────────────────"
}

# ── Parse argument ─────────────────────────────────────────────────────────────
MODE="${1:-full}"

case "$MODE" in
  frontend|front|fe)  MODE="frontend" ;;
  backend|back|be)    MODE="backend"  ;;
  full|all|"")        MODE="full"     ;;
  *)
    echo "Unknown mode: $MODE"
    echo "Usage: $0 [frontend|backend|full]"
    exit 1
    ;;
esac

echo ""
echo "============================================="
echo "  CMS Deployment — mode: $MODE"
echo "  Target: $SERVER_USER@$SERVER:$REMOTE_DIR"
echo "============================================="

# ── Step 1: Sync files ────────────────────────────────────────────────────────
if [ "$MODE" = "full" ]; then
  print_step "Syncing all project files to server..."
  rsync_to_server "$LOCAL_DIR/" "$REMOTE_DIR/" \
    --exclude='.git' \
    --exclude='node_modules' \
    --exclude='frontend/dist' \
    --exclude='frontend/.angular' \
    --exclude='backend/build' \
    --exclude='backend/.gradle' \
    --exclude='.idea' \
    --exclude='scripts/__pycache__'

elif [ "$MODE" = "frontend" ]; then
  print_step "Syncing frontend files to server..."
  rsync_to_server "$LOCAL_DIR/frontend/" "$REMOTE_DIR/frontend/" \
    --exclude='node_modules' \
    --exclude='dist' \
    --exclude='.angular'

  print_step "Syncing Keycloak themes..."
  rsync_to_server "$LOCAL_DIR/infrastructure/keycloak/themes/" "$REMOTE_DIR/infrastructure/keycloak/themes/"

elif [ "$MODE" = "backend" ]; then
  print_step "Syncing backend files to server..."
  rsync_to_server "$LOCAL_DIR/backend/" "$REMOTE_DIR/backend/" \
    --exclude='build' \
    --exclude='.gradle'

  print_step "Syncing Keycloak themes..."
  rsync_to_server "$LOCAL_DIR/infrastructure/keycloak/themes/" "$REMOTE_DIR/infrastructure/keycloak/themes/"
fi

# ── Step 2: Build Docker image(s) ─────────────────────────────────────────────
if [ "$MODE" = "full" ]; then
  print_step "Building all Docker images (this takes 10-15 min)..."
  ssh_run "$COMPOSE_CMD build --no-cache"

elif [ "$MODE" = "frontend" ]; then
  print_step "Building frontend Docker image (3-4 min)..."
  ssh_run "$COMPOSE_CMD build --no-cache frontend"

elif [ "$MODE" = "backend" ]; then
  print_step "Building backend Docker image (4-6 min)..."
  ssh_run "$COMPOSE_CMD build --no-cache backend"
fi

# ── Step 3: Stop existing CMS containers ──────────────────────────────────────
if [ "$MODE" = "full" ]; then
  print_step "Stopping existing CMS containers..."
  ssh_run "$COMPOSE_CMD down --remove-orphans"

elif [ "$MODE" = "frontend" ]; then
  print_step "Stopping existing frontend container..."
  ssh_run "$COMPOSE_CMD stop frontend || true"
  ssh_run "$COMPOSE_CMD rm -f frontend || true"

elif [ "$MODE" = "backend" ]; then
  print_step "Stopping existing backend container..."
  ssh_run "$COMPOSE_CMD stop backend || true"
  ssh_run "$COMPOSE_CMD rm -f backend || true"
fi

# ── Step 4: Start latest containers ───────────────────────────────────────────
if [ "$MODE" = "full" ]; then
  print_step "Starting latest CMS containers..."
  ssh_run "$COMPOSE_CMD up -d --force-recreate"

elif [ "$MODE" = "frontend" ]; then
  print_step "Starting latest frontend container..."
  ssh_run "$COMPOSE_CMD up -d --no-deps --force-recreate frontend"

elif [ "$MODE" = "backend" ]; then
  print_step "Starting latest backend container..."
  ssh_run "$COMPOSE_CMD up -d --no-deps --force-recreate backend"
fi

# ── Step 5: Health check ──────────────────────────────────────────────────────
print_step "Running health checks..."
sleep 6
SSHPASS="$SERVER_PASS" sshpass -e ssh $SSH_OPTS "$SERVER_USER@$SERVER" "
  echo '--- Container Status ---'
  echo '$SERVER_PASS' | sudo -S $COMPOSE_CMD ps 2>/dev/null
  echo ''
  echo '--- Service Health ---'
  curl -sk -o /dev/null -w 'Frontend (HTTPS):  %{http_code}\n' https://localhost:8443/
  curl -sk -o /dev/null -w 'Keycloak (OIDC):   %{http_code}\n' https://localhost:8443/realms/cms
  curl -sk -o /dev/null -w 'Backend API:       %{http_code}\n' https://localhost:8443/api/v1/health
"

# ── Step 6: Keycloak realm theme reconciliation ───────────────────────────────
if [ "$MODE" = "full" ] || [ "$MODE" = "frontend" ]; then
  print_step "Reconciling Keycloak realm login theme..."

  KC_HOST="172.17.1.243"
  KC_PORT="8280"
  KC_ADMIN_USER="admin"
  KC_ADMIN_PASS="admin"

  TOKEN_JSON=""
  for attempt in $(seq 1 20); do
    TOKEN_JSON=$(curl -sk -X POST "http://$KC_HOST:$KC_PORT/realms/master/protocol/openid-connect/token" \
      -H 'Content-Type: application/x-www-form-urlencoded' \
      --data-urlencode 'client_id=admin-cli' \
      --data-urlencode "username=$KC_ADMIN_USER" \
      --data-urlencode "password=$KC_ADMIN_PASS" \
      --data-urlencode 'grant_type=password' || true)

    if TOKEN_JSON="$TOKEN_JSON" python3 - <<'PY' >/dev/null 2>&1
import json, os
payload = json.loads(os.environ['TOKEN_JSON'])
raise SystemExit(0 if payload.get('access_token') else 1)
PY
    then
      break
    fi

    echo "Waiting for Keycloak admin API... attempt $attempt/20"
    sleep 3
  done

  TOKEN=$(TOKEN_JSON="$TOKEN_JSON" python3 - <<'PY'
import json, os, sys
payload = json.loads(os.environ['TOKEN_JSON'])
if 'access_token' not in payload:
    print(payload, file=sys.stderr)
    sys.exit(1)
print(payload['access_token'])
PY
)

  REALM_THEME_STATUS=$(curl -sk -o /tmp/cms243_realm_theme.out -w '%{http_code}' \
    -X PUT \
    -H "Authorization: Bearer $TOKEN" \
    -H 'Content-Type: application/json' \
    -d '{"loginTheme":"cms"}' \
    "http://$KC_HOST:$KC_PORT/admin/realms/cms")
  rm -f /tmp/cms243_realm_theme.out

  if [ "$REALM_THEME_STATUS" != "204" ]; then
    echo "Warning: Failed to set realm loginTheme. HTTP status: $REALM_THEME_STATUS (non-fatal)"
  else
    echo "Realm loginTheme set to 'cms' successfully."
  fi
fi

echo ""
echo "============================================="
echo "  Deployment complete!"
echo "  App URL: https://dev.raster.in:212"
echo "============================================="
echo ""
