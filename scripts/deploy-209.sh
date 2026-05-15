#!/usr/bin/env bash
# =============================================================================
#  CMS Deployment Script — deploys to 172.16.7.209
#  Public URL: https://cms.nursing.sksh.ac.in
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
PUBLIC_HOST="${DEPLOY209_PUBLIC_HOST:-cms.nursing.sksh.ac.in}"
PUBLIC_IP="${DEPLOY209_PUBLIC_IP:-137.97.6.147}"
LOCAL_HOST="${DEPLOY209_LOCAL_HOST:-172.16.7.209}"
SERVER_USER="${DEPLOY209_USER:-sksadmin}"
SERVER_PASS="${DEPLOY209_PASS:-ra5terpass@sksh}"
DB_PASSWORD="${DEPLOY209_DB_PASS:-cms_db_pass_209@sksh}"
KC_PASS="${DEPLOY209_KC_PASS:-K3yCloak@sksh}"
TLS_CERT_FILE="${DEPLOY209_TLS_CERT_FILE:-/home/raster/Downloads/Telegram Desktop/Certificate.txt}"
TLS_INTERMEDIATE_FILE="${DEPLOY209_TLS_INTERMEDIATE_FILE:-/home/raster/Downloads/Telegram Desktop/Intermediate Certificate.txt}"
TLS_PRIVATE_KEY_FILE="${DEPLOY209_TLS_PRIVATE_KEY_FILE:-/home/raster/Downloads/Telegram Desktop/RSA Private Key.txt}"
REMOTE_DIR="${DEPLOY209_DIR:-/docker_data/skscms209}"
REMOTE_STAGE="${DEPLOY209_STAGE:-/tmp/skscms209-deploy}"
PROJECT_NAME="skscms209"
LOCAL_DIR="$(cd "$(dirname "$0")/.." && pwd)"   # project root
TMP_TLS_DIR=""

cleanup() {
  if [ -n "$TMP_TLS_DIR" ] && [ -d "$TMP_TLS_DIR" ]; then
    rm -rf "$TMP_TLS_DIR"
  fi
}
trap cleanup EXIT

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

prepare_tls_bundle() {
  if [ "$MODE" != "full" ] && [ "$MODE" != "frontend" ]; then
    return
  fi

  print_step "Preparing TLS certificate bundle..."

  if ! command -v openssl >/dev/null 2>&1; then
    echo "openssl is required to validate TLS files before deployment."
    exit 1
  fi

  for file in "$TLS_CERT_FILE" "$TLS_INTERMEDIATE_FILE" "$TLS_PRIVATE_KEY_FILE"; do
    if [ ! -r "$file" ]; then
      echo "TLS file is missing or unreadable: $file"
      exit 1
    fi
  done

  TMP_TLS_DIR=$(mktemp -d /tmp/skscms209_tls_XXXXXX)
  { cat "$TLS_CERT_FILE"; printf '\n'; cat "$TLS_INTERMEDIATE_FILE"; printf '\n'; } > "$TMP_TLS_DIR/self.crt"
  cp "$TLS_PRIVATE_KEY_FILE" "$TMP_TLS_DIR/self.key"
  chmod 600 "$TMP_TLS_DIR/self.key"

  openssl x509 -in "$TMP_TLS_DIR/self.crt" -noout >/dev/null
  openssl pkey -in "$TMP_TLS_DIR/self.key" -noout >/dev/null
  openssl x509 -in "$TMP_TLS_DIR/self.crt" -pubkey -noout > "$TMP_TLS_DIR/cert.pub"
  openssl pkey -in "$TMP_TLS_DIR/self.key" -pubout > "$TMP_TLS_DIR/key.pub"

  if ! cmp -s "$TMP_TLS_DIR/cert.pub" "$TMP_TLS_DIR/key.pub"; then
    echo "TLS certificate and private key do not match."
    exit 1
  fi

  remote_run "mkdir -p $REMOTE_STAGE/tls"
  rsync_to_server "$TMP_TLS_DIR/self.crt" "$REMOTE_STAGE/tls/self.crt"
  rsync_to_server "$TMP_TLS_DIR/self.key" "$REMOTE_STAGE/tls/self.key"
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
echo "  Public URL: https://$PUBLIC_HOST"
echo "  Public IP URL: https://$PUBLIC_IP"
echo "  Local URL: https://$LOCAL_HOST"
echo "  Compose project: $PROJECT_NAME"
echo "============================================="

# ── Step 1: Stage files on server ─────────────────────────────────────────────
print_step "Preparing remote staging directory..."
remote_run "rm -rf $REMOTE_STAGE && mkdir -p $REMOTE_STAGE/deploy $REMOTE_STAGE/backend $REMOTE_STAGE/frontend"
prepare_tls_bundle

print_step "Syncing 209 deployment bundle..."
rsync_to_server "$LOCAL_DIR/deploy/production-209/" "$REMOTE_STAGE/deploy/" \
  --exclude='.env'

print_step "Syncing Keycloak themes..."
remote_run "mkdir -p $REMOTE_STAGE/deploy/keycloak/themes"
rsync_to_server "$LOCAL_DIR/infrastructure/keycloak/themes/" "$REMOTE_STAGE/deploy/keycloak/themes/"

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
    --exclude='.angular' \
    --exclude='ssl'
fi

# ── Step 2: Install staged files into /docker_data/skscms209 ──────────────────
print_step "Installing staged files into $REMOTE_DIR..."
ssh_run "mkdir -p $REMOTE_DIR/build && rsync -a --delete --exclude=.env $REMOTE_STAGE/deploy/ $REMOTE_DIR/"

if [ "$MODE" = "full" ] || [ "$MODE" = "backend" ]; then
  ssh_run "mkdir -p $REMOTE_DIR/build/backend && rsync -a --delete $REMOTE_STAGE/backend/ $REMOTE_DIR/build/backend/"
fi

if [ "$MODE" = "full" ] || [ "$MODE" = "frontend" ]; then
  ssh_run "mkdir -p $REMOTE_DIR/build/frontend && rsync -a --delete $REMOTE_STAGE/frontend/ $REMOTE_DIR/build/frontend/"
  ssh_run "mkdir -p $REMOTE_DIR/ssl && cp $REMOTE_STAGE/tls/self.crt $REMOTE_DIR/ssl/self.crt && cp $REMOTE_STAGE/tls/self.key $REMOTE_DIR/ssl/self.key && chmod 644 $REMOTE_DIR/ssl/self.crt && chmod 600 $REMOTE_DIR/ssl/self.key"
fi

# ── Bootstrap .env on first run ───────────────────────────────────────────────
print_step "Checking / bootstrapping .env on server..."
TMP_ENV=$(mktemp /tmp/cms209_env_XXXXXX)
cat > "$TMP_ENV" << EOF
DEPLOY_HOST=${PUBLIC_HOST}
PUBLIC_IP=${PUBLIC_IP}
LOCAL_HOST=${LOCAL_HOST}
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

# Keep origin variables current without touching stored credentials.
ssh_run "
  if [ -f $REMOTE_DIR/.env ]; then
    if grep -q '^DEPLOY_HOST=' $REMOTE_DIR/.env; then
      sed -i 's|^DEPLOY_HOST=.*|DEPLOY_HOST=$PUBLIC_HOST|' $REMOTE_DIR/.env
    else
      printf '\nDEPLOY_HOST=$PUBLIC_HOST\n' >> $REMOTE_DIR/.env
    fi
    if grep -q '^PUBLIC_IP=' $REMOTE_DIR/.env; then
      sed -i 's|^PUBLIC_IP=.*|PUBLIC_IP=$PUBLIC_IP|' $REMOTE_DIR/.env
    else
      printf 'PUBLIC_IP=$PUBLIC_IP\n' >> $REMOTE_DIR/.env
    fi
    if grep -q '^LOCAL_HOST=' $REMOTE_DIR/.env; then
      sed -i 's|^LOCAL_HOST=.*|LOCAL_HOST=$LOCAL_HOST|' $REMOTE_DIR/.env
    else
      printf 'LOCAL_HOST=$LOCAL_HOST\n' >> $REMOTE_DIR/.env
    fi
    chmod 600 $REMOTE_DIR/.env
    echo 'Origins set to $PUBLIC_HOST, $PUBLIC_IP, $LOCAL_HOST'
  fi
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

# ── Step 6: Keycloak public URL reconciliation ────────────────────────────────
if [ "$MODE" = "full" ] || [ "$MODE" = "frontend" ]; then
  print_step "Updating Keycloak client public URLs..."

  TOKEN_JSON=""
  for attempt in $(seq 1 30); do
    TOKEN_JSON=$(curl -sk -X POST "http://$LOCAL_HOST:8180/realms/master/protocol/openid-connect/token" \
      -H 'Content-Type: application/x-www-form-urlencoded' \
      --data-urlencode 'client_id=admin-cli' \
      --data-urlencode 'username=admin' \
      --data-urlencode "password=$KC_PASS" \
      --data-urlencode 'grant_type=password' || true)

    if TOKEN_JSON="$TOKEN_JSON" python3 - <<'PY' >/dev/null 2>&1
import json, os
payload = json.loads(os.environ['TOKEN_JSON'])
raise SystemExit(0 if payload.get('access_token') else 1)
PY
    then
      break
    fi

    echo "Waiting for Keycloak admin API... attempt $attempt/30"
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

  CLIENT_JSON=$(curl -sk -H "Authorization: Bearer $TOKEN" \
    "http://$LOCAL_HOST:8180/admin/realms/cms/clients?clientId=cms-frontend")
  CLIENT_ID=$(CLIENT_JSON="$CLIENT_JSON" python3 - <<'PY'
import json, os, sys
items = json.loads(os.environ['CLIENT_JSON'])
if not items:
    print('cms-frontend client not found', file=sys.stderr)
    sys.exit(1)
print(items[0]['id'])
PY
)

  CLIENT_UPDATE=$(mktemp /tmp/cms209_client_update_XXXXXX.json)
  cat > "$CLIENT_UPDATE" <<JSON
{
  "clientId": "cms-frontend",
  "publicClient": true,
  "standardFlowEnabled": true,
  "directAccessGrantsEnabled": true,
  "rootUrl": "",
  "baseUrl": "",
  "redirectUris": [
    "https://$PUBLIC_HOST/*",
    "https://$PUBLIC_IP/*",
    "https://$LOCAL_HOST/*"
  ],
  "webOrigins": [
    "https://$PUBLIC_HOST",
    "https://$PUBLIC_IP",
    "https://$LOCAL_HOST"
  ],
  "protocol": "openid-connect"
}
JSON

  STATUS=$(curl -sk -o /tmp/cms209_keycloak_update.out -w '%{http_code}' \
    -X PUT \
    -H "Authorization: Bearer $TOKEN" \
    -H 'Content-Type: application/json' \
    --data-binary "@$CLIENT_UPDATE" \
    "http://$LOCAL_HOST:8180/admin/realms/cms/clients/$CLIENT_ID")
  rm -f "$CLIENT_UPDATE" /tmp/cms209_keycloak_update.out

  if [ "$STATUS" != "204" ]; then
    echo "Failed to update Keycloak cms-frontend client. HTTP status: $STATUS"
    exit 1
  fi

  # ── Reconcile realm login theme ──────────────────────────────────────────
  echo "Setting cms realm loginTheme to 'cms'..."
  REALM_THEME_STATUS=$(curl -sk -o /tmp/cms209_realm_theme.out -w '%{http_code}' \
    -X PUT \
    -H "Authorization: Bearer $TOKEN" \
    -H 'Content-Type: application/json' \
    -d '{"loginTheme":"cms"}' \
    "http://$LOCAL_HOST:8180/admin/realms/cms")
  rm -f /tmp/cms209_realm_theme.out

  if [ "$REALM_THEME_STATUS" != "204" ]; then
    echo "Warning: Failed to set realm loginTheme. HTTP status: $REALM_THEME_STATUS (non-fatal)"
  else
    echo "Realm loginTheme set to 'cms' successfully."
  fi
fi

# ── Step 7: Health check ──────────────────────────────────────────────────────
print_step "Running health checks..."
sleep 6
ssh_run "$COMPOSE_CMD ps"
remote_run "
  echo ''
  echo '--- Service Health ---'
  curl -sk -o /dev/null -w 'Frontend (HTTPS):  %{http_code}\n' https://localhost/
  curl -sk -o /dev/null -w 'Keycloak (OIDC):   %{http_code}\n' https://localhost/realms/cms/.well-known/openid-configuration
  curl -sk -o /dev/null -w 'Backend API:       %{http_code}\n' https://localhost/api/v1/health
  curl -sk -o /dev/null -w 'Local IP URL:      %{http_code}\n' https://$LOCAL_HOST/
"

print_step "Cleaning remote staging directory..."
remote_run "rm -rf $REMOTE_STAGE"

echo ""
echo "============================================="
echo "  Deployment complete!"
echo "  Public domain: https://$PUBLIC_HOST"
echo "  Public IP:     https://$PUBLIC_IP"
echo "  Local LAN:     https://$LOCAL_HOST"
echo "============================================="
echo ""
