package com.bruno.inventory.service;

import com.bruno.inventory.domain.InventoryItem;
import com.bruno.inventory.exception.ConflictException;
import com.bruno.inventory.exception.NotFoundException;
import com.bruno.inventory.repository.InventoryItemRepository;
import com.bruno.inventory.repository.ShipmentItemRepository;
import com.bruno.inventory.web.dto.InventoryItemRequest;
import com.bruno.inventory.web.dto.InventoryItemResponse;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class InventoryItemService {

    private final InventoryItemRepository itemRepository;
    private final ShipmentItemRepository shipmentItemRepository;

    public InventoryItemService(InventoryItemRepository itemRepository, ShipmentItemRepository shipmentItemRepository) {
        this.itemRepository = itemRepository;
        this.shipmentItemRepository = shipmentItemRepository;
    }

    public List<InventoryItemResponse> findAll() {
        return itemRepository.findAll(Sort.by("id")).stream().map(InventoryItemResponse::from).toList();
    }

    public InventoryItemResponse findById(Long id) {
        return InventoryItemResponse.from(getItem(id));
    }

    @Transactional
    public InventoryItemResponse create(InventoryItemRequest request) {
        String sku = normalizeSku(request.sku());
        if (itemRepository.existsBySku(sku)) {
            throw new ConflictException("An item with SKU " + sku + " already exists");
        }
        InventoryItem item = new InventoryItem(sku, request.name().strip(), request.description(),
                request.quantity(), request.unitPrice());
        return InventoryItemResponse.from(itemRepository.save(item));
    }

    @Transactional
    public InventoryItemResponse update(Long id, InventoryItemRequest request) {
        InventoryItem item = getItem(id);
        String sku = normalizeSku(request.sku());
        if (itemRepository.existsBySkuAndIdNot(sku, id)) {
            throw new ConflictException("An item with SKU " + sku + " already exists");
        }
        item.update(sku, request.name().strip(), request.description(), request.quantity(), request.unitPrice());
        // Flush so a concurrent-update conflict surfaces here, and the response carries the new updatedAt.
        return InventoryItemResponse.from(itemRepository.saveAndFlush(item));
    }

    /**
     * Items that appear in any shipment are kept, because deleting them would erase shipment history.
     */
    @Transactional
    public void delete(Long id) {
        InventoryItem item = getItem(id);
        if (shipmentItemRepository.existsByInventoryItemId(id)) {
            throw new ConflictException("Item " + id + " is part of a shipment and cannot be deleted");
        }
        itemRepository.delete(item);
    }

    private InventoryItem getItem(Long id) {
        return itemRepository.findById(id).orElseThrow(() -> new NotFoundException("Inventory item", id));
    }

    /** SKUs are identifiers people type and read aloud, so "box-1" and "BOX-1" must be the same item. */
    private static String normalizeSku(String sku) {
        return sku.strip().toUpperCase();
    }
}
