package com.blog.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.blog.common.PageResult;
import com.blog.common.ResultCode;
import com.blog.common.exception.BusinessException;
import com.blog.dto.CommentCreateRequest;
import com.blog.entity.Article;
import com.blog.entity.Comment;
import com.blog.mapper.ArticleMapper;
import com.blog.mapper.CommentMapper;
import com.blog.service.CommentService;
import com.blog.vo.CommentVO;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 评论业务实现
 *
 * <p>前台评论列表按「一级评论分页 + 二级回复内嵌」组织，
 * 与常见博客评论区的展示方式一致：先分页查一级评论，
 * 再批量查这些一级评论下的回复，避免逐条查询造成的 N+1 问题。
 */
@Service
@RequiredArgsConstructor
public class CommentServiceImpl implements CommentService {

    private final CommentMapper commentMapper;
    private final ArticleMapper articleMapper;

    @Override
    public PageResult<CommentVO> pageComments(Long articleId, long page, long size) {
        // 校验文章存在且已发布（前台只能看已发布文章的评论）
        checkArticlePublished(articleId);

        // 1. 分页查询一级评论（parent_id 为 NULL），按时间倒序
        Page<Comment> mpPage = commentMapper.selectPage(new Page<>(page, size),
                new LambdaQueryWrapper<Comment>()
                        .eq(Comment::getArticleId, articleId)
                        .eq(Comment::getStatus, 1)
                        .isNull(Comment::getParentId)
                        .orderByDesc(Comment::getCreateTime)
                        .orderByDesc(Comment::getId));

        List<Comment> parents = mpPage.getRecords();
        // 2. 批量查询这些一级评论下的二级回复，按时间正序
        Map<Long, List<CommentVO>> childrenMap = loadChildren(parents.stream().map(Comment::getId).toList());

        // 3. 组装：一级评论 + children
        List<CommentVO> records = new ArrayList<>(parents.size());
        for (Comment parent : parents) {
            CommentVO vo = toCommentVO(parent);
            vo.setChildren(childrenMap.getOrDefault(parent.getId(), new ArrayList<>()));
            records.add(vo);
        }
        return new PageResult<>(records, mpPage.getTotal(), mpPage.getCurrent(), mpPage.getSize());
    }

    @Override
    public CommentVO createComment(CommentCreateRequest request) {
        // 1. 文章必须存在且已发布
        checkArticlePublished(request.getArticleId());

        // 2. 校验父评论：必须存在、属于同一篇文章；若回复的是二级回复，则归到它的一级评论下
        Long parentId = request.getParentId();
        if (parentId != null) {
            Comment parent = commentMapper.selectById(parentId);
            if (parent == null || !request.getArticleId().equals(parent.getArticleId())) {
                throw new BusinessException(ResultCode.PARAM_ERROR.getCode(), "回复的评论不存在");
            }
            if (parent.getParentId() != null) {
                parentId = parent.getParentId();
            }
        }

        // 3. 保存评论：游客可匿名，登录用户记录 userId
        Comment comment = new Comment();
        comment.setArticleId(request.getArticleId());
        comment.setParentId(parentId);
        comment.setContent(request.getContent());
        comment.setNickname(request.getNickname());
        comment.setEmail(request.getEmail());
        comment.setUserId(getCurrentUserIdOrNull());
        // 默认显示（status=1），后台可改为隐藏
        comment.setStatus(1);
        commentMapper.insert(comment);

        // 4. 转为 VO 返回
        CommentVO vo = toCommentVO(comment);
        vo.setChildren(new ArrayList<>());
        return vo;
    }

    @Override
    public PageResult<CommentVO> pageAdminComments(long page, long size, Long articleId, Integer status) {
        // 后台扁平列表：包含一级评论与回复，方便统一审核
        LambdaQueryWrapper<Comment> wrapper = new LambdaQueryWrapper<>();
        if (articleId != null) {
            wrapper.eq(Comment::getArticleId, articleId);
        }
        if (status != null) {
            wrapper.eq(Comment::getStatus, status);
        }
        wrapper.orderByDesc(Comment::getCreateTime).orderByDesc(Comment::getId);

        Page<Comment> mpPage = commentMapper.selectPage(new Page<>(page, size), wrapper);
        List<CommentVO> records = new ArrayList<>(mpPage.getRecords().size());
        for (Comment comment : mpPage.getRecords()) {
            CommentVO vo = toCommentVO(comment);
            vo.setChildren(new ArrayList<>());
            records.add(vo);
        }
        return new PageResult<>(records, mpPage.getTotal(), mpPage.getCurrent(), mpPage.getSize());
    }

    @Override
    public void updateCommentStatus(Long id, Integer status) {
        Comment existing = commentMapper.selectById(id);
        if (existing == null) {
            throw new BusinessException(ResultCode.NOT_FOUND.getCode(), "评论不存在");
        }
        Comment update = new Comment();
        update.setId(id);
        update.setStatus(status);
        commentMapper.updateById(update);
    }

    @Override
    public void deleteComment(Long id) {
        Comment existing = commentMapper.selectById(id);
        if (existing == null) {
            throw new BusinessException(ResultCode.NOT_FOUND.getCode(), "评论不存在");
        }
        // 逻辑删除评论本身
        commentMapper.deleteById(id);
        // 如果是一级评论，其二级回复一并逻辑删除，避免出现「无主回复」
        commentMapper.delete(new LambdaQueryWrapper<Comment>().eq(Comment::getParentId, id));
    }

    /** 批量加载一级评论的二级回复：article_id 由一级评论决定，parent_id 直接关联 */
    private Map<Long, List<CommentVO>> loadChildren(List<Long> parentIds) {
        if (parentIds == null || parentIds.isEmpty()) {
            return Map.of();
        }
        List<Comment> children = commentMapper.selectList(new LambdaQueryWrapper<Comment>()
                .in(Comment::getParentId, parentIds)
                .eq(Comment::getStatus, 1)
                .orderByAsc(Comment::getCreateTime)
                .orderByAsc(Comment::getId));

        Map<Long, List<CommentVO>> result = new HashMap<>();
        for (Comment child : children) {
            CommentVO childVO = toCommentVO(child);
            childVO.setChildren(new ArrayList<>());
            result.computeIfAbsent(child.getParentId(), key -> new ArrayList<>()).add(childVO);
        }
        return result;
    }

    /** 校验文章存在且已发布 */
    private void checkArticlePublished(Long articleId) {
        Article article = articleMapper.selectById(articleId);
        if (article == null || !Integer.valueOf(1).equals(article.getStatus())) {
            throw new BusinessException(ResultCode.NOT_FOUND.getCode(), "文章不存在或已下架");
        }
    }

    /** 评论实体 → VO */
    private CommentVO toCommentVO(Comment comment) {
        CommentVO vo = new CommentVO();
        vo.setId(comment.getId());
        vo.setArticleId(comment.getArticleId());
        vo.setUserId(comment.getUserId());
        vo.setParentId(comment.getParentId());
        vo.setContent(comment.getContent());
        vo.setNickname(comment.getNickname());
        vo.setEmail(comment.getEmail());
        vo.setStatus(comment.getStatus());
        vo.setCreateTime(comment.getCreateTime());
        return vo;
    }

    /** 取当前登录用户 ID；游客未登录时返回 null（不抛异常） */
    private Long getCurrentUserIdOrNull() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof Long userId) {
            return userId;
        }
        return null;
    }
}
