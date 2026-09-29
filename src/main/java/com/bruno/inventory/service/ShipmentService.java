package com.bruno.inventory.service;

import com.bruno.inventory.domain.InventoryItem;
import com.bruno.inventory.domain.Shipment;
import com.bruno.inventory.exception.NotFoundException;
import com.bruno.inventory.repository.InventoryItemRepository;
import com.bruno.inventory.repository.ShipmentRepository;
import com.bruno.inventory.web.dto.AddShipmentItemRequest;
import com.bruno.inventory.web.dto.ShipmentRequest;
import com.bruno.inventory.web.dto.ShipmentResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.function.Consumer;

/**
 * Each operation runs in one transaction: the shipment line and the stock change
 * are saved together or not at all.
 */
@Service
@Transactional(readOnly = true)
public class ShipmentService {

    private final ShipmentRepository shipmentRepository;
    private final InventoryItemRepository itemRepository;

    public ShipmentService(ShipmentRepository shipmentRepository, InventoryItemRepository itemRepository) {
        this.shipmentRepository = shipmentRepository;
        this.itemRepository = itemRepository;
    }

    public List<ShipmentResponse> findAll() {
        return shipmentRepository.findAllByOrderByIdDesc().stream().map(ShipmentResponse::from).toList();
    }

    public ShipmentResponse findById(Long id) {
        return ShipmentResponse.from(getShipment(id));
    }

    @Transactional
    public ShipmentResponse create(ShipmentRequest request) {
        return ShipmentResponse.from(shipmentRepository.save(new Shipment(request.destination().strip())));
    }

    @Transactional
    public ShipmentResponse addItem(Long shipmentId, AddShipmentItemRequest request) {
        Shipment shipment = getShipment(shipmentId);
        InventoryItem item = itemRepository.findById(request.inventoryItemId())
                .orElseThrow(() -> new NotFoundException("Inventory item", request.inventoryItemId()));
        shipment.addItem(item, request.quantity());
        return saveAndRespond(shipment);
    }

    @Transactional
    public ShipmentResponse removeItem(Long shipmentId, Long shipmentItemId) {
        return change(shipmentId, shipment -> shipment.removeItem(shipmentItemId));
    }

    @Transactional
    public ShipmentResponse ship(Long shipmentId) {
        return change(shipmentId, Shipment::ship);
    }

    @Transactional
    public ShipmentResponse cancel(Long shipmentId) {
        return change(shipmentId, Shipment::cancel);
    }

    private ShipmentResponse change(Long shipmentId, Consumer<Shipment> action) {
        Shipment shipment = getShipment(shipmentId);
        action.accept(shipment);
        return saveAndRespond(shipment);
    }

    /** Flush inside the transaction so generated ids and version conflicts show up before we answer. */
    private ShipmentResponse saveAndRespond(Shipment shipment) {
        return ShipmentResponse.from(shipmentRepository.saveAndFlush(shipment));
    }

    private Shipment getShipment(Long id) {
        return shipmentRepository.findWithItemsById(id).orElseThrow(() -> new NotFoundException("Shipment", id));
    }
}
