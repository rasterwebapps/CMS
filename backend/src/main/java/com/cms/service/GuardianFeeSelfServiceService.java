package com.cms.service;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.cms.dto.PenaltyResponse;
import com.cms.dto.ReceiptResponse;
import com.cms.dto.StudentFeeAllocationResponse;
import com.cms.exception.ResourceNotFoundException;

/**
 * A guardian's ward fee status (self-service portal) -- full ledger, same scope as
 * {@link StudentFeeSelfServiceService} grants a student over their own record. The caller passes
 * a {@code studentId} that the controller has already run through
 * {@link GuardianService#assertIsMyWard} (403s on a non-ward before this class is ever reached),
 * so unlike {@code StudentFeeSelfServiceService} there is no "resolve the caller's own linked
 * record" step here -- the ward is already confirmed, this just delegates by id.
 *
 * <p>Deliberately carries no class-level {@code @Transactional}, mirroring {@link
 * StudentFeeSelfServiceService}'s own javadoc reasoning exactly: {@link #safeGetSummary}/{@link
 * #safeGetPenalties} below catch {@link ResourceNotFoundException} from a nested call into
 * another {@code @Transactional} service bean, expecting to continue normally. If this class were
 * itself {@code @Transactional}, that nested call would join the same physical transaction
 * (REQUIRED propagation), and Spring marks a transaction rollback-only the moment any exception
 * escapes a participating {@code @Transactional} method -- surfacing as {@code
 * UnexpectedRollbackException} on every ward with no fee allocation finalized yet, the same real
 * bug OC-253 hit for direct student self-service. */
@Service
public class GuardianFeeSelfServiceService {

    private final FeeFinalizationService feeFinalizationService;
    private final PaymentCollectionService paymentCollectionService;
    private final PenaltyCalculationService penaltyCalculationService;

    public GuardianFeeSelfServiceService(FeeFinalizationService feeFinalizationService,
                                          PaymentCollectionService paymentCollectionService,
                                          PenaltyCalculationService penaltyCalculationService) {
        this.feeFinalizationService = feeFinalizationService;
        this.paymentCollectionService = paymentCollectionService;
        this.penaltyCalculationService = penaltyCalculationService;
    }

    /** Empty (never an error) when the ward has no fee allocation finalized yet -- a real, normal
     *  state for a newly admitted student. Caller must have already verified ward ownership. */
    public Optional<StudentFeeAllocationResponse> findWardSummary(Long studentId) {
        try {
            return Optional.of(feeFinalizationService.getByStudentId(studentId));
        } catch (ResourceNotFoundException e) {
            return Optional.empty();
        }
    }

    /** Every receipt issued against the ward's payments. Empty (never an error) when nothing has
     *  been collected yet. */
    public List<ReceiptResponse> findWardReceipts(Long studentId) {
        return paymentCollectionService.getReceipts(studentId);
    }

    /** Outstanding late-fee penalties, same empty-not-error handling as the summary. */
    public Optional<PenaltyResponse> findWardPenalties(Long studentId) {
        return safeGetPenalties(studentId);
    }

    private Optional<PenaltyResponse> safeGetPenalties(Long studentId) {
        try {
            return Optional.of(penaltyCalculationService.calculatePenalties(studentId));
        } catch (ResourceNotFoundException e) {
            return Optional.empty();
        }
    }
}
