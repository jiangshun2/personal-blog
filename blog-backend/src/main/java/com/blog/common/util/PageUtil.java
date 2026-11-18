package com.blog.common.util;

/**
 * 分页参数规范化工具
 *
 * <p>防止前端传入 page=0、size=10000 之类的非法值导致分页异常或一次性查太多数据：
 * page 最小为 1，size 限制在 1~100 之间，缺省为 10。
 */
public final class PageUtil {

    /** 默认每页条数 */
    private static final long DEFAULT_SIZE = 10L;

    /** 每页最大条数 */
    private static final long MAX_SIZE = 100L;

    private PageUtil() {
    }

    /** 页码最小为 1 */
    public static long normalizePage(long page) {
        return Math.max(page, 1);
    }

    /** 每页条数限制在 1~100，缺省 10 */
    public static long normalizeSize(long size) {
        if (size < 1) {
            return DEFAULT_SIZE;
        }
        return Math.min(size, MAX_SIZE);
    }
}
