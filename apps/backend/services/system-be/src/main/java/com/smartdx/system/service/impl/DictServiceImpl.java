package com.smartdx.system.service.impl;

import cn.hutool.core.lang.Assert;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.smartdx.core.exception.BusinessException;
import com.smartdx.core.model.Option;
import com.smartdx.system.converter.DictConverter;
import com.smartdx.system.mapper.DictMapper;
import com.smartdx.system.model.entity.Dict;
import com.smartdx.system.model.form.DictForm;
import com.smartdx.system.model.query.DictQuery;
import com.smartdx.system.model.vo.DictPageVO;
import com.smartdx.system.service.DictItemService;
import com.smartdx.system.service.DictService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 辞書サービス実装
 */
@Service
@RequiredArgsConstructor
public class DictServiceImpl extends ServiceImpl<DictMapper, Dict> implements DictService {

    private final DictItemService dictItemService;
    private final DictConverter dictConverter;

    @Override
    public Page<DictPageVO> getDictPage(DictQuery query) {
        Page<Dict> page = new Page<>(query.getPageNum(), query.getPageSize());

        LambdaQueryWrapper<Dict> wrapper = new LambdaQueryWrapper<Dict>()
                .like(StrUtil.isNotBlank(query.getKeywords()), Dict::getName, query.getKeywords())
                .or()
                .like(StrUtil.isNotBlank(query.getKeywords()), Dict::getCode, query.getKeywords())
                .orderByDesc(Dict::getUpdateTime);

        Page<Dict> result = this.page(page, wrapper);
        return dictConverter.toPageVo(result);
    }

    @Override
    public List<Option<String>> getDictOptions() {
        return this.list(new LambdaQueryWrapper<Dict>().eq(Dict::getStatus, 1))
                .stream()
                .map(item -> new Option<>(item.getCode(), item.getName()))
                .toList();
    }

    @Override
    public DictForm getDictForm(Long id) {
        Dict entity = this.getById(id);
        if (entity == null) {
            throw new BusinessException("辞書が存在しません");
        }
        return dictConverter.toForm(entity);
    }

    @Override
    public boolean saveDict(DictForm form) {
        Dict entity = dictConverter.toEntity(form);

        // コード重複チェック
        String code = entity.getCode();
        long count = this.count(new LambdaQueryWrapper<Dict>().eq(Dict::getCode, code));
        Assert.isTrue(count == 0, "辞書コードが既に存在します");

        return this.save(entity);
    }

    @Override
    @Transactional
    public boolean updateDict(Long id, DictForm form) {
        Dict entity = this.getById(id);
        if (entity == null) {
            throw new BusinessException("辞書が存在しません");
        }

        // コード重複チェック（自身以外）
        String code = form.getCode();
        if (!entity.getCode().equals(code)) {
            long count = this.count(new LambdaQueryWrapper<Dict>().eq(Dict::getCode, code));
            Assert.isTrue(count == 0, "辞書コードが既に存在します");
        }

        Dict dict = dictConverter.toEntity(form);
        dict.setId(id);
        return this.updateById(dict);
    }

    @Override
    @Transactional
    public void deleteDictByIds(List<Long> ids) {
        // 辞書コードを取得して関連項目も削除
        List<Dict> dicts = this.listByIds(ids);
        for (Dict dict : dicts) {
            dictItemService.deleteByDictCode(dict.getCode());
        }
        this.removeByIds(ids);
    }
}
