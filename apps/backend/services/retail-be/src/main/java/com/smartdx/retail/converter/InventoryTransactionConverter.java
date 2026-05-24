package com.smartdx.retail.converter;

import com.smartdx.retail.model.entity.InventoryTransaction;
import com.smartdx.retail.model.form.InventoryTransactionForm;
import com.smartdx.retail.model.vo.InventoryTransactionPageVO;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;

/**
 * 入出庫履歴コンバーター
 */
@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface InventoryTransactionConverter {

    /**
     * InventoryTransactionForm から InventoryTransaction エンティティへの変換
     *
     * @param form 入出庫履歴フォーム
     * @return 入出庫履歴エンティティ
     */
    InventoryTransaction form2Entity(InventoryTransactionForm form);

    /**
     * InventoryTransaction エンティティから InventoryTransactionPageVO への変換
     * 注意: storeName, productName, productCode は別途設定が必要
     *
     * @param entity 入出庫履歴エンティティ
     * @return 入出庫履歴ページングVO
     */
    InventoryTransactionPageVO entity2Vo(InventoryTransaction entity);
}
