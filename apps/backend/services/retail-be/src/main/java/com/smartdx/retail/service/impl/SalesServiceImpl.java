package com.smartdx.retail.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.smartdx.retail.mapper.SalesMapper;
import com.smartdx.retail.model.entity.Sales;
import com.smartdx.retail.service.SalesService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Sales Service Implementation
 */
@Service
@RequiredArgsConstructor
public class SalesServiceImpl extends ServiceImpl<SalesMapper, Sales> implements SalesService {

    @Override
    public List<Sales> listSales(Long storeId) {
        LambdaQueryWrapper<Sales> queryWrapper = new LambdaQueryWrapper<Sales>()
                .eq(storeId != null, Sales::getStoreId, storeId)
                .orderByDesc(Sales::getSaleTimestamp);
        return this.list(queryWrapper);
    }

    @Override
    public Sales getSalesById(Long id) {
        return this.getById(id);
    }

    @Override
    public boolean createSales(Sales sales) {
        return this.save(sales);
    }
}
