package com.blog.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * JWT 工具类
 *
 * <p>负责令牌的生成与解析：
 * <ul>
 *   <li>生成：把用户 id 放进 subject，用户名放进自定义 claim</li>
 *   <li>解析：校验签名与过期时间，返回 Claims</li>
 * </ul>
 */
@Component
@RequiredArgsConstructor
public class JwtUtil {

    private final JwtProperties jwtProperties;

    /** 由配置密钥生成 HMAC-SHA 密钥对象（可复用） */
    private SecretKey getSecretKey() {
        return Keys.hmacShaKeyFor(jwtProperties.getSecret().getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 生成 JWT
     *
     * @param userId   用户 id
     * @param username 用户名
     * @return 令牌字符串
     */
    public String generateToken(Long userId, String username) {
        Date now = new Date();
        Date expireAt = new Date(now.getTime() + jwtProperties.getExpireHours() * 60L * 60L * 1000L);
        return Jwts.builder()
                .subject(String.valueOf(userId))   // 主题：用户 id
                .claim("username", username)        // 自定义声明：用户名
                .issuedAt(now)                      // 签发时间
                .expiration(expireAt)               // 过期时间
                .signWith(getSecretKey())           // 签名
                .compact();
    }

    /**
     * 解析并校验令牌
     *
     * @param token 令牌
     * @return Claims（载荷）；签名错误或已过期会抛异常
     */
    public Claims parseToken(String token) {
        return Jwts.parser()
                .verifyWith(getSecretKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /** 从令牌中取用户 id */
    public Long getUserId(String token) {
        return Long.valueOf(parseToken(token).getSubject());
    }

    /** 从令牌中取用户名 */
    public String getUsername(String token) {
        return parseToken(token).get("username", String.class);
    }
}
