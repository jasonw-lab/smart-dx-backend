package com.smartdx.retail.service;

import com.smartdx.retail.model.entity.Inventory;
import com.smartdx.retail.model.form.InventoryDiscardForm;
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

    /**
     * 在庫廃棄（ADR-011）。対象ロットの数量減算と DISPOSAL 取引履歴の登録を同一トランザクションで行う
     *
     * @param inventoryId 在庫ID（ロット単位）
     * @param form 廃棄フォーム
     * @return 成功可否
     */
    boolean discardInventory(Long inventoryId, InventoryDiscardForm form);
}
