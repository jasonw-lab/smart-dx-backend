package com.smartdx.property.model.req;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 物件登録リクエスト DTO
 * LST-INT-01: POST /api/v1/properties
 */
@Data
@Schema(description = "物件登録リクエスト")
public class PropertyCreateReq {

    @Schema(description = "掲載年（2000〜現在年）", example = "2026", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "掲載年は必須です")
    @Min(value = 2000, message = "掲載年は2000年以降で指定してください")
    @Max(value = 2100, message = "掲載年の値が不正です")
    private Integer listedYear;

    @Schema(description = "エリアコード", example = "tokyo-shibuya", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "エリアコードは必須です")
    private String area;
}
