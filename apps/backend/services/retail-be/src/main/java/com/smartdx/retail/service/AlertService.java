package com.smartdx.retail.service;

import com.smartdx.retail.model.vo.AlertPageVO;

import java.util.List;

/**
 * Alert Service Interface
 */
public interface AlertService {

    List<AlertPageVO> listAlerts(Long storeId, String status);

    AlertPageVO getAlertById(Long id);

    boolean createAlert(com.smartdx.retail.model.entity.Alert alert);

    boolean updateAlertStatus(Long id, String status, String resolutionNote);

    boolean deleteAlert(Long id);
}
