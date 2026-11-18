package com.blog.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 分类新增 / 修改请求体
 */
@Data
public class CategorySaveRequest {

    /** 分类名称 */
    @NotBlank(message = "分类名称不能为空")
    @Size(max = 50, message = "分类名称不能超过 50 个字符")
    private String name;

    /** 分类描述 */
    @Size(max = 255, message = "分类描述不能超过 255 个字符")
    private String description;

    /** 排序值，越小越靠前 */
    private Integer sort = 0;
}
