# Security Hardening Report — OneCMS / SKS College Of Nursing
**Server:** 172.16.7.209 (Internal LXC — Proxmox ID 123)
**Hardened on:** 2026-05-27
**Signed off by:** ravishankar.r@raster.sksh.ac.in
**Status:** ✅ Production-ready for internal use — public IP pending SSL setup

---

## Phase 0 — Pre-Flight

| # | Check | Status | Notes |
|---|-------|--------|-------|
| 0.1 | `.env` audited, confirmed in `.gitignore`, git history checked for leaks | ✅ Done | |
| 0.2 | All secrets rotated — DB password, Keycloak client secret, JWT secret | ✅ Done | |
| 0.3 | SSL certificate provisioned via Certbot (Let's Encrypt) | ⏸ Deferred | Server is internal-only; no public domain reachable yet. Self-signed cert in use. |
| 0.4 | Proxmox snapshot taken before hardening (`pre-hardening-20260527`) | ✅ Done | |
| 0.5 | Current docker-compose state documented as rollback reference | ✅ Done | Backed up to `/docker_data/skscms209/` |

---

## Phase 1 — Network Perimeter

| # | Check | Status | Notes |
|---|-------|--------|-------|
| 1.1 | UFW installed — default deny incoming, allow outgoing | ✅ Done | |
| 1.2 | Port 80/tcp open to public | ✅ Done | |
| 1.3 | SSH (port 22) restricted to office IP `172.17.3.133` only | ✅ Done | |
| 1.4 | Internal ports 8080, 8180, 5433 verified NOT open to public | ✅ Done | Confirmed via `ufw status` |
| 1.5 | UFW enabled and active | ✅ Done | |
| 1.6 | HTTPS live on port 443 with self-signed certificate | ✅ Done | Replace with Let's Encrypt cert when domain is live |
| 1.7 | HTTP → HTTPS permanent redirect (301) active | ✅ Done | Verified with `curl -sI http://172.16.7.209` |
| 1.8 | HSTS header active (`max-age=31536000; includeSubDomains`) | ✅ Done | Verified with `curl -sI https://172.16.7.209 --insecure` |
| 1.9 | TLS 1.2 + TLS 1.3 only — TLS 1.0 and 1.1 disabled | ✅ Done | `ssl_protocols TLSv1.2 TLSv1.3` in nginx template |
| 1.10 | nmap scan clean — only ports 80 and 443 respond | ✅ Done | Verified: `22/tcp open (from allowed IP), 80/tcp open, 443/tcp open, 65532 filtered` |

---

## Phase 2 — Auth & Data Layer Hardening

### Keycloak

| # | Check | Status | Notes |
|---|-------|--------|-------|
| 2.1 | Keycloak default `admin` password changed | ✅ Done | |
| 2.2 | Brute-force protection enabled on `cms` realm | ✅ Done | Mode: Lockout temporarily · Max failures: 5 · Wait: 30s · Max wait: 15 min |
| 2.3 | Password policy set on `cms` realm | ✅ Done | Min length: 8 · Uppercase required · Digits required · Not username |
| 2.4 | `Require SSL → All requests` in Realm Settings | ⏸ Deferred | Enable only after HTTPS with real cert is live |
| 2.5 | Wildcard `*` removed from Valid Redirect URIs | ✅ Done | Exact URIs: `cms.nursing.sksh.ac.in`, `137.97.6.147`, `172.16.7.209` |
| 2.6 | Self-registration disabled (`User registration → OFF`) | ✅ Done | Was already off |
| 2.7 | Token lifetimes set — Access: 15 min | ✅ Done | |
| 2.8 | Keycloak `/admin/` path blocked via Nginx (returns 403) | ✅ Done | Verified from browser |

### PostgreSQL

| # | Check | Status | Notes |
|---|-------|--------|-------|
| 2.9 | PostgreSQL bound to `127.0.0.1` only (`listen_addresses`) | ✅ Done | Changed in `postgresql.conf`, restarted container |
| 2.10 | Dedicated low-privilege app user `cms_app` created | ✅ Done | Grants: CONNECT, USAGE, SELECT/INSERT/UPDATE/DELETE on all tables + sequences. Backend switch pending redeploy. |
| 2.11 | `pg_hba.conf` audited — open remote rule `host all all all` removed | ✅ Done | Only loopback (127.0.0.1, ::1) and local socket connections remain |
| 2.12 | Connection logging enabled (`log_connections = on`) | ✅ Done | |

---

## Phase 3 — Application Layer Hardening

### Spring Boot

| # | Check | Status | Notes |
|---|-------|--------|-------|
| 3.1 | Spring Boot bound to `127.0.0.1` only (`SERVER_ADDRESS=127.0.0.1`) | ✅ Done | Verified: `ss -tlnp` shows `[::ffff:127.0.0.1]:8080` |
| 3.2 | Swagger UI and API docs disabled in production | ✅ Done | `SPRINGDOC_API_DOCS_ENABLED=false`, `SPRINGDOC_SWAGGER_UI_ENABLED=false` |
| 3.3 | Actuator restricted to health endpoint only, details hidden | ✅ Done | `MANAGEMENT_ENDPOINTS_WEB_EXPOSURE_INCLUDE=health`, `MANAGEMENT_ENDPOINT_HEALTH_SHOW_DETAILS=never` |
| 3.4 | CORS locked to exact production domains — no wildcards | ✅ Done | `CORS_ALLOWED_ORIGINS` has specific URLs only |
| 3.5 | Production profile active (`SPRING_PROFILES_ACTIVE=209`) | ✅ Done | |

### Nginx

| # | Check | Status | Notes |
|---|-------|--------|-------|
| 3.6 | Security headers set — X-Frame-Options, X-Content-Type-Options, Referrer-Policy, Permissions-Policy | ✅ Done | Applied in nginx.conf.template |
| 3.7 | `server_tokens off` — nginx version hidden | ✅ Done | Verified: `Server: nginx` (no version) |
| 3.8 | Rate limiting on auth and API endpoints | ✅ Done | Auth zone: 20r/s · API zone: 30r/s |
| 3.9 | `client_max_body_size 10m` | ✅ Done | |
| 3.10 | `autoindex off` | ✅ Done | |

---

## Phase 4 — OS & Docker Hardening

| # | Check | Status | Notes |
|---|-------|--------|-------|
| 4.1 | Docker socket fixed — `sksadmin` added to `docker` group | ✅ Done | No more `chmod 666` on docker.sock |
| 4.2 | SSH password authentication disabled (`PasswordAuthentication no`) | ✅ Done | Key-based auth confirmed working before disabling |
| 4.3 | `fail2ban` installed and watching SSH — max 5 retries, 1hr ban | ✅ Done | `fail2ban-client status sshd` confirmed active |
| 4.4 | Unattended security upgrades enabled | ✅ Done | `APT::Periodic::Unattended-Upgrade "1"` |
| 4.5 | Sudoers audited — no `NOPASSWD` entries | ✅ Done | Clean |
| 4.6 | Cron jobs audited — no unknown jobs | ✅ Done | Leftover `certbot` cron removed |

---

## Phase 5 — Backup & Recovery

| # | Check | Status | Notes |
|---|-------|--------|-------|
| 5.1 | Automated daily PostgreSQL dump scheduled at 2 AM | ✅ Done | Script: `/usr/local/bin/pg_backup.sh` · Cron: `/etc/cron.d/pg_backup` |
| 5.2 | Backups stored off-server | ⏸ Deferred | Must be set up before go-live — copy to NAS or secondary LXC |
| 5.3 | 14-day backup retention policy | ✅ Done | Built into backup script (`find -mtime +14 -delete`) |
| 5.4 | Keycloak realm exported to JSON (`cms-realm.json`, 77K) | ✅ Done | Saved to `/backups/keycloak-export-20260527/` |
| 5.5 | Full restore drill completed — table counts matched (179/179) | ✅ Done | Restored to `cms_restore_test`, verified, dropped |
| 5.6 | Post-hardening Proxmox snapshot | ⏸ Pending | Take on Proxmox host after this session |

---

## Phase 6 — Monitoring & Observability

| # | Check | Status | Notes |
|---|-------|--------|-------|
| 6.1 | Docker log rotation configured (`max-size: 10m, max-file: 3`) | ✅ Done | Set in `/etc/docker/daemon.json` |
| 6.2 | Spring Boot log level set to WARN in production | ✅ Done | `LOGGING_LEVEL_ROOT=WARN` |
| 6.3 | Uptime monitoring — UptimeRobot or Grafana Cloud | ⏸ Deferred | Set up when public domain `cms.nursing.sksh.ac.in` is live |
| 6.4 | Keycloak login event logging enabled (`Save Events → ON`) | ✅ Done | Events: LOGIN, LOGIN_ERROR, LOGOUT · Retention: 90 days |
| 6.5 | Disk space alert at 80% for `/` and `/docker_root` | ✅ Done | Script: `/usr/local/bin/disk_alert.sh` · Cron: daily at 8 AM |
| 6.6 | All Docker containers set to `restart: unless-stopped` | ✅ Done | All 4 containers configured |

---

## Phase 7 — Final Validation

| # | Check | Status | Notes |
|---|-------|--------|-------|
| 7.1 | nmap scan — only ports 80 and 443 open | ✅ Done | 65532 ports filtered |
| 7.2 | SSL Labs A+ rating | ⏸ Deferred | Run after real SSL cert is installed on public domain |
| 7.3 | securityheaders.com — all green | ⏸ Deferred | Run after public domain is live |
| 7.4 | Keycloak `/admin/` returns 403 from public browser | ✅ Done | Verified |
| 7.5 | Spring Boot port 8080 unreachable from outside | ✅ Done | Connection refused / timeout |
| 7.6 | PostgreSQL port 5433 unreachable from outside | ✅ Done | Connection refused / timeout |
| 7.7 | All user roles verified — Admin, Front Office, Faculty, Cashier | ✅ Done | All dashboards load correctly |
| 7.8 | HTTP → HTTPS redirect verified | ✅ Done | `curl -sI http://172.16.7.209` → 301 to `https://` |
| 7.9 | Final sign-off | ✅ Done | Signed off by ravishankar.r@raster.sksh.ac.in · 2026-05-27 |

---

## Deferred Items — Must Complete Before Public IP is Assigned

| Item | Action Required |
|------|----------------|
| 0.3 SSL Certificate | Run Certbot once `cms.nursing.sksh.ac.in` points to the server |
| 2.4 Keycloak Require SSL | Enable in Realm Settings after real cert is live |
| 5.2 Off-server backups | Set up rsync to NAS or secondary LXC |
| 5.6 Post-hardening snapshot | Take on Proxmox host |
| 6.3 Uptime monitoring | Set up UptimeRobot on `/health` or login page |
| 7.2 SSL Labs A+ | Run at `https://www.ssllabs.com/ssltest/` after real cert |
| 7.3 securityheaders.com | Run at `https://securityheaders.com` after public domain is live |
| — | Backend DB user switched from `cms_user` to `cms_app` (redeploy required) |

---

## Stack Reference

| Component | Port | Exposure |
|-----------|------|----------|
| Nginx | 80, 443 | ✅ Public |
| Spring Boot API | 8080 | 🔒 localhost only |
| Keycloak 26 | 8180 | 🔒 localhost only |
| PostgreSQL 17 | 5433 | 🔒 localhost only |

**Server:** Ubuntu 24.04 LTS · Proxmox LXC ID 123 · Docker host networking
**Deployment dir:** `/docker_data/skscms209/`
**Backups dir:** `/backups/`
