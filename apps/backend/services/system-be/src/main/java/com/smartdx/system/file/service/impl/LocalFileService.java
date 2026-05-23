package com.smartdx.system.file.service.impl;

import cn.hutool.core.date.DatePattern;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.IdUtil;
import com.smartdx.system.file.model.FileInfo;
import com.smartdx.system.file.service.FileService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.InputStream;
import java.time.LocalDateTime;

/**
 * ローカルファイルストレージサービス
 */
@Data
@Slf4j
@Component
@ConditionalOnProperty(value = "oss.type", havingValue = "local")
@ConfigurationProperties(prefix = "oss.local")
@RequiredArgsConstructor
public class LocalFileService implements FileService {

    @Value("${oss.local.storage-path}")
    private String storagePath;

    @Override
    public FileInfo uploadFile(MultipartFile file) {
        String originalFilename = file.getOriginalFilename();
        String suffix = FileUtil.getSuffix(originalFilename);
        String fileName = IdUtil.simpleUUID() + "." + suffix;
        String folder = DateUtil.format(LocalDateTime.now(), DatePattern.PURE_DATE_PATTERN);
        String filePrefix = storagePath.endsWith(File.separator) ? storagePath : storagePath + File.separator;

        try (InputStream inputStream = file.getInputStream()) {
            FileUtil.writeFromStream(inputStream, filePrefix + folder + File.separator + fileName);
        } catch (Exception e) {
            log.error("ファイルアップロード失敗", e);
            throw new RuntimeException("ファイルアップロード失敗");
        }

        String fileUrl = File.separator + folder + File.separator + fileName;
        FileInfo fileInfo = new FileInfo();
        fileInfo.setName(originalFilename);
        fileInfo.setUrl(fileUrl);
        return fileInfo;
    }

    @Override
    public boolean deleteFile(String filePath) {
        if (filePath == null || filePath.isEmpty()) {
            return false;
        }
        if (FileUtil.isDirectory(storagePath + filePath)) {
            return false;
        }
        return FileUtil.del(storagePath + filePath);
    }
}
