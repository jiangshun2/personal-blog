package com.blog.controller;

import com.blog.common.Result;
import com.blog.service.SiteConfigService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 站点配置前台接口（无需登录）
 */
@Tag(name = "站点配置（前台）", description = "获取站点配置")
@RestController
@RequestMapping("/api/site")
@RequiredArgsConstructor
public class SiteConfigController {

    private final SiteConfigService siteConfigService;

    /** 获取站点配置（带多级缓存，TTL 1 小时） */
    @Operation(summary = "获取站点配置", description = "返回 site_name / site_description / site_avatar / github / email 等键值")
    @GetMapping("/config")
    public Result<Map<String, String>> config() {
        return Result.ok(siteConfigService.getSiteConfig());
    }
}
