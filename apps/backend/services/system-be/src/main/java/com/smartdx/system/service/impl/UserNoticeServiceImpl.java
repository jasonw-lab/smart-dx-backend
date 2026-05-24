package com.smartdx.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.smartdx.security.util.SecurityUtils;
import com.smartdx.system.mapper.UserNoticeMapper;
import com.smartdx.system.model.entity.UserNotice;
import com.smartdx.system.service.UserNoticeService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * ユーザー通知サービス実装
 */
@Service
public class UserNoticeServiceImpl extends ServiceImpl<UserNoticeMapper, UserNotice> implements UserNoticeService {

    @Override
    public void readAll() {
        Long userId = SecurityUtils.getUserId();
        this.update(
                new LambdaUpdateWrapper<UserNotice>()
                        .eq(UserNotice::getUserId, userId)
                        .eq(UserNotice::getIsRead, 0)
                        .set(UserNotice::getIsRead, 1)
                        .set(UserNotice::getReadTime, LocalDateTime.now())
        );
    }

    @Override
    public void markAsRead(Long noticeId, Long userId) {
        UserNotice userNotice = this.getOne(
                new LambdaQueryWrapper<UserNotice>()
                        .eq(UserNotice::getNoticeId, noticeId)
                        .eq(UserNotice::getUserId, userId)
        );

        if (userNotice == null) {
            // 新規作成（既読）
            userNotice = new UserNotice();
            userNotice.setNoticeId(noticeId);
            userNotice.setUserId(userId);
            userNotice.setIsRead(1);
            userNotice.setReadTime(LocalDateTime.now());
            this.save(userNotice);
        } else if (userNotice.getIsRead() == 0) {
            // 既読に更新
            userNotice.setIsRead(1);
            userNotice.setReadTime(LocalDateTime.now());
            this.updateById(userNotice);
        }
    }
}
