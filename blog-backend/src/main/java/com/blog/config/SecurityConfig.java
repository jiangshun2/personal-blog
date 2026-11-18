package com.blog.config;

import com.blog.common.Result;
import com.blog.common.ResultCode;
import com.blog.security.JwtAuthenticationFilter;
import com.blog.security.JwtUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfigurationSource;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Spring Security 配置
 *
 * <p>核心思路：
 * <ul>
 *   <li>关闭 CSRF（前后端分离 + JWT 不需要）</li>
 *   <li>不创建 Session（无状态）</li>
 *   <li>登录接口与前台接口放行，/api/admin/** 与 /api/auth/me 需要登录</li>
 *   <li>在用户名密码过滤器之前加入 JWT 过滤器</li>
 * </ul>
 */
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtUtil jwtUtil;
    private final CorsConfigurationSource corsConfigurationSource;
    private final ObjectMapper objectMapper;

    /** 密码加密器：BCrypt（加盐哈希，不可逆） */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // 关闭 CSRF
                .csrf(AbstractHttpConfigurer::disable)
                // 跨域
                .cors(cors -> cors.configurationSource(corsConfigurationSource))
                // 不使用 Session
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                // 授权规则：从上到下匹配，先匹配到的生效
                .authorizeHttpRequests(auth -> auth
                        // 登录接口放行
                        .requestMatchers("/api/auth/login").permitAll()
                        // 当前用户信息需要登录
                        .requestMatchers("/api/auth/me").authenticated()
                        // 接口文档相关放行
                        .requestMatchers("/doc.html", "/webjars/**", "/v3/api-docs/**",
                                "/swagger-ui/**", "/swagger-resources/**", "/favicon.ico").permitAll()
                        // 前台公开接口放行
                        .requestMatchers("/api/article/**", "/api/category/**",
                                "/api/tag/**", "/api/comment/**", "/api/site/**").permitAll()
                        // 后台管理接口需要登录
                        .requestMatchers("/api/admin/**").authenticated()
                        // 其余放行
                        .anyRequest().permitAll())
                // 未登录时返回统一 JSON，而不是默认的登录页 / 错误页
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint((request, response, authException) -> writeUnauthorized(response)))
                // 把 JWT 过滤器放到用户名密码过滤器之前
                .addFilterBefore(new JwtAuthenticationFilter(jwtUtil), UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /** 输出 401 统一 JSON */
    private void writeUnauthorized(HttpServletResponse response) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        Result<Void> result = Result.fail(ResultCode.UNAUTHORIZED);
        response.getWriter().write(objectMapper.writeValueAsString(result));
    }
}
