package com.smartdx.system.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.smartdx.system.converter.DictItemConverter;
import com.smartdx.system.mapper.DictItemMapper;
import com.smartdx.system.model.entity.DictItem;
import com.smartdx.system.model.form.DictItemForm;
import com.smartdx.system.model.query.DictItemQuery;
import com.smartdx.system.model.vo.DictItemOptionVO;
import com.smartdx.system.model.vo.DictItemPageVO;
import com.smartdx.system.service.DictItemService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 辞書項目サービス実装
 */
@Service
@RequiredArgsConstructor
public class DictItemServiceImpl extends ServiceImpl<DictItemMapper, DictItem> implements DictItemService {

    private final DictItemConverter dictItemConverter;

    @Override
    public Page<DictItemPageVO> getDictItemPage(DictItemQuery query) {
        Page<DictItem> page = new Page<>(query.getPageNum(), query.getPageSize());

        LambdaQueryWrapper<DictItem> wrapper = new LambdaQueryWrapper<DictItem>()
                .eq(StrUtil.isNotBlank(query.getDictCode()), DictItem::getDictCode, query.getDictCode())
                .and(StrUtil.isNotBlank(query.getKeywords()), w -> w
                        .like(DictItem::getLabel, query.getKeywords())
                        .or()
                        .like(DictItem::getValue, query.getKeywords())
                )
                .orderByAsc(DictItem::getSort);

        Page<DictItem> result = this.page(page, wrapper);
        return dictItemConverter.toPageVo(result);
    }

    @Override
    public List<DictItemOptionVO> getDictItemOptions(String dictCode) {
        return this.list(
                        new LambdaQueryWrapper<DictItem>()
                                .eq(DictItem::getDictCode, dictCode)
                                .eq(DictItem::getStatus, 1)
                                .orderByAsc(DictItem::getSort)
                ).stream()
                .map(item -> {
                    DictItemOptionVO vo = new DictItemOptionVO();
                    vo.setLabel(item.getLabel());
                    vo.setValue(item.getValue());
                    vo.setTagType(item.getTagType());
                    return vo;
                }).toList();
    }

    @Override
    public DictItemForm getDictItemForm(Long itemId) {
        DictItem entity = this.getById(itemId);
        return dictItemConverter.toForm(entity);
    }

    @Override
    public boolean saveDictItem(DictItemForm form) {
        DictItem entity = dictItemConverter.toEntity(form);
        return this.save(entity);
    }

    @Override
    public boolean updateDictItem(DictItemForm form) {
        DictItem entity = dictItemConverter.toEntity(form);
        return this.updateById(entity);
    }

    @Override
    public void deleteDictItemByIds(List<Long> ids) {
        this.removeByIds(ids);
    }

    @Override
    public void deleteByDictCode(String dictCode) {
        this.remove(new LambdaQueryWrapper<DictItem>().eq(DictItem::getDictCode, dictCode));
    }
}
