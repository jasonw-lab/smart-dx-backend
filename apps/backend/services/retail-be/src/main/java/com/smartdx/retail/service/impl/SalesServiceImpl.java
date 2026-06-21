package com.smartdx.retail.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.smartdx.retail.mapper.SalesMapper;
import com.smartdx.retail.mapper.StoreMapper;
import com.smartdx.retail.model.entity.Sales;
import com.smartdx.retail.model.entity.Store;
import com.smartdx.retail.service.SalesService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Sales Service Implementation
 */
@Service
@RequiredArgsConstructor
public class SalesServiceImpl extends ServiceImpl<SalesMapper, Sales> implements SalesService {

    private final StoreMapper storeMapper;

    @Override
    public List<Sales> listSales(Long storeId) {
        LambdaQueryWrapper<Sales> queryWrapper = new LambdaQueryWrapper<Sales>()
                .eq(storeId != null, Sales::getStoreId, storeId)
                .orderByDesc(Sales::getSaleTimestamp);
        List<Sales> salesList = this.list(queryWrapper);
        populateStoreName(salesList);
        return salesList;
    }

    @Override
    public Sales getSalesById(Long id) {
        Sales sales = this.getById(id);
        if (sales != null) {
            populateStoreName(Collections.singletonList(sales));
        }
        return sales;
    }

    @Override
    public boolean createSales(Sales sales) {
        return this.save(sales);
    }

    private void populateStoreName(List<Sales> salesList) {
        if (salesList.isEmpty()) {
            return;
        }

        List<Long> storeIds = salesList.stream()
                .map(Sales::getStoreId)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());

        if (storeIds.isEmpty()) {
            return;
        }

        List<Store> stores = storeMapper.selectBatchIds(storeIds);
        Map<Long, String> storeNameMap = stores.stream()
                .collect(Collectors.toMap(Store::getId, Store::getStoreName));

        for (Sales sales : salesList) {
            if (sales.getStoreId() != null) {
                sales.setStoreName(storeNameMap.getOrDefault(sales.getStoreId(), ""));
            }
        }
    }
}
