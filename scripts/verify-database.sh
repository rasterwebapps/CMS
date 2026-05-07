#!/bin/bash
# Database Verification Script for CMS
# Checks PostgreSQL connection and database state

echo "=================================="
echo "CMS Database Verification Script"
echo "=================================="
echo ""

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Check if PostgreSQL container is running
echo "1. Checking PostgreSQL container..."
if docker ps | grep -q "cms-postgres"; then
    echo -e "${GREEN}✓ PostgreSQL container is running${NC}"
    POSTGRES_STATUS="running"
else
    echo -e "${RED}✗ PostgreSQL container is NOT running${NC}"
    echo "Start it with: docker run -d --name cms-postgres -e POSTGRES_DB=cmsdb -e POSTGRES_USER=cms -e POSTGRES_PASSWORD=cms -p 5435:5432 postgres:17"
    POSTGRES_STATUS="not_running"
fi
echo ""

# Check PostgreSQL connection
if [ "$POSTGRES_STATUS" = "running" ]; then
    echo "2. Testing PostgreSQL connection..."
    if docker exec cms-postgres psql -U cms -d cmsdb -c "SELECT version();" > /dev/null 2>&1; then
        echo -e "${GREEN}✓ PostgreSQL connection successful${NC}"

        # Get database version
        DB_VERSION=$(docker exec cms-postgres psql -U cms -d cmsdb -t -c "SELECT version();" | head -1)
        echo "   Database: $DB_VERSION"
    else
        echo -e "${RED}✗ Cannot connect to PostgreSQL${NC}"
    fi
    echo ""

    # Check Flyway schema_version table
    echo "3. Checking Flyway migration status..."
    if docker exec cms-postgres psql -U cms -d cmsdb -c "SELECT COUNT(*) FROM flyway_schema_history;" > /dev/null 2>&1; then
        MIGRATION_COUNT=$(docker exec cms-postgres psql -U cms -d cmsdb -t -c "SELECT COUNT(*) FROM flyway_schema_history;")
        echo -e "${GREEN}✓ Flyway schema exists${NC}"
        echo "   Total migrations applied: $MIGRATION_COUNT"

        # Check for V97 and V98
        V97_EXISTS=$(docker exec cms-postgres psql -U cms -d cmsdb -t -c "SELECT COUNT(*) FROM flyway_schema_history WHERE version = '97';")
        V98_EXISTS=$(docker exec cms-postgres psql -U cms -d cmsdb -t -c "SELECT COUNT(*) FROM flyway_schema_history WHERE version = '98';")

        if [ "$V97_EXISTS" -gt 0 ]; then
            echo -e "   ${GREEN}✓ V97 (seed_sample_users) - APPLIED${NC}"
        else
            echo -e "   ${YELLOW}✗ V97 (seed_sample_users) - NOT APPLIED${NC}"
        fi

        if [ "$V98_EXISTS" -gt 0 ]; then
            echo -e "   ${GREEN}✓ V98 (fix_admin_user_role) - APPLIED${NC}"
        else
            echo -e "   ${YELLOW}✗ V98 (fix_admin_user_role) - NOT APPLIED${NC}"
        fi

        # Show last 3 migrations
        echo ""
        echo "   Last 3 migrations applied:"
        docker exec cms-postgres psql -U cms -d cmsdb -c "SELECT version, description, installed_on FROM flyway_schema_history ORDER BY installed_rank DESC LIMIT 3;" 2>/dev/null | tail -n +3 | head -n 3
    else
        echo -e "${YELLOW}✗ Flyway schema does not exist (no migrations run yet)${NC}"
        echo "   This is normal for a fresh database before first backend startup."
    fi
    echo ""

    # Check app_users table
    echo "4. Checking app_users table..."
    if docker exec cms-postgres psql -U cms -d cmsdb -c "SELECT COUNT(*) FROM app_users;" > /dev/null 2>&1; then
        USER_COUNT=$(docker exec cms-postgres psql -U cms -d cmsdb -t -c "SELECT COUNT(*) FROM app_users;")
        echo -e "${GREEN}✓ app_users table exists${NC}"
        echo "   Total users: $USER_COUNT"

        # Show users with their roles
        echo ""
        echo "   Users in database:"
        docker exec cms-postgres psql -U cms -d cmsdb -c "
            SELECT
                u.keycloak_username,
                u.email,
                r.name as role,
                r.hierarchy_level as level,
                u.is_active
            FROM app_users u
            JOIN app_roles r ON u.app_role_id = r.id
            ORDER BY r.hierarchy_level, u.keycloak_username;
        " 2>/dev/null
    else
        echo -e "${YELLOW}✗ app_users table does not exist${NC}"
        echo "   Run backend with: SPRING_PROFILES_ACTIVE=prod ./gradlew bootRun"
    fi
    echo ""

    # Check app_roles table
    echo "5. Checking app_roles table..."
    if docker exec cms-postgres psql -U cms -d cmsdb -c "SELECT COUNT(*) FROM app_roles;" > /dev/null 2>&1; then
        ROLE_COUNT=$(docker exec cms-postgres psql -U cms -d cmsdb -t -c "SELECT COUNT(*) FROM app_roles;")
        echo -e "${GREEN}✓ app_roles table exists${NC}"
        echo "   Total roles: $ROLE_COUNT"
    else
        echo -e "${YELLOW}✗ app_roles table does not exist${NC}"
    fi
fi

echo ""
echo "=================================="
echo "Verification Complete"
echo "=================================="
echo ""

# Provide next steps
if [ "$POSTGRES_STATUS" = "running" ]; then
    echo "Next Steps:"
    echo "1. Start backend with PostgreSQL:"
    echo "   cd backend && SPRING_PROFILES_ACTIVE=prod ./gradlew bootRun"
    echo ""
    echo "2. Check startup logs for migrations:"
    echo "   Look for: 'Flyway successfully applied X migrations'"
    echo ""
    echo "3. Verify User Management screen:"
    echo "   Login at http://localhost:4200 → Settings → User Management"
fi

