package com.smartdx.retail.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.smartdx.core.exception.BusinessException;
import com.smartdx.retail.exception.InventoryShortageException;
import com.smartdx.retail.mapper.InventoryMapper;
import com.smartdx.retail.mapper.InventoryTransactionMapper;
import com.smartdx.retail.mapper.ProductMapper;
import com.smartdx.retail.mapper.StoreMapper;
import com.smartdx.retail.model.entity.Inventory;
import com.smartdx.retail.model.entity.InventoryTransaction;
import com.smartdx.retail.model.entity.Product;
import com.smartdx.retail.model.entity.Store;
import com.smartdx.retail.model.form.InventoryDiscardForm;
import com.smartdx.retail.model.vo.InventoryPageVO;
import com.smartdx.retail.service.InventoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Inventory Service Implementation
 */
@Service
@RequiredArgsConstructor
public class InventoryServiceImpl extends ServiceImpl<InventoryMapper, Inventory> implements InventoryService {

    private final StoreMapper storeMapper;
    private final ProductMapper productMapper;
    private final InventoryTransactionMapper inventoryTransactionMapper;

    @Override
    public List<InventoryPageVO> listInventories(Long storeId, Long productId) {
        LambdaQueryWrapper<Inventory> queryWrapper = new LambdaQueryWrapper<Inventory>()
                .eq(storeId != null, Inventory::getStoreId, storeId)
                .eq(productId != null, Inventory::getProductId, productId)
                .orderByAsc(Inventory::getExpiryDate);
        List<Inventory> inventories = this.list(queryWrapper);
        return toPageVO(inventories);
    }

    @Override
    public InventoryPageVO getInventoryById(Long id) {
        Inventory inventory = this.getById(id);
        if (inventory == null) {
            return null;
        }
        List<InventoryPageVO> vos = toPageVO(Collections.singletonList(inventory));
        return vos.isEmpty() ? null : vos.get(0);
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

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean discardInventory(Long inventoryId, InventoryDiscardForm form) {
        // 対象ロットを排他ロックして取得（同時更新による残量不整合を防ぐ）
        Inventory inventory = this.baseMapper.selectForUpdate(inventoryId);
        if (inventory == null) {
            throw new BusinessException("対象の在庫ロットが存在しません: id=" + inventoryId);
        }

        int currentQty = inventory.getQuantity() == null ? 0 : inventory.getQuantity();
        if (form.getQuantity() > currentQty) {
            throw new InventoryShortageException(
                    "在庫残量が不足しています（current=" + currentQty + ", requested=" + form.getQuantity() + "）");
        }

        // ロット数量減算と DISPOSAL 履歴登録を同一トランザクションで行う
        inventory.setQuantity(currentQty - form.getQuantity());
        this.baseMapper.updateById(inventory);

        InventoryTransaction transaction = new InventoryTransaction();
        transaction.setInventoryId(inventory.getId());
        transaction.setStoreId(inventory.getStoreId());
        transaction.setProductId(inventory.getProductId());
        transaction.setLotNumber(inventory.getLotNumber());
        transaction.setTxnType("DISPOSAL");
        transaction.setQuantityDelta(-form.getQuantity());
        transaction.setSourceType("MANUAL");
        transaction.setReason(form.getReason());
        transaction.setNote(form.getRemarks());
        transaction.setOccurredAt(LocalDateTime.now());
        inventoryTransactionMapper.insert(transaction);

        return true;
    }

    private List<InventoryPageVO> toPageVO(List<Inventory> inventories) {
        if (inventories.isEmpty()) {
            return Collections.emptyList();
        }

        List<Long> storeIds = inventories.stream()
                .map(Inventory::getStoreId)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        List<Long> productIds = inventories.stream()
                .map(Inventory::getProductId)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());

        final Map<Long, String> storeNameMap = new java.util.HashMap<>();
        final Map<Long, Product> productMap = new java.util.HashMap<>();

        if (!storeIds.isEmpty()) {
            List<Store> stores = storeMapper.selectBatchIds(storeIds);
            storeNameMap.putAll(stores.stream()
                    .collect(Collectors.toMap(Store::getId, Store::getStoreName)));
        }

        if (!productIds.isEmpty()) {
            List<Product> products = productMapper.selectBatchIds(productIds);
            productMap.putAll(products.stream()
                    .collect(Collectors.toMap(Product::getId, p -> p)));
        }

        LocalDate today = LocalDate.now();

        return inventories.stream().map(inv -> {
            InventoryPageVO vo = new InventoryPageVO();
            vo.setId(inv.getId());
            vo.setStoreId(inv.getStoreId());
            vo.setStoreName(storeNameMap.getOrDefault(inv.getStoreId(), ""));
            vo.setProductId(inv.getProductId());
            vo.setLotNumber(inv.getLotNumber());
            vo.setQuantity(inv.getQuantity());
            vo.setExpiryDate(inv.getExpiryDate());
            vo.setLocation(inv.getLocation());
            vo.setLastCountDate(inv.getLastCountDate());
            vo.setRemarks(inv.getRemarks());
            vo.setCreateTime(inv.getCreateTime());
            vo.setUpdateTime(inv.getUpdateTime());

            Product product = productMap.get(inv.getProductId());
            if (product != null) {
                vo.setProductName(product.getProductName());
                vo.setProductCode(product.getProductCode());
                vo.setMinStock(product.getReorderPoint());
                vo.setMaxStock(product.getMaxStock());
            } else {
                vo.setProductName("");
                vo.setProductCode("");
                vo.setMinStock(0);
                vo.setMaxStock(0);
            }

            calculateStatus(vo, today);

            return vo;
        }).collect(Collectors.toList());
    }

    private void calculateStatus(InventoryPageVO vo, LocalDate today) {
        Integer quantity = vo.getQuantity() != null ? vo.getQuantity() : 0;
        Integer minStock = vo.getMinStock() != null ? vo.getMinStock() : 0;
        Integer maxStock = vo.getMaxStock() != null ? vo.getMaxStock() : 0;
        LocalDate expiryDate = vo.getExpiryDate();

        boolean expired = expiryDate != null && !expiryDate.isAfter(today);
        boolean expirySoon = expiryDate != null && !expired && ChronoUnit.DAYS.between(today, expiryDate) <= 7;
        vo.setDaysUntilExpiry(expiryDate != null ? (int) ChronoUnit.DAYS.between(today, expiryDate) : null);
        vo.setHasExpiredLot(expired);

        String status;
        if (expired) {
            status = "EXPIRED";
        } else if (expirySoon) {
            status = "EXPIRY_SOON";
        } else if (quantity <= 0) {
            status = "LOW_STOCK";
        } else if (quantity <= minStock) {
            status = "LOW_STOCK";
        } else if (maxStock > 0 && quantity > maxStock) {
            status = "HIGH_STOCK";
        } else {
            status = "NORMAL";
        }
        vo.setStatus(status);

        switch (status) {
            case "EXPIRED" -> {
                vo.setStatusLabel("期限切れ");
                vo.setStatusColor("#F56C6C");
            }
            case "EXPIRY_SOON" -> {
                vo.setStatusLabel("期限接近");
                vo.setStatusColor("#E6A23C");
            }
            case "LOW_STOCK" -> {
                vo.setStatusLabel("在庫切れ");
                vo.setStatusColor("#F56C6C");
            }
            case "HIGH_STOCK" -> {
                vo.setStatusLabel("在庫過多");
                vo.setStatusColor("#E6A23C");
            }
            default -> {
                vo.setStatusLabel("正常");
                vo.setStatusColor("#67C23A");
            }
        }
    }
}
