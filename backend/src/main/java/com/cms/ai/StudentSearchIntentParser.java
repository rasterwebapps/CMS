package com.cms.ai;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Turns a free-text query into a validated {@link SearchIntent} by asking the local Ollama
 * model to emit structured JSON against the {@link SearchFieldRegistry} allow-list, then
 * validating that JSON before trusting a single field of it.
 *
 * This is the safety boundary described in the AI Smart Search plan: the model is only ever
 * asked to reference field names it's given, and any condition naming a field outside the
 * registry, or using an operator not allowed for that field, or malformed in shape, causes the
 * whole query to be rejected -- never partially honored, never passed through as-is.
 */
@Component
public class StudentSearchIntentParser {

    private final OllamaClient ollamaClient;
    private final ObjectMapper objectMapper;
    private final String systemPrompt;

    public StudentSearchIntentParser(OllamaClient ollamaClient, ObjectMapper objectMapper) {
        this.ollamaClient = ollamaClient;
        this.objectMapper = objectMapper;
        this.systemPrompt = buildSystemPrompt();
    }

    /**
     * @throws IllegalArgumentException if the query is blank, or the model's response can't be
     *                                  parsed into a valid, allow-listed {@link SearchIntent}.
     *                                  Callers should surface this as "couldn't understand that
     *                                  query, try rephrasing" -- never fall back to any other
     *                                  interpretation of the raw query.
     */
    public SearchIntent parse(String query) {
        if (query == null || query.isBlank()) {
            throw new IllegalArgumentException("Search query must not be empty.");
        }

        String rawJson = ollamaClient.chatJson(systemPrompt, query.trim());
        JsonNode root;
        try {
            root = objectMapper.readTree(rawJson);
        } catch (Exception e) {
            throw new IllegalArgumentException("Couldn't understand that query. Try rephrasing it.");
        }

        JsonNode conditionsNode = root.get("conditions");
        if (conditionsNode == null || !conditionsNode.isArray() || conditionsNode.isEmpty()) {
            throw new IllegalArgumentException("Couldn't understand that query. Try rephrasing it.");
        }

        List<SearchIntent.Condition> conditions = new ArrayList<>();
        for (JsonNode conditionNode : conditionsNode) {
            conditions.add(toValidatedCondition(conditionNode));
        }

        return new SearchIntent(List.copyOf(conditions));
    }

    private SearchIntent.Condition toValidatedCondition(JsonNode node) {
        String field = textOrNull(node, "field");
        String operatorRaw = textOrNull(node, "operator");
        String value = textOrNull(node, "value");

        if (field == null || operatorRaw == null || value == null || value.isBlank()) {
            throw new IllegalArgumentException("Couldn't understand that query. Try rephrasing it.");
        }

        SearchOperator operator;
        try {
            operator = SearchOperator.valueOf(operatorRaw.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Couldn't understand that query. Try rephrasing it.");
        }

        if (!SearchFieldRegistry.isAllowed(field, operator)) {
            // Deliberately the same generic message as every other rejection here: never tell
            // the caller which internal field names exist, that's an unnecessary information
            // leak for no benefit to a legitimate user.
            throw new IllegalArgumentException("Couldn't understand that query. Try rephrasing it.");
        }

        return new SearchIntent.Condition(field, operator, value.trim());
    }

    private static String textOrNull(JsonNode node, String field) {
        JsonNode value = node.get(field);
        return (value == null || value.isNull()) ? null : value.asText(null);
    }

    private static String buildSystemPrompt() {
        StringBuilder sb = new StringBuilder();
        sb.append("You translate a college staff member's plain-English question about students ")
          .append("into a strict JSON search filter. You must reply with ONLY a JSON object of ")
          .append("this exact shape, no other text:\n")
          .append("{\"conditions\":[{\"field\":\"<field>\",\"operator\":\"<operator>\",\"value\":\"<value>\"}]}\n\n")
          .append("You may ONLY use these fields, each with ONLY its listed operator(s):\n");

        for (Map.Entry<String, SearchField> entry : SearchFieldRegistry.all().entrySet()) {
            SearchField field = entry.getValue();
            sb.append("- ").append(field.name())
              .append(" (").append(field.type()).append("): ").append(field.description())
              .append(" -- operators: ");
            for (int i = 0; i < field.allowedOperators().length; i++) {
                if (i > 0) sb.append(", ");
                sb.append(field.allowedOperators()[i]);
            }
            sb.append('\n');
        }

        sb.append("\nCONTAINS means a partial/substring text match. EQUALS means an exact enum match. ")
          .append("GTE means the numeric value must be at least the given value.\n")
          .append("If the question can't be answered using only these fields, reply with ")
          .append("{\"conditions\":[]}.\n\n")
          .append("Example -- question: \"find students who studied at St. Mary's School\"\n")
          .append("Reply: {\"conditions\":[{\"field\":\"previousSchoolOrCollegeName\",\"operator\":\"CONTAINS\",\"value\":\"St. Mary's School\"}]}");

        return sb.toString();
    }
}
