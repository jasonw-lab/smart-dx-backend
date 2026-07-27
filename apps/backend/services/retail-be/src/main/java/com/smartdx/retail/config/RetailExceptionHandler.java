package com.smartdx.retail.config;

import com.smartdx.core.result.Result;
import com.smartdx.retail.exception.InventoryShortageException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * retail-be モジュール固有の例外ハンドラ
 * <p>
 * core の GlobalExceptionHandler（catch-all は 500）より先に評価されるよう最高優先とする。
 * </p>
 */
@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RetailExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(RetailExceptionHandler.class);

    /**
     * 在庫残量不足（409 Conflict）
     */
    @ExceptionHandler(InventoryShortageException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public Result<Void> handleInventoryShortageException(InventoryShortageException e) {
        log.warn("Inventory shortage: {}", e.getMessage());
        return Result.failed(e.getMessage());
    }
}
