package com.blog.vo;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * 后台文章详情
 *
 * <p>在列表项字段基础上额外返回正文 content 与标签 ID 列表 tagIds，
 * 供编辑页回填表单。
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class ArticleAdminDetailVO extends ArticleAdminVO {

    /** 正文（Markdown） */
    private String content;

    /** 标签 ID 列表（编辑页回填用） */
    private List<Long> tagIds;
}
