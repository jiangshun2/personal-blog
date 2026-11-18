package com.blog.cache;

import com.blog.mapper.ArticleMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;

/**
 * 浏览量统计服务
 *
 * <p>设计思路（避免每次访问都 UPDATE 数据库）：
 * <ul>
 *   <li>访问文章详情时：Redis {@code INCR blog:view:{articleId}}，只做内存计数，速度极快；</li>
 *   <li>展示浏览量时：数据库 view_count + Redis 当前增量；</li>
 *   <li>定时任务（每 60 秒）：把 Redis 增量批量回写 MySQL，并删除对应计数键。</li>
 * </ul>
 *
 * <p>增量回写必须保证「读取 + 清零」的原子性，否则并发访问时可能丢计数，
 * 因此用 Lua 脚本把 GET 与 DEL 放在一次原子执行中完成；
 * 数据库侧用 {@code view_count = view_count + delta} 原子累加。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ViewCountService {

    /**
     * 「读取增量并清零」的原子 Lua 脚本
     *
     * <p>KEYS[1] = blog:view:{articleId}；返回读取到的增量（没有则返回 0）。
     */
    private static final DefaultRedisScript<Long> TAKE_VIEW_INCREMENT_SCRIPT = new DefaultRedisScript<>(
            "local value = redis.call('GET', KEYS[1])\n" +
            "if value then\n" +
            "    redis.call('DEL', KEYS[1])\n" +
            "end\n" +
            "return tonumber(value) or 0",
            Long.class);

    private final StringRedisTemplate stringRedisTemplate;
    private final ArticleMapper articleMapper;
    private final CacheService cacheService;

    /**
     * 浏览量 +1
     *
     * @param articleId 文章 ID
     * @return Redis 中当前累计的增量（用于展示：DB 值 + 该增量）
     */
    public long increment(Long articleId) {
        Long count = stringRedisTemplate.opsForValue().increment(CacheKeys.viewCount(articleId));
        return count == null ? 0L : count;
    }

    /**
     * 获取 Redis 中的浏览量增量（只读，不增加）
     *
     * @param articleId 文章 ID
     * @return 增量，Redis 中没有记录时返回 0
     */
    public long getIncrement(Long articleId) {
        String value = stringRedisTemplate.opsForValue().get(CacheKeys.viewCount(articleId));
        if (value == null) {
            return 0L;
        }
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            // 理论上不会发生；容错处理避免脏数据影响接口
            log.warn("浏览量增量格式异常：articleId={}，value={}", articleId, value);
            return 0L;
        }
    }

    /** 删除某篇文章的浏览量增量键（文章被删除时调用，避免定时任务继续回写） */
    public void clear(Long articleId) {
        stringRedisTemplate.delete(CacheKeys.viewCount(articleId));
    }

    /**
     * 定时任务：每 60 秒把 Redis 中的浏览量增量批量回写 MySQL
     *
     * <p>固定延迟（fixedDelay）表示「上一次执行结束后再等 60 秒」，
     * 避免任务执行时间过长时任务堆叠。
     */
    @Scheduled(fixedDelay = 60_000L, initialDelay = 60_000L)
    public void flushViewCount() {
        Set<String> keys;
        try {
            keys = stringRedisTemplate.keys(CacheKeys.VIEW_COUNT + "*");
        } catch (Exception e) {
            log.error("浏览量回写：扫描 Redis 键失败", e);
            return;
        }
        if (keys == null || keys.isEmpty()) {
            return;
        }
        int flushedArticles = 0;
        long flushedViews = 0L;
        for (String key : keys) {
            try {
                // 原子读取并清零增量
                Long delta = stringRedisTemplate.execute(
                        TAKE_VIEW_INCREMENT_SCRIPT, List.of(key));
                if (delta == null || delta <= 0) {
                    continue;
                }
                Long articleId = parseArticleId(key);
                if (articleId == null) {
                    continue;
                }
                // 数据库原子累加
                int rows = articleMapper.increaseViewCount(articleId, delta);
                if (rows > 0) {
                    // 回写后详情缓存里的 viewCount 已过期，精准删除让下次读取重新回源
                    cacheService.evict(CacheKeys.articleDetail(articleId));
                    flushedArticles++;
                    flushedViews += delta;
                }
            } catch (Exception e) {
                // 单个键失败不影响其它文章
                log.error("浏览量回写失败：key={}", key, e);
            }
        }
        if (flushedArticles > 0) {
            log.info("浏览量回写完成：{} 篇文章，共 {} 次浏览", flushedArticles, flushedViews);
        }
    }

    /** 从 blog:view:{articleId} 中解析出文章 ID */
    private Long parseArticleId(String key) {
        try {
            return Long.valueOf(key.substring(CacheKeys.VIEW_COUNT.length()));
        } catch (Exception e) {
            log.warn("无法从键中解析文章 ID：{}", key);
            return null;
        }
    }
}
