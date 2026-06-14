package com.smartdx.retail.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.smartdx.core.result.PageResult;
import com.smartdx.retail.converter.ProductConverter;
import com.smartdx.retail.mapper.AlertMapper;
import com.smartdx.retail.mapper.InventoryMapper;
import com.smartdx.retail.mapper.InventoryTransactionMapper;
import com.smartdx.retail.mapper.ProductMapper;
import com.smartdx.retail.mapper.SalesDetailMapper;
import com.smartdx.retail.model.entity.Alert;
import com.smartdx.retail.model.entity.Inventory;
import com.smartdx.retail.model.entity.InventoryTransaction;
import com.smartdx.retail.model.entity.Product;
import com.smartdx.retail.model.entity.SalesDetail;
import com.smartdx.retail.model.form.ProductForm;
import com.smartdx.retail.model.query.ProductPageQuery;
import com.smartdx.retail.model.vo.ProductPageVO;
import com.smartdx.retail.service.ProductService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Product Service Implementation
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProductServiceImpl extends ServiceImpl<ProductMapper, Product> implements ProductService {

    private final ProductConverter productConverter;
    private final AlertMapper alertMapper;
    private final InventoryMapper inventoryMapper;
    private final InventoryTransactionMapper inventoryTransactionMapper;
    private final SalesDetailMapper salesDetailMapper;

    @Override
    public PageResult<ProductPageVO> getProductPage(ProductPageQuery queryParams) {
        Page<Product> page = new Page<>(queryParams.getPageNum(), queryParams.getPageSize());

        LambdaQueryWrapper<Product> queryWrapper = new LambdaQueryWrapper<Product>()
                .like(StringUtils.hasText(queryParams.getProductName()), Product::getProductName, queryParams.getProductName())
                .like(StringUtils.hasText(queryParams.getProductCode()), Product::getProductCode, queryParams.getProductCode())
                .eq(queryParams.getCategoryId() != null, Product::getCategoryId, queryParams.getCategoryId())
                .eq(StringUtils.hasText(queryParams.getStatus()), Product::getStatus, queryParams.getStatus())
                .orderByDesc(Product::getCreateTime);

        IPage<Product> result = this.page(page, queryWrapper);

        List<ProductPageVO> list = result.getRecords().stream()
                .map(productConverter::entity2Vo)
                .collect(Collectors.toList());

        return PageResult.success(list, result.getTotal());
    }

    @Override
    public List<Product> listProducts() {
        LambdaQueryWrapper<Product> queryWrapper = new LambdaQueryWrapper<Product>()
                .orderByDesc(Product::getCreateTime);
        return this.list(queryWrapper);
    }

    @Override
    public Product getProductById(Long id) {
        return this.getById(id);
    }

    @Override
    @Transactional(timeout = 15)
    public boolean createProduct(ProductForm form) {
        log.info("Creating product. code={}", form.getCode());

        // Check duplicate product code
        LambdaQueryWrapper<Product> queryWrapper = new LambdaQueryWrapper<Product>()
                .eq(Product::getProductCode, form.getCode());
        Product existingProduct = this.getOne(queryWrapper, false);

        if (existingProduct != null) {
            throw new IllegalArgumentException("Product code '" + form.getCode() + "' already exists");
        }

        Product product = productConverter.form2Entity(form);
        boolean saved = this.save(product);
        log.info("Created product. code={}, id={}", product.getProductCode(), product.getId());
        return saved;
    }

    @Override
    @Transactional(timeout = 15)
    public boolean updateProduct(Long id, ProductForm form) {
        log.info("Updating product. id={}, code={}", id, form.getCode());
        Product product = productConverter.form2Entity(form);
        product.setId(id);
        boolean updated = this.updateById(product);
        log.info("Updated product. id={}, code={}, updated={}", id, product.getProductCode(), updated);
        return updated;
    }

    @Override
    @Transactional
    public boolean deleteProduct(Long id) {
        // Delete related sales details
        LambdaQueryWrapper<SalesDetail> salesDetailWrapper = new LambdaQueryWrapper<SalesDetail>()
                .eq(SalesDetail::getProductId, id);
        salesDetailMapper.delete(salesDetailWrapper);

        // Delete related inventory transactions
        LambdaQueryWrapper<InventoryTransaction> txnWrapper = new LambdaQueryWrapper<InventoryTransaction>()
                .eq(InventoryTransaction::getProductId, id);
        inventoryTransactionMapper.delete(txnWrapper);

        // Delete related inventory
        LambdaQueryWrapper<Inventory> inventoryWrapper = new LambdaQueryWrapper<Inventory>()
                .eq(Inventory::getProductId, id);
        inventoryMapper.delete(inventoryWrapper);

        // Delete related alerts
        LambdaQueryWrapper<Alert> alertWrapper = new LambdaQueryWrapper<Alert>()
                .eq(Alert::getProductId, id);
        alertMapper.delete(alertWrapper);

        return this.removeById(id);
    }
}
