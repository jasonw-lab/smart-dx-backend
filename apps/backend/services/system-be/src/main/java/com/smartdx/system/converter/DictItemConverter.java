package com.smartdx.system.converter;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.smartdx.system.model.entity.DictItem;
import com.smartdx.system.model.form.DictItemForm;
import com.smartdx.system.model.vo.DictItemPageVO;
import org.mapstruct.Mapper;

/**
 * 辞書項目コンバーター
 */
@Mapper(componentModel = "spring")
public interface DictItemConverter {

    DictItemPageVO toPageVo(DictItem item);

    default Page<DictItemPageVO> toPageVo(Page<DictItem> page) {
        Page<DictItemPageVO> result = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        result.setRecords(page.getRecords().stream().map(this::toPageVo).toList());
        return result;
    }

    DictItem toEntity(DictItemForm form);

    DictItemForm toForm(DictItem entity);
}
