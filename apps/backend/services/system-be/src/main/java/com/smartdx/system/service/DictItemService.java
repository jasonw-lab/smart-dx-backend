package com.smartdx.system.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.smartdx.system.model.entity.DictItem;
import com.smartdx.system.model.form.DictItemForm;
import com.smartdx.system.model.query.DictItemQuery;
import com.smartdx.system.model.vo.DictItemOptionVO;
import com.smartdx.system.model.vo.DictItemPageVO;

import java.util.List;

/**
 * 辞書項目サービス
 */
public interface DictItemService extends IService<DictItem> {

    /**
     * 辞書項目ページリスト取得
     *
     * @param query クエリパラメータ
     * @return ページリスト
     */
    Page<DictItemPageVO> getDictItemPage(DictItemQuery query);

    /**
     * 辞書コードで辞書項目オプションリスト取得
     *
     * @param dictCode 辞書コード
     * @return オプションリスト
     */
    List<DictItemOptionVO> getDictItemOptions(String dictCode);

    /**
     * 辞書項目フォーム取得
     *
     * @param itemId 辞書項目ID
     * @return フォーム
     */
    DictItemForm getDictItemForm(Long itemId);

    /**
     * 辞書項目保存
     *
     * @param form フォーム
     * @return 成功フラグ
     */
    boolean saveDictItem(DictItemForm form);

    /**
     * 辞書項目更新
     *
     * @param form フォーム
     * @return 成功フラグ
     */
    boolean updateDictItem(DictItemForm form);

    /**
     * 辞書項目削除
     *
     * @param ids 辞書項目IDリスト
     */
    void deleteDictItemByIds(List<Long> ids);

    /**
     * 辞書コードで辞書項目削除
     *
     * @param dictCode 辞書コード
     */
    void deleteByDictCode(String dictCode);
}
