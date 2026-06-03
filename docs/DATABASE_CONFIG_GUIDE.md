# Database Configuration Guide

## Profiles Summary

| Run Command | Profile | Database | Flyway | Data Persists |
|-------------|---------|----------|--------|---------------|
| `./gradlew bootRun` | local | H2 in-memory | ❌ | ❌ |
| `SPRING_PROFILES_ACTIVE=prod ./gradlew bootRun` | prod | PostgreSQL | ✅ | ✅ |
| `docker compose up` | prod | PostgreSQL | ✅ | ✅ |

> **Default profile is `local` (H2).** Flyway migrations and data persistence only work with the `prod` profile and PostgreSQL.

---

## Running with PostgreSQL (Recommended for Development)

### Step 1 — Start PostgreSQL
```bash
docker run -d \
  --name cms-postgres \
  -e POSTGRES_DB=cmsdb \
  -e POSTGRES_USER=cms \
  -e POSTGRES_PASSWORD=cms \
  -p 5435:5432 \
  postgres:17
```

### Step 2 — Run Backend
```bash
cd backend
DB_URL=jdbc:postgresql://localhost:5435/cmsdb \
DB_USERNAME=cms \
DB_PASSWORD=cms \
SPRING_PROFILES_ACTIVE=prod ./gradlew bootRun
```

### Step 3 — Verify in Logs
Look for:
```
HikariPool-1 - Starting... jdbc:postgresql://localhost:5435/cmsdb
Flyway successfully applied X migrations
```

---

## Checking Which Database Is Active

```bash
# Check active profile
./gradlew bootRun 2>&1 | grep -i "active.*profile"

# Check datasource URL
./gradlew bootRun 2>&1 | grep -i "datasource.*url"

# Check if Flyway is running
./gradlew bootRun 2>&1 | grep -i flyway
```

If Flyway is disabled:
```
Flyway is disabled. Not executing any migrations.
```

---

## H2 Console (local profile only)

- URL: `http://localhost:8080/api/v1/h2-console`
- JDBC URL: `jdbc:h2:mem:cmsdb`
- Username: `sa` / Password: (empty)

---

## Verifying Database Content

```sql
-- Check current database
SELECT current_database(), version();

-- Check users
SELECT keycloak_username, email, r.name as role
FROM app_users u
JOIN app_roles r ON u.app_role_id = r.id;

-- Check Flyway migration history
SELECT version, description, success FROM flyway_schema_history ORDER BY installed_rank;
```

---

## Important Notes

1. **Migrations only run on PostgreSQL** (H2 + Flyway disabled in local profile).
2. **H2 data is lost on restart** — use PostgreSQL for any meaningful local development.
3. **Production uses `docker compose up`** — PostgreSQL container at port 5435, Flyway enabled automatically.
4. Running `./gradlew bootRun` without `SPRING_PROFILES_ACTIVE=prod` will always use H2 in-memory.
