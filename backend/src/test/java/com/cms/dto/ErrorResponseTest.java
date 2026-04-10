package com.cms.dto;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class ErrorResponseTest {

    @Test
    void errorResponse_createsRecordWithCorrectValues() {
        Instant now = Instant.now();
        ErrorResponse response = new ErrorResponse(404, "Not found", now);

        assertEquals(404, response.status());
        assertEquals("Not found", response.message());
        assertEquals(now, response.timestamp());
    }

    @Test
    void errorResponse_equalityWorks() {
        Instant now = Instant.now();
        ErrorResponse response1 = new ErrorResponse(400, "Bad request", now);
        ErrorResponse response2 = new ErrorResponse(400, "Bad request", now);

        assertEquals(response1, response2);
    }
}
