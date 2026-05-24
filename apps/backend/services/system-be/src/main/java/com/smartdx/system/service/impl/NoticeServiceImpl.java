package com.smartdx.system.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.smartdx.core.exception.BusinessException;
import com.smartdx.security.util.SecurityUtils;
import com.smartdx.system.converter.NoticeConverter;
import com.smartdx.system.mapper.NoticeMapper;
import com.smartdx.system.mapper.UserMapper;
import com.smartdx.system.model.entity.Notice;
import com.smartdx.system.model.entity.User;
import com.smartdx.system.model.form.NoticeForm;
import com.smartdx.system.model.query.NoticeQuery;
import com.smartdx.system.model.vo.NoticeDetailVO;
import com.smartdx.system.model.vo.NoticePageVO;
import com.smartdx.system.model.vo.UserNoticePageVO;
import com.smartdx.system.service.NoticeService;
import com.smartdx.system.service.UserNoticeService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 通知サービス実装
 */
@Service
@RequiredArgsConstructor
public class NoticeServiceImpl extends ServiceImpl<NoticeMapper, Notice> implements NoticeService {

    private final NoticeConverter noticeConverter;
    private final UserNoticeService userNoticeService;
    private final UserMapper userMapper;
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Override
    public Page<NoticePageVO> getNoticePage(NoticeQuery query) {
        Page<Notice> page = new Page<>(query.getPageNum(), query.getPageSize());

        LambdaQueryWrapper<Notice> wrapper = new LambdaQueryWrapper<>();

        if (StrUtil.isNotBlank(query.getTitle())) {
            wrapper.like(Notice::getTitle, query.getTitle());
        }
        if (query.getPublishStatus() != null) {
            wrapper.eq(Notice::getPublishStatus, query.getPublishStatus());
        }
        if (CollUtil.isNotEmpty(query.getPublishTime()) && query.getPublishTime().size() == 2) {
            LocalDateTime startTime = LocalDateTime.parse(query.getPublishTime().get(0), FORMATTER);
            LocalDateTime endTime = LocalDateTime.parse(query.getPublishTime().get(1), FORMATTER);
            wrapper.between(Notice::getPublishTime, startTime, endTime);
        }

        wrapper.orderByDesc(Notice::getCreateTime);

        Page<Notice> result = this.page(page, wrapper);
        Page<NoticePageVO> voPage = noticeConverter.toPageVo(result);

        // 発行者名を設定
        if (CollUtil.isNotEmpty(voPage.getRecords())) {
            List<Long> publisherIds = result.getRecords().stream()
                    .map(Notice::getPublisherId)
                    .filter(id -> id != null)
                    .distinct()
                    .toList();
            if (CollUtil.isNotEmpty(publisherIds)) {
                List<User> users = userMapper.selectBatchIds(publisherIds);
                Map<Long, String> userMap = users.stream()
                        .collect(Collectors.toMap(User::getId, User::getNickname));
                for (int i = 0; i < voPage.getRecords().size(); i++) {
                    Long publisherId = result.getRecords().get(i).getPublisherId();
                    if (publisherId != null) {
                        voPage.getRecords().get(i).setPublisherName(userMap.get(publisherId));
                    }
                }
            }
        }

        return voPage;
    }

    @Override
    public NoticeForm getNoticeForm(Long id) {
        Notice entity = this.getById(id);
        if (entity == null) {
            throw new BusinessException("通知が存在しません");
        }
        return noticeConverter.toForm(entity);
    }

    @Override
    public boolean saveNotice(NoticeForm form) {
        Notice entity = noticeConverter.toEntity(form);
        entity.setPublishStatus(0); // 未発行
        entity.setCreateBy(SecurityUtils.getUserId());
        return this.save(entity);
    }

    @Override
    public boolean updateNotice(Long id, NoticeForm form) {
        Notice entity = this.getById(id);
        if (entity == null) {
            throw new BusinessException("通知が存在しません");
        }
        if (entity.getPublishStatus() == 1) {
            throw new BusinessException("発行済みの通知は編集できません");
        }

        Notice notice = noticeConverter.toEntity(form);
        notice.setId(id);
        notice.setUpdateBy(SecurityUtils.getUserId());
        return this.updateById(notice);
    }

    @Override
    @Transactional
    public boolean deleteNotices(String ids) {
        List<Long> idList = Arrays.stream(ids.split(","))
                .map(Long::parseLong)
                .toList();
        return this.removeByIds(idList);
    }

    @Override
    public boolean publishNotice(Long id) {
        Notice entity = this.getById(id);
        if (entity == null) {
            throw new BusinessException("通知が存在しません");
        }
        if (entity.getPublishStatus() == 1) {
            throw new BusinessException("既に発行済みです");
        }

        entity.setPublishStatus(1);
        entity.setPublishTime(LocalDateTime.now());
        entity.setPublisherId(SecurityUtils.getUserId());
        return this.updateById(entity);
    }

    @Override
    public boolean revokeNotice(Long id) {
        Notice entity = this.getById(id);
        if (entity == null) {
            throw new BusinessException("通知が存在しません");
        }
        if (entity.getPublishStatus() != 1) {
            throw new BusinessException("発行済みの通知のみ取り消し可能です");
        }

        entity.setPublishStatus(-1);
        entity.setRevokeTime(LocalDateTime.now());
        return this.updateById(entity);
    }

    @Override
    public NoticeDetailVO getNoticeDetail(Long id) {
        Notice entity = this.getById(id);
        if (entity == null) {
            throw new BusinessException("通知が存在しません");
        }

        NoticeDetailVO vo = noticeConverter.toDetailVo(entity);

        // 発行者名を設定
        if (entity.getPublisherId() != null) {
            User user = userMapper.selectById(entity.getPublisherId());
            if (user != null) {
                vo.setPublisherName(user.getNickname());
            }
        }

        // 既読にマーク
        Long userId = SecurityUtils.getUserId();
        if (userId != null) {
            userNoticeService.markAsRead(id, userId);
        }

        return vo;
    }

    @Override
    public Page<UserNoticePageVO> getMyNoticePage(NoticeQuery query) {
        Long userId = SecurityUtils.getUserId();
        query.setUserId(userId);

        Page<Notice> page = new Page<>(query.getPageNum(), query.getPageSize());

        // 発行済み通知のみ（全員向け or 自分宛て）
        LambdaQueryWrapper<Notice> wrapper = new LambdaQueryWrapper<Notice>()
                .eq(Notice::getPublishStatus, 1)
                .and(w -> w
                        .eq(Notice::getTargetType, 1) // 全員向け
                        .or()
                        .apply("FIND_IN_SET({0}, target_user_ids) > 0", userId.toString())
                );

        if (StrUtil.isNotBlank(query.getTitle())) {
            wrapper.like(Notice::getTitle, query.getTitle());
        }

        wrapper.orderByDesc(Notice::getPublishTime);

        Page<Notice> result = this.page(page, wrapper);

        Page<UserNoticePageVO> voPage = new Page<>(page.getCurrent(), page.getSize(), result.getTotal());
        List<UserNoticePageVO> records = result.getRecords().stream().map(notice -> {
            UserNoticePageVO vo = new UserNoticePageVO();
            vo.setId(notice.getId());
            vo.setTitle(notice.getTitle());
            vo.setType(notice.getType());
            vo.setLevel(notice.getLevel());
            vo.setPublishTime(notice.getPublishTime());
            return vo;
        }).toList();
        voPage.setRecords(records);

        return voPage;
    }
}
