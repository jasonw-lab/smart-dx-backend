package com.smartdx.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.smartdx.system.model.entity.Tenant;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * Tenant mapper
 */
@Mapper
public interface TenantMapper extends BaseMapper<Tenant> {

    /**
     * Get tenant ID by domain
     */
    @Select("SELECT id FROM sys_tenant WHERE domain = #{domain} AND status = 1 AND deleted = 0")
    Long getTenantIdByDomain(@Param("domain") String domain);
}
