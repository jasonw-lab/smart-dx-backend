package com.smartdx.retail.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.smartdx.retail.mapper.InventoryMapper;
import com.smartdx.retail.mapper.ProductMapper;
import com.smartdx.retail.mapper.StoreMapper;
import com.smartdx.retail.model.entity.Inventory;
import com.smartdx.retail.model.entity.Product;
import com.smartdx.retail.model.entity.Store;
import com.smartdx.retail.model.vo.InventorySummaryVO;
import com.smartdx.retail.service.InventorySummaryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 在庫集約サービス実装クラス（ADR-011）
 * <p>
 * ロット単位の在庫を店舗×商品単位に集約する読取専用サービス。
 * ステータスは各ロットに InventoryServiceImpl#calculateStatus と同一ロジックを適用し、
 * 重大度最大（EXPIRED > LOW_STOCK > EXPIRY_SOON > HIGH_STOCK > NORMAL）を集約ステータスとする。
 * </p>
 */
@Service
@RequiredArgsConstructor
public class InventorySummaryServiceImpl implements InventorySummaryService {

    /**
     * ステータス重大度（大きいほど重大）
     */
    private static final Map<String, Integer> STATUS_SEVERITY = Map.of(
            "NORMAL", 0,
            "HIGH_STOCK", 1,
            "EXPIRY_SOON", 2,
            "LOW_STOCK", 3,
            "EXPIRED", 4
    );

    private final InventoryMapper inventoryMapper;
    private final StoreMapper storeMapper;
    private final ProductMapper productMapper;

    @Override
    public List<InventorySummaryVO> listSummaries(Long storeId, Long productId, String productName, String status) {
        // 商品名指定時は商品側で絞り込み、対象商品IDで在庫を限定する
        List<Long> productIdsFilter = null;
        if (StringUtils.hasText(productName)) {
            productIdsFilter = productMapper.selectList(new LambdaQueryWrapper<Product>()
                            .like(Product::getProductName, productName))
                    .stream().map(Product::getId).collect(Collectors.toList());
            if (productIdsFilter.isEmpty()) {
                return new ArrayList<>();
            }
        }

        LambdaQueryWrapper<Inventory> queryWrapper = new LambdaQueryWrapper<Inventory>()
                .eq(storeId != null, Inventory::getStoreId, storeId)
                .eq(productId != null, Inventory::getProductId, productId)
                .in(productIdsFilter != null, Inventory::getProductId, productIdsFilter)
                .orderByAsc(Inventory::getExpiryDate);
        List<Inventory> inventories = inventoryMapper.selectList(queryWrapper);
        if (inventories.isEmpty()) {
            return new ArrayList<>();
        }

        // 店舗・商品マスタ取得
        List<Long> storeIds = inventories.stream()
                .map(Inventory::getStoreId).filter(Objects::nonNull).distinct().collect(Collectors.toList());
        List<Long> productIds = inventories.stream()
                .map(Inventory::getProductId).filter(Objects::nonNull).distinct().collect(Collectors.toList());

        Map<Long, String> storeNameMap = new HashMap<>();
        if (!storeIds.isEmpty()) {
            storeNameMap.putAll(storeMapper.selectBatchIds(storeIds).stream()
                    .collect(Collectors.toMap(Store::getId, Store::getStoreName)));
        }
        Map<Long, Product> productMap = new HashMap<>();
        if (!productIds.isEmpty()) {
            productMap.putAll(productMapper.selectBatchIds(productIds).stream()
                    .collect(Collectors.toMap(Product::getId, p -> p)));
        }

        // 店舗×商品でグルーピング（キー出現順を維持）
        Map<String, List<Inventory>> groups = new LinkedHashMap<>();
        for (Inventory inv : inventories) {
            String key = inv.getStoreId() + "-" + inv.getProductId();
            groups.computeIfAbsent(key, k -> new ArrayList<>()).add(inv);
        }

        LocalDate today = LocalDate.now();

        List<InventorySummaryVO> summaries = groups.entrySet().stream().map(entry -> {
            List<Inventory> lots = entry.getValue();
            Inventory first = lots.get(0);

            InventorySummaryVO vo = new InventorySummaryVO();
            vo.setSummaryKey(entry.getKey());
            vo.setStoreId(first.getStoreId());
            vo.setStoreName(storeNameMap.getOrDefault(first.getStoreId(), ""));
            vo.setProductId(first.getProductId());

            Product product = productMap.get(first.getProductId());
            int minStock = 0;
            int maxStock = 0;
            if (product != null) {
                vo.setProductCode(product.getProductCode());
                vo.setProductName(product.getProductName());
                minStock = product.getReorderPoint() != null ? product.getReorderPoint() : 0;
                maxStock = product.getMaxStock() != null ? product.getMaxStock() : 0;
            } else {
                vo.setProductCode("");
                vo.setProductName("");
            }
            vo.setReorderPoint(minStock);
            vo.setUpperLimit(maxStock);

            int totalQuantity = 0;
            LocalDate oldestExpiryDate = null;
            String worstStatus = "NORMAL";
            List<InventorySummaryVO.SummaryLotVO> lotVOs = new ArrayList<>();
            for (Inventory lot : lots) {
                int qty = lot.getQuantity() != null ? lot.getQuantity() : 0;
                totalQuantity += qty;
                if (lot.getExpiryDate() != null
                        && (oldestExpiryDate == null || lot.getExpiryDate().isBefore(oldestExpiryDate))) {
                    oldestExpiryDate = lot.getExpiryDate();
                }
                String lotStatus = calculateLotStatus(qty, minStock, maxStock, lot.getExpiryDate(), today);
                if (STATUS_SEVERITY.get(lotStatus) > STATUS_SEVERITY.get(worstStatus)) {
                    worstStatus = lotStatus;
                }

                InventorySummaryVO.SummaryLotVO lotVO = new InventorySummaryVO.SummaryLotVO();
                lotVO.setId(lot.getId());
                lotVO.setLotNumber(lot.getLotNumber());
                lotVO.setQuantity(lot.getQuantity());
                lotVO.setExpiryDate(lot.getExpiryDate());
                lotVOs.add(lotVO);
            }
            vo.setTotalQuantity(totalQuantity);
            vo.setOldestExpiryDate(oldestExpiryDate);
            vo.setStatus(worstStatus);
            vo.setLots(lotVOs);
            // turnoverRate は要件未確定のため常に null（ADR-011）
            vo.setTurnoverRate(null);
            return vo;
        }).collect(Collectors.toList());

        // 集約後ステータスでの絞り込み
        if (StringUtils.hasText(status)) {
            summaries = summaries.stream()
                    .filter(s -> status.equals(s.getStatus()))
                    .collect(Collectors.toList());
        }

        // キー（summaryKey）昇順で安定化
        summaries.sort(Comparator.comparing(InventorySummaryVO::getSummaryKey));
        return summaries;
    }

    /**
     * ロット単位のステータスを算出する（InventoryServiceImpl#calculateStatus と同一ロジック）
     */
    private String calculateLotStatus(Integer quantity, Integer minStock, Integer maxStock,
                                      LocalDate expiryDate, LocalDate today) {
        int qty = quantity != null ? quantity : 0;
        int min = minStock != null ? minStock : 0;
        int max = maxStock != null ? maxStock : 0;

        boolean expired = expiryDate != null && !expiryDate.isAfter(today);
        boolean expirySoon = expiryDate != null && !expired && ChronoUnit.DAYS.between(today, expiryDate) <= 7;

        if (expired) {
            return "EXPIRED";
        } else if (expirySoon) {
            return "EXPIRY_SOON";
        } else if (qty <= 0) {
            return "LOW_STOCK";
        } else if (qty <= min) {
            return "LOW_STOCK";
        } else if (max > 0 && qty > max) {
            return "HIGH_STOCK";
        } else {
            return "NORMAL";
        }
    }
}
