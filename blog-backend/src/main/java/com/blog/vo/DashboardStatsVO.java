package com.blog.vo;

import lombok.Data;

import java.util.List;

/**
 * 后台仪表盘统计
 */
@Data
public class DashboardStatsVO {

    /** 文章总数 */
    private Long articleCount;
    /** 评论总数 */
    private Long commentCount;
    /** 分类总数 */
    private Long categoryCount;
    /** 标签总数 */
    private Long tagCount;
    /** 所有文章浏览量总和 */
    private Long totalViewCount;
    /** 最近 5 篇文章 */
    private List<RecentArticleVO> recentArticles;
}
