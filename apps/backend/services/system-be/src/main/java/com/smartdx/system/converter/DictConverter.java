package com.smartdx.system.converter;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.smartdx.system.model.entity.Dict;
import com.smartdx.system.model.form.DictForm;
import com.smartdx.system.model.vo.DictPageVO;
import org.mapstruct.Mapper;

/**
 * 辞書コンバーター
 */
@Mapper(componentModel = "spring")
public interface DictConverter {

    DictPageVO toPageVo(Dict dict);

    default Page<DictPageVO> toPageVo(Page<Dict> page) {
        Page<DictPageVO> result = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        result.setRecords(page.getRecords().stream().map(this::toPageVo).toList());
        return result;
    }

    Dict toEntity(DictForm form);

    DictForm toForm(Dict entity);
}
