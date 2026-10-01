package com.cms.ai;

import java.util.LinkedHashMap;
import java.util.Map;

import static com.cms.ai.SearchFieldType.ENUM;
import static com.cms.ai.SearchFieldType.NUMBER;
import static com.cms.ai.SearchFieldType.TEXT;
import static com.cms.ai.SearchOperator.CONTAINS;
import static com.cms.ai.SearchOperator.EQUALS;
import static com.cms.ai.SearchOperator.GTE;

/**
 * The fixed, backend-owned allow-list of fields AI Smart Search may filter Student records on.
 * This is the safety boundary for the whole feature: the LLM is given this schema and asked to
 * emit a {@link SearchIntent} against it, never raw SQL/JPQL. {@link StudentSearchIntentParser}
 * rejects anything referencing a field name not present here.
 *
 * Every entry here must have a matching method in
 * {@code com.cms.repository.StudentSpecification} (see its "AI Smart Search" section) -- adding
 * a field to one without the other is a bug, not a shortcut.
 */
public final class SearchFieldRegistry {

    private SearchFieldRegistry() {
    }

    private static final Map<String, SearchField> FIELDS = buildFields();

    private static Map<String, SearchField> buildFields() {
        Map<String, SearchField> fields = new LinkedHashMap<>();
        register(fields, "firstName", "Student's first name", TEXT, CONTAINS);
        register(fields, "lastName", "Student's last name", TEXT, CONTAINS);
        register(fields, "rollNumber", "Student's roll number", TEXT, CONTAINS, EQUALS);
        register(fields, "admissionNumber", "Student's admission number", TEXT, CONTAINS, EQUALS);
        register(fields, "programName", "Name of the program the student is enrolled in", TEXT, CONTAINS);
        register(fields, "courseName", "Name of the course the student is enrolled in", TEXT, CONTAINS);
        register(fields, "specialityName", "Name of the speciality/department", TEXT, CONTAINS);
        register(fields, "city", "City/town the student's home address is in", TEXT, CONTAINS);
        register(fields, "district", "District the student's home address is in", TEXT, CONTAINS);
        register(fields, "state", "State the student's home address is in", TEXT, CONTAINS);
        register(fields, "status", "Student status: ACTIVE, INACTIVE, GRADUATED, ON_LEAVE, SUSPENDED, WITHDRAWN, EXPELLED", ENUM, EQUALS);
        register(fields, "admissionCategory", "Admission category: MANAGEMENT, COUNSELLING", ENUM, EQUALS);
        register(fields, "gender", "Gender: MALE, FEMALE, OTHER", ENUM, EQUALS);
        register(fields, "previousSchoolOrCollegeName",
            "Name of the school or college the student previously studied at, from their academic qualification records",
            TEXT, CONTAINS);
        register(fields, "previousBoardOrUniversity",
            "Name of the board or university that awarded a prior academic qualification", TEXT, CONTAINS);
        register(fields, "qualificationType", "Prior qualification type: SSLC, HSC, DIPLOMA, DEGREE, OTHER", ENUM, EQUALS);
        register(fields, "qualificationPercentage", "Percentage scored in a prior qualification", NUMBER, GTE);
        register(fields, "qualificationPassingPeriod",
            "Month/year a prior qualification was passed, as free text (e.g. \"2019\", \"March 2020\")", TEXT, CONTAINS);
        return Map.copyOf(fields);
    }

    private static void register(Map<String, SearchField> fields, String name, String description,
                                  SearchFieldType type, SearchOperator... operators) {
        fields.put(name, new SearchField(name, description, type, operators));
    }

    public static Map<String, SearchField> all() {
        return FIELDS;
    }

    public static SearchField get(String name) {
        return FIELDS.get(name);
    }

    public static boolean isAllowed(String fieldName, SearchOperator operator) {
        SearchField field = FIELDS.get(fieldName);
        if (field == null) {
            return false;
        }
        for (SearchOperator allowed : field.allowedOperators()) {
            if (allowed == operator) {
                return true;
            }
        }
        return false;
    }
}
