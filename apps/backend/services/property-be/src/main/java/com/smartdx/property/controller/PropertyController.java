package com.smartdx.property.controller;

import com.smartdx.core.result.Result;
import com.smartdx.property.model.vo.PropertyCreateAcceptedVO;
import com.smartdx.property.service.PropertyCreateService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 物件 API Controller
 * LST-INT-01: 物件登録 API
 */
@Tag(name = "物件管理", description = "物件の登録・管理API")
@RestController
@RequestMapping("/api/v1/properties")
@RequiredArgsConstructor
@Slf4j
public class PropertyController {

    private final PropertyCreateService propertyCreateService;

    @Operation(
            summary = "物件登録",
            description = "物件メタデータ・写真・関連文書を受け付け、非同期でインデックス登録を行う。" +
                    "登録処理はジョブキューに投入され、Workerが処理を実行する。"
    )
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('ADMIN', 'FIELD_AGENT')")
    public ResponseEntity<Result<PropertyCreateAcceptedVO>> createProperty(
            @Parameter(description = "物件台帳（CSV）。excel と排他")
            @RequestPart(value = "csv", required = false) MultipartFile csv,

            @Parameter(description = "物件台帳（XLSX）。csv と排他")
            @RequestPart(value = "excel", required = false) MultipartFile excel,

            @Parameter(description = "物件写真（JPG/PNG、各80MB以下、最大30枚）")
            @RequestPart(value = "photos[]", required = false) List<MultipartFile> photos,

            @Parameter(description = "関連文書（PDF/DOCX/XLSX/PPTX/HTML/TXT、各80MB以下、最大20件）")
            @RequestPart(value = "docs[]", required = false) List<MultipartFile> docs,

            @Parameter(description = "掲載年（2000〜現在年）", required = true)
            @RequestParam("listedYear") Integer listedYear,

            @Parameter(description = "エリアコード", required = true)
            @RequestParam("area") String area,

            @Parameter(description = "修正再提出時: 維持するアセットキー")
            @RequestParam(value = "keepAssets[]", required = false) List<String> keepAssets,

            @Parameter(description = "修正再提出時: 維持するドキュメントキー")
            @RequestParam(value = "keepDocs[]", required = false) List<String> keepDocs,

            @Parameter(description = "修正再提出時: 対象 propertyKey")
            @RequestHeader(value = "X-Resubmit-Of", required = false) String resubmitOf,

            @Parameter(description = "修正再提出時: 対象バージョン（楽観ロック）")
            @RequestHeader(value = "If-Match", required = false) Integer ifMatch
    ) {
        log.info("API call start: POST /api/v1/properties. listedYear={}, area={}, photoCount={}, docCount={}, resubmitOf={}",
                listedYear, area,
                photos != null ? photos.size() : 0,
                docs != null ? docs.size() : 0,
                resubmitOf);

        PropertyCreateAcceptedVO accepted;

        if (resubmitOf != null && !resubmitOf.isBlank()) {
            accepted = propertyCreateService.acceptPropertyResubmit(
                    resubmitOf, ifMatch, csv, excel, photos, docs, keepAssets, keepDocs, listedYear, area
            );
        } else {
            accepted = propertyCreateService.acceptPropertyCreate(
                    csv, excel, photos, docs, listedYear, area
            );
        }

        log.info("API call end: POST /api/v1/properties. jobRef={}, propertyKey={}",
                accepted.getJobRef(), accepted.getPropertyKey());

        return ResponseEntity.status(HttpStatus.ACCEPTED).body(Result.success(accepted));
    }
}
