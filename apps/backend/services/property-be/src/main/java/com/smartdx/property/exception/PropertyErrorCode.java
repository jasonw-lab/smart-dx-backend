package com.smartdx.property.exception;

import com.smartdx.core.result.IResultCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 物件API エラーコード
 */
@Getter
@RequiredArgsConstructor
public enum PropertyErrorCode implements IResultCode {

    VALIDATION_ERROR("VALIDATION_ERROR", "入力内容に誤りがあります"),
    INVALID_PROPERTY_KEY("A0400", "物件キーの形式が不正です"),
    INVALID_SCOPE("A0401", "無効なスコープ値です"),
    SCOPE_NOT_ALLOWED("A0301", "指定されたスコープへのアクセス権がありません"),
    LISTING_NOT_FOUND("LISTING_NOT_FOUND", "指定された物件が見つかりません"),
    UNAUTHORIZED("UNAUTHORIZED", "認証が必要です"),
    FORBIDDEN("FORBIDDEN", "この操作を行う権限がありません"),
    INVALID_CSV_SCHEMA("INVALID_CSV_SCHEMA", "CSVの形式が正しくありません"),
    INVALID_MIME("INVALID_MIME", "サポート外のファイル形式です"),
    INVALID_AREA("INVALID_AREA", "指定されたエリアは存在しません"),
    INVALID_PROPERTY_TYPE("INVALID_PROPERTY_TYPE", "指定された物件種別は存在しません"),
    INVALID_STATE_TRANSITION("INVALID_STATE_TRANSITION", "この状態からの更新はできません"),
    VERSION_MISMATCH("VERSION_MISMATCH", "他のユーザーが更新しました"),
    ASSET_TOO_LARGE("ASSET_TOO_LARGE", "ファイルサイズが上限を超えています"),
    RATE_LIMIT_EXCEEDED("RATE_LIMIT_EXCEEDED", "リクエスト数が上限を超えました"),
    INTERNAL_ERROR("INTERNAL_ERROR", "サーバーエラーが発生しました"),

    // LST-EMB-01: Embedding errors
    EMBEDDING_QUOTA_EXCEEDED("EMBEDDING_QUOTA_EXCEEDED", "embeddingRef の上限（100件）に達しました"),
    EMBEDDING_EXTRACTION_FAILED("EMBEDDING_EXTRACTION_FAILED", "特徴量の抽出に失敗しました"),
    EMBEDDING_NOT_FOUND("EMBEDDING_NOT_FOUND", "指定された embeddingRef が見つからないか期限切れです"),
    INVALID_FLAG("INVALID_FLAG", "flag は main, sub, layout のいずれかを指定してください"),

    // LST-AST-01/02/03: Asset errors
    ASSET_NOT_FOUND("ASSET_NOT_FOUND", "指定されたアセットが見つかりません"),
    VECTOR_NOT_READY("VECTOR_NOT_READY", "特徴量ベクトルがまだ準備されていません"),

    // LST-SIM-01: Similar search errors
    DEMO_REF_NOT_FOUND("DEMO_REF_NOT_FOUND", "指定されたDEMO画像が見つかりません"),
    IMAGE_NOT_FOUND("IMAGE_NOT_FOUND", "指定された画像が見つかりません"),
    NO_SEARCH_ORIGIN("NO_SEARCH_ORIGIN", "検索条件を指定してください"),

    // ADR-011: OpenSearch errors
    OPENSEARCH_UNAVAILABLE("OPENSEARCH_UNAVAILABLE", "類似検索サービスは現在利用できません");

    private final String code;
    private final String msg;
}
