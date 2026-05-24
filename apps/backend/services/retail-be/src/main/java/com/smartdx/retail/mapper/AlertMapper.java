package com.smartdx.retail.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.smartdx.retail.model.entity.Alert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * Alert Mapper
 */
@Mapper
public interface AlertMapper extends BaseMapper<Alert> {

    /**
     * 未解決のデバイスアラートが存在するかチェック
     */
    @Select("SELECT COUNT(*) > 0 FROM retail_alert " +
            "WHERE store_id = #{storeId} AND device_id = #{deviceId} AND alert_type = #{alertType} " +
            "AND status IN ('NEW', 'ACKNOWLEDGED') AND is_deleted = 0")
    boolean existsUnresolvedDeviceAlert(
            @Param("storeId") Long storeId,
            @Param("deviceId") Long deviceId,
            @Param("alertType") String alertType);
}
