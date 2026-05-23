package com.smartdx.retail.service;

import com.smartdx.retail.model.entity.Alert;

import java.util.List;

/**
 * Alert Service Interface
 */
public interface AlertService {

    List<Alert> listAlerts(Long storeId, String status);

    Alert getAlertById(Long id);

    boolean createAlert(Alert alert);

    boolean updateAlertStatus(Long id, String status, String resolutionNote);

    boolean deleteAlert(Long id);
}
