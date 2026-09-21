package com.cms.model.enums;

public enum SubjectType {
    CORE,
    FOUNDATIONAL,
    ELECTIVE,
    /** Advisory/co-curricular curriculum line (e.g. Self-Study) — real curriculum data with its
     *  own hours, but never mandatory: the timetable auto-scheduler always places CORE/FOUNDATIONAL/
     *  ELECTIVE rows first and only schedules CO_CURRICULAR rows into whatever's left. */
    CO_CURRICULAR
}
