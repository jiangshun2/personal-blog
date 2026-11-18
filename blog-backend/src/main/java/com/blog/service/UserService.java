package com.blog.service;

import com.blog.dto.LoginRequest;
import com.blog.vo.LoginVO;
import com.blog.vo.UserInfoVO;

/**
 * 用户业务接口
 */
public interface UserService {

    /**
     * 登录：校验用户名密码，成功后签发 JWT
     *
     * @param request 登录参数
     * @return 令牌 + 用户信息
     */
    LoginVO login(LoginRequest request);

    /**
     * 根据用户 id 获取用户信息
     *
     * @param userId 用户 id
     * @return 用户信息（不含密码）
     */
    UserInfoVO getUserInfo(Long userId);
}
