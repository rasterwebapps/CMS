package com.cms.service;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.cms.dto.PenaltyResponse;
import com.cms.dto.ReceiptResponse;
import com.cms.dto.StudentFeeAllocationResponse;
import com.cms.exception.ResourceNotFoundException;
import com.cms.model.AppUser;
import com.cms.model.Student;
import com.cms.repository.AppUserRepository;

/**
 * Current authenticated student's own fee status (self-service portal) -- full ledger, not just
 * current dues, per the specialist-review scope decision for this slice. Resolves the caller's
 * linked {@link Student} the same way {@link ProfileService}/{@link
 * AttendanceService#findMyAttendance}/{@link ExamResultService#findMyResults} do, via the {@code
 * app_users} FK, never a client-supplied studentId.
 *
 * <p>A dedicated composing service rather than adding this resolution directly into {@link
 * FeeFinalizationService}/{@link PaymentCollectionService}/{@link PenaltyCalculationService} (the
 * per-domain-service shape Attendance/ExamResult use) -- those three already carry
 * 5-12 constructor dependencies each for genuinely unrelated fee-management concerns (finalization,
 * collection, penalty math), and this self-service slice needs nothing from any of them but their
 * existing by-studentId read methods. No mapping/business logic is duplicated here; every method
 * below is a one-line delegation.
 *
 * <p>Deliberately carries no class-level {@code @Transactional} of its own: {@link
 * #safeGetSummary}/{@link #safeGetPenalties} below catch {@link ResourceNotFoundException} from a
 * nested call into another {@code @Transactional} service bean expecting to continue normally --
 * if this class were itself {@code @Transactional}, that nested call would join the *same*
 * physical transaction (REQUIRED propagation), and Spring marks a transaction rollback-only the
 * moment any exception escapes a participating {@code @Transactional} method, even one this class
 * catches immediately after -- surfacing as a real, confirmed-live {@code
 * UnexpectedRollbackException} on every unlinked/not-yet-finalized call, not a hypothetical risk.
 * Leaving this class un-annotated lets each inner service call run (and, on
 * {@code ResourceNotFoundException}, roll back) in its own independent transaction, exactly what
 * "empty is not an error" here requires. */
@Service
public class StudentFeeSelfServiceService {

    private final AppUserRepository appUserRepository;
    private final FeeFinalizationService feeFinalizationService;
    private final PaymentCollectionService paymentCollectionService;
    private final PenaltyCalculationService penaltyCalculationService;

    public StudentFeeSelfServiceService(AppUserRepository appUserRepository,
                                         FeeFinalizationService feeFinalizationService,
                                         PaymentCollectionService paymentCollectionService,
                                         PenaltyCalculationService penaltyCalculationService) {
        this.appUserRepository = appUserRepository;
        this.feeFinalizationService = feeFinalizationService;
        this.paymentCollectionService = paymentCollectionService;
        this.penaltyCalculationService = penaltyCalculationService;
    }

    /** Semester-wise fee breakdown/allocation -- empty when the caller has no linked student
     *  account, or when that student has no fee allocation finalized yet (a real, normal state for
     *  a newly admitted student, not an error). */
    public Optional<StudentFeeAllocationResponse> findMySummary(String keycloakUsername) {
        return resolveStudentId(keycloakUsername).flatMap(this::safeGetSummary);
    }

    /** Every receipt issued against the caller's own payments -- the "payment history" half of the
     *  full-ledger scope. Empty (never an error) when unlinked or nothing has been collected yet. */
    public List<ReceiptResponse> findMyReceipts(String keycloakUsername) {
        return resolveStudentId(keycloakUsername)
            .map(paymentCollectionService::getReceipts)
            .orElse(List.of());
    }

    /** Outstanding late-fee penalties, same empty-not-error handling as the other two. */
    public Optional<PenaltyResponse> findMyPenalties(String keycloakUsername) {
        return resolveStudentId(keycloakUsername).flatMap(this::safeGetPenalties);
    }

    private Optional<StudentFeeAllocationResponse> safeGetSummary(Long studentId) {
        try {
            return Optional.of(feeFinalizationService.getByStudentId(studentId));
        } catch (ResourceNotFoundException e) {
            return Optional.empty();
        }
    }

    private Optional<PenaltyResponse> safeGetPenalties(Long studentId) {
        try {
            return Optional.of(penaltyCalculationService.calculatePenalties(studentId));
        } catch (ResourceNotFoundException e) {
            return Optional.empty();
        }
    }

    private Optional<Long> resolveStudentId(String keycloakUsername) {
        return appUserRepository.findByKeycloakUsername(keycloakUsername)
            .map(AppUser::getLinkedStudent)
            .filter(java.util.Objects::nonNull)
            .map(Student::getId);
    }
}
