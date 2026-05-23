package com.smartdx.retail.service;

import com.smartdx.retail.model.entity.Inventory;

import java.util.List;

/**
 * Inventory Service Interface
 */
public interface InventoryService {

    List<Inventory> listInventories(Long storeId, Long productId);

    Inventory getInventoryById(Long id);

    boolean createInventory(Inventory inventory);

    boolean updateInventory(Long id, Inventory inventory);

    boolean deleteInventory(Long id);
}
