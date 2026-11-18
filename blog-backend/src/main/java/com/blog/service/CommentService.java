package com.blog.service;

import com.blog.common.PageResult;
import com.blog.dto.CommentCreateRequest;
import com.blog.vo.CommentVO;

/**
 * 评论业务接口
 */
public interface CommentService {

    /**
     * 前台评论分页：按一级评论分页，children 内嵌二级回复
     *
     * @param articleId 文章 ID（必填）
     * @param page      页码
     * @param size      每页条数
     * @return 分页结果
     */
    PageResult<CommentVO> pageComments(Long articleId, long page, long size);

    /**
     * 发表评论（游客可发，登录用户自动记录 userId）
     *
     * @param request 评论内容
     * @return 新建的评论
     */
    CommentVO createComment(CommentCreateRequest request);

    /**
     * 后台评论分页（扁平列表，可按文章 / 状态筛选）
     *
     * @param page      页码
     * @param size      每页条数
     * @param articleId 文章 ID（可选）
     * @param status    状态（可选）
     * @return 分页结果
     */
    PageResult<CommentVO> pageAdminComments(long page, long size, Long articleId, Integer status);

    /**
     * 修改评论状态（审核 / 隐藏）
     *
     * @param id     评论 ID
     * @param status 0=隐藏，1=显示
     */
    void updateCommentStatus(Long id, Integer status);

    /**
     * 删除评论（逻辑删除，同时删除其二级回复）
     *
     * @param id 评论 ID
     */
    void deleteComment(Long id);
}
