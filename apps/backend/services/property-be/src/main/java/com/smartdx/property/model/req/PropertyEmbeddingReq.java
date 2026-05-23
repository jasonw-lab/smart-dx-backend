package com.smartdx.property.model.req;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;
import org.springframework.web.multipart.MultipartFile;

/**
 * LST-EMB-01: 画像特徴量抽出リクエスト
 */
@Getter
@Setter
public class PropertyEmbeddingReq {

    /**
     * 画像種別: main / sub / layout
     */
    @NotNull(message = "flag は必須です")
    @Pattern(regexp = "^(main|sub|layout)$", message = "flag は main, sub, layout のいずれかを指定してください")
    private String flag;
}
