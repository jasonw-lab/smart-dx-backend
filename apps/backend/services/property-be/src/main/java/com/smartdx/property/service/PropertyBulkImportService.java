package com.smartdx.property.service;

import com.smartdx.property.model.vo.BulkImportAcceptedVO;
import com.smartdx.property.model.vo.JobDetailVO;
import com.smartdx.property.model.vo.JobErrorPageVO;
import com.smartdx.property.model.vo.JobListVO;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.List;

public interface PropertyBulkImportService {

    BulkImportAcceptedVO acceptBulkImport(
            MultipartFile csv,
            String csvObjectKey,
            List<MultipartFile> photos,
            List<MultipartFile> docs,
            Integer listedYear,
            String area
    );

    JobDetailVO getJob(String jobRef);

    JobListVO listJobs(String targetScope, String status, LocalDate from, LocalDate to, String owner, int page, int size);

    JobErrorPageVO listErrors(String jobRef, int page, int size);

    void writeErrorsCsv(String jobRef, HttpServletResponse response);
}
