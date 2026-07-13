package com.cms.dto;

import java.util.List;

import jakarta.validation.constraints.NotEmpty;

public record LibraryBarcodeLabelsRequest(

    @NotEmpty(message = "At least one item must be selected")
    List<Long> ids
) {}
