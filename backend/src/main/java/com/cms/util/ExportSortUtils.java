package com.cms.util;

import java.util.Set;

import org.springframework.data.domain.Sort;

/**
 * Resolves the sort field/direction an export endpoint should use from the same
 * {@code sort}/{@code direction} query params the screen's {@code /page} endpoint accepts,
 * so "Export" always reflects whatever the user currently has the on-screen table sorted by.
 * Falls back to the screen's default when the requested field isn't in the allowed set —
 * every allowed field must be a real, directly-queryable entity property (never a DTO-only
 * or joined field), matching the same constraint mat-sort-header columns are held to.
 */
public final class ExportSortUtils {

    private ExportSortUtils() {}

    public static Sort resolve(String sort, String direction, Set<String> allowedFields,
                                String defaultField, Sort.Direction defaultDirection) {
        if (sort != null && allowedFields.contains(sort)) {
            Sort.Direction dir = "desc".equalsIgnoreCase(direction) ? Sort.Direction.DESC : Sort.Direction.ASC;
            return Sort.by(dir, sort);
        }
        return Sort.by(defaultDirection, defaultField);
    }

    /** The first (or only) order clause of a resolved export Sort, for building a metadata sort label. */
    public static Sort.Order firstOrder(Sort sort, String defaultField, Sort.Direction defaultDirection) {
        return sort.stream().findFirst().orElse(new Sort.Order(defaultDirection, defaultField));
    }
}
