package com.smartdx.property.service;

import com.smartdx.property.model.vo.PropertyCreateAcceptedVO;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 物件登録サービス
 * LST-INT-01: POST /api/v1/properties
 */
public interface PropertyCreateService {

    /**
     * 物件登録受付（新規登録）
     *
     * @param csv        物件台帳CSV
     * @param excel      物件台帳Excel
     * @param photos     物件写真
     * @param docs       関連文書
     * @param listedYear 掲載年
     * @param area       エリアコード
     * @return 受付結果
     */
    PropertyCreateAcceptedVO acceptPropertyCreate(
            MultipartFile csv,
            MultipartFile excel,
            List<MultipartFile> photos,
            List<MultipartFile> docs,
            Integer listedYear,
            String area
    );

    /**
     * 物件登録受付（修正再提出）
     *
     * @param resubmitOf      修正再提出対象 propertyKey
     * @param expectedVersion 楽観ロック用バージョン
     * @param csv             物件台帳CSV
     * @param excel           物件台帳Excel
     * @param photos          物件写真
     * @param docs            関連文書
     * @param keepAssets      維持するアセットキー
     * @param keepDocs        維持するドキュメントキー
     * @param listedYear      掲載年
     * @param area            エリアコード
     * @return 受付結果
     */
    PropertyCreateAcceptedVO acceptPropertyResubmit(
            String resubmitOf,
            Integer expectedVersion,
            MultipartFile csv,
            MultipartFile excel,
            List<MultipartFile> photos,
            List<MultipartFile> docs,
            List<String> keepAssets,
            List<String> keepDocs,
            Integer listedYear,
            String area
    );
}
