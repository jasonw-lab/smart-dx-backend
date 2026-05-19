package com.smartdx.retail.service;

import com.smartdx.core.result.PageResult;
import com.smartdx.retail.model.entity.Product;
import com.smartdx.retail.model.form.ProductForm;
import com.smartdx.retail.model.query.ProductPageQuery;
import com.smartdx.retail.model.vo.ProductPageVO;

import java.util.List;

/**
 * Product Service Interface
 */
public interface ProductService {

    /**
     * Get product page
     */
    PageResult<ProductPageVO> getProductPage(ProductPageQuery queryParams);

    /**
     * Get all products
     */
    List<Product> listProducts();

    /**
     * Get product by ID
     */
    Product getProductById(Long id);

    /**
     * Create product
     */
    boolean createProduct(ProductForm form);

    /**
     * Update product
     */
    boolean updateProduct(Long id, ProductForm form);

    /**
     * Delete product
     */
    boolean deleteProduct(Long id);
}
