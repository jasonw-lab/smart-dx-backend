package com.smartdx.system.service.impl;

import cn.hutool.captcha.AbstractCaptcha;
import cn.hutool.captcha.CaptchaUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import com.smartdx.system.captcha.CaptchaProperties;
import com.smartdx.system.mapper.RoleMapper;
import com.smartdx.system.mapper.UserMapper;
import com.smartdx.system.model.entity.User;
import com.smartdx.system.model.vo.CaptchaVO;
import com.smartdx.system.service.AuthService;
import com.smartdx.core.exception.BusinessException;
import com.smartdx.security.model.AuthenticationToken;
import com.smartdx.security.model.UserDetails;
import com.smartdx.security.token.TokenManager;
import com.smartdx.tenant.TenantContextHolder;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * Authentication service implementation
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AuthServiceImpl implements AuthService {

    private final UserMapper userMapper;
    private final RoleMapper roleMapper;
    private final PasswordEncoder passwordEncoder;
    private final TokenManager tokenManager;
    private final RedisTemplate<String, Object> redisTemplate;
    private final CaptchaProperties captchaProperties;

    private static final String CAPTCHA_KEY_PREFIX = "auth:captcha:";
    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    @Override
    public AuthenticationToken login(String username, String password, Long tenantId) {
        // Set tenant context if specified
        if (tenantId != null) {
            TenantContextHolder.setTenantId(tenantId);
        }

        // Find user
        User user;
        if (tenantId != null) {
            user = userMapper.findByUsernameAndTenantId(username, tenantId);
        } else {
            // Default tenant
            user = userMapper.findByUsernameAndTenantId(username, 1L);
        }

        if (user == null) {
            throw new BusinessException("Invalid username or password");
        }

        // Check password
        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new BusinessException("Invalid username or password");
        }

        // Check status
        if (user.getStatus() == null || user.getStatus() != 1) {
            throw new BusinessException("Account is disabled");
        }

        // Build UserDetails
        UserDetails userDetails = new UserDetails();
        userDetails.setUserId(user.getId());
        userDetails.setUsername(user.getUsername());
        userDetails.setTenantId(user.getTenantId());
        userDetails.setDeptId(user.getDeptId());
        userDetails.setCanSwitchTenant(user.getCanSwitchTenant());
        userDetails.setStatus(user.getStatus());

        // Fetch user's role codes from database
        Set<String> roleCodes = roleMapper.selectRoleCodesByUserId(user.getId());
        userDetails.setRoleCodes(roleCodes != null ? roleCodes : Collections.emptySet());

        // Create authentication
        Authentication authentication = new UsernamePasswordAuthenticationToken(
                userDetails, null, userDetails.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(authentication);

        // Generate token
        return tokenManager.generateToken(authentication);
    }

    @Override
    public void logout() {
        String token = getAccessTokenFromRequest();
        if (StrUtil.isNotBlank(token)) {
            tokenManager.invalidateToken(token);
            SecurityContextHolder.clearContext();
        }
    }

    /**
     * Get access token from request header
     */
    private String getAccessTokenFromRequest() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            return null;
        }
        HttpServletRequest request = attributes.getRequest();
        String header = request.getHeader(AUTHORIZATION_HEADER);
        if (StrUtil.isNotBlank(header) && header.startsWith(BEARER_PREFIX)) {
            return header.substring(BEARER_PREFIX.length());
        }
        return null;
    }

    @Override
    public CaptchaVO getCaptcha() {
        int width = captchaProperties.getWidth();
        int height = captchaProperties.getHeight();
        int codeLength = captchaProperties.getCodeLength();

        AbstractCaptcha captcha = CaptchaUtil.createLineCaptcha(width, height, codeLength, 50);

        String captchaCode = captcha.getCode();
        String imageBase64 = captcha.getImageBase64Data();

        // Save to Redis
        String captchaId = IdUtil.fastSimpleUUID();
        String key = CAPTCHA_KEY_PREFIX + captchaId;
        redisTemplate.opsForValue().set(key, captchaCode, captchaProperties.getExpireSeconds(), TimeUnit.SECONDS);

        return CaptchaVO.builder()
                .captchaId(captchaId)
                .captchaBase64(imageBase64)
                .build();
    }

    @Override
    public boolean verifyCaptcha(String captchaId, String code) {
        if (StrUtil.isBlank(captchaId) || StrUtil.isBlank(code)) {
            return false;
        }

        String key = CAPTCHA_KEY_PREFIX + captchaId;
        Object cachedCode = redisTemplate.opsForValue().get(key);

        if (cachedCode == null) {
            return false;
        }

        // Delete after verification (one-time use)
        redisTemplate.delete(key);

        return code.equalsIgnoreCase(cachedCode.toString());
    }

    @Override
    public AuthenticationToken refreshToken(String refreshToken) {
        return tokenManager.refreshToken(refreshToken);
    }
}
