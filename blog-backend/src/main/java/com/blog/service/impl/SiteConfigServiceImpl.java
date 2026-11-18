package com.blog.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.blog.cache.CacheKeys;
import com.blog.cache.CacheService;
import com.blog.dto.SiteConfigUpdateRequest;
import com.blog.entity.SiteConfig;
import com.blog.mapper.SiteConfigMapper;
import com.blog.service.SiteConfigService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 站点设置业务实现
 *
 * <p>站点配置是低频写、高频读的数据，非常适合缓存：
 * 缓存键 blog:site:config，TTL 1 小时；后台修改后立即失效缓存，前台马上能看到新配置。
 */
@Service
@RequiredArgsConstructor
public class SiteConfigServiceImpl implements SiteConfigService {

    private final SiteConfigMapper siteConfigMapper;
    private final CacheService cacheService;

    @Override
    public Map<String, String> getSiteConfig() {
        // 读穿多级缓存：Caffeine → Redis → MySQL
        return cacheService.getOrLoad(
                CacheKeys.SITE_CONFIG,
                CacheKeys.SITE_CONFIG_TTL_SECONDS,
                this::loadSiteConfig);
    }

    @Override
    public void updateSiteConfig(SiteConfigUpdateRequest request) {
        // 契约中的 5 个配置项：只更新请求里传了的字段
        Map<String, String> values = new LinkedHashMap<>();
        if (request.getSiteName() != null) {
            values.put("site_name", request.getSiteName());
        }
        if (request.getSiteDescription() != null) {
            values.put("site_description", request.getSiteDescription());
        }
        if (request.getSiteAvatar() != null) {
            values.put("site_avatar", request.getSiteAvatar());
        }
        if (request.getGithub() != null) {
            values.put("github", request.getGithub());
        }
        if (request.getEmail() != null) {
            values.put("email", request.getEmail());
        }
        values.forEach(this::saveOrUpdateConfig);

        // 写后精准失效站点配置缓存
        cacheService.evict(CacheKeys.SITE_CONFIG);
    }

    /** 站点配置回源：config_key → config_value */
    private Map<String, String> loadSiteConfig() {
        List<SiteConfig> configs = siteConfigMapper.selectList(
                new LambdaQueryWrapper<SiteConfig>().orderByAsc(SiteConfig::getId));
        Map<String, String> result = new LinkedHashMap<>();
        for (SiteConfig config : configs) {
            // value 为 null 时返回空串，避免前端拿到 null 需要额外判空
            result.put(config.getConfigKey(), config.getConfigValue() == null ? "" : config.getConfigValue());
        }
        return result;
    }

    /** 单条配置的 upsert：存在则更新 value，不存在则新增 */
    private void saveOrUpdateConfig(String configKey, String configValue) {
        SiteConfig existing = siteConfigMapper.selectOne(new LambdaQueryWrapper<SiteConfig>()
                .eq(SiteConfig::getConfigKey, configKey));
        if (existing != null) {
            SiteConfig update = new SiteConfig();
            update.setId(existing.getId());
            update.setConfigValue(configValue);
            // updateTime 由 MyMetaObjectHandler 自动填充
            siteConfigMapper.updateById(update);
        } else {
            SiteConfig config = new SiteConfig();
            config.setConfigKey(configKey);
            config.setConfigValue(configValue);
            siteConfigMapper.insert(config);
        }
    }
}
