package com.smartdx.system.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.smartdx.system.mapper.RoleMenuMapper;
import com.smartdx.system.model.entity.RoleMenu;
import com.smartdx.system.service.RoleMenuService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 角色菜单 Service 实装
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RoleMenuServiceImpl extends ServiceImpl<RoleMenuMapper, RoleMenu> implements RoleMenuService {

    @Override
    public List<Long> listMenuIdsByRoleId(Long roleId) {
        return baseMapper.selectMenuIdsByRoleId(roleId);
    }

    @Override
    public Set<String> listPermsByRoleCodes(Set<String> roleCodes) {
        if (roleCodes == null || roleCodes.isEmpty()) {
            return new HashSet<>();
        }
        return baseMapper.selectPermsByRoleCodes(roleCodes);
    }

    @Override
    public void refreshRolePermsCache(String... roleCodes) {
        // キャッシュ更新ロジック（必要に応じて Redis キャッシュを更新）
        log.info("Refreshing role permissions cache for roles: {}", (Object) roleCodes);
    }
}
