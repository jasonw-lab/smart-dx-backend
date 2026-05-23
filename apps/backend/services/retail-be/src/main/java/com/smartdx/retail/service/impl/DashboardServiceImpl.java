package com.smartdx.retail.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.smartdx.retail.mapper.AlertMapper;
import com.smartdx.retail.mapper.SalesMapper;
import com.smartdx.retail.mapper.StoreMapper;
import com.smartdx.retail.model.entity.Alert;
import com.smartdx.retail.model.entity.Sales;
import com.smartdx.retail.model.entity.Store;
import com.smartdx.retail.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Dashboard Service Implementation
 */
@Service
@RequiredArgsConstructor
public class DashboardServiceImpl implements DashboardService {

    private final SalesMapper salesMapper;
    private final StoreMapper storeMapper;
    private final AlertMapper alertMapper;

    @Override
    public Map<String, Object> getKpi() {
        Map<String, Object> kpi = new HashMap<>();

        // Today's sales
        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
        LocalDateTime endOfDay = startOfDay.plusDays(1);
        LambdaQueryWrapper<Sales> salesQuery = new LambdaQueryWrapper<Sales>()
                .ge(Sales::getSaleTimestamp, startOfDay)
                .lt(Sales::getSaleTimestamp, endOfDay);
        List<Sales> todaySales = salesMapper.selectList(salesQuery);
        BigDecimal todaySalesTotal = todaySales.stream()
                .map(Sales::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        kpi.put("todaySales", todaySalesTotal);

        // Store counts
        LambdaQueryWrapper<Store> onlineQuery = new LambdaQueryWrapper<Store>()
                .eq(Store::getStatus, "ONLINE");
        Long onlineStoreCount = storeMapper.selectCount(onlineQuery);
        Long totalStoreCount = storeMapper.selectCount(null);
        kpi.put("onlineStoreCount", onlineStoreCount);
        kpi.put("totalStoreCount", totalStoreCount);

        // Active alerts
        LambdaQueryWrapper<Alert> alertQuery = new LambdaQueryWrapper<Alert>()
                .in(Alert::getStatus, "NEW", "ACK", "IN_PROGRESS");
        Long activeAlertCount = alertMapper.selectCount(alertQuery);
        kpi.put("activeAlertCount", activeAlertCount);

        return kpi;
    }
}
