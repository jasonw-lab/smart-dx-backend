package com.smartdx.retail.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.smartdx.retail.model.entity.Device;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Device Mapper
 */
@Mapper
public interface DeviceMapper extends BaseMapper<Device> {

    /**
     * 店舗IDとデバイスタイプでデバイスを検索
     */
    @Select("SELECT * FROM retail_device WHERE store_id = #{storeId} AND device_type = #{deviceType} AND is_deleted = 0 LIMIT 1")
    Device findByStoreIdAndDeviceType(@Param("storeId") Long storeId, @Param("deviceType") String deviceType);

    /**
     * 最終Heartbeatが閾値を超過したデバイスを検索
     */
    @Select("SELECT * FROM retail_device WHERE (last_heartbeat IS NULL OR last_heartbeat < #{threshold}) AND is_deleted = 0")
    List<Device> findStaleDevices(@Param("threshold") LocalDateTime threshold);

    /**
     * デバイスタイプでデバイスを検索
     */
    @Select("SELECT * FROM retail_device WHERE device_type = #{deviceType} AND is_deleted = 0")
    List<Device> findByDeviceType(@Param("deviceType") String deviceType);
}
