package com.smartdx.system.service.impl;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.lang.Assert;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.smartdx.core.enums.DataScopeEnum;
import com.smartdx.core.exception.BusinessException;
import com.smartdx.core.model.Option;
import com.smartdx.security.util.SecurityUtils;
import com.smartdx.system.converter.RoleConverter;
import com.smartdx.system.mapper.RoleMapper;
import com.smartdx.system.model.entity.Role;
import com.smartdx.system.model.entity.RoleMenu;
import com.smartdx.system.model.form.RoleForm;
import com.smartdx.system.model.query.RoleQuery;
import com.smartdx.system.model.vo.RolePageVO;
import com.smartdx.system.service.RoleDeptService;
import com.smartdx.system.service.RoleMenuService;
import com.smartdx.system.service.RoleService;
import com.smartdx.system.service.UserRoleService;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

/**
 * 角色 Service 実装
 */
@Service
@RequiredArgsConstructor
public class RoleServiceImpl extends ServiceImpl<RoleMapper, Role> implements RoleService {

    private final RoleMenuService roleMenuService;
    private final RoleDeptService roleDeptService;
    private final UserRoleService userRoleService;
    private final RoleConverter roleConverter;

    private static final String ROOT_ROLE_CODE = "ROOT";

    @Override
    public Page<RolePageVO> getRolePage(RoleQuery queryParams) {
        int pageNum = queryParams.getPageNum();
        int pageSize = queryParams.getPageSize();
        String keywords = queryParams.getKeywords();

        Page<Role> rolePage = this.page(new Page<>(pageNum, pageSize),
                new LambdaQueryWrapper<Role>()
                        .and(StrUtil.isNotBlank(keywords),
                                wrapper -> wrapper.like(Role::getName, keywords)
                                        .or()
                                        .like(Role::getCode, keywords)
                        )
                        .ne(!SecurityUtils.isRoot(), Role::getCode, ROOT_ROLE_CODE)
                        .orderByAsc(Role::getSort)
                        .orderByDesc(Role::getCreateTime)
                        .orderByDesc(Role::getUpdateTime)
        );

        return roleConverter.toPageVo(rolePage);
    }

    @Override
    public List<Option<Long>> listRoleOptions() {
        List<Role> roleList = this.list(new LambdaQueryWrapper<Role>()
                .ne(!SecurityUtils.isRoot(), Role::getCode, ROOT_ROLE_CODE)
                .select(Role::getId, Role::getName)
                .orderByAsc(Role::getSort)
        );
        return roleConverter.toOptions(roleList);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean saveRole(RoleForm roleForm) {
        Long roleId = roleForm.getId();

        Role oldRole = null;
        List<Long> oldDeptIds = null;
        if (roleId != null) {
            oldRole = this.getById(roleId);
            Assert.isTrue(oldRole != null, "角色不存在");

            if (DataScopeEnum.CUSTOM.getValue().equals(oldRole.getDataScope())) {
                oldDeptIds = roleDeptService.getDeptIdsByRoleId(roleId);
            }
        }

        String roleCode = roleForm.getCode();
        long count = this.count(new LambdaQueryWrapper<Role>()
                .ne(roleId != null, Role::getId, roleId)
                .and(wrapper -> wrapper.eq(Role::getCode, roleCode)
                        .or()
                        .eq(Role::getName, roleForm.getName())
                ));
        Assert.isTrue(count == 0, "角色名称或角色编码已存在，请修改后重试！");

        Role role = roleConverter.toEntity(roleForm);
        boolean result = this.saveOrUpdate(role);

        if (result) {
            Long savedRoleId = role.getId();
            if (DataScopeEnum.CUSTOM.getValue().equals(roleForm.getDataScope())) {
                roleDeptService.saveRoleDepts(savedRoleId, roleForm.getDeptIds());
            } else {
                roleDeptService.deleteByRoleId(savedRoleId);
            }

            if (oldRole != null &&
                    (!StrUtil.equals(oldRole.getCode(), roleCode) ||
                            !ObjectUtil.equals(oldRole.getStatus(), roleForm.getStatus()))) {
                roleMenuService.refreshRolePermsCache(oldRole.getCode(), roleCode);
            }
        }
        return result;
    }

    @Override
    public RoleForm getRoleForm(Long roleId) {
        Role entity = this.getById(roleId);
        RoleForm roleForm = roleConverter.toForm(entity);
        if (roleForm != null && DataScopeEnum.CUSTOM.getValue().equals(roleForm.getDataScope())) {
            List<Long> deptIds = roleDeptService.getDeptIdsByRoleId(roleId);
            roleForm.setDeptIds(deptIds);
        }
        return roleForm;
    }

    @Override
    public boolean updateRoleStatus(Long roleId, Integer status) {
        Role role = this.getById(roleId);
        if (role == null) {
            throw new BusinessException("角色不存在");
        }

        role.setStatus(status);
        boolean result = this.updateById(role);
        if (result) {
            roleMenuService.refreshRolePermsCache(role.getCode());
        }
        return result;
    }

    @Override
    public void deleteRoles(String ids) {
        Assert.isTrue(StrUtil.isNotBlank(ids), "删除的角色ID不能为空");
        List<Long> roleIds = Arrays.stream(ids.split(","))
                .map(Long::parseLong)
                .toList();

        for (Long roleId : roleIds) {
            Role role = this.getById(roleId);
            Assert.isTrue(role != null, "角色不存在");

            boolean isRoleAssigned = userRoleService.hasAssignedUsers(roleId);
            Assert.isTrue(!isRoleAssigned, "角色【{}】已分配用户，请先解除关联后删除", role.getName());

            boolean deleteResult = this.removeById(roleId);
            if (deleteResult) {
                roleMenuService.refreshRolePermsCache(role.getCode());
            }
        }
    }

    @Override
    public List<Long> getRoleMenuIds(Long roleId) {
        return roleMenuService.listMenuIdsByRoleId(roleId);
    }

    @Override
    @Transactional
    @CacheEvict(cacheNames = "menu", key = "'routes'")
    public void assignMenusToRole(Long roleId, List<Long> menuIds) {
        Role role = this.getById(roleId);
        if (role == null) {
            throw new BusinessException("角色不存在");
        }

        roleMenuService.remove(new LambdaQueryWrapper<RoleMenu>().eq(RoleMenu::getRoleId, roleId));

        if (CollectionUtil.isNotEmpty(menuIds)) {
            Long tenantId = role.getTenantId();
            List<RoleMenu> roleMenus = menuIds.stream()
                    .map(menuId -> new RoleMenu(roleId, menuId, tenantId))
                    .toList();
            roleMenuService.saveBatch(roleMenus);
        }

        roleMenuService.refreshRolePermsCache(role.getCode());
    }

    @Override
    public Integer getMaximumDataScope(Set<String> roles) {
        if (roles == null || roles.isEmpty()) {
            return DataScopeEnum.SELF.getValue();
        }
        List<Role> roleList = this.list(new LambdaQueryWrapper<Role>()
                .in(Role::getCode, roles)
                .select(Role::getDataScope));
        return roleList.stream()
                .map(Role::getDataScope)
                .filter(ds -> ds != null)
                .min(Integer::compareTo)
                .orElse(DataScopeEnum.SELF.getValue());
    }

    @Override
    public List<Long> getRoleDeptIds(Long roleId) {
        return roleDeptService.getDeptIdsByRoleId(roleId);
    }
}
