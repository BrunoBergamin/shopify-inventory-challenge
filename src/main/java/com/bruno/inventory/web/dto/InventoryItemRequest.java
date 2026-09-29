package com.bruno.inventory.web.dto;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;

public record InventoryItemRequest(
        @NotBlank @Size(max = 64)
        @Pattern(regexp = "[A-Za-z0-9._-]+", message = "must contain only letters, digits, '.', '_' or '-'")
        String sku,

        @NotBlank @Size(max = 120)
        String name,

        @Size(max = 500)
        String description,

        @NotNull @PositiveOrZero
        Integer quantity,

        @NotNull @DecimalMin("0.00") @Digits(integer = 10, fraction = 2)
        BigDecimal unitPrice
) {
}
