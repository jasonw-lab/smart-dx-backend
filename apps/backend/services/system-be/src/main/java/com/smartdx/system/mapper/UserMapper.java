package com.smartdx.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.smartdx.system.model.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * User mapper
 */
@Mapper
public interface UserMapper extends BaseMapper<User> {

    /**
     * Find user by username within a specific tenant
     */
    @Select("SELECT * FROM sys_user WHERE username = #{username} AND tenant_id = #{tenantId}")
    User findByUsernameAndTenantId(@Param("username") String username, @Param("tenantId") Long tenantId);

    /**
     * Find user by username across all tenants (for multi-tenant login without tenant specified)
     */
    @Select("SELECT * FROM sys_user WHERE username = #{username}")
    List<User> findByUsernameAcrossAllTenants(@Param("username") String username);

    /**
     * Find user by mobile
     */
    @Select("SELECT * FROM sys_user WHERE mobile = #{mobile} AND tenant_id = #{tenantId}")
    User findByMobileAndTenantId(@Param("mobile") String mobile, @Param("tenantId") Long tenantId);
}
