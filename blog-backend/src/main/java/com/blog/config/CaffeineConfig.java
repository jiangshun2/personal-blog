package com.blog.config;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

/**
 * Caffeine 本地缓存配置（多级缓存中的一级缓存）
 *
 * <p>说明：
 * <ul>
 *   <li>一级缓存放在进程内存里，命中时没有网络开销，速度最快；</li>
 *   <li>本地缓存 TTL（10 分钟）故意短于 Redis TTL（5 分钟 ~ 1 小时），
 *       即使某次写操作失效失败，脏数据最多存活 10 分钟；</li>
 *   <li>写操作会通过 CacheService 精准 invalidate 对应 key，保证一致性。</li>
 * </ul>
 */
@Configuration
public class CaffeineConfig {

    @Bean
    public Cache<String, Object> localCache() {
        return Caffeine.newBuilder()
                // 最多缓存 1000 个键，防止内存无限增长
                .maximumSize(1000)
                // 写入 10 分钟后过期（与 Redis 配合，Redis 才是权威缓存）
                .expireAfterWrite(10, TimeUnit.MINUTES)
                .build();
    }
}
