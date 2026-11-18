package com.blog.common;

import lombok.Getter;

/**
 * 统一响应状态码
 *
 * <p>约定：
 * <ul>
 *   <li>0：成功</li>
 *   <li>4xxx：客户端错误（参数、登录、权限等）</li>
 *   <li>5xxx：服务端错误</li>
 * </ul>
 */
@Getter
public enum ResultCode {

    /** 成功 */
    SUCCESS(0, "成功"),
    /** 参数校验失败 */
    PARAM_ERROR(4000, "参数错误"),
    /** 用户名或密码错误 */
    LOGIN_ERROR(4001, "用户名或密码错误"),
    /** 未登录或令牌过期 */
    UNAUTHORIZED(4010, "未登录或登录已过期"),
    /** 无权限 */
    FORBIDDEN(4030, "没有权限"),
    /** 资源不存在 */
    NOT_FOUND(4040, "资源不存在"),
    /** 请求过于频繁（限流） */
    RATE_LIMITED(4290, "请求过于频繁，请稍后再试"),
    /** 系统异常 */
    ERROR(5000, "系统异常");

    /** 业务状态码 */
    private final int code;
    /** 提示信息 */
    private final String message;

    ResultCode(int code, String message) {
        this.code = code;
        this.message = message;
    }
}
