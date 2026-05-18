package com.smartdx.property;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

/**
 * Property Backend Application
 * <p>
 * Smart Property DX - Real Estate Domain Service
 * </p>
 */
@SpringBootApplication(scanBasePackages = {
        "com.smartdx.property",
        "com.smartdx.core",
        "com.smartdx.security",
        "com.smartdx.tenant",
        "com.smartdx.redis",
        "com.smartdx.opensearch",
        "com.smartdx.observability"
})
@MapperScan("com.smartdx.property.mapper")
@EnableCaching
public class PropertyApplication {

    public static void main(String[] args) {
        SpringApplication.run(PropertyApplication.class, args);
    }
}
