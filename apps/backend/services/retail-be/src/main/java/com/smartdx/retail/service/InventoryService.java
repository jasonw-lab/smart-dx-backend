package com.smartdx.retail.service;

import com.smartdx.retail.model.entity.Inventory;
import com.smartdx.retail.model.vo.InventoryPageVO;

import java.util.List;

/**
 * Inventory Service Interface
 */
public interface InventoryService {

    List<InventoryPageVO> listInventories(Long storeId, Long productId);

    InventoryPageVO getInventoryById(Long id);

    boolean createInventory(Inventory inventory);

    boolean updateInventory(Long id, Inventory inventory);

    boolean deleteInventory(Long id);
}
