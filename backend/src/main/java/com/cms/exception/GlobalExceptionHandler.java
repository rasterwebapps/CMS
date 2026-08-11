package com.cms.exception;

import java.time.Instant;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.server.ResponseStatusException;

import com.cms.dto.ErrorResponse;
import com.cms.dto.LifecycleConflictResponse;
import com.cms.dto.TimetableConstraintViolationResponse;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);


    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(ResourceNotFoundException ex) {
        ErrorResponse error = new ErrorResponse(
            HttpStatus.NOT_FOUND.value(),
            ex.getMessage(),
            Instant.now()
        );
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(IllegalArgumentException ex) {
        ErrorResponse error = new ErrorResponse(
            HttpStatus.BAD_REQUEST.value(),
            ex.getMessage(),
            Instant.now()
        );
        return ResponseEntity.badRequest().body(error);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
            .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
            .collect(Collectors.joining(", "));
        ErrorResponse error = new ErrorResponse(
            HttpStatus.BAD_REQUEST.value(),
            message,
            Instant.now()
        );
        return ResponseEntity.badRequest().body(error);
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ErrorResponse> handleIllegalState(IllegalStateException ex) {
        ErrorResponse error = new ErrorResponse(
            HttpStatus.CONFLICT.value(),
            ex.getMessage(),
            Instant.now()
        );
        return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
    }

    @ExceptionHandler(LifecycleConflictException.class)
    public ResponseEntity<LifecycleConflictResponse> handleLifecycleConflict(LifecycleConflictException ex) {
        LifecycleConflictResponse error = new LifecycleConflictResponse(
            HttpStatus.CONFLICT.value(),
            ex.getMessage(),
            ex.getCode(),
            ex.getEntity(),
            ex.getEntityId(),
            ex.getBlockerCount(),
            Instant.now()
        );
        return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
    }

    @ExceptionHandler(TimetableConstraintViolationException.class)
    public ResponseEntity<TimetableConstraintViolationResponse> handleTimetableConstraintViolation(
            TimetableConstraintViolationException ex) {
        TimetableConstraintViolationResponse error = new TimetableConstraintViolationResponse(
            HttpStatus.CONFLICT.value(),
            ex.getMessage(),
            ex.getViolations(),
            Instant.now()
        );
        return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
    }

    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    public ResponseEntity<ErrorResponse> handleOptimisticLocking(ObjectOptimisticLockingFailureException ex) {
        ErrorResponse error = new ErrorResponse(
            HttpStatus.CONFLICT.value(),
            "This record was modified by another user. Please refresh and try again.",
            Instant.now()
        );
        return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleDataIntegrity(DataIntegrityViolationException ex) {
        String message;
        Throwable cause = ex.getMostSpecificCause();
        String causeMsg = cause != null && cause.getMessage() != null
            ? cause.getMessage().toLowerCase() : "";
        if (causeMsg.contains("foreign key")) {
            message = "Cannot delete or update this record because it is referenced by other records.";
        } else if (causeMsg.contains("uq_fee_structure_group")) {
            message = "A fee structure already exists for this combination "
                + "(program + year + quota + state + gender + student type). "
                + "Use the edit function to update it.";
        } else if (causeMsg.contains("uq_fee_structure_group_fee_type")) {
            message = "A fee entry with this fee type already exists in this group. "
                + "Each fee type can appear only once per combination.";
        } else if (causeMsg.contains("uq_fee_refunds_active_receipt")) {
            message = "An active refund request already exists for this receipt.";
        } else if (causeMsg.contains("roll_number")) {
            message = "A student with this Roll Number already exists. Please verify the roll number and try again.";
        } else if (causeMsg.contains("university_registration_number")) {
            message = "A student with this University Registration Number already exists. Please verify the URN and try again.";
        } else if (causeMsg.contains("umis_number")) {
            message = "A student with this UMIS Number already exists. Please verify the UMIS number and try again.";
        } else if (causeMsg.contains("admission_number")) {
            message = "A student with this Admission Number already exists. Please contact the administrator.";
        } else if (causeMsg.contains("uq_faculty_availability_slot")) {
            message = "This faculty member is already marked unavailable for that day and time.";
        } else {
            message = "A record with the same value already exists. Please check for duplicates and try again.";
        }
        ErrorResponse error = new ErrorResponse(
            HttpStatus.CONFLICT.value(),
            message,
            Instant.now()
        );
        return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleMessageNotReadable(HttpMessageNotReadableException ex) {
        ErrorResponse error = new ErrorResponse(
            HttpStatus.BAD_REQUEST.value(),
            "Invalid request body",
            Instant.now()
        );
        return ResponseEntity.badRequest().body(error);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(AccessDeniedException ex) {
        ErrorResponse error = new ErrorResponse(
            HttpStatus.FORBIDDEN.value(),
            "Access denied",
            Instant.now()
        );
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error);
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ErrorResponse> handleResponseStatus(ResponseStatusException ex) {
        ErrorResponse error = new ErrorResponse(
            ex.getStatusCode().value(),
            ex.getReason() != null ? ex.getReason() : "Request could not be completed",
            Instant.now()
        );
        return ResponseEntity.status(ex.getStatusCode()).body(error);
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ErrorResponse> handleMaxUploadSize(MaxUploadSizeExceededException ex) {
        ErrorResponse error = new ErrorResponse(
            HttpStatus.PAYLOAD_TOO_LARGE.value(),
            "Uploaded file is too large. Maximum allowed size is 10 MB.",
            Instant.now()
        );
        return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE).body(error);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneric(Exception ex) {
        // Log full stack trace so the actual root cause is visible in server logs.
        log.error("Unhandled exception bubbled up to GlobalExceptionHandler", ex);
        ErrorResponse error = new ErrorResponse(
            HttpStatus.INTERNAL_SERVER_ERROR.value(),
            "An unexpected error occurred",
            Instant.now()
        );
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }
}
