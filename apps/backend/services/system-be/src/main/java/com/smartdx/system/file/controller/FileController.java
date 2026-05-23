package com.smartdx.system.file.controller;

import com.smartdx.core.result.Result;
import com.smartdx.system.file.model.FileInfo;
import com.smartdx.system.file.service.FileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * ファイルコントローラー
 */
@Tag(name = "10.ファイル")
@RestController
@RequestMapping("/api/v1/files")
@RequiredArgsConstructor
public class FileController {

    private final FileService fileService;

    @PostMapping
    @Operation(summary = "ファイルアップロード")
    public Result<FileInfo> uploadFile(
            @Parameter(
                    name = "file",
                    description = "フォームファイルオブジェクト",
                    required = true,
                    in = ParameterIn.DEFAULT,
                    schema = @Schema(name = "file", format = "binary")
            )
            @RequestPart(value = "file") MultipartFile file
    ) {
        FileInfo fileInfo = fileService.uploadFile(file);
        return Result.success(fileInfo);
    }

    @DeleteMapping
    @Operation(summary = "ファイル削除")
    @SneakyThrows
    public Result<?> deleteFile(
            @Parameter(description = "ファイルパス") @RequestParam String filePath
    ) {
        boolean result = fileService.deleteFile(filePath);
        return Result.judge(result);
    }
}
