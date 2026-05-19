package com.smartdx.property.model.req;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PropertyLookupReq {

    @Pattern(regexp = "^(published|draft|all)$", message = "scope must be published, draft, or all")
    private String scope = "published";

    @Size(max = 200, message = "keyword must be less than or equal to 200 characters")
    private String keyword;

    @Valid
    private PropertySearchItemsReq searchItems;

    private String sortBy = "listedDate";

    @Pattern(regexp = "^(asc|desc)$", message = "orderBy must be asc or desc")
    private String orderBy = "desc";

    @Min(value = 0, message = "page must be greater than or equal to 0")
    private int page = 0;

    @Min(value = 1, message = "size must be greater than or equal to 1")
    @Max(value = 1000, message = "size must be less than or equal to 1000")
    private int size = 20;

    // === 類似画像検索パラメータ ===

    /**
     * 既存アセットキー（類似検索の起点）
     * embeddingRef と排他
     */
    private String referenceAsset;

    /**
     * 画像種別（main / sub / layout）
     * referenceAsset 指定時は必須
     */
    @Pattern(regexp = "^(main|sub|layout)$", message = "referenceFlag must be main, sub, or layout")
    private String referenceFlag;

    /**
     * LST-EMB-01 から取得した短命参照
     * referenceAsset と排他、flag 内包
     */
    private String embeddingRef;

    /**
     * 類似検索の取得上限（類似検索時のみ有効）
     */
    @Min(value = 1, message = "topK must be greater than or equal to 1")
    @Max(value = 200, message = "topK must be less than or equal to 200")
    private Integer topK;

    /**
     * 類似画像検索が有効かどうか
     */
    public boolean isSimilaritySearchEnabled() {
        return referenceAsset != null || embeddingRef != null;
    }
}
