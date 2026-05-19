package com.smartdx.property.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.smartdx.core.exception.BusinessException;
import com.smartdx.security.model.UserDetails;
import com.smartdx.security.util.SecurityUtils;
import com.smartdx.tenant.TenantContextHolder;
import com.smartdx.property.mapper.PropertyIntakeJobErrorMapper;
import com.smartdx.property.mapper.PropertyIntakeJobMapper;
import com.smartdx.property.mapper.PropertyIntakeJobSeqMapper;
import com.smartdx.property.mapper.PropertyListingMapper;
import com.smartdx.property.model.entity.PropertyIntakeJob;
import com.smartdx.property.model.entity.PropertyIntakeJobError;
import com.smartdx.property.model.entity.PropertyIntakeJobSeq;
import com.smartdx.property.model.entity.PropertyListing;
import com.smartdx.property.model.enums.PropertyIntakeJobStatus;
import com.smartdx.property.model.vo.BulkImportAcceptedVO;
import com.smartdx.property.model.vo.JobDetailVO;
import com.smartdx.property.model.vo.JobErrorPageVO;
import com.smartdx.property.model.vo.JobErrorVO;
import com.smartdx.property.model.vo.JobListItemVO;
import com.smartdx.property.model.vo.JobListVO;
import com.smartdx.property.model.vo.JobOwnerVO;
import com.smartdx.property.model.vo.ReceivedCountVO;
import com.smartdx.property.service.PropertyBulkImportService;
import com.smartdx.property.storage.PropertyIntakeStorage;
import com.smartdx.property.support.CsvLineParser;
import com.smartdx.property.support.PropertyIntakeHash;
import com.smartdx.property.support.PropertyLedgerRow;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;

@Service
@Slf4j
public class PropertyBulkImportServiceImpl implements PropertyBulkImportService {

    private static final ZoneId TOKYO = ZoneId.of("Asia/Tokyo");
    private static final String TARGET_SCOPE_PUBLISHED = "published";
    private static final String JOB_TYPE_BULK = "bulk";
    private static final int ERROR_PREVIEW_LIMIT = 100;
    private static final int MAX_ERROR_PAGE_SIZE = 500;

    private final PropertyIntakeJobMapper jobMapper;
    private final PropertyIntakeJobErrorMapper errorMapper;
    private final PropertyIntakeJobSeqMapper seqMapper;
    private final PropertyListingMapper listingMapper;
    private final PropertyIntakeStorage storage;
    private final Executor executor;

    public PropertyBulkImportServiceImpl(
            PropertyIntakeJobMapper jobMapper,
            PropertyIntakeJobErrorMapper errorMapper,
            PropertyIntakeJobSeqMapper seqMapper,
            PropertyListingMapper listingMapper,
            PropertyIntakeStorage storage,
            @Qualifier("propertyBulkImportExecutor") Executor executor
    ) {
        this.jobMapper = jobMapper;
        this.errorMapper = errorMapper;
        this.seqMapper = seqMapper;
        this.listingMapper = listingMapper;
        this.storage = storage;
        this.executor = executor;
    }

    @Override
    @Transactional
    public BulkImportAcceptedVO acceptBulkImport(
            MultipartFile csv,
            String requestedCsvObjectKey,
            List<MultipartFile> photos,
            List<MultipartFile> docs,
            Integer listedYear,
            String area
    ) {
        Long tenantId = ensureTenantContext();
        UserDetails user = SecurityUtils.getUser().orElse(null);
        LocalDateTime acceptedAt = LocalDateTime.now();
        String jobRef = generateJobRef(tenantId, JOB_TYPE_BULK, acceptedAt.toLocalDate());
        String csvObjectKey = resolveCsvObjectKey(jobRef, csv, requestedCsvObjectKey);

        int recordCount = countRecords(csvObjectKey);
        int photoCount = nonNullSize(photos);
        int docCount = nonNullSize(docs);

        storeAttachments(jobRef, photos);
        storeAttachments(jobRef, docs);

        PropertyIntakeJob job = new PropertyIntakeJob();
        job.setJobRef(jobRef);
        job.setJobType(JOB_TYPE_BULK);
        job.setTargetScope(TARGET_SCOPE_PUBLISHED);
        job.setStatus(PropertyIntakeJobStatus.PENDING.name());
        job.setTotalCount(recordCount);
        job.setRecordsCount(recordCount);
        job.setPhotosCount(photoCount);
        job.setDocsCount(docCount);
        job.setSucceededCount(0);
        job.setFailedCount(0);
        job.setAcceptedAt(acceptedAt);
        job.setOwnerUserId(user != null ? user.getUserId() : null);
        job.setOwnerDisplayName(displayName(user));
        job.setListedYear(listedYear);
        job.setArea(blankToNull(area));
        job.setErrorsTruncated(0);
        job.setIsDeleted(0);
        job.setParamsJson(paramsJson(csvObjectKey, listedYear, area));
        jobMapper.insert(job);

        submitWorkerAfterCommit(jobRef, tenantId);

        BulkImportAcceptedVO accepted = new BulkImportAcceptedVO();
        accepted.setJobRef(jobRef);
        accepted.setScope(TARGET_SCOPE_PUBLISHED);
        accepted.setAcceptedAt(toOffset(acceptedAt));
        accepted.setReceivedCount(new ReceivedCountVO(recordCount, photoCount, docCount));
        return accepted;
    }

    @Override
    public JobDetailVO getJob(String jobRef) {
        PropertyIntakeJob job = requireJob(jobRef);
        List<PropertyIntakeJobError> errors = errorMapper.selectPage(
                new Page<>(1, ERROR_PREVIEW_LIMIT + 1),
                new LambdaQueryWrapper<PropertyIntakeJobError>()
                        .eq(PropertyIntakeJobError::getJobRef, jobRef)
                        .orderByAsc(PropertyIntakeJobError::getId)
        ).getRecords();

        JobDetailVO vo = toDetailVO(job);
        vo.setErrors(errors.stream().limit(ERROR_PREVIEW_LIMIT).map(this::toErrorVO).toList());
        vo.setErrorsTruncated(errors.size() > ERROR_PREVIEW_LIMIT || Objects.equals(job.getErrorsTruncated(), 1));
        return vo;
    }

    @Override
    public JobListVO listJobs(String targetScope, String status, LocalDate from, LocalDate to, String owner, int page, int size) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.max(1, Math.min(size, 200));
        LambdaQueryWrapper<PropertyIntakeJob> query = buildJobQuery(targetScope, status, from, to, owner);

        IPage<PropertyIntakeJob> result = jobMapper.selectPage(
                new Page<>(safePage + 1L, safeSize),
                query.orderByDesc(PropertyIntakeJob::getAcceptedAt)
        );

        List<PropertyIntakeJob> summaryRows = jobMapper.selectList(buildJobQuery(targetScope, null, from, to, owner));
        Map<String, Long> summary = new LinkedHashMap<>();
        for (PropertyIntakeJobStatus jobStatus : PropertyIntakeJobStatus.values()) {
            summary.put(jobStatus.name(), 0L);
        }
        summary.putAll(summaryRows.stream()
                .collect(Collectors.groupingBy(PropertyIntakeJob::getStatus, LinkedHashMap::new, Collectors.counting())));

        JobListVO vo = new JobListVO();
        vo.setTotal(result.getTotal());
        vo.setPage(safePage);
        vo.setSize(safeSize);
        vo.setSummary(summary);
        vo.setJobs(result.getRecords().stream().map(this::toListItemVO).toList());
        return vo;
    }

    @Override
    public JobErrorPageVO listErrors(String jobRef, int page, int size) {
        requireJob(jobRef);
        int safePage = Math.max(page, 0);
        int safeSize = Math.max(1, Math.min(size, MAX_ERROR_PAGE_SIZE));
        IPage<PropertyIntakeJobError> result = errorMapper.selectPage(
                new Page<>(safePage + 1L, safeSize),
                new LambdaQueryWrapper<PropertyIntakeJobError>()
                        .eq(PropertyIntakeJobError::getJobRef, jobRef)
                        .orderByAsc(PropertyIntakeJobError::getId)
        );

        JobErrorPageVO vo = new JobErrorPageVO();
        vo.setTotal(result.getTotal());
        vo.setPage(safePage);
        vo.setSize(safeSize);
        vo.setErrors(result.getRecords().stream().map(this::toErrorVO).toList());
        return vo;
    }

    @Override
    public void writeErrorsCsv(String jobRef, HttpServletResponse response) {
        requireJob(jobRef);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType("text/csv; charset=UTF-8");
        response.setHeader("Content-Disposition", "attachment; filename=\"" + jobRef + "-errors.csv\"");

        try (PrintWriter writer = response.getWriter()) {
            writer.println("rowNo,targetType,fileName,errorCode,message");
            long current = 1;
            while (true) {
                IPage<PropertyIntakeJobError> page = errorMapper.selectPage(
                        new Page<>(current, MAX_ERROR_PAGE_SIZE),
                        new LambdaQueryWrapper<PropertyIntakeJobError>()
                                .eq(PropertyIntakeJobError::getJobRef, jobRef)
                                .orderByAsc(PropertyIntakeJobError::getId)
                );
                for (PropertyIntakeJobError error : page.getRecords()) {
                    writer.printf(
                            "%s,%s,%s,%s,%s%n",
                            csv(error.getRowNo()),
                            csv(error.getTargetType()),
                            csv(error.getFileName()),
                            csv(error.getErrorCode()),
                            csv(error.getMessage())
                    );
                }
                if (current >= page.getPages()) {
                    break;
                }
                current++;
            }
        } catch (IOException e) {
            throw new BusinessException("エラー CSV の出力に失敗しました");
        }
    }

    private void runWorker(String jobRef, Long tenantId) {
        TenantContextHolder.setTenantId(tenantId);
        try {
            processJob(jobRef);
        } catch (Exception e) {
            log.error("Property bulk import failed. jobRef={}", jobRef, e);
            markJobFailed(jobRef, e.getMessage());
        } finally {
            TenantContextHolder.clear();
        }
    }

    private void submitWorkerAfterCommit(String jobRef, Long tenantId) {
        Runnable worker = () -> executor.execute(() -> runWorker(jobRef, tenantId));
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    worker.run();
                }
            });
        } else {
            worker.run();
        }
    }

    private void processJob(String jobRef) {
        PropertyIntakeJob job = requireJob(jobRef);
        job.setStatus(PropertyIntakeJobStatus.RUNNING.name());
        job.setStartedAt(LocalDateTime.now());
        jobMapper.updateById(job);

        int success = 0;
        int failed = 0;
        int total = 0;

        try (InputStream inputStream = storage.open(inputCsvKey(jobRef));
             BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {

            String headerLine = reader.readLine();
            if (headerLine == null || headerLine.isBlank()) {
                throw new BusinessException("CSV ヘッダーが空です");
            }
            List<String> headers = CsvLineParser.parse(stripBom(headerLine));

            String line;
            int rowNo = 1;
            while ((line = reader.readLine()) != null) {
                rowNo++;
                if (line.isBlank()) {
                    continue;
                }
                total++;
                RowResult result = processRow(job, headers, line, rowNo);
                if (result.successful()) {
                    success++;
                } else {
                    failed++;
                }
                if (total % 100 == 0) {
                    updateProgress(jobRef, total, success, failed, null, null);
                }
            }
        } catch (Exception e) {
            markJobFailed(jobRef, e.getMessage());
            return;
        }

        PropertyIntakeJobStatus finalStatus = failed == 0
                ? PropertyIntakeJobStatus.SUCCESS
                : (success == 0 ? PropertyIntakeJobStatus.FAILED : PropertyIntakeJobStatus.PARTIAL_SUCCESS);
        LocalDateTime endedAt = LocalDateTime.now();
        updateProgress(jobRef, total, success, failed, finalStatus, endedAt);
        writeResultSummary(jobRef, total, success, failed, finalStatus.name(), endedAt);
    }

    private RowResult processRow(PropertyIntakeJob job, List<String> headers, String line, int rowNo) {
        List<String> values = CsvLineParser.parse(line);
        if (values.size() != headers.size()) {
            recordError(job.getJobRef(), "validation", null, null, rowNo, null,
                    "CSV_COLUMN_MISMATCH", "CSV の列数がヘッダーと一致しません");
            return RowResult.failed();
        }

        Map<String, String> map = new HashMap<>();
        for (int i = 0; i < headers.size(); i++) {
            map.put(headers.get(i), values.get(i));
        }

        PropertyLedgerRow row = PropertyLedgerRow.from(rowNo, map);
        validateRequestScope(job, row);
        validateMedia(job.getJobRef(), row);
        if (!row.getErrors().isEmpty()) {
            recordError(job.getJobRef(), "validation", row.getPropertyKey(), null, rowNo, row.getMainPhoto(),
                    row.getErrors().get(0), String.join("|", row.getErrors()));
            return RowResult.failed();
        }

        String versionError = upsertListing(job, row);
        if (versionError != null) {
            recordError(job.getJobRef(), "record", row.getPropertyKey(), null, rowNo, row.getMainPhoto(),
                    "VERSION_MISMATCH", versionError);
            return RowResult.failed();
        }
        return RowResult.passed();
    }

    private void validateRequestScope(PropertyIntakeJob job, PropertyLedgerRow row) {
        if (job.getListedYear() != null && row.getListedYear() != null && !job.getListedYear().equals(row.getListedYear())) {
            row.getErrors().add("LISTED_YEAR_MISMATCH");
        }
        if (job.getArea() != null && row.getArea() != null && !job.getArea().equals(row.getArea())) {
            row.getErrors().add("AREA_MISMATCH");
        }
    }

    private void validateMedia(String jobRef, PropertyLedgerRow row) {
        for (String ref : row.mediaRefs()) {
            if (!isSafeObjectRef(ref)) {
                row.getErrors().add("INVALID_OBJECT_KEY");
                continue;
            }
            if (!storage.exists(ref) && !storage.exists(attachmentKey(jobRef, fileName(ref)))) {
                row.getErrors().add("ATTACHMENT_NOT_FOUND:" + ref);
            }
        }
    }

    private String upsertListing(PropertyIntakeJob job, PropertyLedgerRow row) {
        PropertyListing existing = listingMapper.selectOne(
                new LambdaQueryWrapper<PropertyListing>()
                        .eq(PropertyListing::getPropertyKey, row.getPropertyKey())
                        .eq(PropertyListing::getScope, job.getTargetScope())
                        .eq(PropertyListing::getIsDeleted, 0)
                        .last("LIMIT 1")
        );
        if (existing != null && row.getVersion() != null && !row.getVersion().equals(existing.getVersion())) {
            return "CSV version=" + row.getVersion() + " does not match current version=" + existing.getVersion();
        }

        PropertyListing listing = existing != null ? existing : new PropertyListing();
        listing.setPropertyKey(row.getPropertyKey());
        listing.setScope(job.getTargetScope());
        listing.setVersion(existing == null ? 1 : nullToOne(existing.getVersion()) + 1);
        listing.setArea(row.getArea());
        listing.setAddress(row.getAddress());
        listing.setPropertyType(row.getPropertyType());
        listing.setPriceJpy(row.getPriceJpy());
        listing.setLayout(row.getLayout());
        listing.setAreaSqm(row.getAreaSqm() != null ? row.getAreaSqm() : BigDecimal.ZERO);
        listing.setStationWalkMin(row.getStationWalkMin());
        listing.setBuiltYearMonth(row.getBuiltYearMonth());
        listing.setListedDate(row.getListedDate());
        listing.setListedYear(row.getListedYear());
        listing.setPriorityRank(row.getPriorityRank());
        listing.setReviewStatus(TARGET_SCOPE_PUBLISHED.equals(job.getTargetScope()) ? "APPROVED" : "PENDING");
        listing.setRegistrantUserId(job.getOwnerUserId());
        listing.setRegistrantDisplayName(job.getOwnerDisplayName());
        listing.setRegisteredAt(LocalDateTime.now());
        listing.setPublishedAt(TARGET_SCOPE_PUBLISHED.equals(job.getTargetScope()) ? LocalDateTime.now() : null);
        listing.setIsDeleted(0);

        if (existing == null) {
            listingMapper.insert(listing);
        } else {
            listingMapper.updateById(listing);
        }
        return null;
    }

    private void recordError(
            String jobRef,
            String targetType,
            String propertyKey,
            String assetKey,
            Integer rowNo,
            String fileName,
            String errorCode,
            String message
    ) {
        PropertyIntakeJobError error = new PropertyIntakeJobError();
        error.setTenantId(ensureTenantContext());
        error.setJobRef(jobRef);
        error.setTargetType(targetType);
        error.setPropertyKey(propertyKey);
        error.setAssetKey(assetKey);
        error.setRecordNo(rowNo != null ? Math.max(rowNo - 1, 0) : null);
        error.setRowNo(rowNo);
        error.setFileName(fileName);
        error.setErrorCode(errorCode);
        error.setMessage(message);
        error.setErrorFingerprint(PropertyIntakeHash.sha256(jobRef + "|" + targetType + "|" + rowNo + "|" + fileName + "|" + errorCode));
        error.setRetried(0);
        error.setCreatedAt(LocalDateTime.now());
        try {
            errorMapper.insert(error);
        } catch (RuntimeException e) {
            log.debug("Duplicate or failed intake error insert. jobRef={}, rowNo={}, code={}", jobRef, rowNo, errorCode, e);
        }
    }

    private void updateProgress(
            String jobRef,
            int total,
            int success,
            int failed,
            PropertyIntakeJobStatus finalStatus,
            LocalDateTime endedAt
    ) {
        PropertyIntakeJob job = requireJob(jobRef);
        job.setTotalCount(total);
        job.setRecordsCount(total);
        job.setSucceededCount(success);
        job.setFailedCount(failed);
        job.setErrorsTruncated(failed > ERROR_PREVIEW_LIMIT ? 1 : 0);
        if (finalStatus != null) {
            job.setStatus(finalStatus.name());
        }
        if (endedAt != null) {
            job.setEndedAt(endedAt);
        }
        jobMapper.updateById(job);
    }

    private void markJobFailed(String jobRef, String message) {
        PropertyIntakeJob job = jobMapper.selectOne(
                new LambdaQueryWrapper<PropertyIntakeJob>().eq(PropertyIntakeJob::getJobRef, jobRef).last("LIMIT 1")
        );
        if (job == null) {
            return;
        }
        job.setStatus(PropertyIntakeJobStatus.FAILED.name());
        job.setEndedAt(LocalDateTime.now());
        job.setLastErrorMessage(message);
        jobMapper.updateById(job);
    }

    private void writeResultSummary(String jobRef, int total, int success, int failed, String status, LocalDateTime endedAt) {
        String json = """
                {"jobRef":"%s","status":"%s","total":%d,"succeeded":%d,"failed":%d,"endedAt":"%s"}
                """.formatted(jobRef, status, total, success, failed, endedAt);
        storage.putString("bulk-import/jobs/" + jobRef + "/result/import-result.json", json);
    }

    private LambdaQueryWrapper<PropertyIntakeJob> buildJobQuery(
            String targetScope,
            String status,
            LocalDate from,
            LocalDate to,
            String owner
    ) {
        LambdaQueryWrapper<PropertyIntakeJob> query = new LambdaQueryWrapper<PropertyIntakeJob>()
                .eq(PropertyIntakeJob::getJobType, JOB_TYPE_BULK)
                .eq(PropertyIntakeJob::getIsDeleted, 0);
        if (targetScope != null && !targetScope.isBlank()) {
            query.eq(PropertyIntakeJob::getTargetScope, targetScope);
        }
        if (status != null && !status.isBlank()) {
            query.eq(PropertyIntakeJob::getStatus, status);
        }
        if (from != null) {
            query.ge(PropertyIntakeJob::getAcceptedAt, from.atStartOfDay());
        }
        if (to != null) {
            query.lt(PropertyIntakeJob::getAcceptedAt, to.plusDays(1).atStartOfDay());
        }
        if ("me".equals(owner)) {
            query.eq(PropertyIntakeJob::getOwnerUserId, SecurityUtils.getUserId());
        } else if (owner != null && !owner.isBlank()) {
            try {
                query.eq(PropertyIntakeJob::getOwnerUserId, Long.valueOf(owner));
            } catch (NumberFormatException ignored) {
                query.eq(PropertyIntakeJob::getOwnerUserId, -1L);
            }
        }
        return query;
    }

    private String generateJobRef(Long tenantId, String jobType, LocalDate date) {
        String datePart = date.format(DateTimeFormatter.BASIC_ISO_DATE);
        PropertyIntakeJobSeq seq = seqMapper.selectForUpdate(tenantId, jobType, datePart);
        if (seq == null) {
            seq = new PropertyIntakeJobSeq();
            seq.setTenantId(tenantId);
            seq.setJobType(jobType);
            seq.setDatePart(datePart);
            seq.setSeq(1);
            seqMapper.insertSeq(seq);
        } else {
            seq.setSeq(nullToZero(seq.getSeq()) + 1);
            seqMapper.updateSeq(seq);
        }
        return "%s-%s-%04d".formatted(jobType, datePart, seq.getSeq());
    }

    private void storeAttachments(String jobRef, List<MultipartFile> files) {
        if (files == null) {
            return;
        }
        for (MultipartFile file : files) {
            if (file == null || file.isEmpty()) {
                continue;
            }
            storage.put(attachmentKey(jobRef, safeOriginalFilename(file)), file);
        }
    }

    private String resolveCsvObjectKey(String jobRef, MultipartFile csv, String requestedCsvObjectKey) {
        String csvObjectKey = blankToNull(requestedCsvObjectKey);
        if (csvObjectKey != null) {
            if (!isSafeObjectRef(csvObjectKey)) {
                throw new BusinessException("不正な CSV object key です: {}", csvObjectKey);
            }
            if (!storage.exists(csvObjectKey)) {
                throw new BusinessException("CSV object が bucket に存在しません: {}", csvObjectKey);
            }
            return csvObjectKey;
        }

        if (csv == null || csv.isEmpty()) {
            throw new BusinessException("csvObjectKey は必須です");
        }

        String fallbackKey = inputCsvKey(jobRef);
        storage.put(fallbackKey, csv);
        return fallbackKey;
    }

    private int countRecords(String csvObjectKey) {
        try (InputStream inputStream = storage.open(csvObjectKey);
             BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            int count = 0;
            boolean header = true;
            String line;
            while ((line = reader.readLine()) != null) {
                if (header) {
                    header = false;
                    continue;
                }
                if (!line.isBlank()) {
                    count++;
                }
            }
            return count;
        } catch (IOException e) {
            throw new BusinessException("CSV ファイルの読込に失敗しました");
        }
    }

    private PropertyIntakeJob requireJob(String jobRef) {
        PropertyIntakeJob job = jobMapper.selectOne(
                new LambdaQueryWrapper<PropertyIntakeJob>()
                        .eq(PropertyIntakeJob::getJobRef, jobRef)
                        .eq(PropertyIntakeJob::getIsDeleted, 0)
                        .last("LIMIT 1")
        );
        if (job == null) {
            throw new BusinessException("ジョブが存在しません: {}", jobRef);
        }
        return job;
    }

    private JobDetailVO toDetailVO(PropertyIntakeJob job) {
        JobDetailVO vo = new JobDetailVO();
        vo.setJobRef(job.getJobRef());
        vo.setTargetScope(job.getTargetScope());
        vo.setStatus(job.getStatus());
        vo.setSucceeded(nullToZero(job.getSucceededCount()));
        vo.setFailed(nullToZero(job.getFailedCount()));
        vo.setTotal(nullToZero(job.getTotalCount()));
        vo.setStartedAt(toOffset(job.getStartedAt()));
        vo.setEndedAt(toOffset(job.getEndedAt()));
        vo.setOwner(new JobOwnerVO(toString(job.getOwnerUserId()), job.getOwnerDisplayName()));
        return vo;
    }

    private JobListItemVO toListItemVO(PropertyIntakeJob job) {
        JobListItemVO vo = new JobListItemVO();
        vo.setJobRef(job.getJobRef());
        vo.setTargetScope(job.getTargetScope());
        vo.setStatus(job.getStatus());
        vo.setSucceeded(nullToZero(job.getSucceededCount()));
        vo.setFailed(nullToZero(job.getFailedCount()));
        vo.setTotal(nullToZero(job.getTotalCount()));
        vo.setStartedAt(toOffset(job.getStartedAt()));
        vo.setEndedAt(toOffset(job.getEndedAt()));
        vo.setOwner(new JobOwnerVO(toString(job.getOwnerUserId()), job.getOwnerDisplayName()));
        return vo;
    }

    private JobErrorVO toErrorVO(PropertyIntakeJobError error) {
        JobErrorVO vo = new JobErrorVO();
        vo.setAssetKey(error.getAssetKey());
        vo.setDocKey(error.getDocKey());
        vo.setRowNo(error.getRowNo());
        vo.setErrorCode(error.getErrorCode());
        vo.setMessage(error.getMessage());
        vo.setFileName(error.getFileName() != null ? error.getFileName() : error.getAssetKey());
        return vo;
    }

    private Long ensureTenantContext() {
        Long tenantId = TenantContextHolder.getTenantId();
        if (tenantId == null) {
            tenantId = SecurityUtils.getUser().map(UserDetails::getTenantId).orElse(0L);
            TenantContextHolder.setTenantId(tenantId);
        }
        return tenantId;
    }

    private static String inputCsvKey(String jobRef) {
        return "bulk-import/jobs/" + jobRef + "/input/ledger.csv";
    }

    private static String attachmentKey(String jobRef, String fileName) {
        return "bulk-import/jobs/" + jobRef + "/input/files/" + fileName;
    }

    private static String safeOriginalFilename(MultipartFile file) {
        return fileName(file.getOriginalFilename());
    }

    private static String fileName(String ref) {
        String normalized = ref == null ? "unknown" : ref.replace('\\', '/');
        int slash = normalized.lastIndexOf('/');
        String fileName = slash >= 0 ? normalized.substring(slash + 1) : normalized;
        return fileName.isBlank() ? "unknown" : fileName;
    }

    private static boolean isSafeObjectRef(String ref) {
        if (ref == null || ref.isBlank()) {
            return false;
        }
        String normalized = ref.replace('\\', '/');
        return !normalized.startsWith("/")
                && !normalized.contains("../")
                && !normalized.equals("..")
                && !normalized.contains("\u0000");
    }

    private static String paramsJson(String csvObjectKey, Integer listedYear, String area) {
        return "{\"inputCsvPath\":\"" + escapeJson(csvObjectKey) + "\",\"listedYear\":"
                + (listedYear == null ? "null" : listedYear)
                + ",\"area\":" + (area == null ? "null" : "\"" + escapeJson(area) + "\"") + "}";
    }

    private static String escapeJson(String value) {
        return value == null ? "" : value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private static String stripBom(String value) {
        if (value != null && value.startsWith("\uFEFF")) {
            return value.substring(1);
        }
        return value;
    }

    private static String displayName(UserDetails user) {
        if (user == null) {
            return null;
        }
        return user.getNickname() != null ? user.getNickname() : user.getUsername();
    }

    private static OffsetDateTime toOffset(LocalDateTime dateTime) {
        return dateTime == null ? null : dateTime.atZone(TOKYO).toOffsetDateTime();
    }

    private static int nonNullSize(List<MultipartFile> files) {
        if (files == null) {
            return 0;
        }
        int count = 0;
        for (MultipartFile file : files) {
            if (file != null && !file.isEmpty()) {
                count++;
            }
        }
        return count;
    }

    private static int nullToZero(Integer value) {
        return value == null ? 0 : value;
    }

    private static int nullToOne(Integer value) {
        return value == null ? 1 : value;
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }

    private static String toString(Long value) {
        return value == null ? null : String.valueOf(value);
    }

    private static String csv(Object value) {
        if (value == null) {
            return "";
        }
        String text = String.valueOf(value);
        if (text.contains(",") || text.contains("\"") || text.contains("\n")) {
            return "\"" + text.replace("\"", "\"\"") + "\"";
        }
        return text;
    }

    private record RowResult(boolean successful) {
        static RowResult passed() {
            return new RowResult(true);
        }

        static RowResult failed() {
            return new RowResult(false);
        }
    }
}
