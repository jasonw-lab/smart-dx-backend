package com.smartdx.system.converter;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.smartdx.system.model.entity.Notice;
import com.smartdx.system.model.form.NoticeForm;
import com.smartdx.system.model.vo.NoticeDetailVO;
import com.smartdx.system.model.vo.NoticePageVO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

/**
 * 通知コンバーター
 */
@Mapper(componentModel = "spring")
public interface NoticeConverter {

    @Mapping(target = "publisherName", ignore = true)
    @Mapping(target = "isRead", ignore = true)
    NoticePageVO toPageVo(Notice notice);

    default Page<NoticePageVO> toPageVo(Page<Notice> page) {
        Page<NoticePageVO> result = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        result.setRecords(page.getRecords().stream().map(this::toPageVo).toList());
        return result;
    }

    @Mapping(target = "targetUserIds", expression = "java(toTargetUserIds(notice.getTargetUserIds()))")
    NoticeForm toForm(Notice notice);

    @Mapping(target = "targetUserIds", expression = "java(toTargetUserIdsString(form.getTargetUserIds()))")
    Notice toEntity(NoticeForm form);

    @Mapping(target = "publisherName", ignore = true)
    NoticeDetailVO toDetailVo(Notice notice);

    default List<String> toTargetUserIds(String targetUserIds) {
        if (targetUserIds == null || targetUserIds.isBlank()) {
            return null;
        }
        return List.of(targetUserIds.split(","));
    }

    default String toTargetUserIdsString(List<String> targetUserIds) {
        if (targetUserIds == null || targetUserIds.isEmpty()) {
            return null;
        }
        return String.join(",", targetUserIds);
    }
}
