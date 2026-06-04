# Security Hardening Checklist — Pre-Public-IP Go-Live

> **Purpose:** Every time this application is deployed to a new server and exposed to a public IP,
> this checklist MUST be completed in full before opening traffic.
> No phase may be skipped. Phases 0 → 1 → 2 → 7 are hard blockers for go-live.

---

## Stack Context

| Component | Port | Notes |
|-----------|------|-------|
| Nginx | 80, 443 | Public-facing reverse proxy |
| Spring Boot API | 8080 | Internal only — must never be public |
| Keycloak 26 | 8180 | Internal only — must never be public |
| PostgreSQL 17 | 5433 | Internal only — must never be public |
| OS | Ubuntu 24.04 LTS | Proxmox LXC, Docker host networking |

> If the stack changes on the new server, update this table before proceeding.

---

## Phase 0 — Pre-Flight (No Server Touches)

**Goal:** Prepare everything before any server changes. Zero downtime risk.

- [x] **0.1** Audit `.env` and confirm it is in `.gitignore`
  — Check git history for any past leaks: `git log --all --full-history -- .env`
- [x] **0.2** Rotate all secrets before deployment: DB password, Keycloak client secret, JWT secret, any API keys
- [x] **0.3** Procure SSL certificate — Let's Encrypt via Certbot (auto-renews, expires 2026-09-02)
- [x] **0.4** Take a snapshot / backup of the server before any changes (Proxmox snapshot or equivalent)
- [x] **0.5** Document the current working docker-compose state as a rollback reference

---

## Phase 1 — Network Perimeter (Maintenance Window Required)

**HARD BLOCKER — Nothing goes public before this phase is complete.**

**Goal:** Lock down what is publicly reachable before assigning a public IP.

- [x] **1.1** Install `ufw` and set default deny incoming:
  ```bash
  ufw default deny incoming
  ufw default allow outgoing
  ```
- [x] **1.2** Allow only public-facing ports:
  ```bash
  ufw allow 80/tcp
  ufw allow 443/tcp
  ```
- [x] **1.3** Restrict SSH to known admin/office IPs only:
  ```bash
  ufw allow from <office-ip> to any port 22
  ```
- [x] **1.4** Explicitly confirm internal ports are NOT open to public (ufw default deny handles this, but verify):
  — Ports 8080, 8180, 5433 must be unreachable from outside
- [x] **1.5** Enable ufw: `ufw enable`
- [x] **1.6** Install SSL certificate and configure Nginx HTTPS on port 443
  — Let's Encrypt cert installed via Certbot standalone
  — Certs at `/docker_data/skscms209/ssl/`, auto-renewal hook configured
- [x] **1.7** Add HTTP → HTTPS permanent redirect in Nginx (301) — already in nginx.conf
- [x] **1.8** Add HSTS header — already in nginx.conf
- [x] **1.9** Enforce TLS 1.2+ only — Mozilla Intermediate cipher suite added to nginx.conf
- [x] **1.10** nmap from external machine — only 80 and 443 open (22, 5433, 8080, 8180 all filtered) ✓

**Exit criteria met:** nmap clean. SSL Labs A+. ✓

---

## Phase 2 — Auth & Data Layer Hardening

**HARD BLOCKER — Must complete before real users access the system.**

**Goal:** Harden Keycloak and PostgreSQL before any real data enters the system.

### Keycloak

- [x] **2.1** Changed default `admin` password
- [x] **2.2** Brute-force protection enabled — Realm Settings → Security Defenses → ON (max 5 failures, 15 min lockout)
- [x] **2.3** Password policy set — min 8 chars, uppercase, digit required
- [x] **2.4** Require SSL → External requests set (NOT "All requests" — internal localhost calls must stay HTTP)
- [x] **2.5** Valid Redirect URIs locked to `https://cms.nursing.sksh.ac.in/*` only — no `*`
- [x] **2.6** Self-registration disabled — Realm Settings → Login → OFF
- [x] **2.7** Token lifetimes set — Access: 10 min, SSO Idle: 30 min, SSO Max: 8 hours
- [x] **2.8** Keycloak admin console blocked from public Nginx — `location /admin/ { return 403; }` in nginx.conf

### PostgreSQL

- [x] **2.9** PostgreSQL bound to localhost — ufw blocks port 5433 externally (verified via nmap)
- [x] **2.10** Low-privilege app user `cms_app` created — SELECT/INSERT/UPDATE/DELETE only, no superuser
- [x] **2.11** pg_hba.conf audited — only 127.0.0.1 and ::1 connections allowed, no remote entries
- [x] **2.12** Connection logging enabled — `log_connections = on` applied via ALTER SYSTEM

---

## Phase 3 — Application Layer Hardening

**Goal:** Harden Spring Boot API and Nginx before exposing to real traffic.

### Spring Boot

- [x] **3.1** Spring Boot bound to localhost — `server.address: 127.0.0.1` in application-prod.yml
- [x] **3.2** Swagger UI and API docs disabled in prod — `springdoc.api-docs.enabled: false`
- [x] **3.3** Actuator restricted — only `/health` exposed, details hidden
- [x] **3.4** CORS locked to exact production domains via `CORS_ALLOWED_ORIGINS` env var — no `*`
- [x] **3.5** `SPRING_PROFILES_ACTIVE: 209` set in docker-compose

### Nginx

- [x] **3.6** All security headers present — HSTS, X-Content-Type-Options, X-Frame-Options, Referrer-Policy, Permissions-Policy, CSP
- [x] **3.7** `server_tokens off` — nginx version hidden
- [x] **3.8** Rate limiting on auth and API endpoints — 20r/s auth zone, 30r/s API zone
- [x] **3.9** `client_max_body_size 10m` set
- [x] **3.10** `autoindex off` set

---

## Phase 4 — OS & Docker Hardening

**Goal:** Close OS-level gaps that survive server restarts.

- [x] **4.1** `sksadmin` added to docker group — no more `chmod 666` on docker socket
- [x] **4.2** SSH password authentication disabled — key-only login enforced
- [x] **4.3** fail2ban installed and running — SSH: maxretry=5, bantime=3600s
- [x] **4.4** Unattended security upgrades enabled
- [x] **4.5** Sudoers audited — no NOPASSWD entries found
- [x] **4.6** Cron jobs audited — all jobs legitimate (certbot, pg_backup, disk_alert, e2scrub_all, sysstat)

---

## Phase 5 — Backup & Recovery

**Goal:** Ensure data can be recovered before any real data enters the system.

- [x] **5.1** Automated daily PostgreSQL dump — `/etc/cron.d/pg_backup` runs at 02:00, gzipped
- [x] **5.2** Backups stored on separate Proxmox volume (`storage_archive/subvol-123-disk-0`, 100GB) — ⚠️ not truly off-server; move to NAS when possible
- [x] **5.3** 14-day retention — `find $BACKUP_DIR -mtime +14 -delete` in pg_backup.sh
- [x] **5.4** Keycloak realm exported to `/backups/cms-keycloak-realm-20260604.json`
- [x] **5.5** Restore drill completed — backup restored to `cms_restore_test`, tables verified, test DB dropped
- [x] **5.6** Final Proxmox snapshot taken — `pre-golive-2026-06-04` on LXC 123

---

## Phase 6 — Monitoring & Observability

**Goal:** Know when something goes wrong in production.

- [x] **6.1** Nginx log rotation configured — `/etc/logrotate.d/docker-nginx`, daily, 30-day retention
- [x] **6.2** Spring Boot log level set to WARN in prod — `logging.level.root: WARN` in application-prod.yml
- [x] **6.3** UptimeRobot monitoring active — HTTPS check on `cms.nursing.sksh.ac.in`, 5-min interval
- [x] **6.4** Keycloak login event logging enabled — Save Events ON, 30-day retention
- [x] **6.5** Disk space alert active — `/usr/local/bin/disk_alert.sh` runs daily at 08:00, alerts at 80%
- [x] **6.6** All Docker containers set to `restart: unless-stopped` in docker-compose

---

## Phase 7 — Final Validation & Go-Live Sign-Off

**HARD BLOCKER — No public traffic until all items below are checked.**

- [x] **7.1** nmap from external machine — only 80 and 443 open ✓
- [x] **7.2** SSL Labs — **A+** ✓
- [x] **7.3** securityheaders.com — **A+** ✓
- [x] **7.4** Keycloak admin console returns 403 from public browser ✓
- [x] **7.5** Spring Boot API port 8080 unreachable from outside (filtered) ✓
- [x] **7.6** PostgreSQL port 5433 unreachable from outside (filtered) ✓
- [x] **7.7** All roles verified — Admin, Front Office, Cashier, Faculty, Student ✓
- [x] **7.8** HTTP → HTTPS redirect confirmed (301) ✓
- [x] **7.9** Go-live sign-off given by Tech Lead — **2026-06-04** ✓

---

## Quick Reference — Hard Blockers Summary

| Phase | Blocker |
|-------|---------|
| Phase 0 | `.env` not in git, all secrets rotated, SSL cert ready |
| Phase 1 | Firewall active, only 80/443 open, HTTPS live, nmap clean |
| Phase 2 | Keycloak admin changed, SSL required, PostgreSQL localhost-only |
| Phase 7 | SSL Labs A+, nmap clean, all roles verified, explicit go-live instruction |

---

## Server-Specific Notes (209 Server)

> Update this section whenever a new server is added.

### 172.16.7.209 (Current Production)
- Proxmox LXC ID 123, Ubuntu 24.04, nesting enabled
- Docker uses `network_mode: host` (bridge networking fails on this LXC)
- PostgreSQL exposed on 5433 (host has PG12 on 5432)
- SSH: `sksadmin@172.16.7.209`
- Deployment dir: `/docker_data/skscms209`
- SSL: Let's Encrypt via Certbot, auto-renews, expires 2026-09-02
- Status: **LIVE — public IP assigned 2026-06-04, full hardening complete, SSL Labs A+**

### Pending (post go-live)
- Move backups from Proxmox volume to off-server NAS
- Run `sudo apt upgrade` for 12 pending packages (next maintenance window)

### Adding a New Server
When deploying to a new server, add a section above with:
- Server IP, OS, hostname
- Any network/container quirks
- Port layout if different from standard
- Status (internal / public-facing)
