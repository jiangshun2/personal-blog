package com.blog.controller.admin;

import com.blog.common.Result;
import com.blog.dto.SiteConfigUpdateRequest;
import com.blog.service.SiteConfigService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 站点设置后台接口（需登录）
 */
@Tag(name = "站点设置（后台）", description = "更新站点配置")
@RestController
@RequestMapping("/api/admin/site")
@RequiredArgsConstructor
public class AdminSiteConfigController {

    private final SiteConfigService siteConfigService;

    /** 更新站点配置：更新后立即失效 blog:site:config 缓存 */
    @Operation(summary = "更新站点配置")
    @PutMapping("/config")
    public Result<Void> update(@Valid @RequestBody SiteConfigUpdateRequest request) {
        siteConfigService.updateSiteConfig(request);
        return Result.ok();
    }
}
