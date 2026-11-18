package com.blog.controller.admin;

import com.blog.common.PageResult;
import com.blog.common.Result;
import com.blog.common.util.PageUtil;
import com.blog.dto.ArticleSaveRequest;
import com.blog.dto.ArticleTopRequest;
import com.blog.service.ArticleService;
import com.blog.vo.ArticleAdminDetailVO;
import com.blog.vo.ArticleAdminVO;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 文章后台管理接口（需登录）
 */
@Tag(name = "文章管理（后台）", description = "文章增删改查、置顶")
@RestController
@RequestMapping("/api/admin/article")
@RequiredArgsConstructor
public class AdminArticleController {

    private final ArticleService articleService;

    /** 后台文章分页：可按关键字 / 状态筛选，包含草稿 */
    @Operation(summary = "后台文章分页")
    @GetMapping("/page")
    public Result<PageResult<ArticleAdminVO>> page(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") long page,
            @Parameter(description = "每页条数") @RequestParam(defaultValue = "10") long size,
            @Parameter(description = "关键字") @RequestParam(required = false) String keyword,
            @Parameter(description = "状态：0草稿 / 1发布") @RequestParam(required = false) Integer status) {
        long currentPage = PageUtil.normalizePage(page);
        long pageSize = PageUtil.normalizeSize(size);
        return Result.ok(articleService.pageAdminArticles(currentPage, pageSize, keyword, status));
    }

    /** 后台文章详情：额外返回正文与标签 ID，供编辑页回填 */
    @Operation(summary = "后台文章详情")
    @GetMapping("/{id}")
    public Result<ArticleAdminDetailVO> detail(@Parameter(description = "文章 ID") @PathVariable Long id) {
        return Result.ok(articleService.getAdminArticle(id));
    }

    /** 新增文章 */
    @Operation(summary = "新增文章")
    @PostMapping
    public Result<Void> create(@Valid @RequestBody ArticleSaveRequest request) {
        articleService.createArticle(request);
        return Result.ok();
    }

    /** 修改文章 */
    @Operation(summary = "修改文章")
    @PutMapping("/{id}")
    public Result<Void> update(@Parameter(description = "文章 ID") @PathVariable Long id,
                               @Valid @RequestBody ArticleSaveRequest request) {
        articleService.updateArticle(id, request);
        return Result.ok();
    }

    /** 删除文章（逻辑删除，同时清理标签关联） */
    @Operation(summary = "删除文章")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@Parameter(description = "文章 ID") @PathVariable Long id) {
        articleService.deleteArticle(id);
        return Result.ok();
    }

    /** 置顶 / 取消置顶 */
    @Operation(summary = "置顶 / 取消置顶")
    @PutMapping("/{id}/top")
    public Result<Void> top(@Parameter(description = "文章 ID") @PathVariable Long id,
                            @Valid @RequestBody ArticleTopRequest request) {
        articleService.updateTop(id, request.getIsTop());
        return Result.ok();
    }
}
