package com.blog.vo;

import lombok.Data;

/**
 * 登录成功返回结果
 */
@Data
public class LoginVO {

    /** JWT 令牌，后续请求放在请求头 Authorization: Bearer &lt;token&gt; */
    private String token;

    /** 用户信息 */
    private UserInfoVO userInfo;
}
