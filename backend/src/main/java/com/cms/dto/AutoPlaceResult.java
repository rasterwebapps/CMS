package com.cms.dto;

import java.util.List;

public record AutoPlaceResult(
    int placedCount,
    List<AutoPlaceUnplacedItem> unplaced
) {}
