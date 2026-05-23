package com.smartdx.property.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.smartdx.property.model.req.PropertyLookupReq;
import com.smartdx.property.model.vo.PropertyDetailVO;
import com.smartdx.property.model.vo.PropertySummaryVO;

public interface PropertySearchService {

    IPage<PropertySummaryVO> lookup(PropertyLookupReq req);

    /**
     * 物件詳細取得（LST-LST-01）- DB fallback
     *
     * @param propertyKey 物件キー（UUID v4）
     * @param scope      スコープ: published または draft
     * @return 物件詳細
     */
    PropertyDetailVO getDetail(String propertyKey, String scope);
}
