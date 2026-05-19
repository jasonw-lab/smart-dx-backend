package com.smartdx.property.controller;

import com.smartdx.core.result.Result;
import com.smartdx.property.model.req.SimilarPropertyReq;
import com.smartdx.property.model.vo.SimilarPropertyResponse;
import com.smartdx.property.service.SimilarPropertyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * LST-SIM-01: 類似物件検索 API Controller（v2.0.0）
 * 複数検索起点対応: propertyId, imageId, demoRef
 */
@Tag(name = "類似物件検索", description = "OpenSearch k-NN を使った類似物件検索")
@RestController
@RequestMapping("/api/v1/properties")
@RequiredArgsConstructor
@Validated
@Slf4j
public class SimilarPropertyController {

    private final SimilarPropertyService similarPropertyService;

    /**
     * 統一エンドポイント（v2.0.0）
     * 複数検索起点に対応: propertyId, imageId, demoRef
     */
    @Operation(
            summary = "類似物件検索（統一エンドポイント）",
            description = "複数の検索起点（propertyId/imageId/demoRef）から類似物件を検索。" +
                    "OpenSearch k-NN を使用。"
    )
    @GetMapping("/similar")
    public Result<SimilarPropertyResponse> findSimilarUnified(
            @Parameter(description = "基準物件キー（UUID v4）")
            @RequestParam(required = false)
            @Pattern(regexp = "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$",
                    message = "物件キーの形式が不正です")
            String propertyId,

            @Parameter(description = "アップロード画像ID（UUID v4）※Phase 2")
            @RequestParam(required = false)
            @Pattern(regexp = "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$",
                    message = "画像IDの形式が不正です")
            String imageId,

            @Parameter(description = "DEMO用固定参照キー（demo-* prefix）")
            @RequestParam(required = false)
            @Pattern(regexp = "^demo-[a-zA-Z0-9-]+$",
                    message = "demoRefの形式が不正です")
            String demoRef,

            @Parameter(description = "取得件数（1〜50、デフォルト: 20）")
            @RequestParam(defaultValue = "20")
            @Min(value = 1, message = "limit は 1 以上である必要があります")
            @Max(value = 50, message = "limit は 50 以下である必要があります")
            Integer limit,

            @Parameter(description = "最低類似度スコア（0.0〜1.0、デフォルト: 0.0）")
            @RequestParam(defaultValue = "0.0")
            @Min(value = 0, message = "minScore は 0 以上である必要があります")
            @Max(value = 1, message = "minScore は 1 以下である必要があります")
            Double minScore,

            @Parameter(description = "検索起点と同一物件を除外（デフォルト: true）")
            @RequestParam(defaultValue = "true")
            Boolean excludeSameProperty
    ) {
        log.info("API call start: GET /api/v1/properties/similar. propertyId={}, imageId={}, demoRef={}, limit={}, minScore={}",
                propertyId, imageId, demoRef, limit, minScore);

        SimilarPropertyReq req = new SimilarPropertyReq();
        req.setPropertyId(propertyId);
        req.setImageId(imageId);
        req.setDemoRef(demoRef);
        req.setLimit(limit);
        req.setMinScore(minScore);
        req.setExcludeSameProperty(excludeSameProperty);

        SimilarPropertyResponse response = similarPropertyService.findSimilar(req);

        log.info("API call end: GET /api/v1/properties/similar. total={}", response.getTotal());

        return Result.success(response);
    }

    /**
     * 旧エンドポイント（後方互換性のため維持）
     * @deprecated 新しい /similar エンドポイントを使用してください
     */
    @Operation(
            summary = "類似物件検索（旧エンドポイント）",
            description = "指定した基準物件に類似した物件を検索。後方互換性のため維持。"
    )
    @GetMapping("/{propertyKey}/similar")
    @Deprecated
    public Result<SimilarPropertyResponse> findSimilarLegacy(
            @Parameter(description = "基準物件キー（UUID v4）", required = true)
            @PathVariable("propertyKey")
            @Pattern(regexp = "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$",
                    message = "物件キーの形式が不正です")
            String propertyKey,

            @Parameter(description = "取得件数（1〜50、デフォルト: 20）")
            @RequestParam(defaultValue = "20")
            @Min(value = 1, message = "topK は 1 以上である必要があります")
            @Max(value = 50, message = "topK は 50 以下である必要があります")
            Integer topK,

            @Parameter(description = "最低類似度スコア（0.0〜1.0、デフォルト: 0.0）")
            @RequestParam(defaultValue = "0.0")
            @Min(value = 0, message = "minScore は 0 以上である必要があります")
            @Max(value = 1, message = "minScore は 1 以下である必要があります")
            Double minScore
    ) {
        log.info("API call start (legacy): GET /api/v1/properties/{}/similar. topK={}, minScore={}",
                propertyKey, topK, minScore);

        SimilarPropertyReq req = new SimilarPropertyReq();
        req.setPropertyId(propertyKey);
        req.setLimit(topK);
        req.setMinScore(minScore);
        req.setExcludeSameProperty(true);

        SimilarPropertyResponse response = similarPropertyService.findSimilar(req);

        log.info("API call end (legacy): GET /api/v1/properties/{}/similar. total={}",
                propertyKey, response.getTotal());

        return Result.success(response);
    }
}
