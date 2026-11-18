package com.blog.service;

import com.blog.vo.DashboardStatsVO;

/**
 * 仪表盘统计业务接口
 */
public interface DashboardService {

    /** 统计文章 / 评论 / 分类 / 标签数量、总浏览量、最近文章 */
    DashboardStatsVO getStats();
}
