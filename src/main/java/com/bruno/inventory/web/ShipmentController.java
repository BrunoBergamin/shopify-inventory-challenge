package com.bruno.inventory.web;

import com.bruno.inventory.service.ShipmentService;
import com.bruno.inventory.web.dto.AddShipmentItemRequest;
import com.bruno.inventory.web.dto.ShipmentRequest;
import com.bruno.inventory.web.dto.ShipmentResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/shipments")
@Tag(name = "Shipments", description = "Build shipments from inventory items; stock is reserved as items are added")
public class ShipmentController {

    private final ShipmentService service;

    public ShipmentController(ShipmentService service) {
        this.service = service;
    }

    @GetMapping
    @Operation(summary = "List all shipments, newest first")
    public List<ShipmentResponse> findAll() {
        return service.findAll();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get one shipment with its items")
    public ShipmentResponse findById(@PathVariable Long id) {
        return service.findById(id);
    }

    @PostMapping
    @Operation(summary = "Create an empty shipment (status OPEN)")
    public ResponseEntity<ShipmentResponse> create(@Valid @RequestBody ShipmentRequest request) {
        ShipmentResponse created = service.create(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(created.id()).toUri();
        return ResponseEntity.created(location).body(created);
    }

    @PostMapping("/{id}/items")
    @Operation(summary = "Add an item to an OPEN shipment",
            description = "Takes the quantity out of stock immediately. Returns 422 if there is not enough stock.")
    public ShipmentResponse addItem(@PathVariable Long id, @Valid @RequestBody AddShipmentItemRequest request) {
        return service.addItem(id, request);
    }

    @DeleteMapping("/{id}/items/{shipmentItemId}")
    @Operation(summary = "Remove a line from an OPEN shipment", description = "Its quantity goes back to stock.")
    public ShipmentResponse removeItem(@PathVariable Long id, @PathVariable Long shipmentItemId) {
        return service.removeItem(id, shipmentItemId);
    }

    @PostMapping("/{id}/ship")
    @Operation(summary = "Mark an OPEN shipment as SHIPPED", description = "After this the shipment is read-only.")
    public ShipmentResponse ship(@PathVariable Long id) {
        return service.ship(id);
    }

    @PostMapping("/{id}/cancel")
    @Operation(summary = "Cancel an OPEN shipment", description = "Every reserved unit goes back to stock.")
    public ShipmentResponse cancel(@PathVariable Long id) {
        return service.cancel(id);
    }
}
