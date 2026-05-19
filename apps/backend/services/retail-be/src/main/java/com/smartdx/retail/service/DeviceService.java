package com.smartdx.retail.service;

import com.smartdx.retail.model.entity.Device;

import java.util.List;

/**
 * Device Service Interface
 */
public interface DeviceService {

    List<Device> listDevices(Long storeId);

    Device getDeviceById(Long id);

    Device getDeviceByCode(String deviceCode);

    boolean createDevice(Device device);

    boolean updateDevice(Long id, Device device);

    boolean deleteDevice(Long id);

    boolean updateHeartbeat(String deviceCode);
}
