package com.smartdx.property.model.vo;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

/**
 * LST-AST-01/02/03: アセット署名付きURL レスポンス
 */
@Getter
@Setter
@Builder
public class PropertyAssetSignedUrlVO {

    /**
     * 署名付きURL（60分有効）
     */
    private String url;

    /**
     * MIME タイプ
     */
    private String mimeType;

    /**
     * Content-Disposition ヘッダー値
     */
    private String contentDisposition;
}
