package com.smartdx.retail.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.smartdx.retail.mapper.CategoryMapper;
import com.smartdx.retail.model.entity.Category;
import com.smartdx.retail.service.CategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Category Service Implementation
 */
@Service
@RequiredArgsConstructor
public class CategoryServiceImpl extends ServiceImpl<CategoryMapper, Category> implements CategoryService {

    @Override
    public List<Category> listCategories() {
        LambdaQueryWrapper<Category> queryWrapper = new LambdaQueryWrapper<Category>()
                .orderByAsc(Category::getSortOrder);
        return this.list(queryWrapper);
    }

    @Override
    public Category getCategoryById(Long id) {
        return this.getById(id);
    }

    @Override
    public boolean createCategory(Category category) {
        return this.save(category);
    }

    @Override
    public boolean updateCategory(Long id, Category category) {
        category.setId(id);
        return this.updateById(category);
    }

    @Override
    public boolean deleteCategory(Long id) {
        return this.removeById(id);
    }
}
