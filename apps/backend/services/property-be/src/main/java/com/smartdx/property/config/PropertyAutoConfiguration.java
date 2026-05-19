package com.smartdx.property.config;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.ComponentScan;

/**
 * Property domain auto-configuration.
 * <p>
 * Enables component scanning for the property domain when included
 * as a dependency in the modular monolith bootstrap application.
 * </p>
 *
 * @see com.smartdx.SmartDxApplication
 */
@AutoConfiguration
@ComponentScan(basePackages = "com.smartdx.property")
public class PropertyAutoConfiguration {
}
