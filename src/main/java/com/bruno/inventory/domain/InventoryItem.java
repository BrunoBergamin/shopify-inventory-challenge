package com.bruno.inventory.domain;

import com.bruno.inventory.exception.InsufficientStockException;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "inventory_item")
public class InventoryItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 64)
    private String sku;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(length = 500)
    private String description;

    @Column(nullable = false)
    private int quantity;

    @Column(name = "unit_price", nullable = false, precision = 12, scale = 2)
    private BigDecimal unitPrice;

    /** Optimistic lock: concurrent stock changes fail with 409 instead of silently overwriting. */
    @Version
    private long version;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected InventoryItem() {
        // required by JPA
    }

    public InventoryItem(String sku, String name, String description, int quantity, BigDecimal unitPrice) {
        this.sku = sku;
        this.name = name;
        this.description = description;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
    }

    @PrePersist
    void onCreate() {
        createdAt = Instant.now();
        updatedAt = createdAt;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }

    public void update(String sku, String name, String description, int quantity, BigDecimal unitPrice) {
        this.sku = sku;
        this.name = name;
        this.description = description;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
    }

    public void removeStock(int amount) {
        if (amount > quantity) {
            throw new InsufficientStockException(sku, quantity, amount);
        }
        quantity -= amount;
    }

    public void returnStock(int amount) {
        quantity += amount;
    }

    public Long getId() { return id; }
    public String getSku() { return sku; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public int getQuantity() { return quantity; }
    public BigDecimal getUnitPrice() { return unitPrice; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
