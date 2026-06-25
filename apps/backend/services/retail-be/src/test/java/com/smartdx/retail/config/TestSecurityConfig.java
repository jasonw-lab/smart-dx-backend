package com.smartdx.retail.config;

import com.smartdx.security.config.BaseSecurityConfig;
import com.smartdx.security.config.SecurityProperties;
import com.smartdx.security.token.TokenManager;
import org.springframework.boot.test.context.TestConfiguration;

/**
 * Test-only security configuration that activates the shared BaseSecurityConfig.
 * <p>
 * The retail-be module is a library and does not provide a concrete security
 * config. This test configuration wires one up so that JWT Bearer tokens are
 * validated during E2E tests.
 * </p>
 *
 * @author jason.w
 */
@TestConfiguration
public class TestSecurityConfig extends BaseSecurityConfig {

    public TestSecurityConfig(TokenManager tokenManager, SecurityProperties securityProperties) {
        super(tokenManager, securityProperties);
    }
}
