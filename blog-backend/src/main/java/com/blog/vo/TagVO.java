package com.blog.vo;

import lombok.Data;

/**
 * 标签信息
 */
@Data
public class TagVO {

    private Long id;
    private String name;
    /** 该标签下已发布文章数量 */
    private Long articleCount;
}
