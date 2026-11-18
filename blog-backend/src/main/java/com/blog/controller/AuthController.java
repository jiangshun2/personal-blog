package com.blog.controller;

import com.blog.cache.RateLimitService;
import com.blog.common.Result;
import com.blog.common.util.IpUtil;
import com.blog.dto.LoginRequest;
import com.blog.service.UserService;
import com.blog.vo.LoginVO;
import com.blog.vo.UserInfoVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 认证接口
 */
@Tag(name = "认证管理", description = "登录、获取当前用户信息")
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;
    private final RateLimitService rateLimitService;

    /**
     * 登录
     *
     * <p>限流：同一 IP 每分钟最多 5 次，超限返回 code=4290，
     * 防止暴力破解密码（Lua 脚本原子计数，见 RateLimitService）。
     *
     * @param request     用户名 + 密码
     * @param httpRequest 用于获取客户端 IP
     * @return token 与用户信息
     */
    @Operation(summary = "登录", description = "限流：每 IP 每分钟 5 次")
    @PostMapping("/login")
    public Result<LoginVO> login(@Valid @RequestBody LoginRequest request,
                                 HttpServletRequest httpRequest) {
        // 先限流，再校验账号密码
        rateLimitService.checkLoginLimit(IpUtil.getClientIp(httpRequest));
        return Result.ok(userService.login(request));
    }

    /**
     * 获取当前登录用户信息
     *
     * <p>用户 id 由 JWT 过滤器解析后放入 SecurityContext，这里直接取出即可。
     */
    @Operation(summary = "获取当前用户信息")
    @GetMapping("/me")
    public Result<UserInfoVO> me() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        // principal 是 JWT 过滤器里放入的用户 id
        Long userId = (Long) authentication.getPrincipal();
        return Result.ok(userService.getUserInfo(userId));
    }
}
