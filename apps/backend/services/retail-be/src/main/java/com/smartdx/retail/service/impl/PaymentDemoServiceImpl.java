package com.smartdx.retail.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.smartdx.retail.mapper.InventoryMapper;
import com.smartdx.retail.mapper.ProductMapper;
import com.smartdx.retail.mapper.StoreMapper;
import com.smartdx.retail.model.entity.Inventory;
import com.smartdx.retail.model.entity.Product;
import com.smartdx.retail.model.entity.Store;
import com.smartdx.retail.model.form.PaymentItemPayload;
import com.smartdx.retail.model.form.PaymentPayload;
import com.smartdx.retail.service.PaymentDemoService;
import com.smartdx.retail.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 決済デモサービス実装
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentDemoServiceImpl implements PaymentDemoService {

    private final PaymentService paymentService;
    private final StoreMapper storeMapper;
    private final InventoryMapper inventoryMapper;
    private final ProductMapper productMapper;
    private final Random random = new Random();

    @Value("${DEMO_MODE:false}")
    private String demoMode;

    @Override
    @Transactional
    public int runDemoPayments() {
        if (!isDemoModeEnabled()) {
            return 0;
        }

        List<Store> targetStores = resolveTargetStores();
        if (targetStores.isEmpty()) {
            log.warn("DEMO_MODE(payment): target stores not found. Skipping demo payment generation.");
            return 0;
        }

        int transactionCount = calculateTransactionCount();
        int successCount = 0;

        for (int i = 0; i < transactionCount; i++) {
            Store store = targetStores.get(random.nextInt(targetStores.size()));
            PaymentPayload payload = createPayload(store.getId());
            if (payload == null) {
                continue;
            }

            try {
                paymentService.receivePayment(payload);
                successCount++;
            } catch (Exception e) {
                log.error("DEMO_MODE(payment): failed to process payment for storeId={}", store.getId(), e);
            }
        }

        int resetCount = resetInventoryToHundredWhenDepleted(
                targetStores.stream().map(Store::getId).toList()
        );

        log.info("DEMO_MODE(payment): completed. transactions={}, success={}, inventoryReset={}",
                transactionCount, successCount, resetCount);
        return successCount;
    }

    private boolean isDemoModeEnabled() {
        String value = demoMode == null ? "" : demoMode.trim();
        return "true".equalsIgnoreCase(value)
                || "on".equalsIgnoreCase(value)
                || "yes".equalsIgnoreCase(value)
                || "1".equals(value);
    }

    private List<Store> resolveTargetStores() {
        List<Store> allStores = storeMapper.selectList(null);
        if (allStores.isEmpty()) {
            return List.of();
        }

        List<Store> lanternStores = allStores.stream()
                .filter(s -> s.getStoreName() != null && s.getStoreName().contains("ランタン"))
                .sorted(Comparator.comparing(Store::getId))
                .limit(2)
                .toList();
        if (lanternStores.size() == 2) {
            return lanternStores;
        }

        List<Store> onlineStores = allStores.stream()
                .filter(s -> "ONLINE".equalsIgnoreCase(s.getStatus()))
                .sorted(Comparator.comparing(Store::getId))
                .limit(2)
                .toList();
        if (onlineStores.size() == 2) {
            return onlineStores;
        }

        return allStores.stream()
                .sorted(Comparator.comparing(Store::getId))
                .limit(2)
                .toList();
    }

    private int calculateTransactionCount() {
        int hour = LocalDateTime.now().getHour();
        if (hour >= 11 && hour < 14) {
            return 2 + random.nextInt(4);
        } else if (hour >= 17 && hour < 20) {
            return 2 + random.nextInt(3);
        } else if (hour >= 22 || hour < 8) {
            return random.nextInt(2);
        } else {
            return 1 + random.nextInt(2);
        }
    }

    private PaymentPayload createPayload(Long storeId) {
        List<Inventory> availableInventories = inventoryMapper.selectList(
                new LambdaQueryWrapper<Inventory>()
                        .eq(Inventory::getStoreId, storeId)
                        .gt(Inventory::getQuantity, 0)
                        .orderByDesc(Inventory::getQuantity)
        );
        if (availableInventories.isEmpty()) {
            return null;
        }

        List<Long> productIds = availableInventories.stream()
                .map(Inventory::getProductId)
                .distinct()
                .toList();
        if (productIds.isEmpty()) {
            return null;
        }

        List<Product> products = productMapper.selectBatchIds(productIds);
        Map<Long, Product> productMap = products.stream()
                .collect(Collectors.toMap(Product::getId, p -> p));

        int itemCount = Math.min(1 + random.nextInt(3), productIds.size());
        List<Long> shuffledProductIds = new ArrayList<>(productIds);
        Collections.shuffle(shuffledProductIds);

        List<PaymentItemPayload> items = new ArrayList<>();
        for (int i = 0; i < itemCount; i++) {
            Long productId = shuffledProductIds.get(i);
            Product product = productMap.get(productId);
            if (product == null || product.getUnitPrice() == null || product.getUnitPrice().compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }

            PaymentItemPayload item = new PaymentItemPayload();
            item.setProductId(productId);
            item.setQuantity(1 + random.nextInt(2));
            item.setUnitPrice(product.getUnitPrice());
            items.add(item);
        }

        if (items.isEmpty()) {
            return null;
        }

        PaymentPayload payload = new PaymentPayload();
        payload.setStoreId(storeId);
        payload.setSaleTimestamp(LocalDateTime.now());
        payload.setPaymentMethod(randomPaymentMethod());
        payload.setPaymentProvider(randomPaymentProvider(payload.getPaymentMethod()));
        payload.setPaymentReferenceId("DEMO-" + System.currentTimeMillis() + "-" + random.nextInt(1000));
        payload.setItems(items);

        BigDecimal total = items.stream()
                .map(it -> it.getUnitPrice().multiply(BigDecimal.valueOf(it.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        payload.setTotalAmount(total);
        return payload;
    }

    private String randomPaymentMethod() {
        String[] methods = {"CASH", "CARD", "QR"};
        return methods[random.nextInt(methods.length)];
    }

    private String randomPaymentProvider(String paymentMethod) {
        if ("CARD".equals(paymentMethod)) {
            return random.nextBoolean() ? "VISA" : "Mastercard";
        }
        if ("QR".equals(paymentMethod)) {
            String[] providers = {"PayPay", "LINE Pay", "楽天ペイ"};
            return providers[random.nextInt(providers.length)];
        }
        return null;
    }

    private int resetInventoryToHundredWhenDepleted(List<Long> storeIds) {
        if (storeIds.isEmpty()) {
            return 0;
        }

        int updated = 0;
        for (Long storeId : storeIds) {
            updated += inventoryMapper.update(
                    null,
                    new LambdaUpdateWrapper<Inventory>()
                            .eq(Inventory::getStoreId, storeId)
                            .le(Inventory::getQuantity, 0)
                            .set(Inventory::getQuantity, 100)
                            .set(Inventory::getStatus, "normal")
            );
        }
        return updated;
    }
}
