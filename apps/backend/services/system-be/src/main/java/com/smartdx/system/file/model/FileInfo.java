package com.smartdx.system.file.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * ファイル情報
 */
@Schema(description = "ファイル情報")
@Data
public class FileInfo {

    @Schema(description = "ファイル名")
    private String name;

    @Schema(description = "ファイルURL")
    private String url;
}
