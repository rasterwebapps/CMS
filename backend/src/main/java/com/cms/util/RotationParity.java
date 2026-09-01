package com.cms.util;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/**
 * Shared cyclic (Latin-square) rotation math: counts whole weeks elapsed since an anchor date
 * (not raw ISO week numbers, to stay predictable across year boundaries) and shifts a fixed
 * {@code slotOrder} by that count, modulo the cycle length. Originally inline in
 * {@code RotationResolverService} (student week-parity batch rotation); extracted so
 * {@code EscortRotationResolverService} (OC-175 faculty escort-duty rotation) can reuse the exact
 * same formula instead of re-deriving it.
 */
public final class RotationParity {

    private RotationParity() {
    }

    /** Which member-order occupies {@code slotOrder} on {@code date}, given a group whose parity
     *  is anchored at {@code anchorDate} with the given {@code cycleLength}. */
    public static int resolveMemberOrder(LocalDate anchorDate, int cycleLength, LocalDate date, int slotOrder) {
        long weeksElapsed = ChronoUnit.WEEKS.between(anchorDate, date);
        int weekIndex = Math.floorMod(weeksElapsed, cycleLength);
        return Math.floorMod(slotOrder - weekIndex, cycleLength);
    }
}
