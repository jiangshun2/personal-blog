package com.blog.cache;

import com.blog.common.ResultCode;
import com.blog.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Redis Lua 限流服务（固定窗口计数限流）
 *
 * <p>为什么用 Lua？
 * 「读取计数 → 判断是否超限 → 计数 +1 → 首次设置过期时间」这几步如果拆成多次 Redis 命令，
 * 并发请求下会出现竞态（比如两个请求同时读到 4，都以为还能访问）。
 * 把整个判断逻辑写进 Lua 脚本，Redis 单线程执行脚本时是原子的，不会被其他命令插入，
 * 因此能精确限制每个 IP 在窗口内的请求次数。
 *
 * <p>固定窗口算法：key 第一次被访问时设置 60 秒过期，
 * 窗口内 INCR 累加，超过阈值直接拒绝；窗口自然过期后计数归零。
 *
 * <p>键设计：blog:ratelimit:{ip}:{api}
 * <ul>
 *   <li>登录接口：每 IP 每分钟 5 次（防暴力破解）</li>
 *   <li>发评论接口：每 IP 每分钟 10 次（防刷）</li>
 * </ul>
 *
 * <p>注意：脚本执行使用 StringRedisTemplate，因为限流脚本的 ARGV 必须是普通数字字符串；
 * 如果用配置了 JSON 序列化的 RedisTemplate，参数会被序列化成带引号的 JSON，Lua 无法 tonumber。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RateLimitService {

    /** 登录接口限流阈值：每 IP 每分钟 5 次 */
    public static final int LOGIN_LIMIT_PER_MINUTE = 5;

    /** 发评论接口限流阈值：每 IP 每分钟 10 次 */
    public static final int COMMENT_LIMIT_PER_MINUTE = 10;

    /** 限流窗口大小（秒）：固定 1 分钟窗口 */
    private static final int WINDOW_SECONDS = 60;

    /**
     * 原子限流脚本
     *
     * <p>KEYS[1] = 限流键；ARGV[1] = 窗口秒数；ARGV[2] = 窗口内最大请求数。
     * 返回 1 表示放行，0 表示超限。
     */
    private static final DefaultRedisScript<Long> RATE_LIMIT_SCRIPT = new DefaultRedisScript<>(
            "local current = redis.call('INCR', KEYS[1])\n" +
            "if current == 1 then\n" +
            "    redis.call('EXPIRE', KEYS[1], ARGV[1])\n" +
            "end\n" +
            "if current > tonumber(ARGV[2]) then\n" +
            "    return 0\n" +
            "end\n" +
            "return 1",
            Long.class);

    private final StringRedisTemplate stringRedisTemplate;

    /**
     * 通用限流校验，超限抛出 4290 业务异常
     *
     * @param api             接口标识（login / comment），用于拼限流键
     * @param ip              客户端 IP
     * @param limitPerMinute  每分钟允许的最大次数
     */
    public void checkLimit(String api, String ip, int limitPerMinute) {
        String key = CacheKeys.rateLimit(ip, api);
        Long allowed = stringRedisTemplate.execute(
                RATE_LIMIT_SCRIPT,
                List.of(key),
                String.valueOf(WINDOW_SECONDS),
                String.valueOf(limitPerMinute));
        if (allowed == null || allowed == 0L) {
            log.warn("触发限流：api={}，ip={}，限制={} 次/分钟", api, ip, limitPerMinute);
            throw new BusinessException(ResultCode.RATE_LIMITED);
        }
    }

    /** 登录接口限流：每 IP 每分钟 5 次 */
    public void checkLoginLimit(String ip) {
        checkLimit("login", ip, LOGIN_LIMIT_PER_MINUTE);
    }

    /** 发评论接口限流：每 IP 每分钟 10 次 */
    public void checkCommentLimit(String ip) {
        checkLimit("comment", ip, COMMENT_LIMIT_PER_MINUTE);
    }
}
