package com.cms.ai;

/** Operators a {@link SearchIntent} filter may use -- deliberately not a raw SQL/JPQL fragment. */
public enum SearchOperator {
    CONTAINS,
    EQUALS,
    GTE
}
