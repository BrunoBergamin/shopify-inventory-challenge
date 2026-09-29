package com.bruno.inventory.web;

import com.bruno.inventory.service.InventoryItemService;
import com.bruno.inventory.web.dto.InventoryItemRequest;
import com.bruno.inventory.web.dto.InventoryItemResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/items")
@Tag(name = "Inventory items", description = "Create, list, update and delete inventory items")
public class InventoryItemController {

    private final InventoryItemService service;

    public InventoryItemController(InventoryItemService service) {
        this.service = service;
    }

    @GetMapping
    @Operation(summary = "List all inventory items")
    public List<InventoryItemResponse> findAll() {
        return service.findAll();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get one inventory item")
    public InventoryItemResponse findById(@PathVariable Long id) {
        return service.findById(id);
    }

    @PostMapping
    @Operation(summary = "Create an inventory item", description = "The SKU is stored in upper case and must be unique.")
    public ResponseEntity<InventoryItemResponse> create(@Valid @RequestBody InventoryItemRequest request) {
        InventoryItemResponse created = service.create(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(created.id()).toUri();
        return ResponseEntity.created(location).body(created);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update an inventory item", description = "Replaces every field, including the stock quantity.")
    public InventoryItemResponse update(@PathVariable Long id, @Valid @RequestBody InventoryItemRequest request) {
        return service.update(id, request);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete an inventory item", description = "Returns 409 if the item is part of any shipment.")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
