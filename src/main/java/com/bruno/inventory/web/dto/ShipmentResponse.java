package com.bruno.inventory.web.dto;

import com.bruno.inventory.domain.Shipment;
import com.bruno.inventory.domain.ShipmentItem;
import com.bruno.inventory.domain.ShipmentStatus;

import java.time.Instant;
import java.util.List;

public record ShipmentResponse(
        Long id,
        String destination,
        ShipmentStatus status,
        Instant createdAt,
        Instant shippedAt,
        int totalUnits,
        List<Line> items
) {
    public record Line(Long id, Long inventoryItemId, String sku, String name, int quantity) {
        static Line from(ShipmentItem line) {
            var item = line.getInventoryItem();
            return new Line(line.getId(), item.getId(), item.getSku(), item.getName(), line.getQuantity());
        }
    }

    public static ShipmentResponse from(Shipment shipment) {
        List<Line> lines = shipment.getItems().stream().map(Line::from).toList();
        int totalUnits = lines.stream().mapToInt(Line::quantity).sum();
        return new ShipmentResponse(
                shipment.getId(), shipment.getDestination(), shipment.getStatus(),
                shipment.getCreatedAt(), shipment.getShippedAt(), totalUnits, lines);
    }
}
