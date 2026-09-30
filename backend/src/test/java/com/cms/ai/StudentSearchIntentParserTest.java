package com.cms.ai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Every case here is deterministic and never touches a real Ollama instance -- {@link
 * OllamaClient} is mocked to return a fixed JSON string per test, exactly as the AI Smart
 * Search plan's testing section specifies: this suite validates the parser's own scaffolding
 * (allow-list enforcement, malformed-input rejection), never real model output quality.
 */
@ExtendWith(MockitoExtension.class)
class StudentSearchIntentParserTest {

    @Mock
    private OllamaClient ollamaClient;

    private StudentSearchIntentParser parser;

    @BeforeEach
    void setUp() {
        parser = new StudentSearchIntentParser(ollamaClient, new ObjectMapper());
    }

    @Test
    void parse_rejectsBlankQueryWithoutCallingOllama() {
        assertThatThrownBy(() -> parser.parse("   "))
            .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> parser.parse(null))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void parse_returnsValidatedIntentForAnAllowListedField() {
        when(ollamaClient.chatJson(anyString(), anyString())).thenReturn(
            "{\"conditions\":[{\"field\":\"previousSchoolOrCollegeName\",\"operator\":\"CONTAINS\",\"value\":\"St. Mary's School\"}]}"
        );

        SearchIntent intent = parser.parse("find students who studied at St. Mary's School");

        assertThat(intent.conditions()).hasSize(1);
        SearchIntent.Condition condition = intent.conditions().get(0);
        assertThat(condition.field()).isEqualTo("previousSchoolOrCollegeName");
        assertThat(condition.operator()).isEqualTo(SearchOperator.CONTAINS);
        assertThat(condition.value()).isEqualTo("St. Mary's School");
    }

    @Test
    void parse_rejectsAFieldOutsideTheAllowList() {
        when(ollamaClient.chatJson(anyString(), anyString())).thenReturn(
            "{\"conditions\":[{\"field\":\"salary\",\"operator\":\"CONTAINS\",\"value\":\"100000\"}]}"
        );

        assertThatThrownBy(() -> parser.parse("find students earning a salary of 100000"))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void parse_rejectsAnOperatorNotAllowedForThatField() {
        // "status" only allows EQUALS in the registry, never CONTAINS.
        when(ollamaClient.chatJson(anyString(), anyString())).thenReturn(
            "{\"conditions\":[{\"field\":\"status\",\"operator\":\"CONTAINS\",\"value\":\"ACTIVE\"}]}"
        );

        assertThatThrownBy(() -> parser.parse("find active-ish students"))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void parse_rejectsMalformedJson() {
        when(ollamaClient.chatJson(anyString(), anyString())).thenReturn("not json at all");

        assertThatThrownBy(() -> parser.parse("anything"))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void parse_rejectsEmptyConditionsArray() {
        when(ollamaClient.chatJson(anyString(), anyString())).thenReturn("{\"conditions\":[]}");

        assertThatThrownBy(() -> parser.parse("something unrelated to students entirely"))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void parse_rejectsAConditionMissingAValue() {
        when(ollamaClient.chatJson(anyString(), anyString())).thenReturn(
            "{\"conditions\":[{\"field\":\"firstName\",\"operator\":\"CONTAINS\",\"value\":\"\"}]}"
        );

        assertThatThrownBy(() -> parser.parse("find students named"))
            .isInstanceOf(IllegalArgumentException.class);
    }
}
