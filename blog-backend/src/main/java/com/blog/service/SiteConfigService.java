package com.blog.service;

import com.blog.dto.SiteConfigUpdateRequest;

import java.util.Map;

/**
 * 站点设置业务接口
 */
public interface SiteConfigService {

    /**
     * 获取站点配置（带缓存的读接口）
     *
     * @return key-value，例如 { "site_name": "我的博客", ... }
     */
    Map<String, String> getSiteConfig();

    /**
     * 更新站点配置（不存在则新增），更新后失效缓存
     *
     * @param request 站点配置
     */
    void updateSiteConfig(SiteConfigUpdateRequest request);
}
