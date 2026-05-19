package com.smartdx.property.controller;

import com.smartdx.core.result.Result;
import com.smartdx.property.model.vo.PropertyAssetSignedUrlVO;
import com.smartdx.property.service.PropertyAssetService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotBlank;
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
 * LST-AST-01/02/03: アセット署名付きURL取得 API
 */
@Tag(name = "アセット取得", description = "画像・ドキュメントの署名付きURLを取得する")
@RestController
@RequestMapping("/api/v1/properties/assets")
@RequiredArgsConstructor
@Validated
@Slf4j
public class PropertyAssetController {

    private final PropertyAssetService propertyAssetService;

    @Operation(
            summary = "原寸画像URL取得",
            description = "LST-AST-01: 指定されたアセットの原寸画像への署名付きURL（60分有効）を取得する"
    )
    @GetMapping("/{assetKey}/original")
    public Result<PropertyAssetSignedUrlVO> getOriginalUrl(
            @Parameter(description = "アセットキー（UUID v4）", required = true)
            @PathVariable("assetKey")
            @NotBlank(message = "assetKey は必須です")
            @Pattern(regexp = "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$",
                    message = "assetKey の形式が不正です")
            String assetKey
    ) {
        log.info("API call start: GET /api/v1/properties/assets/{}/original", assetKey);

        PropertyAssetSignedUrlVO result = propertyAssetService.getOriginalUrl(assetKey);

        log.info("API call end: GET /api/v1/properties/assets/{}/original", assetKey);

        return Result.success(result);
    }

    @Operation(
            summary = "サムネイル画像URL取得",
            description = "LST-AST-02: 指定されたアセットのサムネイル画像への署名付きURL（60分有効）を取得する"
    )
    @GetMapping("/{assetKey}/thumbnail")
    public Result<PropertyAssetSignedUrlVO> getThumbnailUrl(
            @Parameter(description = "アセットキー（UUID v4）", required = true)
            @PathVariable("assetKey")
            @NotBlank(message = "assetKey は必須です")
            @Pattern(regexp = "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$",
                    message = "assetKey の形式が不正です")
            String assetKey,

            @Parameter(description = "サムネイルサイズ: sm（120px）, md（300px）, lg（600px）", required = true)
            @RequestParam("size")
            @NotBlank(message = "size は必須です")
            @Pattern(regexp = "^(sm|md|lg)$", message = "size は sm, md, lg のいずれかを指定してください")
            String size
    ) {
        log.info("API call start: GET /api/v1/properties/assets/{}/thumbnail?size={}", assetKey, size);

        PropertyAssetSignedUrlVO result = propertyAssetService.getThumbnailUrl(assetKey, size);

        log.info("API call end: GET /api/v1/properties/assets/{}/thumbnail?size={}", assetKey, size);

        return Result.success(result);
    }

    @Operation(
            summary = "ドキュメントURL取得",
            description = "LST-AST-03: 指定されたドキュメントへの署名付きURL（60分有効）を取得する"
    )
    @GetMapping("/docs/{docKey}")
    public Result<PropertyAssetSignedUrlVO> getDocumentUrl(
            @Parameter(description = "ドキュメントキー", required = true)
            @PathVariable("docKey")
            @NotBlank(message = "docKey は必須です")
            String docKey
    ) {
        log.info("API call start: GET /api/v1/properties/assets/docs/{}", docKey);

        PropertyAssetSignedUrlVO result = propertyAssetService.getDocumentUrl(docKey);

        log.info("API call end: GET /api/v1/properties/assets/docs/{}", docKey);

        return Result.success(result);
    }
}
