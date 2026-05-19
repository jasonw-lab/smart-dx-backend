package com.smartdx.property.storage;

import com.smartdx.core.exception.BusinessException;
import com.smartdx.property.file.MinioFileService;
import io.minio.BucketExistsArgs;
import io.minio.GetObjectArgs;
import io.minio.MakeBucketArgs;
import io.minio.PutObjectArgs;
import io.minio.StatObjectArgs;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

@Component
@RequiredArgsConstructor
public class PropertyIntakeStorage {

    private final ObjectProvider<MinioFileService> minioFileServiceProvider;

    @Value("${property.intake.local-storage-path:${java.io.tmpdir}/smart-property-search/intake}")
    private String localStoragePath;

    public void put(String objectKey, MultipartFile file) {
        try (InputStream inputStream = file.getInputStream()) {
            put(objectKey, inputStream, file.getSize(), file.getContentType());
        } catch (IOException e) {
            throw new BusinessException("一括登録ファイルの保存に失敗しました: {}", objectKey);
        }
    }

    public void putString(String objectKey, String content) {
        byte[] bytes = content.getBytes(StandardCharsets.UTF_8);
        put(objectKey, new ByteArrayInputStream(bytes), bytes.length, "text/plain; charset=UTF-8");
    }

    public InputStream open(String objectKey) {
        MinioFileService minio = minio();
        if (minio != null) {
            try {
                return minio.getMinioClient().getObject(
                        GetObjectArgs.builder()
                                .bucket(minio.getBucketName())
                                .object(objectKey)
                                .build()
                );
            } catch (Exception e) {
                throw new BusinessException("一括登録ファイルの読込に失敗しました: {}", objectKey);
            }
        }

        try {
            return Files.newInputStream(resolveLocalPath(objectKey));
        } catch (IOException e) {
            throw new BusinessException("一括登録ファイルの読込に失敗しました: {}", objectKey);
        }
    }

    public boolean exists(String objectKey) {
        if (objectKey == null || objectKey.isBlank()) {
            return false;
        }

        MinioFileService minio = minio();
        if (minio != null) {
            try {
                minio.getMinioClient().statObject(
                        StatObjectArgs.builder()
                                .bucket(minio.getBucketName())
                                .object(objectKey)
                                .build()
                );
                return true;
            } catch (Exception e) {
                return false;
            }
        }

        return Files.isRegularFile(resolveLocalPath(objectKey));
    }

    private void put(String objectKey, InputStream inputStream, long size, String contentType) {
        MinioFileService minio = minio();
        if (minio != null) {
            try {
                ensureBucket(minio);
                minio.getMinioClient().putObject(
                        PutObjectArgs.builder()
                                .bucket(minio.getBucketName())
                                .object(objectKey)
                                .contentType(contentType)
                                .stream(inputStream, size, -1)
                                .build()
                );
                return;
            } catch (Exception e) {
                throw new BusinessException("一括登録ファイルの保存に失敗しました: {}", objectKey);
            }
        }

        Path path = resolveLocalPath(objectKey);
        try {
            Files.createDirectories(path.getParent());
            Files.copy(inputStream, path, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new BusinessException("一括登録ファイルの保存に失敗しました: {}", objectKey);
        }
    }

    private void ensureBucket(MinioFileService minio) throws Exception {
        boolean exists = minio.getMinioClient().bucketExists(
                BucketExistsArgs.builder().bucket(minio.getBucketName()).build()
        );
        if (!exists) {
            minio.getMinioClient().makeBucket(MakeBucketArgs.builder().bucket(minio.getBucketName()).build());
        }
    }

    private MinioFileService minio() {
        MinioFileService minio = minioFileServiceProvider.getIfAvailable();
        return minio != null && minio.getMinioClient() != null ? minio : null;
    }

    private Path resolveLocalPath(String objectKey) {
        Path root = Path.of(localStoragePath).toAbsolutePath().normalize();
        Path path = root.resolve(objectKey).normalize();
        if (!path.startsWith(root)) {
            throw new BusinessException("不正な object key です: {}", objectKey);
        }
        return path;
    }
}
