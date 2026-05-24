package com.smartdx.system.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.smartdx.core.result.PageResult;
import com.smartdx.core.result.Result;
import com.smartdx.system.model.form.NoticeForm;
import com.smartdx.system.model.query.NoticeQuery;
import com.smartdx.system.model.vo.NoticeDetailVO;
import com.smartdx.system.model.vo.NoticePageVO;
import com.smartdx.system.model.vo.UserNoticePageVO;
import com.smartdx.system.service.NoticeService;
import com.smartdx.system.service.UserNoticeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * 通知コントローラー
 */
@Tag(name = "08.通知API")
@RestController
@RequestMapping("/api/v1/notices")
@RequiredArgsConstructor
public class NoticeController {

    private final NoticeService noticeService;
    private final UserNoticeService userNoticeService;

    @Operation(summary = "通知ページリスト")
    @GetMapping
    @PreAuthorize("@ss.hasPerm('sys:notice:list')")
    public PageResult<NoticePageVO> getNoticePage(NoticeQuery query) {
        Page<NoticePageVO> result = noticeService.getNoticePage(query);
        return PageResult.success(result);
    }

    @Operation(summary = "通知新規作成")
    @PostMapping
    @PreAuthorize("@ss.hasPerm('sys:notice:create')")
    public Result<?> saveNotice(@RequestBody @Valid NoticeForm form) {
        boolean result = noticeService.saveNotice(form);
        return Result.judge(result);
    }

    @Operation(summary = "通知フォーム取得")
    @GetMapping("/{id}/form")
    @PreAuthorize("@ss.hasPerm('sys:notice:update')")
    public Result<NoticeForm> getNoticeForm(
            @Parameter(description = "通知ID") @PathVariable Long id
    ) {
        NoticeForm formData = noticeService.getNoticeForm(id);
        return Result.success(formData);
    }

    @Operation(summary = "通知詳細取得")
    @GetMapping("/{id}/detail")
    public Result<NoticeDetailVO> getNoticeDetail(
            @Parameter(description = "通知ID") @PathVariable Long id
    ) {
        NoticeDetailVO detailVo = noticeService.getNoticeDetail(id);
        return Result.success(detailVo);
    }

    @Operation(summary = "通知更新")
    @PutMapping("/{id}")
    @PreAuthorize("@ss.hasPerm('sys:notice:update')")
    public Result<Void> updateNotice(
            @Parameter(description = "通知ID") @PathVariable Long id,
            @RequestBody @Valid NoticeForm form
    ) {
        boolean result = noticeService.updateNotice(id, form);
        return Result.judge(result);
    }

    @Operation(summary = "通知発行")
    @PutMapping("/{id}/publish")
    @PreAuthorize("@ss.hasPerm('sys:notice:publish')")
    public Result<Void> publishNotice(
            @Parameter(description = "通知ID") @PathVariable Long id
    ) {
        boolean result = noticeService.publishNotice(id);
        return Result.judge(result);
    }

    @Operation(summary = "通知取り消し")
    @PutMapping("/{id}/revoke")
    @PreAuthorize("@ss.hasPerm('sys:notice:revoke')")
    public Result<Void> revokeNotice(
            @Parameter(description = "通知ID") @PathVariable Long id
    ) {
        boolean result = noticeService.revokeNotice(id);
        return Result.judge(result);
    }

    @Operation(summary = "通知削除")
    @DeleteMapping("/{ids}")
    @PreAuthorize("@ss.hasPerm('sys:notice:delete')")
    public Result<Void> deleteNotices(
            @Parameter(description = "通知ID（複数はカンマ区切り）") @PathVariable String ids
    ) {
        boolean result = noticeService.deleteNotices(ids);
        return Result.judge(result);
    }

    @Operation(summary = "全て既読にする")
    @PutMapping("/read-all")
    public Result<Void> readAll() {
        userNoticeService.readAll();
        return Result.success();
    }

    @Operation(summary = "自分の通知ページリスト")
    @GetMapping("/my")
    public PageResult<UserNoticePageVO> getMyNoticePage(NoticeQuery query) {
        Page<UserNoticePageVO> result = noticeService.getMyNoticePage(query);
        return PageResult.success(result);
    }
}
