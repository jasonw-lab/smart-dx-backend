package com.smartdx.retail.e2e;

import com.smartdx.retail.model.entity.Device;
import com.smartdx.retail.service.DeviceService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * E2E tests for device list store name resolution.
 */
class DeviceE2ETest extends RetailE2EBase {

    @Autowired
    private DeviceService deviceService;

    @Test
    void listDevices_returnsDevices_withStoreName() {
        // Given
        Long storeId = seedStore("S003", "名古屋店", "ONLINE");
        seedDevice(storeId, "DEV-001", "PAYMENT_TERMINAL", "レジ端末1", "ONLINE");
        seedDevice(storeId, "DEV-002", "CAMERA", "監視カメラ", "ONLINE");

        // When
        List<Device> devices = deviceService.listDevices(null);

        // Then
        assertThat(devices).hasSize(2);
        devices.forEach(device -> assertThat(device.getStoreName()).isEqualTo("名古屋店"));
    }

    @Test
    void getDeviceById_returnsDevice_withStoreName() {
        // Given
        Long storeId = seedStore("S004", "福岡店", "ONLINE");
        Long deviceId = seedDevice(storeId, "DEV-003", "PRINTER", "キッチンプリンター", "ONLINE");

        // When
        Device device = deviceService.getDeviceById(deviceId);

        // Then
        assertThat(device).isNotNull();
        assertThat(device.getStoreName()).isEqualTo("福岡店");
    }
}
