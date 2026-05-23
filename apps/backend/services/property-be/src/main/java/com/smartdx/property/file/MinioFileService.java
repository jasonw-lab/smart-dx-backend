package com.smartdx.property.file;

import io.minio.*;
import io.minio.http.Method;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import jakarta.annotation.PostConstruct;
import java.io.InputStream;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * MinIOファイルサービス
 */
@Slf4j
@Service
public class MinioFileService {

    @Value("${minio.endpoint:http://localhost:9000}")
    private String endpoint;

    @Value("${minio.access-key:minioadmin}")
    private String accessKey;

    @Value("${minio.secret-key:minioadmin}")
    private String secretKey;

    @Value("${minio.bucket:property-files}")
    private String bucket;

    private MinioClient minioClient;

    @PostConstruct
    public void init() {
        try {
            this.minioClient = MinioClient.builder()
                    .endpoint(endpoint)
                    .credentials(accessKey, secretKey)
                    .build();

            boolean found = minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucket).build());
            if (!found) {
                minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucket).build());
                log.info("Bucket '{}' created", bucket);
            }
        } catch (Exception e) {
            log.warn("Failed to initialize Minio client: {}", e.getMessage());
        }
    }

    /**
     * ファイルをアップロード
     */
    public String uploadFile(MultipartFile file) {
        return uploadFile(file, null);
    }

    /**
     * ファイルをアップロード（パス指定）
     */
    public String uploadFile(MultipartFile file, String path) {
        try {
            String originalFilename = file.getOriginalFilename();
            String extension = "";
            if (originalFilename != null && originalFilename.contains(".")) {
                extension = originalFilename.substring(originalFilename.lastIndexOf("."));
            }

            String objectName = UUID.randomUUID().toString() + extension;
            if (path != null && !path.isEmpty()) {
                objectName = path + "/" + objectName;
            }

            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucket)
                            .object(objectName)
                            .stream(file.getInputStream(), file.getSize(), -1)
                            .contentType(file.getContentType())
                            .build()
            );

            return objectName;
        } catch (Exception e) {
            log.error("Failed to upload file", e);
            throw new RuntimeException("Failed to upload file: " + e.getMessage(), e);
        }
    }

    /**
     * ファイルを取得
     */
    public InputStream getFile(String objectName) {
        try {
            return minioClient.getObject(
                    GetObjectArgs.builder()
                            .bucket(bucket)
                            .object(objectName)
                            .build()
            );
        } catch (Exception e) {
            log.error("Failed to get file: {}", objectName, e);
            throw new RuntimeException("Failed to get file: " + e.getMessage(), e);
        }
    }

    /**
     * 署名付きURLを取得
     */
    public String getPresignedUrl(String objectName) {
        return getPresignedUrl(objectName, 1, TimeUnit.HOURS);
    }

    /**
     * 署名付きURLを取得（有効期限指定）
     */
    public String getPresignedUrl(String objectName, int duration, TimeUnit unit) {
        try {
            return minioClient.getPresignedObjectUrl(
                    GetPresignedObjectUrlArgs.builder()
                            .method(Method.GET)
                            .bucket(bucket)
                            .object(objectName)
                            .expiry(duration, unit)
                            .build()
            );
        } catch (Exception e) {
            log.error("Failed to get presigned URL: {}", objectName, e);
            throw new RuntimeException("Failed to get presigned URL: " + e.getMessage(), e);
        }
    }

    /**
     * ファイルを削除
     */
    public void deleteFile(String objectName) {
        try {
            minioClient.removeObject(
                    RemoveObjectArgs.builder()
                            .bucket(bucket)
                            .object(objectName)
                            .build()
            );
        } catch (Exception e) {
            log.error("Failed to delete file: {}", objectName, e);
            throw new RuntimeException("Failed to delete file: " + e.getMessage(), e);
        }
    }

    /**
     * ファイルの存在確認
     */
    public boolean exists(String objectName) {
        try {
            minioClient.statObject(
                    StatObjectArgs.builder()
                            .bucket(bucket)
                            .object(objectName)
                            .build()
            );
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * MinioClient を取得
     */
    public MinioClient getMinioClient() {
        return minioClient;
    }

    /**
     * バケット名を取得
     */
    public String getBucketName() {
        return bucket;
    }

    /**
     * オブジェクトURLを構築
     */
    public String buildObjectUrl(String objectName) {
        if (objectName == null || objectName.isEmpty()) {
            return null;
        }
        return endpoint + "/" + bucket + "/" + objectName;
    }
}
