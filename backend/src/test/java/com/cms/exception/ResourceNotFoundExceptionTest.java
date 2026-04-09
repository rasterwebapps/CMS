package com.cms.exception;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ResourceNotFoundExceptionTest {

    @Test
    void constructor_setsMessage() {
        ResourceNotFoundException ex = new ResourceNotFoundException("User not found");
        assertEquals("User not found", ex.getMessage());
    }

    @Test
    void exception_isRuntimeException() {
        ResourceNotFoundException ex = new ResourceNotFoundException("Not found");
        assertInstanceOf(RuntimeException.class, ex);
    }
}
