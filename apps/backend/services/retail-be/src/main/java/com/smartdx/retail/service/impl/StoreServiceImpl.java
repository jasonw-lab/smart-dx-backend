package com.smartdx.retail.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.smartdx.retail.mapper.StoreMapper;
import com.smartdx.retail.model.entity.Store;
import com.smartdx.retail.service.StoreService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Store Service Implementation
 */
@Service
@RequiredArgsConstructor
public class StoreServiceImpl extends ServiceImpl<StoreMapper, Store> implements StoreService {

    @Override
    public List<Store> listStores() {
        LambdaQueryWrapper<Store> queryWrapper = new LambdaQueryWrapper<Store>()
                .orderByAsc(Store::getStoreCode);
        return this.list(queryWrapper);
    }

    @Override
    public Store getStoreById(Long id) {
        return this.getById(id);
    }

    @Override
    public boolean createStore(Store store) {
        return this.save(store);
    }

    @Override
    public boolean updateStore(Long id, Store store) {
        store.setId(id);
        return this.updateById(store);
    }

    @Override
    public boolean deleteStore(Long id) {
        return this.removeById(id);
    }
}
