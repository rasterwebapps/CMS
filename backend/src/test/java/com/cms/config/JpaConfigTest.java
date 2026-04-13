package com.cms.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class JpaConfigTest {

    @Autowired
    private JpaConfig jpaConfig;

    @Test
    void shouldLoadJpaConfig() {
        assertThat(jpaConfig).isNotNull();
    }

    @Test
    void shouldHaveEnableJpaAuditingAnnotation() {
        EnableJpaAuditing annotation = JpaConfig.class.getAnnotation(EnableJpaAuditing.class);
        assertThat(annotation).isNotNull();
    }
}
