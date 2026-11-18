package com.blog.cache;

import com.github.benmanes.caffeine.cache.Cache;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Set;
import java.util.function.Supplier;

/**
 * 多级缓存服务（Caffeine 一级 + Redis 二级）
 *
 * <p>本项目缓存的重点在 M5，读路径采用 Cache-Aside 旁路缓存模式：
 * <pre>
 * 请求 → Caffeine（一级，进程内，最快）
 *          │ 未命中
 *          ▼
 *        Redis（二级，分布式）
 *          │ 未命中
 *          ▼
 *        MySQL 查询 → 回填 Redis（带 TTL）→ 回填 Caffeine → 返回
 * </pre>
 *
 * <p>写路径（一致性）：写操作成功后由业务层调用 {@link #evict(String)}
 * 或 {@link #evictByPrefix(String)} 精准删除对应缓存，
 * 下次读取自动回源重建，避免出现「改了文章但前台仍是旧内容」。
 *
 * <p>为什么 Caffeine 里存的是对象引用、而不是再拷贝一份？
 * 因为读取后业务层只读取字段、不会修改缓存对象（浏览量等需要变化的字段
 * 会在业务层复制 VO 后再修改），这样既省内存又避免重复序列化。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CacheService {

    /** 一级缓存：进程内 Caffeine */
    private final Cache<String, Object> localCache;

    /** 二级缓存：Redis（value 使用 JSON 序列化，见 RedisConfig） */
    private final RedisTemplate<String, Object> redisTemplate;

    /**
     * 读穿缓存：一级 → 二级 → 回源加载，并依次回填
     *
     * @param key        缓存键（见 {@link CacheKeys}）
     * @param ttlSeconds Redis 过期时间（秒），一级缓存由 CaffeineConfig 统一控制
     * @param loader     缓存未命中时的回源逻辑（通常是查 MySQL）
     * @param <T>        返回类型
     * @return 缓存 / 数据库中的对象；loader 返回 null 时不缓存 null
     */
    @SuppressWarnings("unchecked")
    public <T> T getOrLoad(String key, long ttlSeconds, Supplier<T> loader) {
        // 1. 先查一级缓存 Caffeine
        Object localValue = localCache.getIfPresent(key);
        if (localValue != null) {
            log.debug("一级缓存命中：{}", key);
            return (T) localValue;
        }

        // 2. 一级未命中，查二级缓存 Redis
        Object redisValue = redisTemplate.opsForValue().get(key);
        if (redisValue != null) {
            log.debug("二级缓存命中：{}", key);
            // 回填一级缓存，下次同进程请求可直接命中
            localCache.put(key, redisValue);
            return (T) redisValue;
        }

        // 3. 两级都未命中，回源查数据库
        log.debug("缓存未命中，回源加载：{}", key);
        T value = loader.get();
        if (value != null) {
            // 回填二级缓存（带 TTL）
            redisTemplate.opsForValue().set(key, value, Duration.ofSeconds(ttlSeconds));
            // 回填一级缓存
            localCache.put(key, value);
        }
        return value;
    }

    /**
     * 精准失效单个键：同时删除 Redis 与 Caffeine 中的缓存
     *
     * @param key 缓存键
     */
    public void evict(String key) {
        localCache.invalidate(key);
        redisTemplate.delete(key);
        log.debug("缓存失效：{}", key);
    }

    /**
     * 按前缀批量失效（文章分页列表、文章详情使用）
     *
     * <p>Redis 没有原生的前缀删除命令，这里按契约要求使用 keys 扫描后批量删除；
     * Caffeine 则直接遍历 keySet 删除匹配前缀的本地缓存。
     * 博客数据量小，keys 可以接受；生产环境数据量极大时可改用 SCAN 游标。
     *
     * @param prefix 键前缀，例如 blog:article:page:
     */
    public void evictByPrefix(String prefix) {
        // 1. 删除 Redis 中所有以 prefix 开头的键
        Set<String> keys = redisTemplate.keys(prefix + "*");
        if (keys != null && !keys.isEmpty()) {
            redisTemplate.delete(keys);
            log.debug("按前缀失效 Redis 缓存：{}，共 {} 个键", prefix, keys.size());
        }
        // 2. 删除 Caffeine 中所有以 prefix 开头的键
        localCache.asMap().keySet().removeIf(key -> key.startsWith(prefix));
    }
}
