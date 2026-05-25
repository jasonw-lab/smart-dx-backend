package com.smartdx.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.smartdx.system.model.entity.UserRole;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 用户角色 Mapper
 */
@Mapper
public interface UserRoleMapper extends BaseMapper<UserRole> {

    @Select("SELECT user_id FROM sys_user_role WHERE role_id = #{roleId}")
    List<Long> selectUserIdsByRoleId(@Param("roleId") Long roleId);

    @Select("SELECT COUNT(*) > 0 FROM sys_user_role WHERE role_id = #{roleId}")
    boolean existsByRoleId(@Param("roleId") Long roleId);

    @Select("SELECT role_id FROM sys_user_role WHERE user_id = #{userId}")
    List<Long> selectRoleIdsByUserId(@Param("userId") Long userId);
}
