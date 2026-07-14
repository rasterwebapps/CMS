package com.cms.util.export;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.data.domain.Sort;

/**
 * Describes the heading, active filters, and sort order to render at the top of an
 * export file, so a downloaded report is self-explanatory about what it contains.
 * Filter lines are only rendered when a value is actually present — {@link #filter}
 * is a no-op for null/blank values, so callers can pass every screen filter unconditionally
 * without checking activeness themselves.
 */
public final class ExportMetadata {

    private final String title;
    private final Map<String, String> filters = new LinkedHashMap<>();
    private String sortLabel;

    private ExportMetadata(String title) {
        this.title = title;
    }

    public static ExportMetadata of(String title) {
        return new ExportMetadata(title);
    }

    public ExportMetadata filter(String label, String value) {
        if (value != null && !value.isBlank()) {
            filters.put(label, value);
        }
        return this;
    }

    public ExportMetadata sort(String fieldLabel, Sort.Direction direction) {
        if (fieldLabel != null && !fieldLabel.isBlank()) {
            this.sortLabel = fieldLabel + " (" + (direction == Sort.Direction.DESC ? "Descending" : "Ascending") + ")";
        }
        return this;
    }

    public String title() {
        return title;
    }

    public Map<String, String> filters() {
        return filters;
    }

    public String sortLabel() {
        return sortLabel;
    }
}
