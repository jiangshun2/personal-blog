package com.blog.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 文章置顶 / 取消置顶请求体
 *
 * <p>对应接口：PUT /api/admin/article/{id}/top
 */
@Data
public class ArticleTopRequest {

    /** true=置顶，false=取消置顶 */
    @NotNull(message = "isTop 不能为空")
    private Boolean isTop;
}
