package com.cms.model.enums;

public enum ClassSessionType {
    THEORY,
    LAB,
    CLINICAL,
    LIBRARY,
    /** Weekly games/PE block placed by Run Automation after curriculum and Library: taught by a
     *  faculty on the system Sports subject's eligible-faculty list, held in a classroom whose Room is
     *  tagged Sports &amp; Recreation. Like LIBRARY it has no CourseOffering and no curriculum hours. */
    SPORTS
}
