package com.bruno.inventory.repository;

import com.bruno.inventory.domain.Shipment;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ShipmentRepository extends JpaRepository<Shipment, Long> {

    @EntityGraph(attributePaths = {"items", "items.inventoryItem"})
    Optional<Shipment> findWithItemsById(Long id);

    @EntityGraph(attributePaths = {"items", "items.inventoryItem"})
    List<Shipment> findAllByOrderByIdDesc();
}
