package com.blog.controller.admin;

import com.blog.common.Result;
import com.blog.service.DashboardService;
import com.blog.vo.DashboardStatsVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 仪表盘后台接口（需登录）
 */
@Tag(name = "仪表盘（后台）", description = "统计数据")
@RestController
@RequestMapping("/api/admin/dashboard")
@RequiredArgsConstructor
public class AdminDashboardController {

    private final DashboardService dashboardService;

    /** 仪表盘统计：文章 / 评论 / 分类 / 标签数量、总浏览量、最近文章 */
    @Operation(summary = "仪表盘统计")
    @GetMapping("/stats")
    public Result<DashboardStatsVO> stats() {
        return Result.ok(dashboardService.getStats());
    }
}
