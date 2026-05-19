package com.smartdx.system.config;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.ComponentScan;

/**
 * System domain auto-configuration.
 * <p>
 * Enables component scanning for the system domain when included
 * as a dependency in the modular monolith bootstrap application.
 * </p>
 *
 * @see com.smartdx.SmartDxApplication
 */
@AutoConfiguration
@ComponentScan(basePackages = "com.smartdx.system")
public class SystemAutoConfiguration {
}
