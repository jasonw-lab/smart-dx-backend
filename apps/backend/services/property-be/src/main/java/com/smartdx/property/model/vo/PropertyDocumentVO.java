package com.smartdx.property.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 文書情報
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "文書情報")
public class PropertyDocumentVO {

    @Schema(description = "文書キー（SHA-256 プレフィクス）")
    private String docKey;

    @Schema(description = "文書種別: introduction/summary/important/contract/valuation")
    private String docType;

    @Schema(description = "表示ファイル名")
    private String fileName;

    @Schema(description = "ファイルサイズ（バイト）")
    private Long sizeBytes;
}
