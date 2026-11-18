package com.blog.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 文章-标签关联实体（对应表 blog_article_tag）
 *
 * <p>注意：这是纯关联表，没有 deleted / create_time / update_time 字段，
 * 删除时直接物理删除记录（MyBatis-Plus 不会追加逻辑删除条件）。
 */
@Data
@TableName("blog_article_tag")
public class ArticleTag {

    /** 主键，数据库自增 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 文章 ID */
    private Long articleId;

    /** 标签 ID */
    private Long tagId;
}
