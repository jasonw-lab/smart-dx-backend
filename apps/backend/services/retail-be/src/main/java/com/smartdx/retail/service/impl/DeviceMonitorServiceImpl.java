package com.smartdx.retail.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartdx.retail.mapper.AlertMapper;
import com.smartdx.retail.mapper.DeviceMapper;
import com.smartdx.retail.mapper.StoreMapper;
import com.smartdx.retail.model.entity.Alert;
import com.smartdx.retail.model.entity.Device;
import com.smartdx.retail.model.entity.Store;
import com.smartdx.retail.service.DeviceMonitorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

/**
 * デバイス監視サービス実装クラス
 * 沈黙監視（Silent Monitoring）のためのデバイス状態チェック機能を提供
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DeviceMonitorServiceImpl implements DeviceMonitorService {

    private final DeviceMapper deviceMapper;
    private final AlertMapper alertMapper;
    private final StoreMapper storeMapper;
    private final ObjectMapper objectMapper;

    @Value("${DEMO_MODE:false}")
    private String demoMode;

    /**
     * Heartbeat 未受信の閾値（分）
     */
    private static final int HEARTBEAT_THRESHOLD_MINUTES = 5;

    @Override
    @Transactional
    public int detectCommunicationDownAlerts() {
        LocalDateTime threshold = LocalDateTime.now().minusMinutes(HEARTBEAT_THRESHOLD_MINUTES);
        List<Device> staleDevices = deviceMapper.findStaleDevices(threshold);

        int count = 0;
        Map<Long, String> storeNameMap = getStoreNameMap(staleDevices);

        for (Device device : staleDevices) {
            // 重複アラート抑制: 未解決アラートが存在する場合はスキップ
            if (alertMapper.existsUnresolvedDeviceAlert(device.getStoreId(), device.getId(), "COMMUNICATION_DOWN")) {
                log.debug("Skipping COMMUNICATION_DOWN alert for device {} - unresolved alert exists", device.getDeviceCode());
                continue;
            }

            Alert alert = createDeviceAlert(
                    device,
                    "COMMUNICATION_DOWN",
                    "P1",
                    String.format("[通信断] 店舗: %s, デバイス: %s (%s), 最終通信: %s",
                            storeNameMap.getOrDefault(device.getStoreId(), "不明"),
                            device.getDeviceName(),
                            device.getDeviceCode(),
                            device.getLastHeartbeat() != null ? device.getLastHeartbeat().toString() : "なし"),
                    String.valueOf(HEARTBEAT_THRESHOLD_MINUTES) + "分",
                    device.getLastHeartbeat() != null ?
                            String.valueOf(java.time.Duration.between(device.getLastHeartbeat(), LocalDateTime.now()).toMinutes()) + "分経過" :
                            "未受信"
            );

            alertMapper.insert(alert);
            count++;
            log.info("COMMUNICATION_DOWN alert created for device: {}", device.getDeviceCode());
        }

        log.info("COMMUNICATION_DOWN alerts detected: {}", count);
        return count;
    }

    @Override
    @Transactional
    public int detectPaymentTerminalDownAlerts() {
        List<Device> paymentTerminals = deviceMapper.findByDeviceType("PAYMENT_TERMINAL");

        int count = 0;
        Map<Long, String> storeNameMap = getStoreNameMap(paymentTerminals);

        for (Device device : paymentTerminals) {
            String status = device.getStatus();
            if (!"OFFLINE".equals(status) && !"ERROR".equals(status)) {
                continue;
            }

            // 重複アラート抑制
            if (alertMapper.existsUnresolvedDeviceAlert(device.getStoreId(), device.getId(), "PAYMENT_TERMINAL_DOWN")) {
                log.debug("Skipping PAYMENT_TERMINAL_DOWN alert for device {} - unresolved alert exists", device.getDeviceCode());
                continue;
            }

            Alert alert = createDeviceAlert(
                    device,
                    "PAYMENT_TERMINAL_DOWN",
                    "P1",
                    String.format("[決済端末停止] 店舗: %s, デバイス: %s (%s), ステータス: %s",
                            storeNameMap.getOrDefault(device.getStoreId(), "不明"),
                            device.getDeviceName(),
                            device.getDeviceCode(),
                            status),
                    "ONLINE",
                    status
            );

            alertMapper.insert(alert);
            count++;
            log.info("PAYMENT_TERMINAL_DOWN alert created for device: {}", device.getDeviceCode());
        }

        log.info("PAYMENT_TERMINAL_DOWN alerts detected: {}", count);
        return count;
    }

    @Override
    @Transactional
    public int detectCardReaderErrorAlerts() {
        List<Device> paymentTerminals = deviceMapper.findByDeviceType("PAYMENT_TERMINAL");

        int count = 0;
        Map<Long, String> storeNameMap = getStoreNameMap(paymentTerminals);

        for (Device device : paymentTerminals) {
            Boolean cardReaderConnected = parseCardReaderConnected(device.getMetadata());

            // cardReaderConnected が false の場合のみアラート
            if (cardReaderConnected == null || cardReaderConnected) {
                continue;
            }

            // 重複アラート抑制
            if (alertMapper.existsUnresolvedDeviceAlert(device.getStoreId(), device.getId(), "CARD_READER_ERROR")) {
                log.debug("Skipping CARD_READER_ERROR alert for device {} - unresolved alert exists", device.getDeviceCode());
                continue;
            }

            Alert alert = createDeviceAlert(
                    device,
                    "CARD_READER_ERROR",
                    "P1",
                    String.format("[カードリーダー異常] 店舗: %s, デバイス: %s (%s), カードリーダー接続: 切断",
                            storeNameMap.getOrDefault(device.getStoreId(), "不明"),
                            device.getDeviceName(),
                            device.getDeviceCode()),
                    "接続",
                    "切断"
            );

            alertMapper.insert(alert);
            count++;
            log.info("CARD_READER_ERROR alert created for device: {}", device.getDeviceCode());
        }

        log.info("CARD_READER_ERROR alerts detected: {}", count);
        return count;
    }

    @Override
    @Transactional
    public int detectPrinterPaperEmptyAlerts() {
        List<Device> printers = deviceMapper.findByDeviceType("PRINTER");

        int count = 0;
        Map<Long, String> storeNameMap = getStoreNameMap(printers);

        for (Device device : printers) {
            String paperLevel = parsePrinterPaperLevel(device.getMetadata());

            // paperLevel が EMPTY の場合のみアラート
            if (!"EMPTY".equals(paperLevel)) {
                continue;
            }

            // 重複アラート抑制
            if (alertMapper.existsUnresolvedDeviceAlert(device.getStoreId(), device.getId(), "PRINTER_PAPER_EMPTY")) {
                log.debug("Skipping PRINTER_PAPER_EMPTY alert for device {} - unresolved alert exists", device.getDeviceCode());
                continue;
            }

            Alert alert = createDeviceAlert(
                    device,
                    "PRINTER_PAPER_EMPTY",
                    "P3",
                    String.format("[プリンター用紙切れ] 店舗: %s, デバイス: %s (%s), 用紙残量: なし",
                            storeNameMap.getOrDefault(device.getStoreId(), "不明"),
                            device.getDeviceName(),
                            device.getDeviceCode()),
                    "OK/LOW",
                    "EMPTY"
            );

            alertMapper.insert(alert);
            count++;
            log.info("PRINTER_PAPER_EMPTY alert created for device: {}", device.getDeviceCode());
        }

        log.info("PRINTER_PAPER_EMPTY alerts detected: {}", count);
        return count;
    }

    @Override
    public int runAllDeviceMonitoring() {
        if (isDemoModeEnabled()) {
            return createDemoModeAlerts();
        }

        log.info("Starting device monitoring...");
        int totalAlerts = 0;

        totalAlerts += detectCommunicationDownAlerts();
        totalAlerts += detectPaymentTerminalDownAlerts();
        totalAlerts += detectCardReaderErrorAlerts();
        totalAlerts += detectPrinterPaperEmptyAlerts();

        log.info("Device monitoring completed. Total alerts generated: {}", totalAlerts);
        return totalAlerts;
    }

    private boolean isDemoModeEnabled() {
        String value = demoMode == null ? "" : demoMode.trim();
        return "true".equalsIgnoreCase(value)
                || "on".equalsIgnoreCase(value)
                || "yes".equalsIgnoreCase(value)
                || "1".equals(value);
    }

    @Transactional
    protected int createDemoModeAlerts() {
        log.info("DEMO_MODE is enabled. Creating 2 demo alerts for device monitoring...");

        List<Device> allDevices = deviceMapper.selectList(null);
        if (allDevices.isEmpty()) {
            log.warn("DEMO_MODE: No devices found. Skipping demo alert generation.");
            return 0;
        }

        Map<Long, List<Device>> devicesByStore = allDevices.stream()
                .filter(d -> d.getStoreId() != null)
                .collect(Collectors.groupingBy(Device::getStoreId));
        if (devicesByStore.isEmpty()) {
            log.warn("DEMO_MODE: No store-linked devices found. Skipping demo alert generation.");
            return 0;
        }

        List<Long> storeIds = new ArrayList<>(devicesByStore.keySet());
        Collections.shuffle(storeIds);

        List<Device> selectedDevices = new ArrayList<>();
        int storesToUse = Math.min(2, storeIds.size());
        for (int i = 0; i < storesToUse; i++) {
            List<Device> storeDevices = devicesByStore.get(storeIds.get(i));
            if (storeDevices == null || storeDevices.isEmpty()) {
                continue;
            }
            selectedDevices.add(storeDevices.get(ThreadLocalRandom.current().nextInt(storeDevices.size())));
        }

        if (selectedDevices.size() < 2) {
            List<Device> shuffled = new ArrayList<>(allDevices);
            Collections.shuffle(shuffled);
            for (Device device : shuffled) {
                boolean alreadySelected = selectedDevices.stream()
                        .anyMatch(d -> d.getId() != null && d.getId().equals(device.getId()));
                if (!alreadySelected) {
                    selectedDevices.add(device);
                }
                if (selectedDevices.size() >= 2) {
                    break;
                }
            }
        }

        if (selectedDevices.isEmpty()) {
            log.warn("DEMO_MODE: Failed to select devices for demo alerts.");
            return 0;
        }
        if (selectedDevices.size() < 2) {
            selectedDevices.add(selectedDevices.get(0));
        }

        Map<Long, String> storeNameMap = getStoreNameMap(selectedDevices);

        int count = 0;
        for (int i = 0; i < 2; i++) {
            Device device = selectedDevices.get(i);
            String storeName = storeNameMap.getOrDefault(device.getStoreId(), "デモ店舗" + (i + 1));
            Alert alert = createRandomDemoAlert(device, storeName);
            alertMapper.insert(alert);
            count++;
            log.info("DEMO_MODE: alert created. storeId={}, deviceId={}, type={}",
                    device.getStoreId(), device.getId(), alert.getAlertType());
        }

        log.info("DEMO_MODE: Device monitoring completed. Total demo alerts generated: {}", count);
        return count;
    }

    private Alert createRandomDemoAlert(Device device, String storeName) {
        List<String> alertTypes = List.of(
                "COMMUNICATION_DOWN",
                "PAYMENT_TERMINAL_DOWN",
                "CARD_READER_ERROR",
                "PRINTER_PAPER_EMPTY"
        );
        String alertType = alertTypes.get(ThreadLocalRandom.current().nextInt(alertTypes.size()));

        String message;
        String priority;
        String thresholdValue;
        String currentValue;

        switch (alertType) {
            case "COMMUNICATION_DOWN" -> {
                long minutes = ThreadLocalRandom.current().nextLong(6, 31);
                message = String.format("[通信断][デモ] 店舗: %s, デバイス: %s (%s), 最終通信: %d分前",
                        storeName, device.getDeviceName(), device.getDeviceCode(), minutes);
                priority = "P1";
                thresholdValue = HEARTBEAT_THRESHOLD_MINUTES + "分";
                currentValue = minutes + "分経過";
            }
            case "PAYMENT_TERMINAL_DOWN" -> {
                String status = ThreadLocalRandom.current().nextBoolean() ? "OFFLINE" : "ERROR";
                message = String.format("[決済端末停止][デモ] 店舗: %s, デバイス: %s (%s), ステータス: %s",
                        storeName, device.getDeviceName(), device.getDeviceCode(), status);
                priority = "P1";
                thresholdValue = "ONLINE";
                currentValue = status;
            }
            case "CARD_READER_ERROR" -> {
                message = String.format("[カードリーダー異常][デモ] 店舗: %s, デバイス: %s (%s), カードリーダー接続: 切断",
                        storeName, device.getDeviceName(), device.getDeviceCode());
                priority = "P1";
                thresholdValue = "接続";
                currentValue = "切断";
            }
            case "PRINTER_PAPER_EMPTY" -> {
                message = String.format("[プリンター用紙切れ][デモ] 店舗: %s, デバイス: %s (%s), 用紙残量: なし",
                        storeName, device.getDeviceName(), device.getDeviceCode());
                priority = "P3";
                thresholdValue = "OK/LOW";
                currentValue = "EMPTY";
            }
            default -> throw new IllegalStateException("Unexpected demo alert type: " + alertType);
        }

        return createDeviceAlert(device, alertType, priority, message, thresholdValue, currentValue);
    }

    /**
     * デバイスアラートを作成
     */
    private Alert createDeviceAlert(Device device, String alertType, String priority,
                                    String message, String thresholdValue, String currentValue) {
        Alert alert = new Alert();
        alert.setStoreId(device.getStoreId());
        alert.setDeviceId(device.getId());
        alert.setAlertType(alertType);
        alert.setPriority(priority);
        alert.setStatus("NEW");
        alert.setMessage(message);
        alert.setThresholdValue(thresholdValue);
        alert.setCurrentValue(currentValue);
        alert.setDetectedAt(LocalDateTime.now());
        return alert;
    }

    /**
     * 店舗名マップを取得
     */
    private Map<Long, String> getStoreNameMap(List<Device> devices) {
        if (devices.isEmpty()) {
            return Map.of();
        }

        List<Long> storeIds = devices.stream()
                .map(Device::getStoreId)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());

        if (storeIds.isEmpty()) {
            return Map.of();
        }

        List<Store> stores = storeMapper.selectBatchIds(storeIds);
        return stores.stream()
                .collect(Collectors.toMap(Store::getId, Store::getStoreName));
    }

    /**
     * metadata JSON から cardReaderConnected を抽出
     */
    private Boolean parseCardReaderConnected(String metadata) {
        if (!StringUtils.hasText(metadata)) {
            return null;
        }

        try {
            JsonNode root = objectMapper.readTree(metadata);

            // deviceStats.paymentTerminal.cardReaderConnected を探す
            JsonNode deviceStats = root.path("deviceStats");
            if (!deviceStats.isMissingNode()) {
                JsonNode paymentTerminal = deviceStats.path("paymentTerminal");
                if (!paymentTerminal.isMissingNode()) {
                    JsonNode cardReaderConnected = paymentTerminal.path("cardReaderConnected");
                    if (!cardReaderConnected.isMissingNode() && cardReaderConnected.isBoolean()) {
                        return cardReaderConnected.asBoolean();
                    }
                }
            }

            // 直接 cardReaderConnected を探す（フラット構造の場合）
            JsonNode cardReaderConnected = root.path("cardReaderConnected");
            if (!cardReaderConnected.isMissingNode() && cardReaderConnected.isBoolean()) {
                return cardReaderConnected.asBoolean();
            }

            return null;
        } catch (Exception e) {
            log.warn("Failed to parse cardReaderConnected from metadata: {}", metadata, e);
            return null;
        }
    }

    /**
     * metadata JSON から printer.paperLevel を抽出
     */
    private String parsePrinterPaperLevel(String metadata) {
        if (!StringUtils.hasText(metadata)) {
            return null;
        }

        try {
            JsonNode root = objectMapper.readTree(metadata);

            // deviceStats.printer.paperLevel を探す
            JsonNode deviceStats = root.path("deviceStats");
            if (!deviceStats.isMissingNode()) {
                JsonNode printer = deviceStats.path("printer");
                if (!printer.isMissingNode()) {
                    JsonNode paperLevel = printer.path("paperLevel");
                    if (!paperLevel.isMissingNode()) {
                        return paperLevel.asText();
                    }
                }
            }

            // 直接 paperLevel を探す（フラット構造の場合）
            JsonNode paperLevel = root.path("paperLevel");
            if (!paperLevel.isMissingNode()) {
                return paperLevel.asText();
            }

            return null;
        } catch (Exception e) {
            log.warn("Failed to parse paperLevel from metadata: {}", metadata, e);
            return null;
        }
    }
}
