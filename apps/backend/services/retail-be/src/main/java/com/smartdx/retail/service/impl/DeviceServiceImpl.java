package com.smartdx.retail.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.smartdx.retail.mapper.DeviceMapper;
import com.smartdx.retail.mapper.StoreMapper;
import com.smartdx.retail.model.entity.Device;
import com.smartdx.retail.model.entity.Store;
import com.smartdx.retail.service.DeviceService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Device Service Implementation
 */
@Service
@RequiredArgsConstructor
public class DeviceServiceImpl extends ServiceImpl<DeviceMapper, Device> implements DeviceService {

    private final StoreMapper storeMapper;

    @Override
    public List<Device> listDevices(Long storeId) {
        LambdaQueryWrapper<Device> queryWrapper = new LambdaQueryWrapper<Device>()
                .eq(storeId != null, Device::getStoreId, storeId)
                .orderByAsc(Device::getDeviceCode);
        List<Device> devices = this.list(queryWrapper);
        populateStoreName(devices);
        return devices;
    }

    @Override
    public Device getDeviceById(Long id) {
        Device device = this.getById(id);
        if (device != null) {
            populateStoreName(Collections.singletonList(device));
        }
        return device;
    }

    @Override
    public Device getDeviceByCode(String deviceCode) {
        LambdaQueryWrapper<Device> queryWrapper = new LambdaQueryWrapper<Device>()
                .eq(Device::getDeviceCode, deviceCode);
        Device device = this.getOne(queryWrapper, false);
        if (device != null) {
            populateStoreName(Collections.singletonList(device));
        }
        return device;
    }

    @Override
    public boolean createDevice(Device device) {
        return this.save(device);
    }

    @Override
    public boolean updateDevice(Long id, Device device) {
        device.setId(id);
        return this.updateById(device);
    }

    @Override
    public boolean deleteDevice(Long id) {
        return this.removeById(id);
    }

    @Override
    public boolean updateHeartbeat(String deviceCode) {
        Device device = getDeviceByCode(deviceCode);
        if (device == null) {
            return false;
        }
        device.setLastHeartbeat(LocalDateTime.now());
        device.setStatus("ONLINE");
        return this.updateById(device);
    }

    private void populateStoreName(List<Device> devices) {
        if (devices.isEmpty()) {
            return;
        }

        List<Long> storeIds = devices.stream()
                .map(Device::getStoreId)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());

        if (storeIds.isEmpty()) {
            return;
        }

        List<Store> stores = storeMapper.selectBatchIds(storeIds);
        Map<Long, String> storeNameMap = stores.stream()
                .collect(Collectors.toMap(Store::getId, Store::getStoreName));

        for (Device device : devices) {
            if (device.getStoreId() != null) {
                device.setStoreName(storeNameMap.getOrDefault(device.getStoreId(), ""));
            }
        }
    }
}
