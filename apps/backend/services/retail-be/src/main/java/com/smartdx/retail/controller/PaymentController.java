package com.smartdx.retail.controller;

import com.smartdx.core.result.Result;
import com.smartdx.retail.model.form.PaymentPayload;
import com.smartdx.retail.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 決済結果受信API
 */
@Tag(name = "決済結果受信API")
@RestController
@RequestMapping("/api/v1/retail/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @Operation(summary = "決済結果受信", description = "外部世界（PowerJobシミュレーター）からの決済結果を受信し、売上データを記録・在庫を減算する")
    @PostMapping
    public Result<Long> receivePayment(@RequestBody @Valid PaymentPayload payload) {
        Long salesId = paymentService.receivePayment(payload);
        return Result.success(salesId);
    }
}
