package com.cms.config;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ScriptUtils;

/**
 * Seeds RBAC reference data for the local profile only.
 *
 * <p>Local startup replays the canonical RBAC migrations so developers get the
 * same DB-driven role/permission model as a migrated environment.
 */
@Configuration
@Profile("local")
@ConditionalOnProperty(prefix = "cms.seed", name = "enabled", havingValue = "true")
public class LocalRbacSeeder {

    private static final Logger log = LoggerFactory.getLogger(LocalRbacSeeder.class);

    private static final String SEED_SCRIPT = "db/migration/V88__seed_roles_and_permissions.sql";
    private static final String GAP_FIX_SCRIPT = "db/migration/V89__fix_role_permission_gaps.sql";
    private static final String COLLEGE_ADMIN_SCRIPT =
        "db/migration/V123__create_collegeadmin_role_and_tighten_permissions.sql";
    private static final String IDENTITY_ONLY_FINAL_PASS_SCRIPT =
        "db/migration/V125__rbac_identity_only_final_pass.sql";

    @Bean
    CommandLineRunner seedRbac(DataSource dataSource) {
        return args -> {
            JdbcTemplate jdbc = new JdbcTemplate(dataSource);
            Integer existing = jdbc.queryForObject("SELECT COUNT(*) FROM app_roles", Integer.class);
            if (existing != null && existing > 0) {
                log.info("RBAC seed: app_roles already populated ({} rows) — skipping.", existing);
                return;
            }

            for (String scriptPath : java.util.List.of(SEED_SCRIPT, GAP_FIX_SCRIPT, COLLEGE_ADMIN_SCRIPT,
                IDENTITY_ONLY_FINAL_PASS_SCRIPT)) {
                Resource script = new ClassPathResource(scriptPath);
                log.info("RBAC seed: replaying {} into local database.", scriptPath);
                try (var conn = dataSource.getConnection()) {
                    ScriptUtils.executeSqlScript(conn, script);
                }
            }

            Integer roles = jdbc.queryForObject("SELECT COUNT(*) FROM app_roles", Integer.class);
            Integer permissions = jdbc.queryForObject("SELECT COUNT(*) FROM permissions", Integer.class);
            Integer grants = jdbc.queryForObject("SELECT COUNT(*) FROM role_permissions", Integer.class);
            log.info("RBAC seed: complete — {} roles, {} permissions, {} grants.",
                roles, permissions, grants);
        };
    }
}
