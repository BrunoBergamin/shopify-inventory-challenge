package com.bruno.inventory.web.dto;

import com.bruno.inventory.domain.InventoryItem;

import java.math.BigDecimal;
import java.time.Instant;

public record InventoryItemResponse(
        Long id,
        String sku,
        String name,
        String description,
        int quantity,
        BigDecimal unitPrice,
        Instant createdAt,
        Instant updatedAt
) {
    public static InventoryItemResponse from(InventoryItem item) {
        return new InventoryItemResponse(
                item.getId(), item.getSku(), item.getName(), item.getDescription(),
                item.getQuantity(), item.getUnitPrice(), item.getCreatedAt(), item.getUpdatedAt());
    }
}
