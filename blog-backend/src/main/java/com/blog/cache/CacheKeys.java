package com.blog.cache;

/**
 * 缓存 / 限流 / 浏览量的键名与 TTL 常量
 *
 * <p>键名与过期时间严格遵循《API 接口文档》第三节「缓存约定」，
 * 统一收口到这里，避免各处手写字符串导致不一致。
 */
public final class CacheKeys {

    /** 文章详情：blog:article:{id} */
    public static final String ARTICLE_DETAIL = "blog:article:";

    /** 文章分页列表前缀：blog:article:page:{page}:{size}:{categoryId}:{tagId}:{keyword} */
    public static final String ARTICLE_PAGE_PREFIX = "blog:article:page:";

    /** 分类列表 */
    public static final String CATEGORY_LIST = "blog:category:list";

    /** 标签列表 */
    public static final String TAG_LIST = "blog:tag:list";

    /** 站点配置 */
    public static final String SITE_CONFIG = "blog:site:config";

    /** 浏览量增量：blog:view:{articleId}（不设过期时间） */
    public static final String VIEW_COUNT = "blog:view:";

    /** 限流：blog:ratelimit:{ip}:{api} */
    public static final String RATE_LIMIT = "blog:ratelimit:";

    /** 文章详情 TTL：30 分钟 */
    public static final long ARTICLE_DETAIL_TTL_SECONDS = 30 * 60L;

    /** 文章分页 TTL：5 分钟 */
    public static final long ARTICLE_PAGE_TTL_SECONDS = 5 * 60L;

    /** 分类列表 TTL：30 分钟 */
    public static final long CATEGORY_LIST_TTL_SECONDS = 30 * 60L;

    /** 标签列表 TTL：30 分钟 */
    public static final long TAG_LIST_TTL_SECONDS = 30 * 60L;

    /** 站点配置 TTL：1 小时 */
    public static final long SITE_CONFIG_TTL_SECONDS = 60 * 60L;

    /** 空值占位：缓存键中可选参数为空时用 all 占位，保证键可读且长度稳定 */
    public static final String NULL_PLACEHOLDER = "all";

    private CacheKeys() {
    }

    /** 文章详情缓存键 */
    public static String articleDetail(Long articleId) {
        return ARTICLE_DETAIL + articleId;
    }

    /**
     * 文章分页缓存键
     *
     * <p>格式：blog:article:page:{page}:{size}:{categoryId}:{tagId}:{keyword}
     * 可选参数为空时用 all 占位。
     */
    public static String articlePage(long page, long size, Long categoryId, Long tagId, String keyword) {
        String category = categoryId == null ? NULL_PLACEHOLDER : String.valueOf(categoryId);
        String tag = tagId == null ? NULL_PLACEHOLDER : String.valueOf(tagId);
        String kw = (keyword == null || keyword.isBlank()) ? NULL_PLACEHOLDER : keyword.trim();
        return ARTICLE_PAGE_PREFIX + page + ":" + size + ":" + category + ":" + tag + ":" + kw;
    }

    /** 浏览量计数键 */
    public static String viewCount(Long articleId) {
        return VIEW_COUNT + articleId;
    }

    /** 限流键 */
    public static String rateLimit(String ip, String api) {
        return RATE_LIMIT + ip + ":" + api;
    }
}
