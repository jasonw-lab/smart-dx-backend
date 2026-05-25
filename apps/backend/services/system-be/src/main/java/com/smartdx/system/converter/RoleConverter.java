package com.smartdx.system.converter;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.smartdx.core.enums.DataScopeEnum;
import com.smartdx.core.model.Option;
import com.smartdx.system.model.entity.Role;
import com.smartdx.system.model.form.RoleForm;
import com.smartdx.system.model.vo.RolePageVO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;
import org.mapstruct.Named;

import java.util.List;

/**
 * 角色对象转换器
 */
@Mapper(componentModel = "spring")
public interface RoleConverter {

    @Mapping(target = "dataScopeLabel", source = "dataScope", qualifiedByName = "dataScopeToLabel")
    RolePageVO toPageVo(Role role);

    default Page<RolePageVO> toPageVo(Page<Role> page) {
        Page<RolePageVO> result = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        result.setRecords(page.getRecords().stream().map(this::toPageVo).toList());
        return result;
    }

    @Mappings({
            @Mapping(target = "value", source = "id"),
            @Mapping(target = "label", source = "name")
    })
    Option<Long> toOption(Role role);

    List<Option<Long>> toOptions(List<Role> roles);

    Role toEntity(RoleForm roleForm);

    RoleForm toForm(Role entity);

    @Named("dataScopeToLabel")
    default String dataScopeToLabel(Integer dataScope) {
        if (dataScope == null) {
            return null;
        }
        DataScopeEnum enumVal = DataScopeEnum.getByValue(dataScope);
        return enumVal != null ? enumVal.getLabel() : null;
    }
}
