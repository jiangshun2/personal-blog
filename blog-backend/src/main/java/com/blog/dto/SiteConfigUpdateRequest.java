package com.blog.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 站点设置更新请求体
 *
 * <p>契约中的请求体字段是下划线风格（site_name 等），
 * 而 Java 字段用驼峰，因此用 {@code @JsonProperty} 建立映射。
 */
@Data
public class SiteConfigUpdateRequest {

    /** 站点名称 */
    @JsonProperty("site_name")
    @Size(max = 1000, message = "站点名称不能超过 1000 个字符")
    private String siteName;

    /** 站点描述 */
    @JsonProperty("site_description")
    @Size(max = 1000, message = "站点描述不能超过 1000 个字符")
    private String siteDescription;

    /** 站长头像地址 */
    @JsonProperty("site_avatar")
    @Size(max = 1000, message = "头像地址不能超过 1000 个字符")
    private String siteAvatar;

    /** GitHub 主页 */
    @JsonProperty("github")
    @Size(max = 1000, message = "GitHub 地址不能超过 1000 个字符")
    private String github;

    /** 联系邮箱 */
    @JsonProperty("email")
    @Size(max = 1000, message = "邮箱不能超过 1000 个字符")
    private String email;
}
