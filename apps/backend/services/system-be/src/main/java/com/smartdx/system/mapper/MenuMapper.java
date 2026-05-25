package com.smartdx.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.smartdx.system.model.entity.Menu;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Set;

/**
 * メニュー Mapper
 */
@Mapper
public interface MenuMapper extends BaseMapper<Menu> {

    /**
     * ロールに基づくメニュー取得
     */
    @Select("<script>" +
            "SELECT DISTINCT m.* FROM sys_menu m " +
            "INNER JOIN sys_role_menu rm ON m.id = rm.menu_id " +
            "INNER JOIN sys_role r ON rm.role_id = r.id " +
            "WHERE r.code IN " +
            "<foreach collection='roleCodes' item='code' open='(' separator=',' close=')'>" +
            "#{code}" +
            "</foreach>" +
            " ORDER BY m.sort" +
            "</script>")
    List<Menu> selectMenusByRoleCodes(@Param("roleCodes") Set<String> roleCodes);

    /**
     * 権限識別子を取得
     */
    @Select("<script>" +
            "SELECT DISTINCT m.perm FROM sys_menu m " +
            "INNER JOIN sys_role_menu rm ON m.id = rm.menu_id " +
            "INNER JOIN sys_role r ON rm.role_id = r.id " +
            "WHERE r.code IN " +
            "<foreach collection='roleCodes' item='code' open='(' separator=',' close=')'>" +
            "#{code}" +
            "</foreach>" +
            " AND m.perm IS NOT NULL AND m.perm != ''" +
            "</script>")
    Set<String> selectPermsByRoleCodes(@Param("roleCodes") Set<String> roleCodes);
}
