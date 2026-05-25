package com.smartdx.retail.service.impl;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartdx.retail.mapper.DeviceMapper;
import com.smartdx.retail.mapper.StoreMapper;
import com.smartdx.retail.model.entity.Device;
import com.smartdx.retail.model.entity.Store;
import com.smartdx.retail.model.form.DeviceStatsPayload;
import com.smartdx.retail.model.form.HeartbeatPayload;
import com.smartdx.retail.service.HeartbeatService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Heartbeatサービス実装クラス
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class HeartbeatServiceImpl implements HeartbeatService {

    private final DeviceMapper deviceMapper;
    private final StoreMapper storeMapper;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public boolean receiveHeartbeat(HeartbeatPayload payload) {
        log.info("Heartbeat received from store: {}", payload.getStoreId());

        // 店舗存在確認
        Store store = storeMapper.selectById(payload.getStoreId());
        if (store == null) {
            log.warn("Store not found: {}", payload.getStoreId());
            return false;
        }

        LocalDateTime heartbeatTime = payload.getTimestamp() != null
                ? payload.getTimestamp()
                : LocalDateTime.now();

        // デバイス状態の更新
        DeviceStatsPayload deviceStats = payload.getDeviceStats();
        if (deviceStats != null) {
            // 決済端末の更新
            if (deviceStats.getPaymentTerminal() != null) {
                updateOrCreateDevice(
                        payload.getStoreId(),
                        "PAYMENT_TERMINAL",
                        store.getStoreName() + " 決済端末",
                        deviceStats.getPaymentTerminal().getStatus(),
                        deviceStats.getPaymentTerminal().getErrorCode(),
                        heartbeatTime,
                        buildPaymentTerminalMetadata(deviceStats.getPaymentTerminal())
                );
            }

            // プリンターの更新
            if (deviceStats.getPrinter() != null) {
                updateOrCreateDevice(
                        payload.getStoreId(),
                        "PRINTER",
                        store.getStoreName() + " プリンター",
                        deviceStats.getPrinter().getStatus(),
                        null,
                        heartbeatTime,
                        buildPrinterMetadata(deviceStats.getPrinter())
                );
            }

            // ネットワークルーターの更新
            if (deviceStats.getNetwork() != null) {
                String networkStatus = determineNetworkStatus(deviceStats.getNetwork());
                updateOrCreateDevice(
                        payload.getStoreId(),
                        "NETWORK_ROUTER",
                        store.getStoreName() + " ネットワーク",
                        networkStatus,
                        null,
                        heartbeatTime,
                        buildNetworkMetadata(deviceStats.getNetwork())
                );
            }
        }

        log.info("Heartbeat processed successfully for store: {}", payload.getStoreId());
        return true;
    }

    /**
     * デバイスを更新または作成する
     */
    private void updateOrCreateDevice(Long storeId, String deviceType, String deviceName,
                                      String status, String errorCode, LocalDateTime heartbeatTime,
                                      String metadata) {
        Device existingDevice = deviceMapper.findByStoreIdAndDeviceType(storeId, deviceType);

        if (existingDevice != null) {
            // 既存デバイスの更新
            LambdaUpdateWrapper<Device> updateWrapper = new LambdaUpdateWrapper<>();
            updateWrapper.eq(Device::getId, existingDevice.getId())
                    .set(Device::getStatus, status != null ? status : "ONLINE")
                    .set(Device::getLastHeartbeat, heartbeatTime)
                    .set(Device::getErrorCode, errorCode)
                    .set(Device::getMetadata, metadata);
            deviceMapper.update(null, updateWrapper);
            log.debug("Device updated: storeId={}, type={}", storeId, deviceType);
        } else {
            // 新規デバイスの作成
            Device newDevice = new Device();
            newDevice.setStoreId(storeId);
            newDevice.setDeviceCode(generateDeviceCode(storeId, deviceType));
            newDevice.setDeviceType(deviceType);
            newDevice.setDeviceName(deviceName);
            newDevice.setStatus(status != null ? status : "ONLINE");
            newDevice.setLastHeartbeat(heartbeatTime);
            newDevice.setErrorCode(errorCode);
            newDevice.setMetadata(metadata);
            deviceMapper.insert(newDevice);
            log.debug("Device created: storeId={}, type={}", storeId, deviceType);
        }
    }

    /**
     * デバイスコードを生成する
     */
    private String generateDeviceCode(Long storeId, String deviceType) {
        String typePrefix = switch (deviceType) {
            case "PAYMENT_TERMINAL" -> "POS";
            case "PRINTER" -> "PTR";
            case "NETWORK_ROUTER" -> "NET";
            case "CAMERA" -> "CAM";
            case "GATE" -> "GAT";
            case "REFRIGERATOR_SENSOR" -> "REF";
            default -> "DEV";
        };
        return String.format("DEV-%d-%s-01", storeId, typePrefix);
    }

    /**
     * 決済端末のメタデータを構築する
     */
    private String buildPaymentTerminalMetadata(DeviceStatsPayload.PaymentTerminalStats stats) {
        try {
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("cardReaderConnected", stats.getCardReaderConnected());
            metadata.put("lastSelfTestAt", stats.getLastSelfTestAt());
            return objectMapper.writeValueAsString(metadata);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize payment terminal metadata", e);
            return null;
        }
    }

    /**
     * プリンターのメタデータを構築する
     */
    private String buildPrinterMetadata(DeviceStatsPayload.PrinterStats stats) {
        try {
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("paperLevel", stats.getPaperLevel());
            return objectMapper.writeValueAsString(metadata);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize printer metadata", e);
            return null;
        }
    }

    /**
     * ネットワークのメタデータを構築する
     */
    private String buildNetworkMetadata(DeviceStatsPayload.NetworkStats stats) {
        try {
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("latencyMs", stats.getLatencyMs());
            metadata.put("signalStrength", stats.getSignalStrength());
            return objectMapper.writeValueAsString(metadata);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize network metadata", e);
            return null;
        }
    }

    /**
     * ネットワーク状態を判定する
     */
    private String determineNetworkStatus(DeviceStatsPayload.NetworkStats stats) {
        if (stats.getLatencyMs() != null && stats.getLatencyMs() > 500) {
            return "ERROR";
        }
        if ("POOR".equals(stats.getSignalStrength())) {
            return "ERROR";
        }
        return "ONLINE";
    }
}
