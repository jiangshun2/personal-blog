package com.blog.common.exception;

import com.blog.common.ResultCode;
import lombok.Getter;

/**
 * 业务异常
 *
 * <p>业务代码中主动抛出该异常，由全局异常处理器统一转成 Result 返回，
 * 避免在 Controller 里写一堆 if-else 判断返回。
 */
@Getter
public class BusinessException extends RuntimeException {

    /** 业务状态码 */
    private final int code;

    public BusinessException(String message) {
        super(message);
        this.code = ResultCode.ERROR.getCode();
    }

    public BusinessException(int code, String message) {
        super(message);
        this.code = code;
    }

    public BusinessException(ResultCode resultCode) {
        super(resultCode.getMessage());
        this.code = resultCode.getCode();
    }
}
