package com.smartdx.retail.ai.service;

import com.smartdx.retail.ai.model.req.AlertAssistantReq;
import com.smartdx.retail.ai.model.vo.AlertAssistantVO;

/**
 * AI alert assistant service.
 *
 * @author jason.w
 */
public interface AlertAssistantService {

    /**
     * Answer the priority alert question.
     *
     * @param request the assistant request
     * @return response containing summary and alerts
     */
    AlertAssistantVO answerPriorityAlerts(AlertAssistantReq request);
}
