package com.blog.common.util;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.util.StringUtils;

/**
 * 获取客户端 IP 工具类
 *
 * <p>限流按 IP 维度统计，但经过 Nginx 等反向代理后 request.getRemoteAddr()
 * 拿到的是代理服务器 IP，因此优先读取 X-Forwarded-For、X-Real-IP 等请求头。
 * X-Forwarded-For 的格式是「客户端IP, 代理1IP, 代理2IP」，取第一个即真实客户端 IP。
 */
public final class IpUtil {

    /** 可能需要读取的代理请求头，按优先级从上到下 */
    private static final String[] IP_HEADERS = {
            "X-Forwarded-For",
            "X-Real-IP",
            "Proxy-Client-IP",
            "WL-Proxy-Client-IP",
            "HTTP_CLIENT_IP",
            "HTTP_X_FORWARDED_FOR"
    };

    private IpUtil() {
    }

    /**
     * 获取客户端 IP
     *
     * @param request HTTP 请求
     * @return 客户端 IP，取不到时回退为 remoteAddr
     */
    public static String getClientIp(HttpServletRequest request) {
        for (String header : IP_HEADERS) {
            String ip = request.getHeader(header);
            if (StringUtils.hasText(ip) && !"unknown".equalsIgnoreCase(ip)) {
                // X-Forwarded-For 可能是多个 IP，取第一个
                int commaIndex = ip.indexOf(',');
                return commaIndex > 0 ? ip.substring(0, commaIndex).trim() : ip.trim();
            }
        }
        return request.getRemoteAddr();
    }
}
