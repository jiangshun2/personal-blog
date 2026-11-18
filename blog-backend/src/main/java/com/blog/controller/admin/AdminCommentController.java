package com.blog.controller.admin;

import com.blog.common.PageResult;
import com.blog.common.Result;
import com.blog.common.util.PageUtil;
import com.blog.dto.CommentStatusRequest;
import com.blog.service.CommentService;
import com.blog.vo.CommentVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 评论后台管理接口（需登录）
 */
@Tag(name = "评论管理（后台）", description = "评论审核、删除")
@RestController
@RequestMapping("/api/admin/comment")
@RequiredArgsConstructor
public class AdminCommentController {

    private final CommentService commentService;

    /** 后台评论分页：可按文章 / 状态筛选 */
    @Operation(summary = "后台评论分页")
    @GetMapping("/page")
    public Result<PageResult<CommentVO>> page(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") long page,
            @Parameter(description = "每页条数") @RequestParam(defaultValue = "10") long size,
            @Parameter(description = "文章 ID") @RequestParam(required = false) Long articleId,
            @Parameter(description = "状态：0隐藏 / 1显示") @RequestParam(required = false) Integer status) {
        long currentPage = PageUtil.normalizePage(page);
        long pageSize = PageUtil.normalizeSize(size);
        return Result.ok(commentService.pageAdminComments(currentPage, pageSize, articleId, status));
    }

    /** 修改评论状态（审核 / 隐藏） */
    @Operation(summary = "修改评论状态")
    @PutMapping("/{id}/status")
    public Result<Void> updateStatus(@Parameter(description = "评论 ID") @PathVariable Long id,
                                     @Valid @RequestBody CommentStatusRequest request) {
        commentService.updateCommentStatus(id, request.getStatus());
        return Result.ok();
    }

    /** 删除评论（逻辑删除，一级评论会连带删除其回复） */
    @Operation(summary = "删除评论")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@Parameter(description = "评论 ID") @PathVariable Long id) {
        commentService.deleteComment(id);
        return Result.ok();
    }
}
