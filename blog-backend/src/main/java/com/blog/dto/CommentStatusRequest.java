package com.blog.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 评论状态修改请求体（审核 / 隐藏）
 */
@Data
public class CommentStatusRequest {

    /** 状态：0=隐藏，1=显示 */
    @NotNull(message = "评论状态不能为空")
    @Min(value = 0, message = "评论状态只能是 0 或 1")
    @Max(value = 1, message = "评论状态只能是 0 或 1")
    private Integer status;
}
