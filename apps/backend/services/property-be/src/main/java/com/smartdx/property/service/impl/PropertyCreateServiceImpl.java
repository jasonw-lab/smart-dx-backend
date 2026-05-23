package com.smartdx.property.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.smartdx.core.exception.BusinessException;
import com.smartdx.security.model.UserDetails;
import com.smartdx.security.util.SecurityUtils;
import com.smartdx.tenant.TenantContextHolder;
import com.smartdx.property.exception.PropertyErrorCode;
import com.smartdx.property.mapper.OutboxEventMapper;
import com.smartdx.property.mapper.PropertyIntakeJobMapper;
import com.smartdx.property.mapper.PropertyIntakeJobSeqMapper;
import com.smartdx.property.mapper.PropertyListingMapper;
import com.smartdx.property.model.entity.OutboxEvent;
import com.smartdx.property.model.entity.PropertyIntakeJob;
import com.smartdx.property.model.entity.PropertyIntakeJobSeq;
import com.smartdx.property.model.entity.PropertyListing;
import com.smartdx.property.model.enums.PropertyIntakeJobStatus;
import com.smartdx.property.model.vo.PropertyCreateAcceptedVO;
import com.smartdx.property.model.vo.ReceivedCountVO;
import com.smartdx.property.model.vo.StateTransitionVO;
import com.smartdx.property.service.PropertyCreateService;
import com.smartdx.property.storage.PropertyIntakeStorage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * 物件登録サービス実装
 * LST-INT-01: POST /api/v1/properties
 */
@Service
@Slf4j
public class PropertyCreateServiceImpl implements PropertyCreateService {

    private static final String JOB_TYPE_SINGLE = "single";
    private static final String TARGET_SCOPE_DRAFT = "draft";
    private static final String OUTBOX_STATUS_PENDING = "PENDING";
    private static final String OUTBOX_EVENT_TYPE_PROPERTY_INTAKE = "PROPERTY_INTAKE";
    private static final String OUTBOX_AGGREGATE_TYPE_PROPERTY = "Property";
    private static final long MAX_FILE_SIZE = 80L * 1024 * 1024; // 80MB
    private static final int MAX_PHOTOS = 30;
    private static final int MAX_DOCS = 20;

    private static final Set<String> ALLOWED_PHOTO_EXTENSIONS = new HashSet<>(Arrays.asList("jpg", "jpeg", "png"));
    private static final Set<String> ALLOWED_DOC_EXTENSIONS = new HashSet<>(Arrays.asList(
            "pdf", "docx", "doc", "xlsx", "xlsm", "xls", "pptx", "ppt", "html", "htm", "txt"
    ));
    private static final Set<String> ALLOWED_LEDGER_EXTENSIONS = new HashSet<>(Arrays.asList("csv", "xlsx"));
    private static final Set<String> ALLOWED_PROPERTY_TYPES = new HashSet<>(Arrays.asList(
            "mansion", "house", "office", "retail", "land"
    ));
    private static final Set<String> RESUBMITTABLE_STATUSES = new HashSet<>(Arrays.asList("PENDING", "REJECTED"));

    private final PropertyIntakeJobMapper jobMapper;
    private final PropertyIntakeJobSeqMapper seqMapper;
    private final PropertyListingMapper listingMapper;
    private final OutboxEventMapper outboxEventMapper;
    private final PropertyIntakeStorage storage;

    public PropertyCreateServiceImpl(
            PropertyIntakeJobMapper jobMapper,
            PropertyIntakeJobSeqMapper seqMapper,
            PropertyListingMapper listingMapper,
            OutboxEventMapper outboxEventMapper,
            PropertyIntakeStorage storage
    ) {
        this.jobMapper = jobMapper;
        this.seqMapper = seqMapper;
        this.listingMapper = listingMapper;
        this.outboxEventMapper = outboxEventMapper;
        this.storage = storage;
    }

    @Override
    @Transactional
    public PropertyCreateAcceptedVO acceptPropertyCreate(
            MultipartFile csv,
            MultipartFile excel,
            List<MultipartFile> photos,
            List<MultipartFile> docs,
            Integer listedYear,
            String area
    ) {
        validateLedger(csv, excel);
        validatePhotos(photos);
        validateDocs(docs);
        validateListedYear(listedYear);
        validateArea(area);

        Long tenantId = ensureTenantContext();
        UserDetails user = SecurityUtils.getUser().orElse(null);
        LocalDateTime acceptedAt = LocalDateTime.now();
        String jobRef = generateJobRef(tenantId, JOB_TYPE_SINGLE, acceptedAt.toLocalDate());
        String propertyKey = UUID.randomUUID().toString();

        int photoCount = countNonEmpty(photos);
        int docCount = countNonEmpty(docs);

        storeFiles(jobRef, csv, excel, photos, docs);

        PropertyIntakeJob job = createJob(
                jobRef, propertyKey, null, null, null,
                photoCount, docCount, acceptedAt, user, listedYear, area
        );
        jobMapper.insert(job);

        createOutboxEvent(tenantId, propertyKey, jobRef);

        return PropertyCreateAcceptedVO.builder()
                .jobRef(jobRef)
                .scope(TARGET_SCOPE_DRAFT)
                .propertyKey(propertyKey)
                .receivedCount(new ReceivedCountVO(1, photoCount, docCount))
                .build();
    }

    @Override
    @Transactional
    public PropertyCreateAcceptedVO acceptPropertyResubmit(
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
    ) {
        if (resubmitOf == null || resubmitOf.isBlank()) {
            throw new BusinessException(PropertyErrorCode.VALIDATION_ERROR, "修正再提出対象の物件キーが必要です");
        }
        if (expectedVersion == null) {
            throw new BusinessException(PropertyErrorCode.VALIDATION_ERROR, "バージョンが必要です");
        }

        validateLedger(csv, excel);
        validatePhotos(photos);
        validateDocs(docs);
        validateListedYear(listedYear);
        validateArea(area);

        Long tenantId = ensureTenantContext();

        PropertyListing existing = listingMapper.selectOne(
                new LambdaQueryWrapper<PropertyListing>()
                        .eq(PropertyListing::getPropertyKey, resubmitOf)
                        .eq(PropertyListing::getScope, TARGET_SCOPE_DRAFT)
                        .eq(PropertyListing::getIsDeleted, 0)
                        .last("LIMIT 1")
        );

        if (existing == null) {
            throw new BusinessException(PropertyErrorCode.LISTING_NOT_FOUND);
        }

        if (!expectedVersion.equals(existing.getVersion())) {
            throw new BusinessException(PropertyErrorCode.VERSION_MISMATCH);
        }

        String currentStatus = existing.getReviewStatus();
        if (currentStatus == null || !RESUBMITTABLE_STATUSES.contains(currentStatus)) {
            throw new BusinessException(PropertyErrorCode.INVALID_STATE_TRANSITION,
                    "現在の状態 [" + currentStatus + "] からは修正再提出できません");
        }

        UserDetails user = SecurityUtils.getUser().orElse(null);
        LocalDateTime acceptedAt = LocalDateTime.now();
        String jobRef = generateJobRef(tenantId, JOB_TYPE_SINGLE, acceptedAt.toLocalDate());

        int photoCount = countNonEmpty(photos);
        int docCount = countNonEmpty(docs);

        storeFiles(jobRef, csv, excel, photos, docs);

        PropertyIntakeJob job = createJob(
                jobRef, resubmitOf, resubmitOf, currentStatus, "PENDING",
                photoCount, docCount, acceptedAt, user, listedYear, area
        );
        jobMapper.insert(job);

        createOutboxEvent(tenantId, resubmitOf, jobRef);

        return PropertyCreateAcceptedVO.builder()
                .jobRef(jobRef)
                .scope(TARGET_SCOPE_DRAFT)
                .propertyKey(resubmitOf)
                .receivedCount(new ReceivedCountVO(1, photoCount, docCount))
                .expectedVersion(existing.getVersion())
                .stateTransition(new StateTransitionVO(currentStatus, "PENDING"))
                .build();
    }

    private PropertyIntakeJob createJob(
            String jobRef,
            String propertyKey,
            String resubmitOfPropertyKey,
            String resubmitStateFrom,
            String resubmitStateTo,
            int photoCount,
            int docCount,
            LocalDateTime acceptedAt,
            UserDetails user,
            Integer listedYear,
            String area
    ) {
        PropertyIntakeJob job = new PropertyIntakeJob();
        job.setJobRef(jobRef);
        job.setJobType(JOB_TYPE_SINGLE);
        job.setTargetScope(TARGET_SCOPE_DRAFT);
        job.setStatus(PropertyIntakeJobStatus.PENDING.name());
        job.setPropertyKey(propertyKey);
        job.setTotalCount(1);
        job.setRecordsCount(1);
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

        if (resubmitOfPropertyKey != null) {
            job.setParamsJson(buildParamsJson(resubmitOfPropertyKey, resubmitStateFrom, resubmitStateTo, listedYear, area));
        } else {
            job.setParamsJson(buildParamsJson(null, null, null, listedYear, area));
        }
        return job;
    }

    private void createOutboxEvent(Long tenantId, String propertyKey, String jobRef) {
        OutboxEvent event = new OutboxEvent();
        event.setTenantId(tenantId);
        event.setEventType(OUTBOX_EVENT_TYPE_PROPERTY_INTAKE);
        event.setAggregateType(OUTBOX_AGGREGATE_TYPE_PROPERTY);
        event.setAggregateId(propertyKey);
        event.setPayload(buildOutboxPayload(propertyKey, jobRef));
        event.setStatus(OUTBOX_STATUS_PENDING);
        event.setRetryCount(0);
        event.setCreatedAt(LocalDateTime.now());
        outboxEventMapper.insert(event);
    }

    private void validateLedger(MultipartFile csv, MultipartFile excel) {
        if ((csv == null || csv.isEmpty()) && (excel == null || excel.isEmpty())) {
            throw new BusinessException(PropertyErrorCode.VALIDATION_ERROR, "CSV または Excel ファイルが必要です");
        }
        if ((csv != null && !csv.isEmpty()) && (excel != null && !excel.isEmpty())) {
            throw new BusinessException(PropertyErrorCode.VALIDATION_ERROR, "CSV と Excel は同時に指定できません");
        }
        MultipartFile ledger = (csv != null && !csv.isEmpty()) ? csv : excel;
        if (ledger != null) {
            validateFileSize(ledger, "台帳ファイル");
            String ext = getFileExtension(ledger.getOriginalFilename());
            if (!ALLOWED_LEDGER_EXTENSIONS.contains(ext.toLowerCase())) {
                throw new BusinessException(PropertyErrorCode.INVALID_MIME, "台帳は CSV または XLSX 形式のみ対応しています");
            }
        }
    }

    private void validatePhotos(List<MultipartFile> photos) {
        if (photos == null) return;
        int count = 0;
        for (MultipartFile photo : photos) {
            if (photo == null || photo.isEmpty()) continue;
            count++;
            validateFileSize(photo, "写真");
            String ext = getFileExtension(photo.getOriginalFilename());
            if (!ALLOWED_PHOTO_EXTENSIONS.contains(ext.toLowerCase())) {
                throw new BusinessException(PropertyErrorCode.INVALID_MIME,
                        "写真は JPG/JPEG/PNG 形式のみ対応しています: " + photo.getOriginalFilename());
            }
        }
        if (count > MAX_PHOTOS) {
            throw new BusinessException(PropertyErrorCode.VALIDATION_ERROR,
                    "写真は最大" + MAX_PHOTOS + "枚までです（現在: " + count + "枚）");
        }
    }

    private void validateDocs(List<MultipartFile> docs) {
        if (docs == null) return;
        int count = 0;
        for (MultipartFile doc : docs) {
            if (doc == null || doc.isEmpty()) continue;
            count++;
            validateFileSize(doc, "文書");
            String ext = getFileExtension(doc.getOriginalFilename());
            if (!ALLOWED_DOC_EXTENSIONS.contains(ext.toLowerCase())) {
                throw new BusinessException(PropertyErrorCode.INVALID_MIME,
                        "文書は PDF/DOCX/XLSX/PPTX/HTML/TXT 形式のみ対応しています: " + doc.getOriginalFilename());
            }
        }
        if (count > MAX_DOCS) {
            throw new BusinessException(PropertyErrorCode.VALIDATION_ERROR,
                    "文書は最大" + MAX_DOCS + "件までです（現在: " + count + "件）");
        }
    }

    private void validateFileSize(MultipartFile file, String fileType) {
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new BusinessException(PropertyErrorCode.ASSET_TOO_LARGE,
                    fileType + "のサイズが上限（80MB）を超えています: " + file.getOriginalFilename());
        }
    }

    private void validateListedYear(Integer listedYear) {
        if (listedYear == null) {
            throw new BusinessException(PropertyErrorCode.VALIDATION_ERROR, "掲載年は必須です");
        }
        int currentYear = LocalDate.now().getYear();
        if (listedYear < 2000 || listedYear > currentYear) {
            throw new BusinessException(PropertyErrorCode.VALIDATION_ERROR,
                    "掲載年は 2000〜" + currentYear + " の範囲で指定してください");
        }
    }

    private void validateArea(String area) {
        if (area == null || area.isBlank()) {
            throw new BusinessException(PropertyErrorCode.VALIDATION_ERROR, "エリアコードは必須です");
        }
    }

    private void storeFiles(
            String jobRef,
            MultipartFile csv,
            MultipartFile excel,
            List<MultipartFile> photos,
            List<MultipartFile> docs
    ) {
        String basePath = "single-import/jobs/" + jobRef + "/input/";
        if (csv != null && !csv.isEmpty()) {
            storage.put(basePath + "ledger.csv", csv);
        }
        if (excel != null && !excel.isEmpty()) {
            storage.put(basePath + "ledger.xlsx", excel);
        }
        if (photos != null) {
            for (MultipartFile photo : photos) {
                if (photo != null && !photo.isEmpty()) {
                    storage.put(basePath + "photos/" + safeFileName(photo), photo);
                }
            }
        }
        if (docs != null) {
            for (MultipartFile doc : docs) {
                if (doc != null && !doc.isEmpty()) {
                    storage.put(basePath + "docs/" + safeFileName(doc), doc);
                }
            }
        }
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
            seq.setSeq((seq.getSeq() == null ? 0 : seq.getSeq()) + 1);
            seqMapper.updateSeq(seq);
        }
        return "%s-%s-%04d".formatted(jobType, datePart, seq.getSeq());
    }

    private Long ensureTenantContext() {
        Long tenantId = TenantContextHolder.getTenantId();
        if (tenantId == null) {
            tenantId = SecurityUtils.getUser().map(UserDetails::getTenantId).orElse(0L);
            TenantContextHolder.setTenantId(tenantId);
        }
        return tenantId;
    }

    private static String displayName(UserDetails user) {
        if (user == null) return null;
        return user.getNickname() != null ? user.getNickname() : user.getUsername();
    }

    private static String blankToNull(String value) {
        return (value == null || value.isBlank()) ? null : value;
    }

    private static String getFileExtension(String filename) {
        if (filename == null) return "";
        int dotIndex = filename.lastIndexOf('.');
        return dotIndex >= 0 ? filename.substring(dotIndex + 1) : "";
    }

    private static String safeFileName(MultipartFile file) {
        String original = file.getOriginalFilename();
        if (original == null || original.isBlank()) return "unknown";
        String normalized = original.replace('\\', '/');
        int slash = normalized.lastIndexOf('/');
        return slash >= 0 ? normalized.substring(slash + 1) : normalized;
    }

    private static int countNonEmpty(List<MultipartFile> files) {
        if (files == null) return 0;
        int count = 0;
        for (MultipartFile file : files) {
            if (file != null && !file.isEmpty()) count++;
        }
        return count;
    }

    private static String buildParamsJson(
            String resubmitOf,
            String stateFrom,
            String stateTo,
            Integer listedYear,
            String area
    ) {
        StringBuilder sb = new StringBuilder("{");
        sb.append("\"listedYear\":").append(listedYear == null ? "null" : listedYear);
        sb.append(",\"area\":").append(area == null ? "null" : "\"" + escapeJson(area) + "\"");
        if (resubmitOf != null) {
            sb.append(",\"resubmitOf\":\"").append(escapeJson(resubmitOf)).append("\"");
            sb.append(",\"stateFrom\":\"").append(stateFrom).append("\"");
            sb.append(",\"stateTo\":\"").append(stateTo).append("\"");
        }
        sb.append("}");
        return sb.toString();
    }

    private static String buildOutboxPayload(String propertyKey, String jobRef) {
        return "{\"propertyKey\":\"" + escapeJson(propertyKey) + "\",\"jobRef\":\"" + escapeJson(jobRef) + "\"}";
    }

    private static String escapeJson(String value) {
        if (value == null) return "";
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
