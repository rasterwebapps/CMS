package com.cms.exception;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import com.cms.dto.ErrorResponse;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void shouldHandleResourceNotFoundException() {
        ResourceNotFoundException ex = new ResourceNotFoundException("Student not found");

        ResponseEntity<ErrorResponse> response = handler.handleNotFound(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().status()).isEqualTo(404);
        assertThat(response.getBody().message()).isEqualTo("Student not found");
        assertThat(response.getBody().timestamp()).isNotNull();
    }

    @Test
    void shouldHandleAccessDeniedException() {
        AccessDeniedException ex = new AccessDeniedException("Forbidden");

        ResponseEntity<ErrorResponse> response = handler.handleAccessDenied(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().status()).isEqualTo(403);
        assertThat(response.getBody().message()).isEqualTo("Access denied");
        assertThat(response.getBody().timestamp()).isNotNull();
    }

    @Test
    void shouldHandleMethodArgumentNotValidException() {
        BindingResult bindingResult = mock(BindingResult.class);
        FieldError fieldError1 = new FieldError("student", "email", "must be a valid email");
        FieldError fieldError2 = new FieldError("student", "firstName", "must not be blank");
        when(bindingResult.getFieldErrors()).thenReturn(List.of(fieldError1, fieldError2));

        MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
        when(ex.getBindingResult()).thenReturn(bindingResult);

        ResponseEntity<ErrorResponse> response = handler.handleValidation(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().status()).isEqualTo(400);
        assertThat(response.getBody().message()).contains("email: must be a valid email");
        assertThat(response.getBody().message()).contains("firstName: must not be blank");
        assertThat(response.getBody().timestamp()).isNotNull();
    }

    @Test
    void shouldHandleDataIntegrityViolationExceptionForUniqueConstraint() {
        DataIntegrityViolationException ex = new DataIntegrityViolationException("Unique constraint violation");

        ResponseEntity<ErrorResponse> response = handler.handleDataIntegrity(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().status()).isEqualTo(409);
        assertThat(response.getBody().message()).isEqualTo("A record with the same name or code already exists.");
        assertThat(response.getBody().timestamp()).isNotNull();
    }

    @Test
    void shouldHandleDataIntegrityViolationExceptionForForeignKeyConstraint() {
        RuntimeException fkCause = new RuntimeException(
            "ERROR: update or delete on table \"fee_structures\" violates foreign key constraint");
        DataIntegrityViolationException ex = new DataIntegrityViolationException("FK violation", fkCause);

        ResponseEntity<ErrorResponse> response = handler.handleDataIntegrity(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().status()).isEqualTo(409);
        assertThat(response.getBody().message())
            .isEqualTo("Cannot delete or update this record because it is referenced by other records.");
        assertThat(response.getBody().timestamp()).isNotNull();
    }

    @Test
    void shouldHandleIllegalStateException() {
        IllegalStateException ex = new IllegalStateException("Cannot delete fee structure because payments have been recorded against it.");

        ResponseEntity<ErrorResponse> response = handler.handleIllegalState(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().status()).isEqualTo(409);
        assertThat(response.getBody().message()).contains("payments");
        assertThat(response.getBody().timestamp()).isNotNull();
    }

    @Test
    void shouldHandleGenericException() {
        Exception ex = new RuntimeException("Something went wrong");

        ResponseEntity<ErrorResponse> response = handler.handleGeneric(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().status()).isEqualTo(500);
        assertThat(response.getBody().message()).isEqualTo("An unexpected error occurred");
        assertThat(response.getBody().timestamp()).isNotNull();
    }
}
