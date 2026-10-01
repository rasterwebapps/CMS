package com.cms.ai.rag;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.ActiveProfiles;

import com.cms.dto.AdmissionDocumentRagResult;

/**
 * OC-277 Phase 5 -- on-demand retrieval-quality eval, run with {@code ./gradlew ragEval}.
 *
 * <p>This hits the real {@link DocumentRagSearchService} end to end: real Ollama embedding call,
 * real pgvector cosine search, real Postgres fetch-join -- nothing here is mocked except the
 * {@link JwtDecoder} (same as {@code CmsApplicationTests}, so this doesn't also need a live
 * Keycloak just to boot the Spring context). It requires the local dev stack to be up
 * (docker-compose: Postgres+pgvector, Ollama with {@code nomic-embed-text} pulled) and the three
 * fixture documents from the Phase 4 critical-fix verification (ids 90/91/92, admission 2,
 * student "Divya Sekar") already ingested -- see project_ai_document_reading_rag_plan.md for how
 * those were seeded. It is deliberately excluded from {@code test}/{@code check}/{@code build};
 * see the {@code ragEval} task in build.gradle.kts.
 */
// allow-bean-definition-overriding: the real auto-configured JwtDecoder (built from the
// application-local.yml issuer-uri) and this test's mock both register under the same bean
// name -- scoped to just this test, not added to application-local.yml itself, since that file
// also drives the real local dev backend run where a silently-overridden bean should still fail
// loudly.
@SpringBootTest(properties = "spring.main.allow-bean-definition-overriding=true")
@ActiveProfiles("local")
@Import(AdmissionDocumentRagEvalTest.TestConfig.class)
class AdmissionDocumentRagEvalTest {

    private static final double SIMILARITY_THRESHOLD = 0.5;

    @TestConfiguration
    static class TestConfig {
        @Bean
        JwtDecoder jwtDecoder() {
            return mock(JwtDecoder.class);
        }
    }

    @Autowired
    private DocumentRagSearchService documentRagSearchService;

    @BeforeEach
    void authenticateAsEvalRunner() {
        // TestingAuthenticationToken's 2-arg (principal, credentials) constructor leaves
        // authenticated=false, which makes CurrentUserResolver.resolve() return null and
        // violates document_rag_query_log.queried_by's NOT NULL constraint -- the 3-arg
        // (principal, credentials, authorities) constructor sets authenticated=true.
        SecurityContextHolder.getContext().setAuthentication(
            new TestingAuthenticationToken("rag-eval", null, List.of()));
    }

    @AfterEach
    void clearAuthentication() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void transcript_answersWhichSchoolQuestion() {
        assertRetrievesAboveThreshold("what school did the student attend before joining?", "tc_test.pdf");
    }

    @Test
    void transcript_answersReasonForLeavingQuestion() {
        assertRetrievesAboveThreshold("why did the student leave their previous school?", "tc_test.pdf");
    }

    @Test
    void marksheet_answersChemistryMarksQuestion() {
        assertRetrievesAboveThreshold("how many marks did the student get in chemistry?", "marksheet_test.pdf");
    }

    @Test
    void marksheet_answersTotalMarksQuestion() {
        assertRetrievesAboveThreshold("what was the student's total marks out of 1000?", "marksheet_test.pdf");
    }

    @Test
    void marksheet_answersRegisterNumberQuestion() {
        assertRetrievesAboveThreshold("what is the student's board exam register number?", "marksheet_test.pdf");
    }

    @Test
    void irrelevantQuestion_returnsNoResults() {
        List<AdmissionDocumentRagResult> results = documentRagSearchService.search("what is the capital of France?");
        assertThat(results).isEmpty();
    }

    private void assertRetrievesAboveThreshold(String question, String expectedFileName) {
        List<AdmissionDocumentRagResult> results = documentRagSearchService.search(question);
        assertThat(results)
            .as("question '%s' should retrieve %s above the %.2f similarity threshold -- got %s",
                question, expectedFileName, SIMILARITY_THRESHOLD, results)
            .anyMatch(r -> expectedFileName.equals(r.fileName()) && r.similarity() >= SIMILARITY_THRESHOLD);
    }
}
