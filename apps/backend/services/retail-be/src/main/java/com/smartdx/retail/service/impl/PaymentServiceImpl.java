package com.smartdx.retail.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.smartdx.retail.mapper.*;
import com.smartdx.retail.model.entity.*;
import com.smartdx.retail.model.form.PaymentItemPayload;
import com.smartdx.retail.model.form.PaymentPayload;
import com.smartdx.retail.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

/**
 * 決済サービス実装クラス
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {

    private final SalesMapper salesMapper;
    private final SalesDetailMapper salesDetailMapper;
    private final InventoryMapper inventoryMapper;
    private final InventoryTransactionMapper inventoryTransactionMapper;
    private final StoreMapper storeMapper;
    private final ProductMapper productMapper;
    private final DeviceMapper deviceMapper;

    @Override
    @Transactional
    public Long receivePayment(PaymentPayload payload) {
        log.info("Payment received from store: {}, orderNumber: {}",
                payload.getStoreId(), payload.getOrderNumber());

        // 店舗存在確認
        Store store = storeMapper.selectById(payload.getStoreId());
        if (store == null) {
            throw new IllegalArgumentException("Store not found: " + payload.getStoreId());
        }

        // 注文番号生成（未指定の場合）
        String orderNumber = payload.getOrderNumber();
        if (orderNumber == null || orderNumber.isBlank()) {
            orderNumber = generateOrderNumber(payload.getStoreId());
        }

        // 売上ヘッダ作成
        Sales sales = new Sales();
        sales.setStoreId(payload.getStoreId());
        sales.setOrderNumber(orderNumber);
        sales.setTotalAmount(payload.getTotalAmount());
        sales.setPaymentMethod(payload.getPaymentMethod());
        sales.setPaymentProvider(payload.getPaymentProvider());
        sales.setPaymentReferenceId(payload.getPaymentReferenceId());
        sales.setSaleTimestamp(payload.getSaleTimestamp() != null
                ? payload.getSaleTimestamp()
                : LocalDateTime.now());
        salesMapper.insert(sales);

        Long salesId = sales.getId();
        log.debug("Sales header created: id={}, orderNumber={}", salesId, orderNumber);

        // 売上明細作成 & 在庫減算（FEFO）
        for (PaymentItemPayload item : payload.getItems()) {
            processPaymentItem(salesId, payload.getStoreId(), item, payload.getSaleTimestamp());
        }

        // 決済端末のHeartbeat更新（決済成功時）
        updatePaymentTerminalHeartbeat(payload.getStoreId());

        log.info("Payment processed successfully: salesId={}, orderNumber={}", salesId, orderNumber);
        return salesId;
    }

    /**
     * 決済明細を処理し、在庫を減算する（FEFO方式）
     */
    private void processPaymentItem(Long salesId, Long storeId, PaymentItemPayload item,
                                    LocalDateTime saleTimestamp) {
        // 商品存在確認
        Product product = productMapper.selectById(item.getProductId());
        if (product == null) {
            throw new IllegalArgumentException("Product not found: " + item.getProductId());
        }

        // FEFO方式で在庫を取得（賞味期限の近い順）
        LambdaQueryWrapper<Inventory> inventoryQuery = new LambdaQueryWrapper<>();
        inventoryQuery.eq(Inventory::getStoreId, storeId)
                .eq(Inventory::getProductId, item.getProductId())
                .gt(Inventory::getQuantity, 0)
                .orderByAsc(Inventory::getExpiryDate)
                .orderByAsc(Inventory::getCreateTime);
        List<Inventory> inventories = inventoryMapper.selectList(inventoryQuery);

        int remainingQuantity = item.getQuantity();

        for (Inventory inventory : inventories) {
            if (remainingQuantity <= 0) {
                break;
            }

            int deductQuantity = Math.min(inventory.getQuantity(), remainingQuantity);

            // 在庫減算
            LambdaUpdateWrapper<Inventory> updateWrapper = new LambdaUpdateWrapper<>();
            updateWrapper.eq(Inventory::getId, inventory.getId())
                    .set(Inventory::getQuantity, inventory.getQuantity() - deductQuantity);
            inventoryMapper.update(null, updateWrapper);

            // 売上明細作成
            SalesDetail detail = new SalesDetail();
            detail.setSalesId(salesId);
            detail.setProductId(item.getProductId());
            detail.setLotNumber(inventory.getLotNumber());
            detail.setQuantity(deductQuantity);
            detail.setUnitPrice(item.getUnitPrice());
            detail.setSubtotal(item.getUnitPrice().multiply(BigDecimal.valueOf(deductQuantity)));
            salesDetailMapper.insert(detail);

            // 入出庫履歴作成
            InventoryTransaction transaction = new InventoryTransaction();
            transaction.setInventoryId(inventory.getId());
            transaction.setStoreId(storeId);
            transaction.setProductId(item.getProductId());
            transaction.setLotNumber(inventory.getLotNumber());
            transaction.setTxnType("SALE");
            transaction.setQuantityDelta(-deductQuantity);
            transaction.setSourceType("POS");
            transaction.setOccurredAt(saleTimestamp != null ? saleTimestamp : LocalDateTime.now());
            transaction.setReferenceNo(String.valueOf(salesId));
            transaction.setNote("販売");
            inventoryTransactionMapper.insert(transaction);

            log.debug("Inventory deducted: productId={}, lotNumber={}, quantity={}",
                    item.getProductId(), inventory.getLotNumber(), deductQuantity);

            remainingQuantity -= deductQuantity;

            // 在庫状態更新
            updateInventoryStatus(inventory.getId());
        }

        // 在庫不足の場合でも処理続行（売上は記録、在庫がない分は0として記録）
        if (remainingQuantity > 0) {
            log.warn("Insufficient inventory: productId={}, requested={}, shortage={}",
                    item.getProductId(), item.getQuantity(), remainingQuantity);

            // 在庫不足分も売上明細に記録（ロット番号なし）
            SalesDetail detail = new SalesDetail();
            detail.setSalesId(salesId);
            detail.setProductId(item.getProductId());
            detail.setLotNumber("N/A");
            detail.setQuantity(remainingQuantity);
            detail.setUnitPrice(item.getUnitPrice());
            detail.setSubtotal(item.getUnitPrice().multiply(BigDecimal.valueOf(remainingQuantity)));
            salesDetailMapper.insert(detail);
        }
    }

    /**
     * 在庫状態を更新する
     */
    private void updateInventoryStatus(Long inventoryId) {
        Inventory inventory = inventoryMapper.selectById(inventoryId);
        if (inventory == null) {
            return;
        }

        String newStatus;
        if (inventory.getQuantity() <= 0) {
            newStatus = "out_of_stock";
        } else {
            newStatus = "normal";
        }

        LambdaUpdateWrapper<Inventory> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(Inventory::getId, inventoryId)
                .set(Inventory::getStatus, newStatus);
        inventoryMapper.update(null, updateWrapper);
    }

    /**
     * 決済端末のHeartbeatを更新する
     */
    private void updatePaymentTerminalHeartbeat(Long storeId) {
        Device device = deviceMapper.findByStoreIdAndDeviceType(storeId, "PAYMENT_TERMINAL");
        if (device != null) {
            LambdaUpdateWrapper<Device> updateWrapper = new LambdaUpdateWrapper<>();
            updateWrapper.eq(Device::getId, device.getId())
                    .set(Device::getLastHeartbeat, LocalDateTime.now())
                    .set(Device::getStatus, "ONLINE");
            deviceMapper.update(null, updateWrapper);
        }
    }

    /**
     * 注文番号を生成する
     */
    private String generateOrderNumber(Long storeId) {
        String dateStr = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String uuid = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        return String.format("ORD-%s-%d-%s", dateStr, storeId, uuid);
    }
}
