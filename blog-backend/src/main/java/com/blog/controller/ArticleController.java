package com.blog.controller;

import com.blog.common.PageResult;
import com.blog.common.Result;
import com.blog.common.util.PageUtil;
import com.blog.service.ArticleService;
import com.blog.vo.ArticleDetailVO;
import com.blog.vo.ArticleListVO;
import com.blog.vo.ArchiveVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 文章前台接口（无需登录）
 *
 * <p>只返回 status=1（已发布）的文章；列表与详情走 Caffeine + Redis 多级缓存。
 */
@Tag(name = "文章（前台）", description = "文章分页、详情、归档")
@RestController
@RequestMapping("/api/article")
@RequiredArgsConstructor
public class ArticleController {

    private final ArticleService articleService;

    /**
     * 文章分页列表
     *
     * @param page       页码，默认 1
     * @param size       每页条数，默认 10
     * @param categoryId 分类筛选（可选）
     * @param tagId      标签筛选（可选）
     * @param keyword    关键字（可选）
     */
    @Operation(summary = "分页查询文章", description = "支持分类 / 标签 / 关键字筛选，带多级缓存")
    @GetMapping("/page")
    public Result<PageResult<ArticleListVO>> page(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") long page,
            @Parameter(description = "每页条数") @RequestParam(defaultValue = "10") long size,
            @Parameter(description = "分类 ID") @RequestParam(required = false) Long categoryId,
            @Parameter(description = "标签 ID") @RequestParam(required = false) Long tagId,
            @Parameter(description = "关键字") @RequestParam(required = false) String keyword) {
        long currentPage = PageUtil.normalizePage(page);
        long pageSize = PageUtil.normalizeSize(size);
        return Result.ok(articleService.pageArticles(currentPage, pageSize, categoryId, tagId, keyword));
    }

    /**
     * 文章详情
     *
     * <p>每次调用浏览量 +1（Redis 计数），并返回上一篇 / 下一篇。
     */
    @Operation(summary = "文章详情", description = "带多级缓存；每次访问浏览量 +1；返回上一篇 / 下一篇")
    @GetMapping("/{id}")
    public Result<ArticleDetailVO> detail(@Parameter(description = "文章 ID") @PathVariable Long id) {
        return Result.ok(articleService.getArticleDetail(id));
    }

    /** 归档：按年份倒序返回已发布文章 */
    @Operation(summary = "文章归档", description = "按年份倒序返回已发布文章")
    @GetMapping("/archive")
    public Result<List<ArchiveVO>> archive() {
        return Result.ok(articleService.listArchive());
    }
}
