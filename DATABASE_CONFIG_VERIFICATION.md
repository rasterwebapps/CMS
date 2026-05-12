# Database Configuration Verification

## Current Configuration Analysis

### Default Profile (Local Development)
**File:** `backend/src/main/resources/application.yml`
```yaml
spring:
  profiles:
    default: local  # ← Default profile
```

**File:** `backend/src/main/resources/application-local.yml`
```yaml
spring:
  datasource:
    url: jdbc:h2:mem:cmsdb  # ← H2 IN-MEMORY DATABASE
    driver-class-name: org.h2.Driver
  
  jpa:
    hibernate:
      ddl-auto: create-drop  # ← Data LOST on restart
  
  flyway:
    enabled: false  # ← Migrations DO NOT RUN
```

### Production Profile (Docker/Deployment)
**File:** `backend/src/main/resources/application-prod.yml`
```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5435/cmsdb  # ← PostgreSQL
    driver-class-name: org.postgresql.Driver
  
  jpa:
    hibernate:
      ddl-auto: validate  # ← Schema validated only
  
  flyway:
    enabled: true  # ← Migrations RUN
```

---

## 🚨 **THE PROBLEM**

### When running `./gradlew bootRun` (default):
- ❌ **Database:** H2 in-memory (temporary)
- ❌ **Flyway:** DISABLED (migrations V97, V98 will NOT run)
- ❌ **Data persistence:** NONE (data lost on restart)
- ❌ **User Management Fix:** WILL NOT WORK

### When running via Docker Compose:
- ✅ **Database:** PostgreSQL (persistent)
- ✅ **Flyway:** ENABLED (migrations will run)
- ✅ **Data persistence:** YES
- ✅ **User Management Fix:** WILL WORK

---

## ✅ **VERIFICATION STEPS**

### Check Current Active Profile
```bash
cd backend
./gradlew bootRun 2>&1 | grep -i "active.*profile"
```

Look for:
- `The following profiles are active: local` → Using H2
- `The following profiles are active: prod` → Using PostgreSQL

### Check Database on Startup
```bash
./gradlew bootRun 2>&1 | grep -i -E "(h2|postgres|datasource)"
```

### Verify Flyway Status
```bash
./gradlew bootRun 2>&1 | grep -i flyway
```

If Flyway is disabled, you'll see:
```
Flyway is disabled. Not executing any migrations.
```

---

## 🔧 **SOLUTIONS**

### Option 1: Use PostgreSQL for Local Development (Recommended)

#### Step 1: Start PostgreSQL via Docker
```bash
# From project root
docker run -d \
  --name cms-postgres \
  -e POSTGRES_DB=cmsdb \
  -e POSTGRES_USER=cms \
  -e POSTGRES_PASSWORD=cms \
  -p 5435:5432 \
  postgres:17
```

#### Step 2: Run Backend with Production Profile
```bash
cd backend
DB_URL=jdbc:postgresql://localhost:5435/cmsdb \
DB_USERNAME=cms \
DB_PASSWORD=cms \
SPRING_PROFILES_ACTIVE=prod ./gradlew bootRun
```

#### Step 3: Verify Migrations Run
Check logs for:
```
Flyway successfully applied 2 migrations
V97__seed_sample_users.sql
V98__fix_admin_user_role.sql
```

---

### Option 2: Enable Flyway for Local Profile (H2)

**Note:** This still uses in-memory H2, but runs migrations. Data is still lost on restart.

**File:** `backend/src/main/resources/application-local.yml`
```yaml
spring:
  flyway:
    enabled: true  # Change from false to true
```

Then restart:
```bash
cd backend
./gradlew bootRun
```

---

### Option 3: Create a "dev" Profile with PostgreSQL

Create new file: `backend/src/main/resources/application-dev.yml`
```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5435/cmsdb
    username: cms
    password: cms
    driver-class-name: org.postgresql.Driver

  jpa:
    hibernate:
      ddl-auto: validate
    show-sql: true

  flyway:
    enabled: true

  security:
    oauth2:
      resourceserver:
        jwt:
          issuer-uri: http://localhost:8280/realms/cms
```

Run with:
```bash
SPRING_PROFILES_ACTIVE=dev ./gradlew bootRun
```

---

## 🎯 **RECOMMENDED WORKFLOW**

### For Active Development with Persistent Data:

1. **Start PostgreSQL**
   ```bash
   docker run -d --name cms-postgres \
     -e POSTGRES_DB=cmsdb \
     -e POSTGRES_USER=cms \
     -e POSTGRES_PASSWORD=cms \
     -p 5435:5432 \
     postgres:17
   ```

2. **Run Backend with PostgreSQL**
   ```bash
   cd backend
   DB_URL=jdbc:postgresql://localhost:5435/cmsdb \
   DB_USERNAME=cms \
   DB_PASSWORD=cms \
   SPRING_PROFILES_ACTIVE=prod ./gradlew bootRun
   ```

3. **Verify in Logs**
   Look for:
   - ✅ `HikariPool-1 - Starting... jdbc:postgresql://localhost:5435/cmsdb`
   - ✅ `Flyway successfully applied X migrations`
   - ✅ `V97__seed_sample_users.sql`
   - ✅ `V98__fix_admin_user_role.sql`

4. **Access User Management**
   - Frontend: http://localhost:4200
   - Login as admin
   - Navigate to Settings → User Management
   - Should see 7 users (admin + 6 sample users)

---

## 🔍 **HOW TO VERIFY DATABASE IN USE**

### Method 1: Check Startup Logs
```bash
./gradlew bootRun 2>&1 | tee startup.log
grep -i "datasource.*url" startup.log
```

### Method 2: Check H2 Console (if using H2)
- URL: http://localhost:8080/api/v1/h2-console
- JDBC URL: jdbc:h2:mem:cmsdb
- Username: sa
- Password: (empty)

### Method 3: Query Database
```sql
-- If using PostgreSQL:
SELECT current_database(), version();

-- Check app_users table:
SELECT keycloak_username, email, r.name as role
FROM app_users u
JOIN app_roles r ON u.app_role_id = r.id;
```

---

## ⚠️ **IMPORTANT NOTES**

1. **H2 In-Memory Limitations:**
   - Data is lost when application stops
   - Migrations don't run (Flyway disabled)
   - User Management fix (V97, V98) won't apply
   - Good for quick testing only

2. **PostgreSQL Benefits:**
   - Data persists across restarts
   - Flyway migrations run automatically
   - Production-like environment
   - Better for development

3. **Migration Files:**
   - V97__seed_sample_users.sql
   - V98__fix_admin_user_role.sql
   - **Only run when Flyway is enabled**
   - **Only work with PostgreSQL** (SQL syntax)

---

## 📊 **Quick Reference Table**

| Scenario | Profile | Database | Flyway | Data Persists | Migrations Run |
|----------|---------|----------|--------|---------------|----------------|
| `./gradlew bootRun` | local | H2 in-memory | ❌ | ❌ | ❌ |
| `DB_URL=jdbc:postgresql://localhost:5435/cmsdb DB_USERNAME=cms DB_PASSWORD=cms SPRING_PROFILES_ACTIVE=prod ./gradlew bootRun` | prod | PostgreSQL | ✅ | ✅ | ✅ |
| `docker compose up` | prod | PostgreSQL | ✅ | ✅ | ✅ |

---

## ✅ **ACTION ITEMS**

To fix the User Management screen issue with persistent data:

1. ✅ Start PostgreSQL: `docker run -d --name cms-postgres -e POSTGRES_DB=cmsdb -e POSTGRES_USER=cms -e POSTGRES_PASSWORD=cms -p 5435:5432 postgres:17`

2. ✅ Run backend with prod profile: `DB_URL=jdbc:postgresql://localhost:5435/cmsdb DB_USERNAME=cms DB_PASSWORD=cms SPRING_PROFILES_ACTIVE=prod ./gradlew bootRun`

3. ✅ Verify migrations in logs: Look for V97 and V98

4. ✅ Test User Management screen: Should show 7 users

5. ✅ Data will persist across restarts!

---

## 🎉 **SUMMARY**

**Current State:**
- Default runs on H2 in-memory
- No data persistence
- Migrations don't run
- User Management fix doesn't apply

**After Following Instructions:**
- Running on PostgreSQL
- Data persists
- Migrations applied
- User Management shows users

The User Management fix (V97 + V98) **ONLY works with PostgreSQL**!

