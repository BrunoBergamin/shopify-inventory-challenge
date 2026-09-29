package com.bruno.inventory.domain;

import jakarta.persistence.*;

@Entity
@Table(name = "shipment_item")
public class ShipmentItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "shipment_id")
    private Shipment shipment;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "inventory_item_id")
    private InventoryItem inventoryItem;

    @Column(nullable = false)
    private int quantity;

    protected ShipmentItem() {
        // required by JPA
    }

    ShipmentItem(Shipment shipment, InventoryItem inventoryItem, int quantity) {
        this.shipment = shipment;
        this.inventoryItem = inventoryItem;
        this.quantity = quantity;
    }

    void increase(int amount) {
        quantity += amount;
    }

    public Long getId() { return id; }
    public Shipment getShipment() { return shipment; }
    public InventoryItem getInventoryItem() { return inventoryItem; }
    public int getQuantity() { return quantity; }
}
