package com.smartdx.property.controller;

import com.smartdx.core.result.PageResult;
import com.smartdx.core.result.Result;
import com.smartdx.property.model.req.PropertyLookupReq;
import com.smartdx.property.model.vo.PropertyDetailVO;
import com.smartdx.property.model.vo.PropertySummaryVO;
import com.smartdx.property.service.PropertyEsSearchService;
import com.smartdx.property.service.PropertySearchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "不動産物件検索")
@RestController
@RequestMapping("/api/v1/properties")
@RequiredArgsConstructor
@Validated
@Slf4j
public class PropertyListingController {

    private final PropertyEsSearchService propertyEsSearchService;

    private final PropertySearchService propertySearchService;

    @Operation(summary = "物件検索API", description = "LST-QRY-01: 属性／全文／類似画像／複合検索")
    @PostMapping("/search")
    public PageResult<PropertySummaryVO> search(@Valid @RequestBody PropertyLookupReq req) {
        log.info("API call start: POST /api/v1/properties/search. scope={}, sortBy={}, page={}, size={}",
                req.getScope(), req.getSortBy(), req.getPage(), req.getSize());
        try {
            PageResult<PropertySummaryVO> result = PageResult.success(propertyEsSearchService.lookup(req));
            log.info("API call end: POST /api/v1/properties/search. scope={}, sortBy={}, page={}, size={}",
                    req.getScope(), req.getSortBy(), req.getPage(), req.getSize());
            return result;
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (RuntimeException e) {
            log.warn("OpenSearch search failed. Falling back to DB search. scope={}, sortBy={}, page={}, size={}",
                    req.getScope(), req.getSortBy(), req.getPage(), req.getSize(), e);
            PageResult<PropertySummaryVO> result = PageResult.success(propertySearchService.lookup(req));
            log.info("API call end (fallback): POST /api/v1/properties/search. scope={}, sortBy={}, page={}, size={}",
                    req.getScope(), req.getSortBy(), req.getPage(), req.getSize());
            return result;
        }
    }

    @Operation(summary = "物件詳細取得API", description = "LST-LST-01: 1件の物件の詳細情報を取得")
    @GetMapping("/{propertyId}")
    public Result<PropertyDetailVO> getDetail(
            @Parameter(description = "物件ID（UUID v4）", required = true)
            @PathVariable("propertyId")
            @Pattern(regexp = "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$",
                    message = "物件IDの形式が不正です")
            String propertyId,

            @Parameter(description = "スコープ: published または draft", required = true)
            @RequestParam
            @NotBlank(message = "scope パラメータは必須です")
            @Pattern(regexp = "^(published|draft)$", message = "無効なスコープ値です")
            String scope
    ) {
        log.info("API call start: GET /api/v1/properties/{}. scope={}", propertyId, scope);
        try {
            Result<PropertyDetailVO> result = Result.success(propertyEsSearchService.getDetail(propertyId, scope));
            log.info("API call end: GET /api/v1/properties/{}. scope={}", propertyId, scope);
            return result;
        } catch (IllegalStateException e) {
            log.warn("OpenSearch getDetail failed. Falling back to DB search. propertyId={}, scope={}",
                    propertyId, scope, e);
            Result<PropertyDetailVO> result = Result.success(propertySearchService.getDetail(propertyId, scope));
            log.info("API call end (fallback): GET /api/v1/properties/{}. scope={}", propertyId, scope);
            return result;
        }
    }
}
