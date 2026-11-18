package com.blog.vo;

import lombok.Data;

/**
 * 用户信息（返回给前端的精简数据，不含密码）
 */
@Data
public class UserInfoVO {

    private Long id;
    private String username;
    private String nickname;
    private String avatar;
    private String email;
}
