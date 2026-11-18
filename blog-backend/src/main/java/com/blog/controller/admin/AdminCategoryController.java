package com.blog.controller.admin;

import com.blog.common.Result;
import com.blog.dto.CategorySaveRequest;
import com.blog.service.CategoryService;
import com.blog.vo.CategoryVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 分类后台管理接口（需登录）
 */
@Tag(name = "分类管理（后台）", description = "分类增删改查")
@RestController
@RequestMapping("/api/admin/category")
@RequiredArgsConstructor
public class AdminCategoryController {

    private final CategoryService categoryService;

    /** 分类列表（含文章数量） */
    @Operation(summary = "后台分类列表")
    @GetMapping("/list")
    public Result<List<CategoryVO>> list() {
        return Result.ok(categoryService.listCategories());
    }

    /** 新增分类 */
    @Operation(summary = "新增分类")
    @PostMapping
    public Result<Void> create(@Valid @RequestBody CategorySaveRequest request) {
        categoryService.createCategory(request);
        return Result.ok();
    }

    /** 修改分类 */
    @Operation(summary = "修改分类")
    @PutMapping("/{id}")
    public Result<Void> update(@Parameter(description = "分类 ID") @PathVariable Long id,
                               @Valid @RequestBody CategorySaveRequest request) {
        categoryService.updateCategory(id, request);
        return Result.ok();
    }

    /** 删除分类：分类下仍有文章时返回 4000 并提示 */
    @Operation(summary = "删除分类", description = "分类下仍有文章时禁止删除")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@Parameter(description = "分类 ID") @PathVariable Long id) {
        categoryService.deleteCategory(id);
        return Result.ok();
    }
}
