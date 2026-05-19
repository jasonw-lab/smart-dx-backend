package com.smartdx.retail.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.smartdx.retail.mapper.AlertMapper;
import com.smartdx.retail.model.entity.Alert;
import com.smartdx.retail.service.AlertService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Alert Service Implementation
 */
@Service
@RequiredArgsConstructor
public class AlertServiceImpl extends ServiceImpl<AlertMapper, Alert> implements AlertService {

    @Override
    public List<Alert> listAlerts(Long storeId, String status) {
        LambdaQueryWrapper<Alert> queryWrapper = new LambdaQueryWrapper<Alert>()
                .eq(storeId != null, Alert::getStoreId, storeId)
                .eq(StringUtils.hasText(status), Alert::getStatus, status)
                .orderByDesc(Alert::getDetectedAt);
        return this.list(queryWrapper);
    }

    @Override
    public Alert getAlertById(Long id) {
        return this.getById(id);
    }

    @Override
    public boolean createAlert(Alert alert) {
        if (alert.getDetectedAt() == null) {
            alert.setDetectedAt(LocalDateTime.now());
        }
        return this.save(alert);
    }

    @Override
    public boolean updateAlertStatus(Long id, String status, String resolutionNote) {
        Alert alert = this.getById(id);
        if (alert == null) {
            return false;
        }

        alert.setStatus(status);

        LocalDateTime now = LocalDateTime.now();
        switch (status) {
            case "ACK":
                alert.setAcknowledgedAt(now);
                break;
            case "RESOLVED":
                alert.setResolvedAt(now);
                alert.setResolutionNote(resolutionNote);
                break;
            case "CLOSED":
                alert.setClosedAt(now);
                break;
        }

        return this.updateById(alert);
    }

    @Override
    public boolean deleteAlert(Long id) {
        return this.removeById(id);
    }
}
