package com.blog.vo;

import lombok.Data;

/**
 * 分类信息
 */
@Data
public class CategoryVO {

    private Long id;
    private String name;
    private String description;
    /** 排序值，越小越靠前 */
    private Integer sort;
    /** 该分类下已发布文章数量 */
    private Long articleCount;
}
