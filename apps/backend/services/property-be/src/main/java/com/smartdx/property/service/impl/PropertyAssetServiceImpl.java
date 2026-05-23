package com.smartdx.property.service.impl;

import cn.hutool.core.util.StrUtil;
import com.smartdx.core.exception.BusinessException;
import com.smartdx.property.file.MinioFileService;
import com.smartdx.property.exception.PropertyErrorCode;
import com.smartdx.property.model.vo.PropertyAssetSignedUrlVO;
import com.smartdx.property.service.PropertyAssetService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

/**
 * LST-AST-01/02/03: アセット署名付きURL取得サービス実装
 */
@Service
@Slf4j
public class PropertyAssetServiceImpl implements PropertyAssetService {

    private final ObjectProvider<MinioFileService> minioFileServiceProvider;

    public PropertyAssetServiceImpl(ObjectProvider<MinioFileService> minioFileServiceProvider) {
        this.minioFileServiceProvider = minioFileServiceProvider;
    }

    @Override
    public PropertyAssetSignedUrlVO getOriginalUrl(String assetKey) {
        validateAssetKey(assetKey);

        // オブジェクトパス: assets/{assetKey}/original
        String objectPath = buildAssetPath(assetKey, "original");
        String mimeType = detectMimeType(assetKey);

        String url = getPresignedUrl(objectPath);

        return PropertyAssetSignedUrlVO.builder()
                .url(url)
                .mimeType(mimeType)
                .contentDisposition("inline")
                .build();
    }

    @Override
    public PropertyAssetSignedUrlVO getThumbnailUrl(String assetKey, String size) {
        validateAssetKey(assetKey);
        validateThumbnailSize(size);

        // オブジェクトパス: assets/{assetKey}/thumb_{size}
        String objectPath = buildAssetPath(assetKey, "thumb_" + size);
        String mimeType = "image/jpeg"; // サムネイルは常にJPEG

        String url = getPresignedUrl(objectPath);

        return PropertyAssetSignedUrlVO.builder()
                .url(url)
                .mimeType(mimeType)
                .contentDisposition("inline")
                .build();
    }

    @Override
    public PropertyAssetSignedUrlVO getDocumentUrl(String docKey) {
        validateDocKey(docKey);

        // オブジェクトパス: docs/{docKey}
        String objectPath = "docs/" + docKey;
        String mimeType = detectDocMimeType(docKey);

        String url = getPresignedUrl(objectPath);

        return PropertyAssetSignedUrlVO.builder()
                .url(url)
                .mimeType(mimeType)
                .contentDisposition("attachment")
                .build();
    }

    private void validateAssetKey(String assetKey) {
        if (StrUtil.isBlank(assetKey)) {
            throw new BusinessException(PropertyErrorCode.VALIDATION_ERROR, "assetKey は必須です");
        }
        // UUID形式のバリデーション
        if (!assetKey.matches("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$")) {
            throw new BusinessException(PropertyErrorCode.VALIDATION_ERROR, "assetKey の形式が不正です");
        }
    }

    private void validateDocKey(String docKey) {
        if (StrUtil.isBlank(docKey)) {
            throw new BusinessException(PropertyErrorCode.VALIDATION_ERROR, "docKey は必須です");
        }
        // path traversal防止: ".." や "/" 始まりを禁止
        if (docKey.contains("..") || docKey.startsWith("/") || docKey.contains("//")) {
            throw new BusinessException(PropertyErrorCode.VALIDATION_ERROR, "docKey に不正な文字が含まれています");
        }
    }

    private void validateThumbnailSize(String size) {
        if (!"sm".equals(size) && !"md".equals(size) && !"lg".equals(size)) {
            throw new BusinessException(PropertyErrorCode.VALIDATION_ERROR, "size は sm, md, lg のいずれかを指定してください");
        }
    }

    private String buildAssetPath(String assetKey, String variant) {
        return "assets/" + assetKey + "/" + variant;
    }

    private String getPresignedUrl(String objectPath) {
        MinioFileService minioFileService = minioFileServiceProvider.getIfAvailable();
        if (minioFileService == null) {
            log.error("MinioFileService is not available");
            throw new BusinessException(PropertyErrorCode.INTERNAL_ERROR);
        }

        // 存在確認
        if (!minioFileService.exists(objectPath)) {
            throw new BusinessException(PropertyErrorCode.ASSET_NOT_FOUND);
        }

        try {
            return minioFileService.getPresignedUrl(objectPath);
        } catch (Exception e) {
            log.error("Failed to generate presigned URL for: {}", objectPath, e);
            throw new BusinessException(PropertyErrorCode.INTERNAL_ERROR);
        }
    }

    private String detectMimeType(String assetKey) {
        // TODO: OpenSearchまたはDBからアセットのMIMEタイプを取得
        // 暫定的にimage/jpegを返す
        return "image/jpeg";
    }

    private String detectDocMimeType(String docKey) {
        // ファイル拡張子からMIMEタイプを推定
        if (docKey.endsWith(".pdf")) {
            return "application/pdf";
        } else if (docKey.endsWith(".docx")) {
            return "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
        } else if (docKey.endsWith(".xlsx")) {
            return "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
        } else if (docKey.endsWith(".pptx")) {
            return "application/vnd.openxmlformats-officedocument.presentationml.presentation";
        } else {
            return "application/octet-stream";
        }
    }
}
