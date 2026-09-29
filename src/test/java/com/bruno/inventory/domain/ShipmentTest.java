package com.bruno.inventory.domain;

import com.bruno.inventory.exception.InsufficientStockException;
import com.bruno.inventory.exception.InvalidShipmentStateException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ShipmentTest {

    private InventoryItem boxes;
    private InventoryItem tape;
    private Shipment shipment;

    @BeforeEach
    void setUp() {
        boxes = item(1L, "BOX-1", 10);
        tape = item(2L, "TAPE-1", 5);
        shipment = new Shipment("Warehouse B");
    }

    @Test
    void addingAnItemReservesItsStock() {
        shipment.addItem(boxes, 4);

        assertThat(boxes.getQuantity()).isEqualTo(6);
        assertThat(shipment.getItems()).singleElement()
                .satisfies(line -> assertThat(line.getQuantity()).isEqualTo(4));
    }

    @Test
    void addingTheSameItemTwiceMergesTheLine() {
        shipment.addItem(boxes, 4);
        shipment.addItem(boxes, 3);

        assertThat(shipment.getItems()).singleElement()
                .satisfies(line -> assertThat(line.getQuantity()).isEqualTo(7));
        assertThat(boxes.getQuantity()).isEqualTo(3);
    }

    @Test
    void cannotReserveMoreThanIsInStock() {
        assertThatThrownBy(() -> shipment.addItem(boxes, 11))
                .isInstanceOf(InsufficientStockException.class);

        assertThat(boxes.getQuantity()).isEqualTo(10);
        assertThat(shipment.getItems()).isEmpty();
    }

    @Test
    void removingALineReturnsItsStock() {
        shipment.addItem(boxes, 4);
        ShipmentItem line = shipment.getItems().getFirst();
        ReflectionTestUtils.setField(line, "id", 100L);

        shipment.removeItem(100L);

        assertThat(boxes.getQuantity()).isEqualTo(10);
        assertThat(shipment.getItems()).isEmpty();
    }

    @Test
    void cancellingReturnsEveryReservedUnit() {
        shipment.addItem(boxes, 4);
        shipment.addItem(tape, 5);

        shipment.cancel();

        assertThat(shipment.getStatus()).isEqualTo(ShipmentStatus.CANCELLED);
        assertThat(boxes.getQuantity()).isEqualTo(10);
        assertThat(tape.getQuantity()).isEqualTo(5);
    }

    @Test
    void shippingKeepsTheStockOutAndLocksTheShipment() {
        shipment.addItem(boxes, 4);

        shipment.ship();

        assertThat(shipment.getStatus()).isEqualTo(ShipmentStatus.SHIPPED);
        assertThat(shipment.getShippedAt()).isNotNull();
        assertThat(boxes.getQuantity()).isEqualTo(6);
        assertThatThrownBy(() -> shipment.addItem(tape, 1)).isInstanceOf(InvalidShipmentStateException.class);
        assertThatThrownBy(shipment::cancel).isInstanceOf(InvalidShipmentStateException.class);
    }

    @Test
    void cannotShipAnEmptyShipment() {
        assertThatThrownBy(shipment::ship).isInstanceOf(InvalidShipmentStateException.class);
    }

    private static InventoryItem item(Long id, String sku, int quantity) {
        InventoryItem item = new InventoryItem(sku, sku, null, quantity, BigDecimal.ONE);
        ReflectionTestUtils.setField(item, "id", id);
        return item;
    }
}
