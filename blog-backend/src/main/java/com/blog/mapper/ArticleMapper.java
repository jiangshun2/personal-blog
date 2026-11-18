package com.blog.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.blog.dto.IdCountDTO;
import com.blog.entity.Article;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * 文章 Mapper
 *
 * <p>继承 BaseMapper 获得单表 CRUD，另外补充几个聚合 / 原子更新 SQL：
 * <ul>
 *   <li>分类、标签下文章数统计（只统计已发布文章，与前台展示口径一致）</li>
 *   <li>仪表盘总浏览量</li>
 *   <li>浏览量增量回写（原子累加，避免并发覆盖）</li>
 * </ul>
 */
public interface ArticleMapper extends BaseMapper<Article> {

    /** 统计各分类下已发布文章数量 */
    @Select("SELECT category_id AS id, COUNT(*) AS count_value FROM blog_article " +
            "WHERE deleted = 0 AND status = 1 AND category_id IS NOT NULL GROUP BY category_id")
    List<IdCountDTO> countArticleByCategory();

    /** 统计各标签下已发布文章数量（关联表 + 文章表） */
    @Select("SELECT at.tag_id AS id, COUNT(*) AS count_value FROM blog_article_tag at " +
            "INNER JOIN blog_article a ON a.id = at.article_id " +
            "WHERE a.deleted = 0 AND a.status = 1 GROUP BY at.tag_id")
    List<IdCountDTO> countArticleByTag();

    /** 仪表盘：所有文章浏览量总和（表为空时返回 0） */
    @Select("SELECT COALESCE(SUM(view_count), 0) FROM blog_article WHERE deleted = 0")
    Long sumViewCount();

    /**
     * 浏览量增量回写：直接在数据库里 view_count = view_count + delta，原子累加
     *
     * @param id    文章 ID
     * @param delta 本次回写的增量
     * @return 受影响行数
     */
    @Update("UPDATE blog_article SET view_count = view_count + #{delta} WHERE id = #{id} AND deleted = 0")
    int increaseViewCount(@Param("id") Long id, @Param("delta") long delta);
}
