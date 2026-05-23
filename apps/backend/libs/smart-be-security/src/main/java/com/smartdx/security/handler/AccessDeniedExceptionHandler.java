package com.smartdx.security.handler;

import com.smartdx.core.result.ResponseWriter;
import com.smartdx.core.result.ResultCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;

import java.io.IOException;

/**
 * アクセス拒否ハンドラー
 */
@Slf4j
public class AccessDeniedExceptionHandler implements AccessDeniedHandler {

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, AccessDeniedException accessDeniedException) throws IOException {
        log.warn("アクセス拒否: {} - {}", accessDeniedException.getMessage(), request.getRequestURI());
        ResponseWriter.writeError(response, ResultCode.ACCESS_PERMISSION_EXCEPTION);
    }
}
