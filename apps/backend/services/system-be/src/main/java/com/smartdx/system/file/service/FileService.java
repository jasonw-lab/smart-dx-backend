package com.smartdx.system.file.service;

import com.smartdx.system.file.model.FileInfo;
import org.springframework.web.multipart.MultipartFile;

/**
 * ファイルストレージサービスインターフェース
 */
public interface FileService {

    /**
     * ファイルをアップロード
     *
     * @param file フォームファイルオブジェクト
     * @return ファイル情報
     */
    FileInfo uploadFile(MultipartFile file);

    /**
     * ファイルを削除
     *
     * @param filePath ファイルのフルURL
     * @return 削除結果
     */
    boolean deleteFile(String filePath);
}
