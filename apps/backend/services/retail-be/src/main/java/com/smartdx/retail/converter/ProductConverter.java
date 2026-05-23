package com.smartdx.retail.converter;

import com.smartdx.retail.model.entity.Product;
import com.smartdx.retail.model.form.ProductForm;
import com.smartdx.retail.model.vo.ProductPageVO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

/**
 * Product Converter
 */
@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface ProductConverter {

    /**
     * Convert ProductForm to Product entity
     */
    @Mapping(source = "code", target = "productCode")
    @Mapping(source = "name", target = "productName")
    @Mapping(source = "price", target = "unitPrice")
    Product form2Entity(ProductForm form);

    /**
     * Convert Product entity to ProductPageVO
     */
    ProductPageVO entity2Vo(Product entity);
}
