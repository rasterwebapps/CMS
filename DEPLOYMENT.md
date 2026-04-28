# CMS Deployment Guide

## Server Details

| Item              | Value                        |
|-------------------|------------------------------|
| Server IP         | 172.17.1.243                 |
| Server User       | raster                       |
| Server Password   | ra5terpass@1234              |
| Public URL        | https://dev.raster.in:212    |
| Alt Public URL    | https://122.165.219.66:212   |
| Keycloak Admin    | http://172.17.1.243:8280     |
| DB Server         | 172.17.3.133:5435 (cmsdb)    |
| DB Credentials    | cms / cms                    |


---

## Quick Deploy (use this every time)

Open a terminal and run from the project root:

```bash
cd "/home/raster/Idea Projects/SKSCMS"
```

### Full rebuild (frontend + backend changed)
```bash
./scripts/deploy.sh
```

### Frontend only changed (Angular / UI)
```bash
./scripts/deploy.sh frontend
```

### Backend only changed (Java / API)
```bash
./scripts/deploy.sh backend
```

| Mode      | Shorthand         | Time        |
|-----------|-------------------|-------------|
| Full      | `deploy.sh`       | 10–15 min   |
| Frontend  | `deploy.sh front` | 3–4 min     |
| Backend   | `deploy.sh back`  | 4–6 min     |

---

## Step-by-Step Manual Process

If the script fails for any reason, follow these steps manually.

### Step 1 — Sync code to server

```bash
cd "/home/raster/Idea Projects/SKSCMS"

SSHPASS="ra5terpass@1234" sshpass -e rsync -avz --progress \
  --exclude='.git' \
  --exclude='node_modules' \
  --exclude='frontend/dist' \
  --exclude='frontend/.angular' \
  --exclude='backend/build' \
  --exclude='backend/.gradle' \
  --exclude='.idea' \
  --exclude='scripts/__pycache__' \
  -e "ssh -o StrictHostKeyChecking=no" \
  ./ raster@172.17.1.243:~/skscms/
```

### Step 2 — Build Docker images on the server

```bash
SSHPASS="ra5terpass@1234" sshpass -e ssh -o StrictHostKeyChecking=no raster@172.17.1.243 \
  "echo 'ra5terpass@1234' | sudo -S docker compose -f ~/skscms/docker-compose.yml build --no-cache"
```

### Step 3 — Start all containers

```bash
SSHPASS="ra5terpass@1234" sshpass -e ssh -o StrictHostKeyChecking=no raster@172.17.1.243 \
  "echo 'ra5terpass@1234' | sudo -S docker compose -f ~/skscms/docker-compose.yml up -d"
```

### Step 4 — Verify everything is running

```bash
SSHPASS="ra5terpass@1234" sshpass -e ssh -o StrictHostKeyChecking=no raster@172.17.1.243 \
  "echo 'ra5terpass@1234' | sudo -S docker compose -f ~/skscms/docker-compose.yml ps"
```

All four containers (`cms-keycloak`, `cms-backend`, `cms-frontend`) should show **Up**.

---

## Architecture on the Server

```
Internet
   │
   ├── dev.raster.in:212  ──►  172.17.1.243:8443  ──►  Docker: nginx (port 443)
   │                                                          │
   │                                                          ├──► /realms/*    → keycloak:8280
   │                                                          ├──► /api/*       → backend:8080
   │                                                          └──► /*           → Angular static files
   │
   └── dev.raster.in:80   ──►  redirects to https://dev.raster.in:212
```

### Docker Containers

| Container     | Image                        | Ports (host→container)       |
|---------------|------------------------------|------------------------------|
| cms-frontend  | skscms-frontend (nginx)      | 80→80, 8443→443              |
| cms-backend   | skscms-backend (Spring Boot) | internal only (8080)         |
| cms-keycloak  | keycloak:26.0                | 8280→8280                    |

### Docker Network
- Custom bridge: `10.100.0.0/24` (avoids conflict with server's 172.17.x.x subnet)

---

## Managing Containers on the Server

SSH into the server first:
```bash
ssh raster@172.17.1.243
# password: ra5terpass@1234
```

Then use these commands:

```bash
# Check container status
sudo docker compose -f ~/skscms/docker-compose.yml ps

# View live logs (all containers)
sudo docker compose -f ~/skscms/docker-compose.yml logs -f

# View logs for one service
sudo docker compose -f ~/skscms/docker-compose.yml logs -f backend
sudo docker compose -f ~/skscms/docker-compose.yml logs -f frontend
sudo docker compose -f ~/skscms/docker-compose.yml logs -f keycloak

# Stop everything
sudo docker compose -f ~/skscms/docker-compose.yml down

# Restart a single service
sudo docker compose -f ~/skscms/docker-compose.yml restart backend

# Check disk usage
sudo docker system df
```

---

## Keycloak Administration

Keycloak admin console (accessible only within the LAN):
```
http://172.17.1.243:8280
Username: admin
Password: admin
Realm:    cms
```

**Important:** Keycloak uses no persistent volume — it re-imports `cms-realm.json`
on every fresh start. If you update `infrastructure/keycloak/cms-realm.json`, the
changes apply automatically on the next `docker compose down && up`.

For live realm changes (without restart), use the Admin REST API:
```bash
# Get admin token
TOKEN=$(curl -s -X POST http://172.17.1.243:8280/realms/master/protocol/openid-connect/token \
  -d 'client_id=admin-cli&grant_type=password&username=admin&password=admin' \
  | python3 -c 'import json,sys; print(json.load(sys.stdin)["access_token"])')

# List clients
curl -s http://172.17.1.243:8280/admin/realms/cms/clients \
  -H "Authorization: Bearer $TOKEN" | python3 -m json.tool
```

---

## Database

The CMS uses a PostgreSQL database running in Docker on a separate server:

| Item     | Value              |
|----------|--------------------|
| Host     | 172.17.3.133       |
| Port     | 5435               |
| Database | cmsdb              |
| Username | cms                |
| Password | cms                |

To connect manually:
```bash
psql postgresql://cms:cms@172.17.3.133:5435/cmsdb
```

Database migrations run automatically via Flyway on every backend startup.
Migration files are in `backend/src/main/resources/db/migration/`.

---

## SSL Certificate

The app uses a self-signed certificate valid for 10 years, covering:
- `dev.raster.in` (CN)
- `122.165.219.66` (SAN IP)
- `172.17.1.243` (SAN IP)

Files: `frontend/ssl/self.crt` and `frontend/ssl/self.key`

**Browser warning:** On first visit, click **Advanced → Proceed to dev.raster.in**.
This is a one-time action per browser.

To regenerate the certificate (e.g., after domain change):
```bash
openssl req -x509 -nodes -days 3650 \
  -newkey rsa:2048 \
  -keyout frontend/ssl/self.key \
  -out frontend/ssl/self.crt \
  -subj "/CN=dev.raster.in" \
  -addext "subjectAltName=DNS:dev.raster.in,IP:122.165.219.66,IP:172.17.1.243"
```
Then run a full deploy.

---

## Troubleshooting

### App loads but screens are blank / login broken
- Keycloak JWT issuer mismatch — check that `KEYCLOAK_ISSUER_URI` in `docker-compose.yml`
  matches the URL the browser uses to access the app.
- Clear browser cache and try in an incognito window.
- Check backend logs: `sudo docker logs cms-backend --tail=50`

### Cannot reach https://dev.raster.in:212
- Verify port 212 is forwarded on the router to `172.17.1.243:8443`.
- Check that the containers are running: `sudo docker compose -f ~/skscms/docker-compose.yml ps`

### Containers keep restarting
```bash
sudo docker logs cms-backend --tail=100
sudo docker logs cms-keycloak --tail=100
```
Common cause: database not reachable (`172.17.3.133:5435`).

### Port 8443 already in use
Another service may be using port 8443. Check with:
```bash
sudo ss -tlnp sport = :8443
```

### Out of disk space on server
```bash
sudo docker system prune -f        # remove unused images/containers
sudo docker image prune -a -f      # remove ALL unused images
```

---

## Tech Stack Reference

| Layer     | Technology                          | Version  |
|-----------|-------------------------------------|----------|
| Frontend  | Angular                             | 21       |
| Styling   | Angular Material + Tailwind + SCSS  | —        |
| Backend   | Spring Boot                         | 3.4.5    |
| Language  | Java                                | 21       |
| Build     | Gradle (Kotlin DSL)                 | 8.12     |
| Auth      | Keycloak (OAuth2 / JWT)             | 26.0     |
| Database  | PostgreSQL                          | 17       |
| Migrations| Flyway                              | —        |
| Server    | Ubuntu, Docker Compose              | —        |
