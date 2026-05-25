package com.smartdx.system.converter;

import com.smartdx.core.model.KeyValue;
import com.smartdx.system.model.entity.Menu;
import com.smartdx.system.model.form.MenuForm;
import com.smartdx.system.model.vo.MenuVO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * メニューコンバーター
 */
@Mapper(componentModel = "spring")
public interface MenuConverter {

    MenuVO toVo(Menu menu);

    List<MenuVO> toVoList(List<Menu> menus);

    @Mapping(target = "params", source = "params", qualifiedByName = "keyValueListToMap")
    Menu toEntity(MenuForm form);

    @Mapping(target = "params", source = "params", qualifiedByName = "mapToKeyValueList")
    MenuForm toForm(Menu entity);

    @org.mapstruct.Named("keyValueListToMap")
    default Map<String, Object> keyValueListToMap(List<KeyValue> params) {
        if (params == null || params.isEmpty()) {
            return null;
        }
        return params.stream()
                .collect(Collectors.toMap(KeyValue::getKey, KeyValue::getValue, (a, b) -> b, HashMap::new));
    }

    @org.mapstruct.Named("mapToKeyValueList")
    default List<KeyValue> mapToKeyValueList(Map<String, Object> params) {
        if (params == null || params.isEmpty()) {
            return null;
        }
        return params.entrySet().stream()
                .map(e -> new KeyValue(e.getKey(), String.valueOf(e.getValue())))
                .toList();
    }
}
