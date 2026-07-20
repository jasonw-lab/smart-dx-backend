package com.smartdx.retail.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.smartdx.retail.model.entity.Inventory;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * Inventory Mapper
 */
@Mapper
public interface InventoryMapper extends BaseMapper<Inventory> {

    /**
     * 在庫ロットを排他ロック（SELECT ... FOR UPDATE）して取得する（廃棄等の減算処理用）
     *
     * @param id 在庫ID
     * @return 在庫エンティティ（不存在時は null）
     */
    @Select("SELECT * FROM retail_inventory WHERE id = #{id} AND is_deleted = 0 FOR UPDATE")
    Inventory selectForUpdate(@Param("id") Long id);
}
