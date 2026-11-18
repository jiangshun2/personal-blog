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
 * 文章实体（对应表 blog_article）
 *
 * <p>字段与 sql/schema.sql 一一对应：
 * <ul>
 *   <li>status：0=草稿，1=已发布</li>
 *   <li>isTop：0=普通，1=置顶</li>
 *   <li>viewCount：数据库中的浏览量，Redis 中的增量由定时任务回写</li>
 * </ul>
 */
@Data
@TableName("blog_article")
public class Article {

    /** 主键，数据库自增 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 标题 */
    private String title;

    /** 摘要 */
    private String summary;

    /** 正文（Markdown 格式） */
    private String content;

    /** 封面图地址 */
    private String cover;

    /** 作者 ID */
    private Long authorId;

    /** 分类 ID */
    private Long categoryId;

    /** 状态：0=草稿，1=已发布 */
    private Integer status;

    /** 浏览量（数据库累计值） */
    private Long viewCount;

    /** 是否置顶：0=否，1=是 */
    private Integer isTop;

    /** 创建时间（插入时自动填充） */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /** 更新时间（插入 / 更新时自动填充） */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    /** 逻辑删除：0=正常，1=已删除 */
    @TableLogic
    private Integer deleted;
}
