package com.smartdx.property.controller;

import com.smartdx.core.exception.BusinessException;
import com.smartdx.core.result.Result;
import com.smartdx.property.exception.PropertyErrorCode;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;

import java.util.stream.Collectors;

/**
 * Property モジュール専用例外ハンドラ
 * PropertyErrorCode に基づいて適切な HTTP ステータスを返す
 */
@RestControllerAdvice(basePackages = "com.smartdx.property.controller")
@Order(Ordered.HIGHEST_PRECEDENCE)
@Slf4j
public class PropertyExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<Result<Void>> handlePropertyBusinessException(BusinessException e) {
        log.error("Property business exception: code={}, message={}",
                e.getResultCode() != null ? e.getResultCode().getCode() : null,
                e.getMessage());

        HttpStatus httpStatus = resolveHttpStatus(e);

        if (e.getResultCode() != null) {
            return ResponseEntity.status(httpStatus).body(Result.failed(e.getResultCode(), e.getMessage()));
        }
        return ResponseEntity.status(httpStatus).body(Result.failed(e.getMessage()));
    }

    private HttpStatus resolveHttpStatus(BusinessException e) {
        if (e.getResultCode() == null) {
            return HttpStatus.INTERNAL_SERVER_ERROR;
        }

        String code = e.getResultCode().getCode();

        // 404 Not Found
        if (PropertyErrorCode.LISTING_NOT_FOUND.getCode().equals(code) ||
            PropertyErrorCode.ASSET_NOT_FOUND.getCode().equals(code) ||
            PropertyErrorCode.EMBEDDING_NOT_FOUND.getCode().equals(code) ||
            PropertyErrorCode.DEMO_REF_NOT_FOUND.getCode().equals(code) ||
            PropertyErrorCode.IMAGE_NOT_FOUND.getCode().equals(code)) {
            return HttpStatus.NOT_FOUND;
        }

        // 422 Unprocessable Entity
        if (PropertyErrorCode.VECTOR_NOT_READY.getCode().equals(code)) {
            return HttpStatus.UNPROCESSABLE_ENTITY;
        }

        // 400 Bad Request
        if (PropertyErrorCode.VALIDATION_ERROR.getCode().equals(code) ||
            PropertyErrorCode.INVALID_PROPERTY_KEY.getCode().equals(code) ||
            PropertyErrorCode.INVALID_SCOPE.getCode().equals(code) ||
            PropertyErrorCode.INVALID_FLAG.getCode().equals(code) ||
            PropertyErrorCode.INVALID_MIME.getCode().equals(code) ||
            PropertyErrorCode.INVALID_AREA.getCode().equals(code) ||
            PropertyErrorCode.INVALID_PROPERTY_TYPE.getCode().equals(code) ||
            PropertyErrorCode.INVALID_CSV_SCHEMA.getCode().equals(code) ||
            PropertyErrorCode.NO_SEARCH_ORIGIN.getCode().equals(code)) {
            return HttpStatus.BAD_REQUEST;
        }

        // 403 Forbidden
        if (PropertyErrorCode.SCOPE_NOT_ALLOWED.getCode().equals(code) ||
            PropertyErrorCode.FORBIDDEN.getCode().equals(code)) {
            return HttpStatus.FORBIDDEN;
        }

        // 401 Unauthorized
        if (PropertyErrorCode.UNAUTHORIZED.getCode().equals(code)) {
            return HttpStatus.UNAUTHORIZED;
        }

        // 409 Conflict
        if (PropertyErrorCode.VERSION_MISMATCH.getCode().equals(code) ||
            PropertyErrorCode.INVALID_STATE_TRANSITION.getCode().equals(code)) {
            return HttpStatus.CONFLICT;
        }

        // 429 Too Many Requests
        if (PropertyErrorCode.RATE_LIMIT_EXCEEDED.getCode().equals(code) ||
            PropertyErrorCode.EMBEDDING_QUOTA_EXCEEDED.getCode().equals(code)) {
            return HttpStatus.TOO_MANY_REQUESTS;
        }

        // 413 Payload Too Large
        if (PropertyErrorCode.ASSET_TOO_LARGE.getCode().equals(code)) {
            return HttpStatus.PAYLOAD_TOO_LARGE;
        }

        // 503 Service Unavailable (ADR-011: OpenSearch専用戦略)
        if (PropertyErrorCode.OPENSEARCH_UNAVAILABLE.getCode().equals(code)) {
            return HttpStatus.SERVICE_UNAVAILABLE;
        }

        // 500 Internal Server Error
        if (PropertyErrorCode.INTERNAL_ERROR.getCode().equals(code) ||
            PropertyErrorCode.EMBEDDING_EXTRACTION_FAILED.getCode().equals(code)) {
            return HttpStatus.INTERNAL_SERVER_ERROR;
        }

        // デフォルト: 200 OK（後方互換性）
        return HttpStatus.OK;
    }

    /**
     * 必須リクエストパラメータ欠落 → 400 Bad Request
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<Result<Void>> handleMissingServletRequestParameterException(MissingServletRequestParameterException e) {
        log.error("Property missing request parameter: {}", e.getMessage());
        String msg = "Required parameter '" + e.getParameterName() + "' is missing";
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Result.failed(PropertyErrorCode.VALIDATION_ERROR, msg));
    }

    /**
     * @RequestBody のバリデーション失敗 → 400 Bad Request
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Result<Void>> handleMethodArgumentNotValidException(MethodArgumentNotValidException e) {
        log.error("Property method argument validation exception: {}", e.getMessage());
        String msg = e.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining("；"));
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Result.failed(PropertyErrorCode.VALIDATION_ERROR, msg));
    }

    /**
     * @PathVariable / @RequestParam のバリデーション失敗 → 400 Bad Request
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<Result<Void>> handleConstraintViolationException(ConstraintViolationException e) {
        log.error("Property validation exception: {}", e.getMessage());
        String msg = e.getConstraintViolations().stream()
                .map(ConstraintViolation::getMessage)
                .collect(Collectors.joining("；"));
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Result.failed(PropertyErrorCode.VALIDATION_ERROR, msg));
    }

    /**
     * Spring 6+ での @Validated 検証失敗 → 400 Bad Request
     */
    @ExceptionHandler(HandlerMethodValidationException.class)
    public ResponseEntity<Result<Void>> handleHandlerMethodValidationException(HandlerMethodValidationException e) {
        log.error("Property handler method validation exception: {}", e.getMessage());
        try {
            String msg = e.getAllErrors().stream()
                    .map(MessageSourceResolvable::getDefaultMessage)
                    .collect(Collectors.joining("；"));
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Result.failed(PropertyErrorCode.VALIDATION_ERROR, msg));
        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Result.failed(PropertyErrorCode.VALIDATION_ERROR, e.getMessage()));
        }
    }

    /**
     * IllegalStateException → 500 Internal Server Error (with logging)
     */
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Result<Void>> handleIllegalStateException(IllegalStateException e) {
        log.error("Property illegal state exception: {}", e.getMessage(), e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Result.failed(PropertyErrorCode.INTERNAL_ERROR, e.getMessage()));
    }

    /**
     * Catch-all for unexpected exceptions → 500 Internal Server Error (with logging)
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Result<Void>> handleGenericException(Exception e) {
        log.error("Property unexpected exception: {}", e.getMessage(), e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Result.failed(PropertyErrorCode.INTERNAL_ERROR, "内部エラーが発生しました"));
    }
}
