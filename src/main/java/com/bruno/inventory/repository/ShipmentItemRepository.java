package com.bruno.inventory.repository;

import com.bruno.inventory.domain.ShipmentItem;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ShipmentItemRepository extends JpaRepository<ShipmentItem, Long> {

    boolean existsByInventoryItemId(Long inventoryItemId);
}
