package com.smartdx.retail.ai.controller;

import com.smartdx.core.result.Result;
import com.smartdx.core.result.ResultCode;
import com.smartdx.retail.ai.model.req.AlertAssistantReq;
import com.smartdx.retail.ai.model.vo.AlertAssistantVO;
import com.smartdx.retail.ai.service.AlertAssistantService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * AI alert assistant controller.
 *
 * @author jason.w
 */
@Tag(name = "AI Alert Assistant API")
@RestController
@RequestMapping("/api/v1/retail/ai/alerts")
@RequiredArgsConstructor
public class AlertAssistantController {

    private final AlertAssistantService alertAssistantService;

    @Operation(summary = "Get today's priority alerts with AI summary")
    @PostMapping("/priority")
    public ResponseEntity<Result<AlertAssistantVO>> priority(@RequestBody AlertAssistantReq request) {
        try {
            return ResponseEntity.ok(Result.success(alertAssistantService.answerPriorityAlerts(request)));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Result.failed(ResultCode.USER_REQUEST_PARAMETER_ERROR, e.getMessage()));
        }
    }
}
