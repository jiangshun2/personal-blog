package com.blog.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.blog.entity.Article;
import com.blog.mapper.ArticleMapper;
import com.blog.mapper.CategoryMapper;
import com.blog.mapper.CommentMapper;
import com.blog.mapper.TagMapper;
import com.blog.service.DashboardService;
import com.blog.vo.DashboardStatsVO;
import com.blog.vo.RecentArticleVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * 仪表盘统计实现
 *
 * <p>数据量小且需要实时性，直接查库即可，不做缓存。
 * 浏览量展示的是数据库累计值（Redis 增量每 60 秒回写一次，最多滞后 1 分钟）。
 */
@Service
@RequiredArgsConstructor
public class DashboardServiceImpl implements DashboardService {

    private final ArticleMapper articleMapper;
    private final CommentMapper commentMapper;
    private final CategoryMapper categoryMapper;
    private final TagMapper tagMapper;

    @Override
    public DashboardStatsVO getStats() {
        DashboardStatsVO stats = new DashboardStatsVO();
        // selectCount(null) 会统计所有未逻辑删除的记录
        stats.setArticleCount(defaultZero(articleMapper.selectCount(null)));
        stats.setCommentCount(defaultZero(commentMapper.selectCount(null)));
        stats.setCategoryCount(defaultZero(categoryMapper.selectCount(null)));
        stats.setTagCount(defaultZero(tagMapper.selectCount(null)));
        stats.setTotalViewCount(defaultZero(articleMapper.sumViewCount()));

        // 最近 5 篇文章：只查需要的列，按创建时间倒序
        List<Article> recent = articleMapper.selectList(new LambdaQueryWrapper<Article>()
                .select(Article::getId, Article::getTitle, Article::getViewCount, Article::getCreateTime)
                .orderByDesc(Article::getCreateTime)
                .orderByDesc(Article::getId)
                .last("LIMIT 5"));
        List<RecentArticleVO> recentArticles = new ArrayList<>(recent.size());
        for (Article article : recent) {
            RecentArticleVO vo = new RecentArticleVO();
            vo.setId(article.getId());
            vo.setTitle(article.getTitle());
            vo.setViewCount(article.getViewCount() == null ? 0L : article.getViewCount());
            vo.setCreateTime(article.getCreateTime());
            recentArticles.add(vo);
        }
        stats.setRecentArticles(recentArticles);
        return stats;
    }

    /** Long 为 null 时按 0 处理 */
    private long defaultZero(Long value) {
        return value == null ? 0L : value;
    }
}
