package com.smartdx.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.smartdx.system.model.entity.RoleMenu;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Set;

/**
 * 角色菜单 Mapper
 */
@Mapper
public interface RoleMenuMapper extends BaseMapper<RoleMenu> {

    @Select("SELECT menu_id FROM sys_role_menu WHERE role_id = #{roleId}")
    List<Long> selectMenuIdsByRoleId(@Param("roleId") Long roleId);

    @Select("<script>" +
            "SELECT DISTINCT m.perms FROM sys_menu m " +
            "INNER JOIN sys_role_menu rm ON m.id = rm.menu_id " +
            "INNER JOIN sys_role r ON rm.role_id = r.id " +
            "WHERE r.code IN " +
            "<foreach collection='roleCodes' item='code' open='(' separator=',' close=')'>" +
            "#{code}" +
            "</foreach>" +
            " AND m.perms IS NOT NULL AND m.perms != ''" +
            "</script>")
    Set<String> selectPermsByRoleCodes(@Param("roleCodes") Set<String> roleCodes);
}
