package com.blog.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 游客发表评论请求体
 *
 * <p>对应接口：POST /api/comment
 */
@Data
public class CommentCreateRequest {

    /** 所属文章 ID */
    @NotNull(message = "文章 ID 不能为空")
    private Long articleId;

    /** 评论内容 */
    @NotBlank(message = "评论内容不能为空")
    @Size(max = 1000, message = "评论内容不能超过 1000 个字符")
    private String content;

    /** 游客昵称（可选） */
    @Size(max = 50, message = "昵称不能超过 50 个字符")
    private String nickname;

    /** 游客邮箱（可选） */
    @Email(message = "邮箱格式不正确")
    @Size(max = 100, message = "邮箱不能超过 100 个字符")
    private String email;

    /** 父评论 ID（回复时传，一级评论不传） */
    private Long parentId;
}
