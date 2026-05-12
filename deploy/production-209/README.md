# SKSCMS Production Deployment — 172.16.7.209

This folder is the dedicated production deployment bundle for `172.16.7.209`.

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
- `.env` created from `.env.example` with production-only secrets
- `keycloak/cms-realm.json`

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
curl -k -i https://172.16.7.209/
curl -k -i https://172.16.7.209/realms/cms/.well-known/openid-configuration
curl -k -i https://172.16.7.209/api/v1/health
```

