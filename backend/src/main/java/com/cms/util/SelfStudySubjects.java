package com.cms.util;

import java.util.Locale;

/** Name-pattern match for a Self-Study/Co-curricular curriculum line — not a typed curriculum
 *  flag, since no such flag exists in the schema today. Matches this curriculum's actual naming
 *  ("Self-Study/Co-curricular I/III/V"); a differently-named self-study line in a future
 *  curriculum would silently not be picked up here. Shared by {@link
 *  com.cms.service.TimetableGlobalAutoScheduleService} (which places Self-Study filler) and
 *  {@link com.cms.service.TimetableStaffingService} (which must not flag an unstaffed Self-Study
 *  fallback row as a genuine staffing gap) — one literal pattern, not two independently
 *  maintained copies. */
public final class SelfStudySubjects {

    private SelfStudySubjects() {
    }

    public static boolean isSelfStudySubject(String subjectName) {
        if (subjectName == null) {
            return false;
        }
        String lower = subjectName.toLowerCase(Locale.ROOT);
        return lower.contains("self-study") || lower.contains("self study")
            || lower.contains("co-curricular") || lower.contains("cocurricular");
    }
}
