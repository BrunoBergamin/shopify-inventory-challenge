package com.bruno.inventory.domain;

public enum ShipmentStatus {
    /** Being assembled: items can be added or removed, stock is already reserved. */
    OPEN,
    /** Left the warehouse: the stock change is final and the shipment can no longer change. */
    SHIPPED,
    /** Abandoned before shipping: every reserved unit went back to stock. */
    CANCELLED
}
