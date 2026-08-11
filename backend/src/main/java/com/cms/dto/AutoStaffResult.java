package com.cms.dto;

import java.util.List;

public record AutoStaffResult(
    int staffedCount,
    List<AutoStaffUnplacedItem> unplaced
) {}
