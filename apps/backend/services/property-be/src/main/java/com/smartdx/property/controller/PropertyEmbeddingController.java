package com.smartdx.property.controller;

import com.smartdx.core.result.Result;
import com.smartdx.property.model.vo.PropertyEmbeddingVO;
import com.smartdx.property.service.PropertyEmbeddingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * LST-EMB-01: 画像特徴量抽出 API
 */
@Tag(name = "画像特徴量抽出", description = "画像から特徴量を抽出し、類似検索用のembeddingRefを発行する")
@RestController
@RequestMapping("/api/v1/properties/embeddings")
@RequiredArgsConstructor
@Validated
@Slf4j
public class PropertyEmbeddingController {

    private final PropertyEmbeddingService propertyEmbeddingService;

    @Operation(
            summary = "画像特徴量抽出",
            description = "アップロードされた画像から特徴量を抽出し、15分有効のembeddingRefを発行する。" +
                    "1ユーザーあたり最大100件まで。古いものから自動削除（FIFO）。"
    )
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Result<PropertyEmbeddingVO> extractEmbedding(
            @Parameter(description = "画像ファイル（JPG/PNG、10MB以下）", required = true)
            @RequestPart("image")
            @NotNull(message = "image は必須です")
            MultipartFile image,

            @Parameter(description = "画像種別: main（メイン画像）, sub（サブ画像）, layout（間取り図）", required = true)
            @RequestParam("flag")
            @NotNull(message = "flag は必須です")
            @Pattern(regexp = "^(main|sub|layout)$", message = "flag は main, sub, layout のいずれかを指定してください")
            String flag
    ) {
        log.info("API call start: POST /api/v1/properties/embeddings. flag={}, imageSize={}",
                flag, image != null ? image.getSize() : 0);

        PropertyEmbeddingVO result = propertyEmbeddingService.extractAndSave(image, flag);

        log.info("API call end: POST /api/v1/properties/embeddings. embeddingRef={}, expiresAt={}",
                result.getEmbeddingRef(), result.getExpiresAt());

        return Result.success(result);
    }
}
