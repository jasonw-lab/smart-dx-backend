package com.smartdx.property.controller;

import com.smartdx.core.result.Result;
import com.smartdx.property.model.vo.BulkImportAcceptedVO;
import com.smartdx.property.model.vo.JobDetailVO;
import com.smartdx.property.model.vo.JobErrorPageVO;
import com.smartdx.property.model.vo.JobListVO;
import com.smartdx.property.service.PropertyBulkImportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.List;

@Tag(name = "物件登録ジョブ")
@RestController
@RequestMapping("/api/v1/property-import-jobs")
@RequiredArgsConstructor
@Slf4j
public class PropertyIntakeController {

    private final PropertyBulkImportService propertyBulkImportService;

    @Operation(summary = "一括登録ジョブ作成", description = "CSV/写真/文書をまとめてアップロードし、登録ジョブを起票する")
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Result<BulkImportAcceptedVO>> createImportJob(
            @RequestPart(value = "csv", required = false) MultipartFile csv,
            @RequestPart(value = "photos[]", required = false) List<MultipartFile> photos,
            @RequestPart(value = "docs[]", required = false) List<MultipartFile> docs,
            @RequestParam(value = "csvObjectKey", required = false) String csvObjectKey,
            @RequestParam(value = "listedYear", required = false) Integer listedYear,
            @RequestParam(value = "area", required = false) String area
    ) {
        log.info("API call start: POST /api/v1/property-import-jobs. csvObjectKey={}, listedYear={}, area={}, photoCount={}, docCount={}",
                csvObjectKey, listedYear, area,
                photos != null ? photos.size() : 0,
                docs != null ? docs.size() : 0);
        BulkImportAcceptedVO accepted = propertyBulkImportService.acceptBulkImport(csv, csvObjectKey, photos, docs, listedYear, area);
        log.info("API call end: POST /api/v1/property-import-jobs. csvObjectKey={}, listedYear={}, area={}",
                csvObjectKey, listedYear, area);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(Result.success(accepted));
    }

    @Operation(summary = "ジョブ進捗取得API", description = "LST-INT-03: 個別ジョブの進捗を取得")
    @GetMapping("/{jobId}")
    public Result<JobDetailVO> getJob(@PathVariable("jobId") String jobId) {
        log.info("API call start: GET /api/v1/property-import-jobs/{}", jobId);
        Result<JobDetailVO> result = Result.success(propertyBulkImportService.getJob(jobId));
        log.info("API call end: GET /api/v1/property-import-jobs/{}", jobId);
        return result;
    }

    @Operation(summary = "ジョブ一覧取得API", description = "LST-INT-04: ジョブ一覧＋集計")
    @GetMapping
    public Result<JobListVO> listJobs(
            @RequestParam(value = "targetScope", required = false) String targetScope,
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "from", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(value = "to", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(value = "owner", required = false) String owner,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "50") int size
    ) {
        log.info("API call start: GET /api/v1/property-import-jobs. targetScope={}, status={}, from={}, to={}, owner={}, page={}, size={}",
                targetScope, status, from, to, owner, page, size);
        Result<JobListVO> result = Result.success(propertyBulkImportService.listJobs(targetScope, status, from, to, owner, page, size));
        log.info("API call end: GET /api/v1/property-import-jobs. targetScope={}, status={}, from={}, to={}, owner={}, page={}, size={}",
                targetScope, status, from, to, owner, page, size);
        return result;
    }

    @Operation(summary = "ジョブ失敗詳細取得API", description = "LST-INT-06: ジョブ失敗詳細（ページング／CSV）")
    @GetMapping("/{jobId}/errors")
    public Object listErrors(
            @PathVariable("jobId") String jobId,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "200") int size,
            @RequestParam(value = "format", defaultValue = "json") String format,
            HttpServletResponse response
    ) {
        log.info("API call start: GET /api/v1/property-import-jobs/{}/errors. page={}, size={}, format={}",
                jobId, page, size, format);
        if ("csv".equalsIgnoreCase(format)) {
            propertyBulkImportService.writeErrorsCsv(jobId, response);
            log.info("API call end: GET /api/v1/property-import-jobs/{}/errors. format=csv", jobId);
            return null;
        }
        JobErrorPageVO errors = propertyBulkImportService.listErrors(jobId, page, size);
        log.info("API call end: GET /api/v1/property-import-jobs/{}/errors. page={}, size={}, format=json",
                jobId, page, size);
        return Result.success(errors);
    }
}
