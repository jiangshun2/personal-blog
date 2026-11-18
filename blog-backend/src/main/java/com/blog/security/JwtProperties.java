package com.blog.security;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * JWT 配置项（对应 application.yml 中的 blog.jwt.*）
 */
@Data
@Component
@ConfigurationProperties(prefix = "blog.jwt")
public class JwtProperties {

    /** 签名密钥（HS256 要求至少 32 字节） */
    private String secret;

    /** 令牌有效期（小时） */
    private Integer expireHours = 24;
}
