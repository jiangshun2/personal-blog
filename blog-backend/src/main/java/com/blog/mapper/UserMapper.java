package com.blog.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.blog.entity.User;

/**
 * 用户 Mapper
 *
 * <p>继承 BaseMapper 后自带单表 CRUD：
 * selectById / selectOne / selectCount / insert / updateById / deleteById ...
 * 复杂 SQL 可在 resources/mapper/UserMapper.xml 中编写。
 */
public interface UserMapper extends BaseMapper<User> {
}
