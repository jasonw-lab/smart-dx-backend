package com.smartdx.property.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 画像情報
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "画像情報")
public class PropertyAssetVO {

    @Schema(description = "画像キー（SHA-256 プレフィクス）")
    private String assetKey;

    @Schema(description = "画像種別: main, sub, layout")
    private String flag;

    @Schema(description = "MIME タイプ")
    private String mimeType;

    @Schema(description = "ファイルサイズ（バイト）")
    private Long sizeBytes;

    /**
     * 後方互換用コンストラクタ
     */
    public PropertyAssetVO(String assetKey, String flag) {
        this.assetKey = assetKey;
        this.flag = flag;
    }
}
