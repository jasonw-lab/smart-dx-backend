package com.smartdx.retail.controller;

import com.smartdx.core.result.PageResult;
import com.smartdx.core.result.Result;
import com.smartdx.retail.model.entity.Product;
import com.smartdx.retail.model.form.ProductForm;
import com.smartdx.retail.model.query.ProductPageQuery;
import com.smartdx.retail.model.vo.ProductPageVO;
import com.smartdx.retail.service.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Product Controller
 * <p>
 * REST API path: /api/v1/retail/products (unchanged from original smart-retail-dx)
 * </p>
 */
@Tag(name = "Product API")
@RestController
@RequestMapping("/api/v1/retail/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    @Operation(summary = "Get product page")
    @GetMapping("/page")
    public PageResult<ProductPageVO> getProductPage(@Valid ProductPageQuery queryParams) {
        return productService.getProductPage(queryParams);
    }

    @Operation(summary = "Get all products")
    @GetMapping
    public Result<List<Product>> listProducts() {
        return Result.success(productService.listProducts());
    }

    @Operation(summary = "Create product")
    @PostMapping
    public Result<?> createProduct(@RequestBody @Valid ProductForm form) {
        return Result.judge(productService.createProduct(form));
    }

    @Operation(summary = "Update product")
    @PutMapping("/{id}")
    public Result<?> updateProduct(@PathVariable Long id, @RequestBody @Valid ProductForm form) {
        return Result.judge(productService.updateProduct(id, form));
    }

    @Operation(summary = "Delete product")
    @DeleteMapping("/{id}")
    public Result<?> deleteProduct(@PathVariable Long id) {
        return Result.judge(productService.deleteProduct(id));
    }

    @Operation(summary = "Get product by ID")
    @GetMapping("/{id}")
    public Result<Product> getProduct(@PathVariable Long id) {
        return Result.success(productService.getProductById(id));
    }
}
