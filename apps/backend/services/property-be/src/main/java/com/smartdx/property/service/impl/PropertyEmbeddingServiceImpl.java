package com.smartdx.property.service.impl;

import cn.hutool.core.util.IdUtil;
import cn.hutool.crypto.digest.DigestUtil;
import com.smartdx.core.exception.BusinessException;
import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartdx.security.util.SecurityUtils;
import com.smartdx.tenant.TenantContextHolder;
import com.smartdx.property.exception.PropertyErrorCode;
import com.smartdx.property.mapper.PropertyEmbeddingRefMapper;
import com.smartdx.property.model.entity.PropertyEmbeddingRef;
import com.smartdx.property.model.vo.PropertyEmbeddingVO;
import com.smartdx.property.service.PropertyEmbeddingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Random;

/**
 * LST-EMB-01: 画像特徴量抽出サービス実装
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PropertyEmbeddingServiceImpl implements PropertyEmbeddingService {

    private static final int EMBEDDING_DIMENSION = 512;
    private static final int TTL_MINUTES = 15;
    private static final int MAX_EMBEDDINGS_PER_USER = 100;
    private static final String FEATURE_MODEL = "mobilenetv3_small";
    private static final String FEATURE_VERSION = "v1";

    private final PropertyEmbeddingRefMapper embeddingRefMapper;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public PropertyEmbeddingVO extractAndSave(MultipartFile image, String flag) {
        Long userId = SecurityUtils.getUserId();
        Long tenantId = TenantContextHolder.getTenantId();

        if (userId == null) {
            throw new BusinessException(PropertyErrorCode.UNAUTHORIZED);
        }
        if (tenantId == null) {
            tenantId = 1L; // デフォルトテナント
        }

        // クォータチェック
        int currentCount = embeddingRefMapper.countByUserAndTenant(userId, tenantId);
        if (currentCount >= MAX_EMBEDDINGS_PER_USER) {
            throw new BusinessException(PropertyErrorCode.EMBEDDING_QUOTA_EXCEEDED);
        }

        // 特徴量抽出（TODO: 実際のML推論サービス呼び出し）
        double[] vector = extractFeatureVector(image);

        // embeddingRef 生成
        String embeddingRef = generateEmbeddingRef(image, flag);

        // エンティティ作成
        PropertyEmbeddingRef entity = new PropertyEmbeddingRef();
        entity.setEmbeddingRef(embeddingRef);
        entity.setUserId(userId);
        entity.setTenantId(tenantId);
        entity.setFlag(flag);
        entity.setFeatureModel(FEATURE_MODEL);
        entity.setFeatureVersion(FEATURE_VERSION);
        entity.setDimension(EMBEDDING_DIMENSION);
        entity.setVectorJson(serializeVector(vector));
        entity.setCreatedAt(LocalDateTime.now());
        entity.setExpiresAt(LocalDateTime.now().plusMinutes(TTL_MINUTES));

        embeddingRefMapper.insert(entity);

        log.info("Created embeddingRef: {} for userId: {}, flag: {}", embeddingRef, userId, flag);

        return PropertyEmbeddingVO.builder()
                .embeddingRef(embeddingRef)
                .expiresAt(entity.getExpiresAt().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
                .dimension(EMBEDDING_DIMENSION)
                .build();
    }

    @Override
    public PropertyEmbeddingRef findByRef(String embeddingRef) {
        return embeddingRefMapper.selectByRefAndNotExpired(embeddingRef, LocalDateTime.now());
    }

    @Override
    @Transactional
    public int cleanupExpired() {
        int deleted = embeddingRefMapper.deleteExpired(LocalDateTime.now());
        log.info("Cleaned up {} expired embedding refs", deleted);
        return deleted;
    }

    /**
     * 画像から特徴量ベクトルを抽出
     * TODO: 実際のML推論サービス（ONNX Runtime, TensorFlow Serving等）に置き換え
     */
    private double[] extractFeatureVector(MultipartFile image) {
        try {
            // バリデーション
            if (image == null || image.isEmpty()) {
                throw new BusinessException(PropertyErrorCode.VALIDATION_ERROR);
            }

            String contentType = image.getContentType();
            if (contentType == null || (!contentType.startsWith("image/jpeg") && !contentType.startsWith("image/png"))) {
                throw new BusinessException(PropertyErrorCode.INVALID_MIME);
            }

            // サイズチェック（10MB）
            if (image.getSize() > 10 * 1024 * 1024) {
                throw new BusinessException(PropertyErrorCode.ASSET_TOO_LARGE);
            }

            // TODO: 実際のML推論に置き換え
            // 現在は画像のハッシュを元にダミーベクトルを生成
            byte[] bytes = image.getBytes();
            return generateDummyVector(bytes);
        } catch (IOException e) {
            log.error("Failed to read image for embedding extraction", e);
            throw new BusinessException(PropertyErrorCode.EMBEDDING_EXTRACTION_FAILED);
        }
    }

    /**
     * ダミーベクトル生成（開発用）
     * 画像のハッシュを元に一貫したベクトルを生成
     */
    private double[] generateDummyVector(byte[] imageBytes) {
        String hash = DigestUtil.sha256Hex(imageBytes);
        Random random = new Random(hash.hashCode());
        double[] vector = new double[EMBEDDING_DIMENSION];
        double norm = 0;
        for (int i = 0; i < EMBEDDING_DIMENSION; i++) {
            vector[i] = random.nextGaussian();
            norm += vector[i] * vector[i];
        }
        // L2正規化
        norm = Math.sqrt(norm);
        for (int i = 0; i < EMBEDDING_DIMENSION; i++) {
            vector[i] /= norm;
        }
        return vector;
    }

    /**
     * embeddingRef を生成（emb-{SHA256prefix16} 形式）
     */
    private String generateEmbeddingRef(MultipartFile image, String flag) {
        String input = IdUtil.fastUUID() + ":" + flag + ":" + System.currentTimeMillis();
        String hash = DigestUtil.sha256Hex(input);
        return "emb-" + hash.substring(0, 16);
    }

    /**
     * ベクトルをJSON配列文字列にシリアライズ
     */
    private String serializeVector(double[] vector) {
        try {
            return objectMapper.writeValueAsString(vector);
        } catch (JacksonException e) {
            log.error("Failed to serialize vector", e);
            throw new BusinessException(PropertyErrorCode.INTERNAL_ERROR);
        }
    }
}
