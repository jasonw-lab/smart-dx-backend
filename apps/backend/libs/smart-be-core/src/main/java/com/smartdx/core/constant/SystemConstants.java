package com.smartdx.core.constant;

/**
 * システム定数
 */
public interface SystemConstants {

    Long ROOT_NODE_ID = 0L;
    Long DEFAULT_TENANT_ID = 0L;
    Long PLATFORM_TENANT_ID = DEFAULT_TENANT_ID;
    Long PLATFORM_MENU_ID = 1L;

    String PLATFORM_ADMIN_USERNAME = "admin";
    String PLATFORM_ROOT_USERNAME = "root";
    String PLATFORM_ADMIN_ROLE_CODE = "ADMIN";
    String TENANT_SWITCH_PERMISSION = "sys:tenant:switch";
    String DEFAULT_PASSWORD = "123456";
    String ROOT_ROLE_CODE = "ROOT";
    String SYSTEM_CONFIG_IP_QPS_LIMIT_KEY = "IP_QPS_THRESHOLD_LIMIT";
}
