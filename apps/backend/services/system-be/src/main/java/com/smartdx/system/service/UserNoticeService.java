package com.smartdx.system.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.smartdx.system.model.entity.UserNotice;

/**
 * ユーザー通知サービス
 */
public interface UserNoticeService extends IService<UserNotice> {

    /**
     * 全て既読にする
     */
    void readAll();

    /**
     * 通知を既読にする
     *
     * @param noticeId 通知ID
     * @param userId   ユーザーID
     */
    void markAsRead(Long noticeId, Long userId);
}
