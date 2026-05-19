package com.smartdx.retail.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.smartdx.retail.mapper.DeviceMapper;
import com.smartdx.retail.model.entity.Device;
import com.smartdx.retail.service.DeviceService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Device Service Implementation
 */
@Service
@RequiredArgsConstructor
public class DeviceServiceImpl extends ServiceImpl<DeviceMapper, Device> implements DeviceService {

    @Override
    public List<Device> listDevices(Long storeId) {
        LambdaQueryWrapper<Device> queryWrapper = new LambdaQueryWrapper<Device>()
                .eq(storeId != null, Device::getStoreId, storeId)
                .orderByAsc(Device::getDeviceCode);
        return this.list(queryWrapper);
    }

    @Override
    public Device getDeviceById(Long id) {
        return this.getById(id);
    }

    @Override
    public Device getDeviceByCode(String deviceCode) {
        LambdaQueryWrapper<Device> queryWrapper = new LambdaQueryWrapper<Device>()
                .eq(Device::getDeviceCode, deviceCode);
        return this.getOne(queryWrapper, false);
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
}
