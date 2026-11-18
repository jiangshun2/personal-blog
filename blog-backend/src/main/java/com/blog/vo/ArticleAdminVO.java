package com.blog.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 后台文章列表项
 */
@Data
public class ArticleAdminVO {

    private Long id;
    private String title;
    private String summary;
    private String cover;
    private Long categoryId;
    private String categoryName;
    /** 标签名称列表 */
    private List<String> tags;
    /** 状态：0=草稿，1=已发布 */
    private Integer status;
    /** 浏览量（数据库值 + Redis 增量） */
    private Long viewCount;
    /** 是否置顶 */
    private Boolean isTop;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;
}
