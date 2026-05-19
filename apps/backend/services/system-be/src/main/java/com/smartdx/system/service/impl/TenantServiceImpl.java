package com.smartdx.system.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.smartdx.core.exception.BusinessException;
import com.smartdx.security.model.UserDetails;
import com.smartdx.security.util.SecurityUtils;
import com.smartdx.system.mapper.TenantMapper;
import com.smartdx.system.mapper.UserMapper;
import com.smartdx.system.model.entity.Tenant;
import com.smartdx.system.model.entity.User;
import com.smartdx.system.model.query.TenantQuery;
import com.smartdx.system.model.vo.TenantVO;
import com.smartdx.system.service.TenantService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Tenant service implementation
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TenantServiceImpl implements TenantService {

    private final TenantMapper tenantMapper;
    private final UserMapper userMapper;

    @Override
    public TenantVO getTenantById(Long id) {
        Tenant tenant = tenantMapper.selectById(id);
        if (tenant == null) {
            throw new BusinessException("Tenant not found");
        }
        return convertToVO(tenant);
    }

    @Override
    public Long getTenantIdByDomain(String domain) {
        return tenantMapper.getTenantIdByDomain(domain);
    }

    @Override
    public IPage<TenantVO> listTenants(TenantQuery query) {
        Page<Tenant> page = new Page<>(query.getPageNum(), query.getPageSize());

        LambdaQueryWrapper<Tenant> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(StrUtil.isNotBlank(query.getName()), Tenant::getName, query.getName())
                .like(StrUtil.isNotBlank(query.getCode()), Tenant::getCode, query.getCode())
                .eq(query.getStatus() != null, Tenant::getStatus, query.getStatus())
                .orderByDesc(Tenant::getCreateTime);

        IPage<Tenant> tenantPage = tenantMapper.selectPage(page, wrapper);

        return tenantPage.convert(this::convertToVO);
    }

    @Override
    @Transactional
    public Long createTenant(Tenant tenant) {
        tenantMapper.insert(tenant);
        return tenant.getId();
    }

    @Override
    @Transactional
    public void updateTenant(Tenant tenant) {
        tenantMapper.updateById(tenant);
    }

    @Override
    @Transactional
    public void deleteTenant(Long id) {
        tenantMapper.deleteById(id);
    }

    @Override
    public boolean hasTenantSwitchPermission() {
        UserDetails user = SecurityUtils.getCurrentUser();
        return user != null && Boolean.TRUE.equals(user.getCanSwitchTenant());
    }

    @Override
    public boolean canAccessTenant(Long userId, Long tenantId) {
        // Check if user has records in the target tenant
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getId, userId)
                .eq(User::getTenantId, tenantId)
                .eq(User::getStatus, 1);
        return userMapper.selectCount(wrapper) > 0;
    }

    private TenantVO convertToVO(Tenant tenant) {
        TenantVO vo = new TenantVO();
        BeanUtil.copyProperties(tenant, vo);
        return vo;
    }
}
