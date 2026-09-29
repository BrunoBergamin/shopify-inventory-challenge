package com.bruno.inventory.web.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record AddShipmentItemRequest(
        @NotNull
        Long inventoryItemId,

        @NotNull @Positive
        Integer quantity
) {
}
