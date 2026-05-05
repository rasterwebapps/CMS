package com.cms.config;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ScriptUtils;

/**
 * Seeds RBAC reference data (roles, permissions, role↔permission grants) for the
 * <strong>local</strong> profile only. The {@code prod} profile uses Flyway
 * migrations {@code V87} and {@code V88} for the same purpose; on local H2,
 * Flyway is disabled (per {@code copilot-instructions.md}), so JPA creates
 * the tables and this runner replays {@code V88__seed_roles_and_permissions.sql}
 * directly against the in-memory database.
 *
 * <p>Idempotent — skips seeding when {@code app_roles} already has rows
 * (avoids re-inserting on hot-reload).
 *
 * <p>Runs <em>before</em> {@link LocalDataSeeder} (lower order) so that any
 * future demo {@code AppUser} rows can reference the seeded {@code AppRole}
 * records.
 */
@Configuration
@Profile("local")
public class LocalRbacSeeder {

    private static final Logger log = LoggerFactory.getLogger(LocalRbacSeeder.class);

    /** Classpath location of the canonical seed script (shared with Flyway V88). */
    private static final String SEED_SCRIPT      = "db/migration/V88__seed_roles_and_permissions.sql";
    /** Gap-fix permissions added for FACULTY, LAB_INCHARGE, TECHNICIAN, FRONT_OFFICE, CASHIER. */
    private static final String GAP_FIX_SCRIPT   = "db/migration/V89__fix_role_permission_gaps.sql";

    @Bean
    CommandLineRunner seedRbac(DataSource dataSource) {
        return args -> {
            JdbcTemplate jdbc = new JdbcTemplate(dataSource);
            Integer existing = jdbc.queryForObject("SELECT COUNT(*) FROM app_roles", Integer.class);
            if (existing != null && existing > 0) {
                log.info("RBAC seed: app_roles already populated ({} rows) — skipping.", existing);
                return;
            }

            Resource script = new ClassPathResource(SEED_SCRIPT);
            log.info("RBAC seed: replaying {} into H2.", SEED_SCRIPT);
            try (var conn = dataSource.getConnection()) {
                ScriptUtils.executeSqlScript(conn, script);
            }

            // Apply V89 gap fixes so FACULTY, LAB_INCHARGE, etc. have the right permissions locally.
            Resource gapFix = new ClassPathResource(GAP_FIX_SCRIPT);
            log.info("RBAC seed: applying gap-fix {} into H2.", GAP_FIX_SCRIPT);
            try (var conn = dataSource.getConnection()) {
                ScriptUtils.executeSqlScript(conn, gapFix);
            }

            Integer roles       = jdbc.queryForObject("SELECT COUNT(*) FROM app_roles",        Integer.class);
            Integer permissions = jdbc.queryForObject("SELECT COUNT(*) FROM permissions",      Integer.class);
            Integer grants      = jdbc.queryForObject("SELECT COUNT(*) FROM role_permissions", Integer.class);
            log.info("RBAC seed: complete — {} roles, {} permissions, {} grants.",
                     roles, permissions, grants);
        };
    }
}

