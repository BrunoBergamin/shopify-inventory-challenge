package com.bruno.inventory.domain;

import com.bruno.inventory.exception.InvalidShipmentStateException;
import com.bruno.inventory.exception.NotFoundException;
import jakarta.persistence.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Stock is reserved as soon as an item is added to an OPEN shipment, so two shipments
 * can never promise the same units. Removing a line or cancelling gives the units back.
 */
@Entity
@Table(name = "shipment")
public class Shipment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String destination;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ShipmentStatus status;

    /** Optimistic lock: concurrent stock changes fail with 409 instead of silently overwriting. */
    @Version
    private long version;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "shipped_at")
    private Instant shippedAt;

    @OneToMany(mappedBy = "shipment", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id")
    private List<ShipmentItem> items = new ArrayList<>();

    protected Shipment() {
        // required by JPA
    }

    public Shipment(String destination) {
        this.destination = destination;
        this.status = ShipmentStatus.OPEN;
    }

    @PrePersist
    void onCreate() {
        createdAt = Instant.now();
    }

    /** Reserves stock; adding an item already in the shipment increases that line. */
    public void addItem(InventoryItem inventoryItem, int quantity) {
        requireOpen();
        inventoryItem.removeStock(quantity);
        items.stream()
                .filter(line -> Objects.equals(line.getInventoryItem().getId(), inventoryItem.getId()))
                .findFirst()
                .ifPresentOrElse(
                        line -> line.increase(quantity),
                        () -> items.add(new ShipmentItem(this, inventoryItem, quantity)));
    }

    public void removeItem(Long shipmentItemId) {
        requireOpen();
        ShipmentItem line = items.stream()
                .filter(it -> Objects.equals(it.getId(), shipmentItemId))
                .findFirst()
                .orElseThrow(() -> new NotFoundException("Shipment item", shipmentItemId));
        line.getInventoryItem().returnStock(line.getQuantity());
        items.remove(line);
    }

    public void ship() {
        requireOpen();
        if (items.isEmpty()) {
            throw new InvalidShipmentStateException("Cannot ship an empty shipment");
        }
        status = ShipmentStatus.SHIPPED;
        shippedAt = Instant.now();
    }

    public void cancel() {
        requireOpen();
        items.forEach(line -> line.getInventoryItem().returnStock(line.getQuantity()));
        status = ShipmentStatus.CANCELLED;
    }

    private void requireOpen() {
        if (status != ShipmentStatus.OPEN) {
            throw new InvalidShipmentStateException(
                    "Shipment " + id + " is " + status + " and can no longer be changed");
        }
    }

    public Long getId() { return id; }
    public String getDestination() { return destination; }
    public ShipmentStatus getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getShippedAt() { return shippedAt; }
    public List<ShipmentItem> getItems() { return Collections.unmodifiableList(items); }
}
