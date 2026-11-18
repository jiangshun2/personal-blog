package com.blog.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 评论实体（对应表 blog_comment）
 *
 * <p>parentId 为 NULL 表示一级评论，否则是二级回复。
 * 表中没有 update_time，只有 create_time。
 */
@Data
@TableName("blog_comment")
public class Comment {

    /** 主键，数据库自增 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 所属文章 ID */
    private Long articleId;

    /** 评论用户 ID（游客评论为 NULL） */
    private Long userId;

    /** 父评论 ID（NULL 表示一级评论） */
    private Long parentId;

    /** 评论内容 */
    private String content;

    /** 游客昵称 */
    private String nickname;

    /** 游客邮箱 */
    private String email;

    /** 状态：0=隐藏，1=显示 */
    private Integer status;

    /** 创建时间（插入时自动填充） */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /** 逻辑删除：0=正常，1=已删除 */
    @TableLogic
    private Integer deleted;
}
