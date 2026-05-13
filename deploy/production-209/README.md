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
- `.env` created from `.env.example` with production-only secrets
- `keycloak/cms-realm.json`
- `ssl/self.crt` and `ssl/self.key` installed by `scripts/deploy-209.sh`

The `.env` file must include:

```dotenv
DEPLOY_HOST=cms.nursing.sksh.ac.in
PUBLIC_IP=137.97.6.147
LOCAL_HOST=172.16.7.209
```

## TLS certificates

Production TLS files are mounted into nginx at runtime and must not be committed
to Git or baked into the frontend Docker image.

The 209 deployment script reads these local files by default:

```bash
/home/raster/Downloads/Telegram Desktop/Certificate.txt
/home/raster/Downloads/Telegram Desktop/Intermediate Certificate.txt
/home/raster/Downloads/Telegram Desktop/RSA Private Key.txt
```

It validates that the certificate and private key match, concatenates
`Certificate.txt` + `Intermediate Certificate.txt` into a full-chain
`self.crt`, and installs these files on the server:

```bash
/docker_data/skscms209/ssl/self.crt
/docker_data/skscms209/ssl/self.key
```

Override the source paths when needed:

```bash
DEPLOY209_TLS_CERT_FILE=/path/to/Certificate.txt \
DEPLOY209_TLS_INTERMEDIATE_FILE=/path/to/Intermediate\ Certificate.txt \
DEPLOY209_TLS_PRIVATE_KEY_FILE=/path/to/RSA\ Private\ Key.txt \
./scripts/deploy-209.sh full
```

The provided Let's Encrypt certificate for `cms.nursing.sksh.ac.in` is valid for
the domain name. It will not remove browser certificate warnings for
`https://137.97.6.147` or `https://172.16.7.209` unless the certificate includes
those IP addresses as Subject Alternative Names and the issuing CA is trusted by
the client browser.

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

