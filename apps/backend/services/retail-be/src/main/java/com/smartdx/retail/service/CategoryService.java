package com.smartdx.retail.service;

import com.smartdx.retail.model.entity.Category;

import java.util.List;

/**
 * Category Service Interface
 */
public interface CategoryService {

    List<Category> listCategories();

    Category getCategoryById(Long id);

    boolean createCategory(Category category);

    boolean updateCategory(Long id, Category category);

    boolean deleteCategory(Long id);
}
