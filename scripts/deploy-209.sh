#!/usr/bin/env bash
# =============================================================================
#  CMS Deployment Script — deploys to 172.16.7.209
#
#  Usage:
#    DEPLOY209_PASS='ssh/sudo password' ./scripts/deploy-209.sh            → full rebuild
#    DEPLOY209_PASS='ssh/sudo password' ./scripts/deploy-209.sh frontend   → frontend only
#    DEPLOY209_PASS='ssh/sudo password' ./scripts/deploy-209.sh backend    → backend only
#
#  SSH key users may omit DEPLOY209_PASS when passwordless sudo is configured.
# =============================================================================

set -euo pipefail

# ── Configuration ─────────────────────────────────────────────────────────────
SERVER="${DEPLOY209_SERVER:-172.16.7.209}"
SERVER_USER="${DEPLOY209_USER:-sksadmin}"
SERVER_PASS="${DEPLOY209_PASS:-ra5terpass@sksh}"
DB_PASSWORD="${DEPLOY209_DB_PASS:-cms_db_pass_209@sksh}"
KC_PASS="${DEPLOY209_KC_PASS:-K3yCloak@sksh}"
REMOTE_DIR="${DEPLOY209_DIR:-/docker_data/skscms209}"
REMOTE_STAGE="${DEPLOY209_STAGE:-/tmp/skscms209-deploy}"
PROJECT_NAME="skscms209"
LOCAL_DIR="$(cd "$(dirname "$0")/.." && pwd)"   # project root

SSH_OPTS="-o StrictHostKeyChecking=no"
COMPOSE_CMD="docker compose -p $PROJECT_NAME -f $REMOTE_DIR/docker-compose.yml --env-file $REMOTE_DIR/.env"

# ── Helpers ───────────────────────────────────────────────────────────────────
require_sshpass_if_needed() {
  if [ -n "$SERVER_PASS" ] && ! command -v sshpass >/dev/null 2>&1; then
    echo "sshpass is required when DEPLOY209_PASS/SERVER_PASS is set."
    exit 1
  fi
}

remote_run() {
  local command="$1"

  if [ -n "$SERVER_PASS" ]; then
    SSHPASS="$SERVER_PASS" sshpass -e ssh $SSH_OPTS "$SERVER_USER@$SERVER" "$command"
  else
    ssh $SSH_OPTS "$SERVER_USER@$SERVER" "$command"
  fi
}

ssh_run() {
  local command="$1"
  local escaped pass_escaped
  escaped=$(printf '%q' "$command")

  if [ -n "$SERVER_PASS" ]; then
    # Embed the password in the remote command so it reaches sudo -S via pipe.
    # sshpass -e intercepts SSH stdin, so <<< never reaches sudo -S — the pipe
    # approach is the only reliable way.
    pass_escaped=$(printf '%q' "$SERVER_PASS")
    SSHPASS="$SERVER_PASS" sshpass -e ssh $SSH_OPTS "$SERVER_USER@$SERVER" \
      "echo ${pass_escaped} | sudo -S bash -lc ${escaped} 2>&1"
  else
    ssh $SSH_OPTS "$SERVER_USER@$SERVER" "sudo -n bash -lc $escaped 2>&1"
  fi
}

rsync_to_server() {
  local src="$1"
  local dst="$2"
  shift 2

  if [ -n "$SERVER_PASS" ]; then
    SSHPASS="$SERVER_PASS" sshpass -e rsync -avz --progress \
      "$@" \
      -e "ssh $SSH_OPTS" \
      "$src" "$SERVER_USER@$SERVER:$dst"
  else
    rsync -avz --progress \
      "$@" \
      -e "ssh $SSH_OPTS" \
      "$src" "$SERVER_USER@$SERVER:$dst"
  fi
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

require_sshpass_if_needed

echo ""
echo "============================================="
echo "  CMS Deployment — mode: $MODE"
echo "  Target: $SERVER_USER@$SERVER:$REMOTE_DIR"
echo "  Compose project: $PROJECT_NAME"
echo "============================================="

# ── Step 1: Stage files on server ─────────────────────────────────────────────
print_step "Preparing remote staging directory..."
remote_run "rm -rf $REMOTE_STAGE && mkdir -p $REMOTE_STAGE/deploy $REMOTE_STAGE/backend $REMOTE_STAGE/frontend"

print_step "Syncing 209 deployment bundle..."
rsync_to_server "$LOCAL_DIR/deploy/production-209/" "$REMOTE_STAGE/deploy/" \
  --exclude='.env'

if [ "$MODE" = "full" ] || [ "$MODE" = "backend" ]; then
  print_step "Syncing backend files..."
  rsync_to_server "$LOCAL_DIR/backend/" "$REMOTE_STAGE/backend/" \
    --exclude='build' \
    --exclude='.gradle'
fi

if [ "$MODE" = "full" ] || [ "$MODE" = "frontend" ]; then
  print_step "Syncing frontend files..."
  rsync_to_server "$LOCAL_DIR/frontend/" "$REMOTE_STAGE/frontend/" \
    --exclude='node_modules' \
    --exclude='dist' \
    --exclude='.angular'
fi

# ── Step 2: Install staged files into /docker_data/skscms209 ──────────────────
print_step "Installing staged files into $REMOTE_DIR..."
ssh_run "mkdir -p $REMOTE_DIR/build && rsync -a --delete --exclude=.env $REMOTE_STAGE/deploy/ $REMOTE_DIR/"

if [ "$MODE" = "full" ] || [ "$MODE" = "backend" ]; then
  ssh_run "mkdir -p $REMOTE_DIR/build/backend && rsync -a --delete $REMOTE_STAGE/backend/ $REMOTE_DIR/build/backend/"
fi

if [ "$MODE" = "full" ] || [ "$MODE" = "frontend" ]; then
  ssh_run "mkdir -p $REMOTE_DIR/build/frontend && rsync -a --delete $REMOTE_STAGE/frontend/ $REMOTE_DIR/build/frontend/"
fi

# ── Bootstrap .env on first run ───────────────────────────────────────────────
print_step "Checking / bootstrapping .env on server..."
TMP_ENV=$(mktemp /tmp/cms209_env_XXXXXX)
cat > "$TMP_ENV" << EOF
DEPLOY_HOST=172.16.7.209
DB_USERNAME=cms_user
DB_PASSWORD=${DB_PASSWORD}
KEYCLOAK_ADMIN_PASSWORD=${KC_PASS}
EOF

# Push candidate .env to staging area (no sudo needed for /tmp)
remote_run "mkdir -p $REMOTE_STAGE"
rsync_to_server "$TMP_ENV" "$REMOTE_STAGE/.env.candidate"
rm -f "$TMP_ENV"

# Move into place only if .env doesn't already exist (preserve live credentials)
ssh_run "
  if [ ! -f $REMOTE_DIR/.env ]; then
    mkdir -p $REMOTE_DIR
    cp $REMOTE_STAGE/.env.candidate $REMOTE_DIR/.env
    chmod 600 $REMOTE_DIR/.env
    echo '.env bootstrapped successfully'
  else
    echo '.env already exists — not overwriting'
  fi
  rm -f $REMOTE_STAGE/.env.candidate
"

# ── Step 3: Build Docker image(s) ─────────────────────────────────────────────
if [ "$MODE" = "full" ] || [ "$MODE" = "backend" ]; then
  print_step "Building latest backend image..."
  ssh_run "docker build -t cms-backend:latest $REMOTE_DIR/build/backend"
fi

if [ "$MODE" = "full" ] || [ "$MODE" = "frontend" ]; then
  print_step "Building latest frontend image..."
  ssh_run "docker build -t cms-frontend:latest $REMOTE_DIR/build/frontend"
fi

# ── Step 4: Stop existing CMS containers ──────────────────────────────────────
if [ "$MODE" = "full" ]; then
  print_step "Stopping existing $PROJECT_NAME containers..."
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

# ── Step 5: Start latest containers ───────────────────────────────────────────
if [ "$MODE" = "full" ]; then
  print_step "Starting latest $PROJECT_NAME containers..."
  ssh_run "$COMPOSE_CMD up -d --force-recreate"

elif [ "$MODE" = "frontend" ]; then
  print_step "Starting latest frontend container..."
  ssh_run "$COMPOSE_CMD up -d --no-deps --force-recreate frontend"

elif [ "$MODE" = "backend" ]; then
  print_step "Starting latest backend container..."
  ssh_run "$COMPOSE_CMD up -d --no-deps --force-recreate backend"
fi

# ── Step 6: Health check ──────────────────────────────────────────────────────
print_step "Running health checks..."
sleep 6
ssh_run "$COMPOSE_CMD ps"
remote_run "
  echo ''
  echo '--- Service Health ---'
  curl -sk -o /dev/null -w 'Frontend (HTTPS):  %{http_code}\n' https://localhost/
  curl -sk -o /dev/null -w 'Keycloak (OIDC):   %{http_code}\n' https://localhost/realms/cms/.well-known/openid-configuration
  curl -sk -o /dev/null -w 'Backend API:       %{http_code}\n' https://localhost/api/v1/health
"

print_step "Cleaning remote staging directory..."
remote_run "rm -rf $REMOTE_STAGE"

echo ""
echo "============================================="
echo "  Deployment complete!"
echo "  App URL: https://$SERVER"
echo "============================================="
echo ""
