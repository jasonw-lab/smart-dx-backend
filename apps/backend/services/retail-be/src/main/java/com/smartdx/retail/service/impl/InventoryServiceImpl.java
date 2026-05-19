package com.smartdx.retail.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.smartdx.retail.mapper.InventoryMapper;
import com.smartdx.retail.model.entity.Inventory;
import com.smartdx.retail.service.InventoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Inventory Service Implementation
 */
@Service
@RequiredArgsConstructor
public class InventoryServiceImpl extends ServiceImpl<InventoryMapper, Inventory> implements InventoryService {

    @Override
    public List<Inventory> listInventories(Long storeId, Long productId) {
        LambdaQueryWrapper<Inventory> queryWrapper = new LambdaQueryWrapper<Inventory>()
                .eq(storeId != null, Inventory::getStoreId, storeId)
                .eq(productId != null, Inventory::getProductId, productId)
                .orderByAsc(Inventory::getExpiryDate);
        return this.list(queryWrapper);
    }

    @Override
    public Inventory getInventoryById(Long id) {
        return this.getById(id);
    }

    @Override
    public boolean createInventory(Inventory inventory) {
        return this.save(inventory);
    }

    @Override
    public boolean updateInventory(Long id, Inventory inventory) {
        inventory.setId(id);
        return this.updateById(inventory);
    }

    @Override
    public boolean deleteInventory(Long id) {
        return this.removeById(id);
    }
}
