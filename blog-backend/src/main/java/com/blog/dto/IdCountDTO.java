package com.blog.dto;

import lombok.Data;

/**
 * 「ID -> 数量」的通用统计结果（Mapper 聚合查询使用）
 *
 * <p>用于分类 / 标签下文章数量的统计：
 * SQL 中把分组字段别名为 id、COUNT(*) 别名为 count_value，
 * MyBatis 开启下划线转驼峰后自动映射到 countValue。
 */
@Data
public class IdCountDTO {

    /** 分组 ID（如 categoryId、tagId） */
    private Long id;

    /** 该分组下的文章数量 */
    private Long countValue;
}
