package com.blog.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 评论信息
 *
 * <p>前台评论列表按「一级评论 + 二级回复」组织：children 存放当前评论的回复。
 */
@Data
public class CommentVO {

    private Long id;
    private Long articleId;
    private Long userId;
    private Long parentId;
    private String content;
    private String nickname;
    private String email;
    /** 状态：0=隐藏，1=显示 */
    private Integer status;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
    /** 子评论（二级回复） */
    private List<CommentVO> children;
}
