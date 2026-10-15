package com.cms.util;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import com.cms.model.SemesterFee;

/**
 * Cascades literal per-installment payments across a student's semester fees in sequence order,
 * so money already collected against one term flows forward to cover a later term's balance —
 * mirroring how {@code PaymentCollectionService} already cascades a brand-new payment across
 * {@code pendingForSemester} at collection time. Without this, a term whose {@code amount} is
 * reduced below what was already paid there (e.g. a one-off fee correction) shows that excess
 * nowhere: it's clamped out of that term's own pending/outstanding and never credited to the
 * next term, even though the money is genuinely in hand. This never alters which {@code
 * FeeInstallment} row a receipt is recorded against — it only changes how "paid so far" is
 * computed per term for display and for how much more collection may still ask for.
 */
public final class FeeCascadeUtil {

    private FeeCascadeUtil() {
    }

    /**
     * @param orderedFees        semester fees for one allocation, ordered by (yearNumber, semesterSequence)
     * @param literalPaidLookup  returns the raw sum of FeeInstallment.amountPaid recorded against a given
     *                           semester fee id, ignoring any cross-term cascade
     * @return each semester fee id mapped to its cascaded paid amount: its own literal payments plus any
     *         carry-forward excess from earlier terms, capped at that term's own amount (the remainder, if
     *         any, carries on to the next term in the list)
     */
    public static Map<Long, BigDecimal> cascadePaid(List<SemesterFee> orderedFees,
                                                      Function<Long, BigDecimal> literalPaidLookup) {
        Map<Long, BigDecimal> result = new LinkedHashMap<>();
        BigDecimal carry = BigDecimal.ZERO;
        for (SemesterFee sf : orderedFees) {
            BigDecimal literal = literalPaidLookup.apply(sf.getId());
            BigDecimal available = literal.add(carry);
            BigDecimal applied = available.min(sf.getAmount());
            carry = available.subtract(applied);
            result.put(sf.getId(), applied);
        }
        return result;
    }
}
