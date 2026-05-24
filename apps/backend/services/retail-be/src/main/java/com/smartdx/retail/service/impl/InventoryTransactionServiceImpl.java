package com.smartdx.retail.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.smartdx.core.result.PageResult;
import com.smartdx.retail.converter.InventoryTransactionConverter;
import com.smartdx.retail.mapper.InventoryMapper;
import com.smartdx.retail.mapper.InventoryTransactionMapper;
import com.smartdx.retail.mapper.ProductMapper;
import com.smartdx.retail.mapper.StoreMapper;
import com.smartdx.retail.model.entity.Inventory;
import com.smartdx.retail.model.entity.InventoryTransaction;
import com.smartdx.retail.model.entity.Product;
import com.smartdx.retail.model.entity.Store;
import com.smartdx.retail.model.form.InventoryTransactionForm;
import com.smartdx.retail.model.query.InventoryTransactionPageQuery;
import com.smartdx.retail.model.vo.InventoryTransactionPageVO;
import com.smartdx.retail.service.InventoryTransactionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 入出庫履歴サービス実装クラス
 */
@Service
@RequiredArgsConstructor
public class InventoryTransactionServiceImpl extends ServiceImpl<InventoryTransactionMapper, InventoryTransaction> implements InventoryTransactionService {

    private final InventoryTransactionConverter inventoryTransactionConverter;
    private final ProductMapper productMapper;
    private final StoreMapper storeMapper;
    private final InventoryMapper inventoryMapper;

    @Override
    public PageResult<InventoryTransactionPageVO> getTransactionPage(InventoryTransactionPageQuery queryParams) {
        return getTransactionPageInternal(queryParams, null);
    }

    @Override
    public PageResult<InventoryTransactionPageVO> getInboundTransactionPage(InventoryTransactionPageQuery queryParams) {
        return getTransactionPageInternal(queryParams, "INBOUND");
    }

    @Override
    public PageResult<InventoryTransactionPageVO> getOutboundTransactionPage(InventoryTransactionPageQuery queryParams) {
        return getTransactionPageInternal(queryParams, "SALE");
    }

    /**
     * 入出庫履歴一覧（ページング）取得の内部実装
     *
     * @param queryParams クエリパラメータ
     * @param txnType 操作タイプ（指定しない場合は全て）
     * @return ページング結果
     */
    private PageResult<InventoryTransactionPageVO> getTransactionPageInternal(InventoryTransactionPageQuery queryParams, String txnType) {
        // ページングパラメータ
        Page<InventoryTransaction> page = new Page<>(queryParams.getPageNum(), queryParams.getPageSize());

        // クエリ条件
        LambdaQueryWrapper<InventoryTransaction> queryWrapper = new LambdaQueryWrapper<InventoryTransaction>()
                .eq(queryParams.getStoreId() != null, InventoryTransaction::getStoreId, queryParams.getStoreId())
                .eq(queryParams.getProductId() != null, InventoryTransaction::getProductId, queryParams.getProductId())
                .like(StringUtils.hasText(queryParams.getLotNumber()), InventoryTransaction::getLotNumber, queryParams.getLotNumber())
                .eq(StringUtils.hasText(txnType), InventoryTransaction::getTxnType, txnType)
                .eq(StringUtils.hasText(queryParams.getTxnType()) && txnType == null, InventoryTransaction::getTxnType, queryParams.getTxnType())
                .eq(StringUtils.hasText(queryParams.getSourceType()), InventoryTransaction::getSourceType, queryParams.getSourceType())
                .ge(queryParams.getOccurredAtStart() != null, InventoryTransaction::getOccurredAt, queryParams.getOccurredAtStart())
                .le(queryParams.getOccurredAtEnd() != null, InventoryTransaction::getOccurredAt, queryParams.getOccurredAtEnd())
                .like(StringUtils.hasText(queryParams.getReferenceNo()), InventoryTransaction::getReferenceNo, queryParams.getReferenceNo())
                .orderByDesc(InventoryTransaction::getOccurredAt);

        // クエリ実行
        IPage<InventoryTransaction> result = this.page(page, queryWrapper);

        if (result.getRecords().isEmpty()) {
            return PageResult.success(Collections.emptyList(), result.getTotal());
        }

        // 店舗情報取得
        List<Long> storeIds = result.getRecords().stream()
                .map(InventoryTransaction::getStoreId)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());

        Map<Long, Store> storeMap = storeIds.isEmpty()
                ? Collections.emptyMap()
                : storeMapper.selectBatchIds(storeIds).stream()
                .collect(Collectors.toMap(Store::getId, store -> store));

        // 商品情報取得
        List<Long> productIds = result.getRecords().stream()
                .map(InventoryTransaction::getProductId)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());

        Map<Long, Product> productMap = productIds.isEmpty()
                ? Collections.emptyMap()
                : productMapper.selectBatchIds(productIds).stream()
                .collect(Collectors.toMap(Product::getId, product -> product));

        // 結果変換
        List<InventoryTransactionPageVO> list = result.getRecords().stream()
                .map(transaction -> {
                    InventoryTransactionPageVO vo = inventoryTransactionConverter.entity2Vo(transaction);
                    Store store = storeMap.get(transaction.getStoreId());
                    if (store != null) {
                        vo.setStoreName(store.getStoreName());
                    }
                    Product product = productMap.get(transaction.getProductId());
                    if (product != null) {
                        vo.setProductName(product.getProductName());
                        vo.setProductCode(product.getProductCode());
                    }
                    return vo;
                })
                .collect(Collectors.toList());

        return PageResult.success(list, result.getTotal());
    }

    @Override
    public List<InventoryTransaction> listTransactions() {
        // 全入出庫履歴取得
        LambdaQueryWrapper<InventoryTransaction> queryWrapper = new LambdaQueryWrapper<InventoryTransaction>()
                .orderByDesc(InventoryTransaction::getOccurredAt);
        return this.list(queryWrapper);
    }

    @Override
    public InventoryTransaction getTransactionById(Long id) {
        return this.getById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean createTransaction(InventoryTransactionForm form) {
        InventoryTransaction transaction = inventoryTransactionConverter.form2Entity(form);
        if (transaction.getOccurredAt() == null) {
            transaction.setOccurredAt(LocalDateTime.now());
        }
        if (transaction.getSourceType() == null) {
            transaction.setSourceType("MANUAL");
        }
        return this.save(transaction);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean createInboundTransaction(InventoryTransactionForm form) {
        form.setTxnType("INBOUND");
        Long inventoryId = upsertInventoryForInbound(form);
        form.setInventoryId(inventoryId);
        return createTransaction(form);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean createOutboundTransaction(InventoryTransactionForm form) {
        form.setTxnType("SALE");
        applyInventoryForOutbound(form);
        return createTransaction(form);
    }

    @Override
    public boolean updateTransaction(Long id, InventoryTransactionForm form) {
        InventoryTransaction transaction = inventoryTransactionConverter.form2Entity(form);
        transaction.setId(id);
        return this.updateById(transaction);
    }

    @Override
    public boolean deleteTransaction(Long id) {
        return this.removeById(id);
    }

    private Long upsertInventoryForInbound(InventoryTransactionForm form) {
        Inventory existing = inventoryMapper.selectOne(new LambdaQueryWrapper<Inventory>()
                .eq(Inventory::getStoreId, form.getStoreId())
                .eq(Inventory::getProductId, form.getProductId())
                .eq(Inventory::getLotNumber, form.getLotNumber()));

        if (existing == null) {
            Inventory inventory = new Inventory();
            inventory.setStoreId(form.getStoreId());
            inventory.setProductId(form.getProductId());
            inventory.setLotNumber(form.getLotNumber());
            inventory.setQuantity(form.getQuantityDelta());
            inventory.setReceivedAt(LocalDateTime.now());
            inventory.setStatus("normal");
            inventoryMapper.insert(inventory);
            return inventory.getId();
        }

        existing.setQuantity((existing.getQuantity() == null ? 0 : existing.getQuantity()) + form.getQuantityDelta());
        inventoryMapper.updateById(existing);
        return existing.getId();
    }

    private void applyInventoryForOutbound(InventoryTransactionForm form) {
        Inventory existing = inventoryMapper.selectOne(new LambdaQueryWrapper<Inventory>()
                .eq(Inventory::getStoreId, form.getStoreId())
                .eq(Inventory::getProductId, form.getProductId())
                .eq(Inventory::getLotNumber, form.getLotNumber()));

        if (existing == null) {
            throw new IllegalArgumentException("指定ロットの在庫が存在しません");
        }

        form.setInventoryId(existing.getId());

        int currentQty = existing.getQuantity() == null ? 0 : existing.getQuantity();
        int outboundQty = Math.abs(form.getQuantityDelta());
        if (currentQty < outboundQty) {
            throw new IllegalArgumentException("在庫が不足しています（current=" + currentQty + "）");
        }

        existing.setQuantity(currentQty - outboundQty);
        inventoryMapper.updateById(existing);
    }
}
