package com.smartdx.retail.controller;

import com.smartdx.core.result.Result;
import com.smartdx.retail.model.entity.Category;
import com.smartdx.retail.service.CategoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Category Controller
 * <p>
 * REST API path: /api/v1/retail/categories (unchanged from original smart-retail-dx)
 * </p>
 */
@Tag(name = "Category API")
@RestController
@RequestMapping("/api/v1/retail/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;

    @Operation(summary = "Get all categories")
    @GetMapping
    public Result<List<Category>> listCategories() {
        return Result.success(categoryService.listCategories());
    }

    @Operation(summary = "Get category by ID")
    @GetMapping("/{id}")
    public Result<Category> getCategory(@PathVariable Long id) {
        return Result.success(categoryService.getCategoryById(id));
    }

    @Operation(summary = "Create category")
    @PostMapping
    public Result<?> createCategory(@RequestBody Category category) {
        return Result.judge(categoryService.createCategory(category));
    }

    @Operation(summary = "Update category")
    @PutMapping("/{id}")
    public Result<?> updateCategory(@PathVariable Long id, @RequestBody Category category) {
        return Result.judge(categoryService.updateCategory(id, category));
    }

    @Operation(summary = "Delete category")
    @DeleteMapping("/{id}")
    public Result<?> deleteCategory(@PathVariable Long id) {
        return Result.judge(categoryService.deleteCategory(id));
    }
}
