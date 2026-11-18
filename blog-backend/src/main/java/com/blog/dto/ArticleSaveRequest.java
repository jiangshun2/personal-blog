package com.blog.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/**
 * 文章新增 / 修改请求体
 *
 * <p>对应接口：POST /api/admin/article、PUT /api/admin/article/{id}
 */
@Data
public class ArticleSaveRequest {

    /** 标题 */
    @NotBlank(message = "文章标题不能为空")
    @Size(max = 200, message = "文章标题不能超过 200 个字符")
    private String title;

    /** 摘要 */
    @Size(max = 500, message = "摘要不能超过 500 个字符")
    private String summary;

    /** 正文（Markdown） */
    private String content;

    /** 封面图地址 */
    @Size(max = 255, message = "封面地址不能超过 255 个字符")
    private String cover;

    /** 分类 ID（可为空，表示未分类） */
    private Long categoryId;

    /** 标签 ID 列表（保存时整体覆盖 blog_article_tag 关联） */
    private List<Long> tagIds;

    /** 状态：0=草稿，1=已发布；不传默认按已发布处理 */
    @Min(value = 0, message = "文章状态只能是 0 或 1")
    @Max(value = 1, message = "文章状态只能是 0 或 1")
    private Integer status = 1;

    /** 是否置顶 */
    private Boolean isTop = false;
}
