# SKSCMS Production Deployment — multi-origin 209

This folder is the dedicated production deployment bundle for private server `172.16.7.209`.

Supported browser entry URLs:

- Public domain: `https://cms.nursing.sksh.ac.in`
- Public IP: `https://137.97.6.147`
- Local LAN: `https://172.16.7.209`

Do **not** use the legacy 243 deployment script or compose files for this server:

- Do not run `scripts/deploy.sh`.
- Do not deploy the root `docker-compose.yml`.
- Do not copy the shared `infrastructure/keycloak/cms-realm.json` to this server.

## Server path

Use this isolated path on the production server:

```bash
/docker_data/skscms209
```

## Files to deploy

Copy only these files/folders to `/docker_data/skscms209`:

- `docker-compose.yml`
- `postgres/Dockerfile` — `postgres:17-alpine` + pgvector built in (required by V561+/OC-277); built into the `cms-postgres-pgvector:latest` image by `scripts/deploy-209.sh` on every `full` deploy
- `.env` created from `.env.example` with production-only secrets
- `keycloak/cms-realm.json`

The `.env` file must include:

```dotenv
DEPLOY_HOST=cms.nursing.sksh.ac.in
PUBLIC_IP=137.97.6.147
LOCAL_HOST=172.16.7.209
```

## TLS certificates

The frontend container bind-mounts the server's `/etc/letsencrypt` directly
(read-only) and nginx reads `live/${DEPLOY_HOST}/fullchain.pem` /
`privkey.pem` straight from it — see the `frontend` service's `volumes` and
`SSL_CERT_PATH`/`SSL_KEY_PATH` env vars in `docker-compose.yml`. There is no
copy step anywhere: `scripts/deploy-209.sh` never stages or installs a TLS
file. Whoever renews the certificate — IT places the renewed
fullchain.pem/privkey.pem directly on the server under
`/etc/letsencrypt/live/${DEPLOY_HOST}/` — only needs to reload the frontend
container afterward for it to take effect:

```bash
docker exec skscms209-frontend-1 nginx -s reload
```

Mounting the whole `/etc/letsencrypt` tree (not just `live/<domain>/`) is
required because `live/<domain>/fullchain.pem` is a symlink whose relative
target reaches two directories up into `archive/<domain>/` — mounting only
the `live` subdirectory breaks that symlink inside the container.

Note: automated certbot renewal (`authenticator = standalone`, via
`certbot.timer`) cannot currently succeed — inbound TCP/80 on the public IP
(`137.97.6.147`) is unreachable from the internet (confirmed with a
`certbot ... --dry-run`: "Timeout during connect (likely firewall
problem)"), so the HTTP-01 challenge never completes regardless of
authenticator (standalone or webroot). Only port 443 is open externally.
This is why the cert has to be renewed by IT directly rather than by the
server's own certbot timer — fixing it would need port 80 opened at the
firewall in front of 137.97.6.147, which is outside this server.

The Let's Encrypt certificate is valid for `cms.nursing.sksh.ac.in` only. It
will not remove browser certificate warnings for `https://137.97.6.147` or
`https://172.16.7.209` unless the certificate includes those IP addresses as
Subject Alternative Names and the issuing CA is trusted by the client
browser.

## Compose project name

Always use project name `skscms209` so volumes and resources remain isolated:

```bash
docker compose -p skscms209 -f docker-compose.yml --env-file .env up -d
```

## Ports

- `80` — frontend / public entry point
- `8080` — backend, proxied by frontend nginx
- `8180` — Keycloak, proxied under `/realms/`
- `5433` — PostgreSQL

## Health checks

```bash
docker compose -p skscms209 -f docker-compose.yml --env-file .env ps
curl -k -i https://cms.nursing.sksh.ac.in/
curl -k -i https://137.97.6.147/
curl -k -i https://172.16.7.209/
curl -k -i https://cms.nursing.sksh.ac.in/realms/cms/.well-known/openid-configuration
curl -k -i https://137.97.6.147/realms/cms/.well-known/openid-configuration
curl -k -i https://172.16.7.209/realms/cms/.well-known/openid-configuration
curl -k -i https://cms.nursing.sksh.ac.in/api/v1/health
```

