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

- [ ] **0.1** Audit `.env` and confirm it is in `.gitignore`
  — Check git history for any past leaks: `git log --all --full-history -- .env`
- [ ] **0.2** Rotate all secrets before deployment: DB password, Keycloak client secret, JWT secret, any API keys
- [ ] **0.3** Procure SSL certificate
  — Option A: Let's Encrypt via Certbot (free, auto-renews)
  — Option B: Upload commercial cert to `/opt/cms/ssl/`
- [ ] **0.4** Take a snapshot / backup of the server before any changes (Proxmox snapshot or equivalent)
- [ ] **0.5** Document the current working docker-compose state as a rollback reference

---

## Phase 1 — Network Perimeter (Maintenance Window Required)

**HARD BLOCKER — Nothing goes public before this phase is complete.**

**Goal:** Lock down what is publicly reachable before assigning a public IP.

- [ ] **1.1** Install `ufw` and set default deny incoming:
  ```bash
  ufw default deny incoming
  ufw default allow outgoing
  ```
- [ ] **1.2** Allow only public-facing ports:
  ```bash
  ufw allow 80/tcp
  ufw allow 443/tcp
  ```
- [ ] **1.3** Restrict SSH to known admin/office IPs only:
  ```bash
  ufw allow from <office-ip> to any port 22
  ```
- [ ] **1.4** Explicitly confirm internal ports are NOT open to public (ufw default deny handles this, but verify):
  — Ports 8080, 8180, 5433 must be unreachable from outside
- [ ] **1.5** Enable ufw: `ufw enable`
- [ ] **1.6** Install SSL certificate and configure Nginx HTTPS on port 443
  — Restore the 443 block and TLS config in `frontend/nginx.conf`
  — Mount cert at `/opt/cms/ssl/` on server
  — Rebuild and push frontend image after nginx.conf update
- [ ] **1.7** Add HTTP → HTTPS permanent redirect in Nginx (301)
- [ ] **1.8** Add HSTS header:
  `Strict-Transport-Security: max-age=31536000; includeSubDomains`
- [ ] **1.9** Enforce TLS 1.2+ only; disable TLS 1.0 and 1.1
  — Use Mozilla SSL Config Generator (Intermediate profile) for cipher suites
- [ ] **1.10** Verify with nmap from an external machine:
  ```bash
  nmap -p 1-65535 <public-ip>
  ```
  Only ports 80 and 443 should respond.

**Exit criteria:** `nmap` shows only 80 and 443. SSL Labs score A or above.

---

## Phase 2 — Auth & Data Layer Hardening

**HARD BLOCKER — Must complete before real users access the system.**

**Goal:** Harden Keycloak and PostgreSQL before any real data enters the system.

### Keycloak

- [ ] **2.1** Change the default `admin` password immediately after first login
- [ ] **2.2** Enable brute-force protection on all realms
  — Realm Settings → Security Defenses → Brute Force Detection → ON
- [ ] **2.3** Set password policies on all realms (min 8 chars, mixed case, digit required)
- [ ] **2.4** Set `Require SSL → All requests` in Realm Settings (only after HTTPS is live)
- [ ] **2.5** Remove `*` from Valid Redirect URIs — replace with exact production URLs only
- [ ] **2.6** Disable self-registration if not a product feature (`Realm Settings → Login → User registration → OFF`)
- [ ] **2.7** Set short token lifetimes (Access: 5–15 min, Refresh: 8–24 hours)
- [ ] **2.8** Restrict Keycloak Admin Console from public Nginx — add to nginx.conf:
  ```nginx
  location /auth/admin {
      allow 127.0.0.1;
      allow <office-ip>;
      deny all;
  }
  ```

### PostgreSQL

- [ ] **2.9** Bind PostgreSQL to localhost only — in `postgresql.conf`:
  `listen_addresses = '127.0.0.1'`
- [ ] **2.10** Create a dedicated low-privilege app DB user (not superuser):
  ```sql
  CREATE USER cms_app WITH PASSWORD '<strong-password>';
  GRANT CONNECT ON DATABASE cms TO cms_app;
  GRANT USAGE ON SCHEMA public TO cms_app;
  GRANT SELECT, INSERT, UPDATE, DELETE ON ALL TABLES IN SCHEMA public TO cms_app;
  ```
- [ ] **2.11** Audit `pg_hba.conf` — all remote connections should be rejected; only local/127.0.0.1 allowed
- [ ] **2.12** Enable connection logging (`log_connections = on` in `postgresql.conf`)

---

## Phase 3 — Application Layer Hardening

**Goal:** Harden Spring Boot API and Nginx before exposing to real traffic.

### Spring Boot

- [ ] **3.1** Bind Spring Boot to localhost only in `application-prod.properties`:
  `server.address=127.0.0.1`
- [ ] **3.2** Disable Swagger UI and API docs in production profile:
  `springdoc.api-docs.enabled=false`
  `springdoc.swagger-ui.enabled=false`
- [ ] **3.3** Disable or restrict Actuator endpoints:
  `management.endpoints.web.exposure.include=health`
  `management.endpoint.health.show-details=never`
- [ ] **3.4** Lock CORS to exact production frontend domain — remove any `*`:
  ```java
  config.setAllowedOrigins(List.of("https://your-domain.com"));
  ```
- [ ] **3.5** Ensure `spring.profiles.active=prod` is set and no dev/debug configs leak

### Nginx

- [ ] **3.6** Add security headers to Nginx config:
  ```nginx
  add_header X-Frame-Options "SAMEORIGIN" always;
  add_header X-Content-Type-Options "nosniff" always;
  add_header X-XSS-Protection "1; mode=block" always;
  add_header Referrer-Policy "strict-origin-when-cross-origin" always;
  add_header Content-Security-Policy "default-src 'self'; script-src 'self'; style-src 'self' 'unsafe-inline'; img-src 'self' data:; font-src 'self';" always;
  ```
- [ ] **3.7** Disable Nginx server version header:
  `server_tokens off;`
- [ ] **3.8** Add rate limiting on login and sensitive API endpoints:
  ```nginx
  limit_req_zone $binary_remote_addr zone=login:10m rate=5r/m;
  # Apply in location block: limit_req zone=login burst=3 nodelay;
  ```
- [ ] **3.9** Set reasonable max upload size: `client_max_body_size 10m;`
- [ ] **3.10** Disable directory listing: `autoindex off;`

---

## Phase 4 — OS & Docker Hardening

**Goal:** Close OS-level gaps that survive server restarts.

- [ ] **4.1** Fix Docker socket permanently — add user to docker group:
  ```bash
  usermod -aG docker sksadmin
  # Re-login or: newgrp docker
  ```
  — Never use `chmod 666 /run/docker.sock` in production
- [ ] **4.2** Disable SSH password authentication:
  In `/etc/ssh/sshd_config`:
  ```
  PasswordAuthentication no
  PubkeyAuthentication yes
  ```
  Then: `systemctl restart sshd`
  — Ensure SSH key is loaded BEFORE disabling password auth
- [ ] **4.3** Install and configure `fail2ban` for SSH:
  ```bash
  apt install fail2ban
  # /etc/fail2ban/jail.local: [sshd] enabled=true, maxretry=5, bantime=3600
  systemctl enable fail2ban && systemctl start fail2ban
  ```
- [ ] **4.4** Enable unattended security upgrades:
  ```bash
  apt install unattended-upgrades
  dpkg-reconfigure --priority=low unattended-upgrades
  ```
- [ ] **4.5** Audit sudoers — remove passwordless sudo if present:
  `visudo` → ensure `NOPASSWD` is not present for production accounts
- [ ] **4.6** Review and remove unknown cron jobs: `crontab -l` and `ls /etc/cron*`

---

## Phase 5 — Backup & Recovery

**Goal:** Ensure data can be recovered before any real data enters the system.

- [ ] **5.1** Set up automated daily PostgreSQL dump:
  ```bash
  # /etc/cron.d/pg_backup
  0 2 * * * postgres pg_dump -U cms_app cms | gzip > /backups/cms_$(date +\%Y\%m\%d).sql.gz
  ```
- [ ] **5.2** Store backups off-server (NAS, S3-compatible bucket, or separate LXC)
- [ ] **5.3** Set retention policy — keep at least 14 days of daily backups
- [ ] **5.4** Export Keycloak realm configuration as JSON and store off-server:
  ```bash
  docker exec <keycloak-container> /opt/keycloak/bin/kc.sh export --dir /tmp/export --realm cms-realm
  ```
- [ ] **5.5** **Run a full restore drill** — restore the dump to a test DB and verify data integrity before go-live
- [ ] **5.6** Take a final server snapshot after all phases are complete

---

## Phase 6 — Monitoring & Observability

**Goal:** Know when something goes wrong in production.

- [ ] **6.1** Configure Nginx log rotation (`/etc/logrotate.d/nginx` — rotate daily, keep 30 days)
- [ ] **6.2** Set Spring Boot log level to WARN/ERROR in prod profile:
  `logging.level.root=WARN`
  — Confirm no secrets, tokens, or passwords appear in log output
- [ ] **6.3** Set up uptime monitoring — UptimeRobot (free) or Grafana Cloud:
  — Monitor `/health` endpoint and the login page
  — Alert to email/Slack on downtime
- [ ] **6.4** Enable Keycloak login event logging and set alert on repeated failures:
  — Realm Settings → Events → Save Events → ON
- [ ] **6.5** Set up disk space alert — alert at 80% usage on `/` and `/docker_data`
- [ ] **6.6** Monitor Docker container health — ensure all containers auto-restart on failure:
  `restart: unless-stopped` in docker-compose.prod.yml for all services

---

## Phase 7 — Final Validation & Go-Live Sign-Off

**HARD BLOCKER — No public traffic until all items below are checked.**

- [ ] **7.1** Re-run `nmap -p 1-65535 <public-ip>` from an external machine — only 80 and 443 open
- [ ] **7.2** Run [SSL Labs](https://www.ssllabs.com/ssltest/) — target **A+** rating
- [ ] **7.3** Run [securityheaders.com](https://securityheaders.com) — fix any red/orange items
- [ ] **7.4** Confirm Keycloak admin console (`/auth/admin`) returns 403 from a public browser
- [ ] **7.5** Confirm Spring Boot API (`/api` or port 8080) is unreachable directly from outside
- [ ] **7.6** Confirm PostgreSQL port 5433 is unreachable from outside: `nc -zv <public-ip> 5433` should time out
- [ ] **7.7** Walk through all user roles (Admin, Front Office, Cashier, Faculty, Student) — verify login and data access
- [ ] **7.8** Verify HTTPS redirect works — `http://` requests must redirect to `https://`
- [ ] **7.9** Final sign-off by Tech Lead and get explicit instruction to open public IP

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
- Deployment dir: `/docker_data`
- Status: **Internal only — public IP not yet assigned**

### Adding a New Server
When deploying to a new server, add a section above with:
- Server IP, OS, hostname
- Any network/container quirks
- Port layout if different from standard
- Status (internal / public-facing)
