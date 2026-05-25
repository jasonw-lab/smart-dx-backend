package com.smartdx.system.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.smartdx.core.model.Option;
import com.smartdx.system.model.entity.Dict;
import com.smartdx.system.model.form.DictForm;
import com.smartdx.system.model.query.DictQuery;
import com.smartdx.system.model.vo.DictPageVO;

import java.util.List;

/**
 * 辞書サービス
 */
public interface DictService extends IService<Dict> {

    /**
     * 辞書ページリスト取得
     *
     * @param query クエリパラメータ
     * @return ページリスト
     */
    Page<DictPageVO> getDictPage(DictQuery query);

    /**
     * 辞書オプションリスト取得
     *
     * @return オプションリスト
     */
    List<Option<String>> getDictOptions();

    /**
     * 辞書フォーム取得
     *
     * @param id 辞書ID
     * @return フォーム
     */
    DictForm getDictForm(Long id);

    /**
     * 辞書保存
     *
     * @param form フォーム
     * @return 成功フラグ
     */
    boolean saveDict(DictForm form);

    /**
     * 辞書更新
     *
     * @param id   辞書ID
     * @param form フォーム
     * @return 成功フラグ
     */
    boolean updateDict(Long id, DictForm form);

    /**
     * 辞書削除
     *
     * @param ids 辞書IDリスト
     */
    void deleteDictByIds(List<Long> ids);
}
