package com.blog.controller;

import com.blog.common.Result;
import com.blog.service.CategoryService;
import com.blog.vo.CategoryVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 分类前台接口（无需登录）
 */
@Tag(name = "分类（前台）", description = "分类列表")
@RestController
@RequestMapping("/api/category")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;

    /** 分类列表（带多级缓存，含每个分类下已发布文章数量） */
    @Operation(summary = "分类列表", description = "带多级缓存，含文章数量")
    @GetMapping("/list")
    public Result<List<CategoryVO>> list() {
        return Result.ok(categoryService.listCategories());
    }
}
