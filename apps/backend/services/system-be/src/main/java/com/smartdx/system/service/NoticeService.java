package com.smartdx.system.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.smartdx.system.model.entity.Notice;
import com.smartdx.system.model.form.NoticeForm;
import com.smartdx.system.model.query.NoticeQuery;
import com.smartdx.system.model.vo.NoticeDetailVO;
import com.smartdx.system.model.vo.NoticePageVO;
import com.smartdx.system.model.vo.UserNoticePageVO;

/**
 * 通知サービス
 */
public interface NoticeService extends IService<Notice> {

    /**
     * 通知ページリスト取得
     *
     * @param query クエリパラメータ
     * @return ページリスト
     */
    Page<NoticePageVO> getNoticePage(NoticeQuery query);

    /**
     * 通知フォーム取得
     *
     * @param id 通知ID
     * @return フォーム
     */
    NoticeForm getNoticeForm(Long id);

    /**
     * 通知保存
     *
     * @param form フォーム
     * @return 成功フラグ
     */
    boolean saveNotice(NoticeForm form);

    /**
     * 通知更新
     *
     * @param id   通知ID
     * @param form フォーム
     * @return 成功フラグ
     */
    boolean updateNotice(Long id, NoticeForm form);

    /**
     * 通知削除
     *
     * @param ids 通知ID（カンマ区切り）
     * @return 成功フラグ
     */
    boolean deleteNotices(String ids);

    /**
     * 通知発行
     *
     * @param id 通知ID
     * @return 成功フラグ
     */
    boolean publishNotice(Long id);

    /**
     * 通知取り消し
     *
     * @param id 通知ID
     * @return 成功フラグ
     */
    boolean revokeNotice(Long id);

    /**
     * 通知詳細取得
     *
     * @param id 通知ID
     * @return 詳細VO
     */
    NoticeDetailVO getNoticeDetail(Long id);

    /**
     * 自分の通知ページリスト取得
     *
     * @param query クエリパラメータ
     * @return ページリスト
     */
    Page<UserNoticePageVO> getMyNoticePage(NoticeQuery query);
}
