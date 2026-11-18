package com.blog.vo;

import lombok.Data;

import java.util.List;

/**
 * 归档分组：按年份组织文章
 */
@Data
public class ArchiveVO {

    /** 年份 */
    private Integer year;

    /** 该年份下的文章（按创建时间倒序） */
    private List<ArchiveItemVO> articles;
}
