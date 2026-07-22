package com.cms.model.enums;

/**
 * Optional gender restriction, settable at any level of the campus infrastructure hierarchy
 * (Block, Floor, or Zone) — whichever level matches the physical reality (a fully separate
 * boys-only block, a single girls-only floor in a mixed block, or just one wing/zone on a shared
 * floor). Setting it (together with {@code isHostel}) on a Block or Floor cascades the same
 * value down to every child underneath — see {@code CampusInfrastructureService}'s cascade logic.
 * Not permanent: an admin can re-set or clear it, at any level, at any time; a child can also be
 * independently edited afterward to differ from its parent's last cascade.
 */
public enum GenderRestriction {
    BOYS,
    GIRLS
}
