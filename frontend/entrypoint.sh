#!/bin/sh
set -e

# ── 1. Write runtime Keycloak URL into assets/env.js ─────────────────────────
# Angular reads this before initialising so the correct Keycloak server is used
# on every deployment without rebuilding the image.
cat > /usr/share/nginx/html/assets/env.js << EOF
window.__env__ = {
  keycloakUrl:      '${KEYCLOAK_URL:-}',
  keycloakRealm:    '${KEYCLOAK_REALM:-cms}',
  keycloakClientId: '${KEYCLOAK_CLIENT_ID:-cms-frontend}'
};
EOF

# ── 2. Stamp a unique version into index.html so browsers never use a stale
#       env.js from cache — each container start gets a fresh query string.
STAMP=$(date +%s)
sed -i "s|assets/env.js\"|assets/env.js?v=${STAMP}\"|g" \
  /usr/share/nginx/html/index.html

# ── 3. Generate nginx.conf from template ──────────────────────────────────────
export KEYCLOAK_UPSTREAM="${KEYCLOAK_UPSTREAM:-127.0.0.1:8180}"
export BACKEND_UPSTREAM="${BACKEND_UPSTREAM:-127.0.0.1:8080}"
export EXTERNAL_HTTPS_PORT="${EXTERNAL_HTTPS_PORT:-212}"

envsubst '${KEYCLOAK_UPSTREAM} ${BACKEND_UPSTREAM} ${EXTERNAL_HTTPS_PORT}' \
  < /etc/nginx/templates/default.conf.template \
  > /etc/nginx/conf.d/default.conf

exec nginx -g 'daemon off;'
