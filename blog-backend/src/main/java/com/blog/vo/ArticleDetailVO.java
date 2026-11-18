package com.blog.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 文章详情（前台）
 *
 * <p>viewCount 返回「数据库值 + Redis 浏览增量」；
 * 另外携带上一篇 / 下一篇的 id 与标题，方便详情页做上下篇跳转。
 */
@Data
public class ArticleDetailVO {

    private Long id;
    private String title;
    private String summary;
    private String content;
    private String cover;
    private Long categoryId;
    private String categoryName;
    /** 标签名称列表 */
    private List<String> tags;
    /** 浏览量（数据库值 + Redis 增量） */
    private Long viewCount;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    /** 上一篇（更早发布的文章）id，没有则为 null */
    private Long prevId;
    private String prevTitle;
    /** 下一篇（更晚发布的文章）id，没有则为 null */
    private Long nextId;
    private String nextTitle;
}
