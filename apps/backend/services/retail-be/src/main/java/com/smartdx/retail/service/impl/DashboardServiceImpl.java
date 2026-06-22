package com.smartdx.retail.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.smartdx.retail.mapper.AlertMapper;
import com.smartdx.retail.mapper.InventoryMapper;
import com.smartdx.retail.mapper.SalesMapper;
import com.smartdx.retail.mapper.StoreMapper;
import com.smartdx.retail.model.entity.Alert;
import com.smartdx.retail.model.entity.Inventory;
import com.smartdx.retail.model.entity.Sales;
import com.smartdx.retail.model.entity.Store;
import com.smartdx.retail.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

/**
 * Dashboard Service Implementation
 */
@Service
@RequiredArgsConstructor
public class DashboardServiceImpl implements DashboardService {

    private final SalesMapper salesMapper;
    private final StoreMapper storeMapper;
    private final AlertMapper alertMapper;
    private final InventoryMapper inventoryMapper;

    @Override
    public Map<String, Object> getKpi() {
        Map<String, Object> kpi = new HashMap<>();

        LocalDate today = LocalDate.now();
        LocalDate yesterday = today.minusDays(1);

        // Today's sales
        BigDecimal todaySalesTotal = sumSalesByDate(today);
        kpi.put("todaySales", todaySalesTotal);

        // Sales growth rate vs yesterday
        BigDecimal yesterdaySalesTotal = sumSalesByDate(yesterday);
        BigDecimal salesGrowthRate = BigDecimal.ZERO;
        if (yesterdaySalesTotal.compareTo(BigDecimal.ZERO) > 0) {
            salesGrowthRate = todaySalesTotal.subtract(yesterdaySalesTotal)
                    .multiply(BigDecimal.valueOf(100))
                    .divide(yesterdaySalesTotal, 2, RoundingMode.HALF_UP);
        }
        kpi.put("salesGrowthRate", salesGrowthRate);

        // Store counts
        LambdaQueryWrapper<Store> onlineQuery = new LambdaQueryWrapper<Store>()
                .eq(Store::getStatus, "ONLINE");
        Long activeStoreCount = storeMapper.selectCount(onlineQuery);
        Long totalStoreCount = storeMapper.selectCount(null);
        kpi.put("activeStoreCount", activeStoreCount);
        kpi.put("totalStoreCount", totalStoreCount);

        // Pending alerts
        LambdaQueryWrapper<Alert> alertQuery = new LambdaQueryWrapper<Alert>()
                .in(Alert::getStatus, "NEW", "ACK", "IN_PROGRESS");
        Long pendingAlertCount = alertMapper.selectCount(alertQuery);
        kpi.put("pendingAlertCount", pendingAlertCount);

        // Out of stock SKU count
        long outOfStockSkuCount = countOutOfStockSkus();
        kpi.put("outOfStockSkuCount", outOfStockSkuCount);

        return kpi;
    }

    @Override
    public List<Map<String, Object>> getSalesTrend(LocalDate startDate, LocalDate endDate) {
        LocalDate end = endDate != null ? endDate : LocalDate.now();
        LocalDate start = startDate != null ? startDate : end.minusDays(6);

        LocalDateTime rangeStart = start.atStartOfDay();
        LocalDateTime rangeEnd = end.plusDays(1).atStartOfDay();

        LambdaQueryWrapper<Sales> salesQuery = new LambdaQueryWrapper<Sales>()
                .ge(Sales::getSaleTimestamp, rangeStart)
                .lt(Sales::getSaleTimestamp, rangeEnd);
        List<Sales> salesList = salesMapper.selectList(salesQuery);

        TreeMap<LocalDate, BigDecimal> dailyTotals = new TreeMap<>();
        for (LocalDate d = start; !d.isAfter(end); d = d.plusDays(1)) {
            dailyTotals.put(d, BigDecimal.ZERO);
        }
        for (Sales sale : salesList) {
            if (sale.getSaleTimestamp() == null || sale.getTotalAmount() == null) {
                continue;
            }
            LocalDate day = sale.getSaleTimestamp().toLocalDate();
            dailyTotals.merge(day, sale.getTotalAmount(), BigDecimal::add);
        }

        List<Map<String, Object>> result = new ArrayList<>(dailyTotals.size());
        BigDecimal prevAmount = null;
        for (Map.Entry<LocalDate, BigDecimal> entry : dailyTotals.entrySet()) {
            BigDecimal amount = entry.getValue();
            BigDecimal growthRate = BigDecimal.ZERO;
            if (prevAmount != null && prevAmount.compareTo(BigDecimal.ZERO) > 0) {
                growthRate = amount.subtract(prevAmount)
                        .multiply(BigDecimal.valueOf(100))
                        .divide(prevAmount, 2, RoundingMode.HALF_UP);
            }
            Map<String, Object> item = new HashMap<>();
            item.put("date", entry.getKey().toString());
            item.put("salesAmount", amount);
            item.put("growthRate", growthRate);
            result.add(item);
            prevAmount = amount;
        }
        return result;
    }

    private BigDecimal sumSalesByDate(LocalDate date) {
        LocalDateTime startOfDay = date.atStartOfDay();
        LocalDateTime endOfDay = startOfDay.plusDays(1);
        LambdaQueryWrapper<Sales> salesQuery = new LambdaQueryWrapper<Sales>()
                .ge(Sales::getSaleTimestamp, startOfDay)
                .lt(Sales::getSaleTimestamp, endOfDay);
        List<Sales> salesList = salesMapper.selectList(salesQuery);
        return salesList.stream()
                .map(Sales::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private long countOutOfStockSkus() {
        LambdaQueryWrapper<Inventory> inventoryQuery = new LambdaQueryWrapper<Inventory>();
        List<Inventory> inventories = inventoryMapper.selectList(inventoryQuery);

        Map<Long, Integer> productStock = inventories.stream()
                .collect(Collectors.groupingBy(
                        Inventory::getProductId,
                        Collectors.summingInt(inv -> inv.getQuantity() != null ? inv.getQuantity() : 0)
                ));

        return productStock.values().stream()
                .filter(quantity -> quantity <= 0)
                .count();
    }
}
