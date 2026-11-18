package com.blog.controller;

import com.blog.cache.RateLimitService;
import com.blog.common.PageResult;
import com.blog.common.Result;
import com.blog.common.util.IpUtil;
import com.blog.common.util.PageUtil;
import com.blog.dto.CommentCreateRequest;
import com.blog.service.CommentService;
import com.blog.vo.CommentVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 评论前台接口（无需登录）
 *
 * <p>发表评论接口使用 Redis Lua 固定窗口限流：每 IP 每分钟最多 10 次。
 */
@Tag(name = "评论（前台）", description = "评论分页、发表评论")
@RestController
@RequestMapping("/api/comment")
@RequiredArgsConstructor
public class CommentController {

    private final CommentService commentService;
    private final RateLimitService rateLimitService;

    /** 文章评论分页：一级评论分页 + 二级回复内嵌 */
    @Operation(summary = "评论分页", description = "按一级评论分页，children 内嵌二级回复")
    @GetMapping("/page")
    public Result<PageResult<CommentVO>> page(
            @Parameter(description = "文章 ID（必填）") @RequestParam Long articleId,
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") long page,
            @Parameter(description = "每页条数") @RequestParam(defaultValue = "10") long size) {
        long currentPage = PageUtil.normalizePage(page);
        long pageSize = PageUtil.normalizeSize(size);
        return Result.ok(commentService.pageComments(articleId, currentPage, pageSize));
    }

    /**
     * 发表评论（游客可匿名，登录用户自动记录 userId）
     *
     * <p>限流：同一 IP 每分钟最多 10 次，超限返回 code=4290。
     */
    @Operation(summary = "发表评论", description = "限流：每 IP 每分钟 10 次")
    @PostMapping
    public Result<CommentVO> create(@Valid @RequestBody CommentCreateRequest request,
                                    HttpServletRequest httpRequest) {
        // 先限流再执行业务，防止刷评论
        rateLimitService.checkCommentLimit(IpUtil.getClientIp(httpRequest));
        return Result.ok(commentService.createComment(request));
    }
}
