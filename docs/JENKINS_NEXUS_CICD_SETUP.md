
# Jenkins + Nexus CI/CD Setup Guide

## Architecture Overview

```
GitHub (code) ─── webhook ──▶ Jenkins (205 machine) ──▶ Nexus (artifacts + Docker images)
                                         │
                                         └── SSH ──▶ Deploy Server (docker-compose pull + up)
```

---

## Step 1 — Prerequisites on Jenkins Machine (205)

> **Build tool**: This is a **Gradle** project. The backend uses `./gradlew` (Gradle wrapper).
> No Maven installation is needed.

```bash
# Java 21 must be installed (for running Gradle builds)
java -version   # should show 21.x

# Gradle wrapper is already committed in the repo — no global Gradle install needed
# Jenkins will call: ./gradlew clean check jacocoTestReport

# Docker must be installed + Jenkins user must be in docker group
sudo usermod -aG docker jenkins
sudo systemctl restart jenkins

# Node.js 22 (for Angular builds) — install via nvm or apt
curl -fsSL https://deb.nodesource.com/setup_22.x | sudo -E bash -
sudo apt install -y nodejs
node --version   # 22.x
```

---

## Step 2 — Jenkins Plugin Installation

Go to **Manage Jenkins → Plugins → Available** and install:

| Plugin | Purpose |
|--------|---------|
| Git Plugin | GitHub checkout |
| GitHub Integration Plugin | Webhooks |
| Gradle Plugin | runs `./gradlew` |
| NodeJS Plugin | Angular `npm` builds |
| Nexus Artifact Uploader | Upload JARs |
| Nexus Platform Plugin | Full Nexus suite |
| Docker Pipeline | `docker.build()` / `.push()` |
| Pipeline | Declarative Jenkinsfile support |
| SSH Agent Plugin | SSH deploy step |
| HTML Publisher Plugin | Show JaCoCo reports |

---

## Step 3 — Jenkins Credentials Setup

Go to **Manage Jenkins → Credentials → System → Global credentials**:

### 3a. GitHub PAT
```
Kind:     Username with password
Username: <your-github-username>
Password: <PAT — needs repo + admin:repo_hook scopes>
ID:       github-credentials
```

### 3b. Nexus credentials
```
Kind:     Username with password
Username: <nexus deployment user>
Password: <nexus password>
ID:       nexus-credentials
```

### 3c. Deploy server SSH key
```
Kind:        SSH Username with private key
Username:    deploy
Private Key: (paste the private key)
ID:          deploy-ssh-key
```
> On the deploy server, add the corresponding public key to `/home/deploy/.ssh/authorized_keys`

---

## Step 4 — Jenkins Tool Configuration

**Manage Jenkins → Tools:**

### NodeJS
```
Name:    NodeJS-22
Version: 22.x  (auto-install)
```

---

## Step 5 — Nexus Repository Setup

In Nexus (http://<nexus-ip>:8081):

> **ℹ️ Gradle + Maven format explained:**
> This project builds with **Gradle** (not Maven). Gradle's `maven-publish` plugin publishes
> artifacts using the **Maven repository layout** (groupId/artifactId/version folder structure).
> That is why Nexus repos use format `maven2` — it refers to the artifact *layout*, not the
> build tool. This is the standard way Gradle projects publish to Nexus/Artifactory.

### Create Gradle artifact repositories (maven2 format)

In Nexus → **Repository → Repositories → Create repository**:

| Repository Name | Recipe | Version Policy | Deployment Policy | Use |
|----------------|--------|---------------|-------------------|-----|
| `cms-releases` | `maven2 (hosted)` | Release | Disable redeploy | Production JARs (e.g. `v1.0.0`) |
| `cms-snapshots` | `maven2 (hosted)` | Snapshot | Allow redeploy | CI builds from non-main branches |

**Settings for each:**
- Layout Policy: `Strict`
- Content Disposition: `Attachment`

### Create Docker registry (optional — if using Docker images via Nexus)
| Repository Name | Recipe | HTTP connector port |
|----------------|--------|---------------------|
| `cms-docker` | `docker (hosted)` | 8082 |

Enable: ☑ Allow anonymous docker pull (or configure auth)

### Create a deployment user
- **Security → Users → Create user**: `cms-deploy`  
- Password: (strong password — store in Jenkins credentials)
- Roles to assign:
  - `nx-repository-view-cms-releases-*`
  - `nx-repository-view-cms-snapshots-*`
  - `nx-repository-view-cms-docker-*` (if using Docker repo)

---

## Step 6 — GitHub Webhook

In your GitHub repo → **Settings → Webhooks → Add webhook**:

```
Payload URL:  http://<205-machine-ip>:8080/github-webhook/
Content type: application/json
Secret:       (optional, add to Jenkins GitHub server config)
Which events: Just the push event + Pull requests
Active:       ✓
```

> **Firewall**: Port 8080 on the 205 machine must be reachable from GitHub's IP ranges.
> If behind a firewall, use **ngrok** for testing: `ngrok http 8080`

---

## Step 7 — Create Jenkins Pipeline Job

1. **New Item → Pipeline** → name: `cms-pipeline`
2. **General:**
   - ☑ GitHub project: `https://github.com/<org>/SKSCMS`
3. **Build Triggers:**
   - ☑ GitHub hook trigger for GITScm polling
4. **Pipeline:**
   - Definition: `Pipeline script from SCM`
   - SCM: Git
   - Repository URL: `https://github.com/<org>/SKSCMS.git`
   - Credentials: `github-credentials`
   - Branch: `*/main`
   - Script Path: `Jenkinsfile`
5. **Save → Build Now** (first manual run)

---

## Step 8 — Deploy Server Setup

On the deploy server (where Docker images will run):

```bash
# Create app directory
sudo mkdir -p /opt/cms
sudo chown deploy:deploy /opt/cms

# Copy production compose file
scp docker-compose.prod.yml deploy@<deploy-server>:/opt/cms/docker-compose.yml

# Create .env file
cat > /opt/cms/.env <<EOF
NEXUS_URL=http://<nexus-ip>:8081
APP_VERSION=latest
DB_PASSWORD=<strong-password>
JWT_ISSUER_URI=http://<keycloak-ip>:8180/realms/cms
DEPLOY_HOST=<deploy-server-fqdn-or-ip>
EOF
chmod 600 /opt/cms/.env

# Login to Nexus Docker registry (if using Docker images)
docker login http://<nexus-ip>:8082 -u cms-deploy
```

---

## Step 9 — Nexus Docker registry config on deploy server

If Nexus Docker registry is HTTP (not HTTPS):
```bash
# /etc/docker/daemon.json
{
  "insecure-registries": ["<nexus-ip>:8082"]
}
sudo systemctl restart docker
```

---

## Environment Variables Reference (Jenkinsfile)

| Variable | Where to set | Example |
|----------|-------------|---------|
| `NEXUS_URL` | Jenkinsfile env | `http://192.168.1.50:8081` |
| `NEXUS_CREDENTIALS` | Jenkins credentials | ID: `nexus-credentials` |
| `DEPLOY_HOST` | Jenkinsfile env | `192.168.1.100` |
| `DEPLOY_USER` | Jenkinsfile env | `deploy` |
| `DEPLOY_SSH_KEY` | Jenkins credentials | ID: `deploy-ssh-key` |

---

## Quick Verification Checklist

- [ ] Jenkins can reach GitHub (`curl https://api.github.com`)
- [ ] Jenkins can reach Nexus (`curl http://<nexus-ip>:8081`)
- [ ] GitHub webhook sends 200 to Jenkins
- [ ] `./gradlew check` passes locally with 95% coverage
- [ ] Docker images build successfully on Jenkins agent
- [ ] Images appear in Nexus Docker registry after push
- [ ] Deploy server pulls images and services start healthy
- [ ] App reachable on deploy server port 80

---

## Troubleshooting

### Webhook not firing
```bash
# Check Jenkins logs
tail -f /var/log/jenkins/jenkins.log | grep -i webhook
# On GitHub: webhook → Recent Deliveries → check response
```

### Gradle publish fails (401)
```bash
# Verify env vars are set
./gradlew publish --info 2>&1 | grep -i nexus
# Make sure cms-deploy user has write access to the repo in Nexus
```

### Docker push fails (connection refused)
```bash
# Add insecure-registries to daemon.json on Jenkins machine too
# Restart Docker: sudo systemctl restart docker
```

### Coverage below 95%
```bash
# Run locally to see which classes need tests
./gradlew check jacocoTestReport
# Open: backend/build/reports/jacoco/test/html/index.html
```

