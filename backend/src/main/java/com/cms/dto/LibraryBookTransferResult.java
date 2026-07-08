package com.cms.dto;

import java.util.List;

public record LibraryBookTransferResult(
    List<Long> succeededBookIds,
    List<Failure> failed
) {
    public record Failure(Long bookId, String reason) {}
}
