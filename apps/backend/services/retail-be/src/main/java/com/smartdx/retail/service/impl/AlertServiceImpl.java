package com.smartdx.retail.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.smartdx.retail.mapper.AlertMapper;
import com.smartdx.retail.mapper.DeviceMapper;
import com.smartdx.retail.mapper.ProductMapper;
import com.smartdx.retail.mapper.StoreMapper;
import com.smartdx.retail.model.entity.Alert;
import com.smartdx.retail.model.entity.Device;
import com.smartdx.retail.model.entity.Product;
import com.smartdx.retail.model.entity.Store;
import com.smartdx.retail.model.vo.AlertPageVO;
import com.smartdx.retail.service.AlertService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Alert Service Implementation
 */
@Service
@RequiredArgsConstructor
public class AlertServiceImpl extends ServiceImpl<AlertMapper, Alert> implements AlertService {

    private final StoreMapper storeMapper;
    private final ProductMapper productMapper;
    private final DeviceMapper deviceMapper;

    @Override
    public List<AlertPageVO> listAlerts(Long storeId, String status) {
        LambdaQueryWrapper<Alert> queryWrapper = new LambdaQueryWrapper<Alert>()
                .eq(storeId != null, Alert::getStoreId, storeId)
                .eq(StringUtils.hasText(status), Alert::getStatus, status)
                .orderByDesc(Alert::getDetectedAt);
        List<Alert> alerts = this.list(queryWrapper);
        return toPageVO(alerts);
    }

    @Override
    public List<AlertPageVO> listTodayAlerts() {
        LocalDateTime todayStart = LocalDate.now().atStartOfDay();
        QueryWrapper<Alert> queryWrapper = new QueryWrapper<Alert>()
                .notIn("status", "RESOLVED", "CLOSED")
                .ge("detected_at", todayStart)
                .orderByAsc("FIELD(priority,'P1','P2','P3','P4')")
                .orderByDesc("detected_at");
        List<Alert> alerts = this.list(queryWrapper);
        return toPageVO(alerts);
    }

    @Override
    public AlertPageVO getAlertById(Long id) {
        Alert alert = this.getById(id);
        if (alert == null) {
            return null;
        }
        List<AlertPageVO> vos = toPageVO(Collections.singletonList(alert));
        return vos.isEmpty() ? null : vos.get(0);
    }

    @Override
    public boolean createAlert(Alert alert) {
        if (alert.getDetectedAt() == null) {
            alert.setDetectedAt(LocalDateTime.now());
        }
        return this.save(alert);
    }

    @Override
    public boolean updateAlertStatus(Long id, String status, String resolutionNote) {
        Alert alert = this.getById(id);
        if (alert == null) {
            return false;
        }

        alert.setStatus(status);

        LocalDateTime now = LocalDateTime.now();
        switch (status) {
            case "ACK":
                alert.setAcknowledgedAt(now);
                break;
            case "RESOLVED":
                alert.setResolvedAt(now);
                alert.setResolutionNote(resolutionNote);
                break;
            case "CLOSED":
                alert.setClosedAt(now);
                break;
        }

        return this.updateById(alert);
    }

    @Override
    public boolean deleteAlert(Long id) {
        return this.removeById(id);
    }

    private List<AlertPageVO> toPageVO(List<Alert> alerts) {
        if (alerts.isEmpty()) {
            return Collections.emptyList();
        }

        List<Long> storeIds = alerts.stream()
                .map(Alert::getStoreId)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        List<Long> productIds = alerts.stream()
                .map(Alert::getProductId)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        List<Long> deviceIds = alerts.stream()
                .map(Alert::getDeviceId)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());

        final Map<Long, String> storeNameMap = new java.util.HashMap<>();
        final Map<Long, Product> productMap = new java.util.HashMap<>();
        final Map<Long, Device> deviceMap = new java.util.HashMap<>();

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

        if (!deviceIds.isEmpty()) {
            List<Device> devices = deviceMapper.selectBatchIds(deviceIds);
            deviceMap.putAll(devices.stream()
                    .collect(Collectors.toMap(Device::getId, d -> d)));
        }

        return alerts.stream().map(alert -> {
            AlertPageVO vo = new AlertPageVO();
            vo.setId(alert.getId());
            vo.setStoreId(alert.getStoreId());
            vo.setStoreName(storeNameMap.getOrDefault(alert.getStoreId(), ""));
            vo.setProductId(alert.getProductId());
            vo.setDeviceId(alert.getDeviceId());
            vo.setDeviceName(deviceMap.getOrDefault(alert.getDeviceId(), new Device()).getDeviceName());
            vo.setLotNumber(alert.getLotNumber());
            vo.setAlertType(alert.getAlertType());
            vo.setPriority(alert.getPriority());
            vo.setStatus(alert.getStatus());
            vo.setMessage(alert.getMessage());
            vo.setThresholdValue(alert.getThresholdValue());
            vo.setCurrentValue(alert.getCurrentValue());
            vo.setDetectedAt(alert.getDetectedAt());
            vo.setAcknowledgedAt(alert.getAcknowledgedAt());
            vo.setResolvedAt(alert.getResolvedAt());
            vo.setClosedAt(alert.getClosedAt());
            vo.setResolutionNote(alert.getResolutionNote());
            vo.setCreateTime(alert.getCreateTime());
            vo.setUpdateTime(alert.getUpdateTime());

            Product product = productMap.get(alert.getProductId());
            if (product != null) {
                vo.setProductName(product.getProductName());
                vo.setProductCode(product.getProductCode());
            } else {
                vo.setProductName("");
                vo.setProductCode("");
            }

            return vo;
        }).collect(Collectors.toList());
    }
}
