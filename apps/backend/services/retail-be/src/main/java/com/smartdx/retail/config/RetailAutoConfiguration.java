package com.smartdx.retail.config;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.ComponentScan;

/**
 * Retail domain auto-configuration.
 * <p>
 * Enables component scanning for the retail domain when included
 * as a dependency in the modular monolith bootstrap application.
 * </p>
 *
 * @see com.smartdx.SmartDxApplication
 */
@AutoConfiguration
@ComponentScan(basePackages = "com.smartdx.retail")
public class RetailAutoConfiguration {
}
